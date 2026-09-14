# API thực tế trên `dev` (14/09/2026) — nguồn cho mobile

Đọc trực tiếp `@RestController`/DTO trong `wallet-core` trên branch `dev` (234/234 test pass),
KHÔNG dựa vào `overview.md` cũ (ghi "Epic DWS-2", thực tế đã qua Epic 6) hay 2 guide cũ
dws-98/dws-203 (đoán sai tên enum). Coi đây là nguồn sự thật duy nhất cho Phase 2 (nối API thật).

## 0. Quyết định đã chốt

- **Dùng prefix `/api`, không dùng `/api/v1`.** `TransactionController` và
  `AdminDashboardController` map đồng thời cả 2 prefix (hoàn toàn tương đương, cùng
  method/response) — chọn `/api` cho toàn bộ app mobile để nhất quán.
  `EXPO_PUBLIC_API_BASE_URL` phải trỏ tới `.../api` (đã bao gồm prefix), code gọi
  `apiClient.get('/wallets/me')` không lặp lại `/api`.
- **Toàn bộ response JSON đã nhất quán snake_case** kể từ commit thêm
  `@JsonNaming(SnakeCaseStrategy.class)` vào 4 DTO còn thiếu (`TransactionHistoryItemResponse`,
  `AdminDashboardOverviewResponse`, `AdminDashboardVolumeResponse`, `DailyVolumePoint` — record
  lồng bên trong `AdminDashboardVolumeResponse.series[]`, dễ bị bỏ sót vì `@JsonNaming` không tự
  lan xuống record con). FE dùng lớp transform toàn cục snake_case → camelCase ở
  `src/api/client.ts`, nên toàn bộ type dưới đây viết bằng **camelCase** (khớp sau transform).
- **`idempotency_key` không bắt buộc ở tầng validate** (không có `@NotNull`/`@NotBlank` trong
  `WalletAmountRequest`/`TransferRequest`) — thiếu key thì request vẫn 200 nhưng mất bảo vệ
  chống double-charge khi client tự retry. Mobile phải tự sinh UUID và **giữ nguyên key khi
  retry cùng 1 thao tác**, coi đây là quy ước bắt buộc về nghiệp vụ, không phải validation sẽ
  chặn hộ.

## 1. Danh sách endpoint (role USER — mobile không có quyền admin)

| Method | Path (`/api` + ...) | Auth | Request | Response (`data`) |
|---|---|---|---|---|
| POST | `/auth/login` | - | `{email, password}` | `AuthResponse` |
| POST | `/auth/register` | - | `{email, full_name, phoneNumber, password}` | `AuthResponse` (201) |
| POST | `/auth/refresh` | - | `{refreshToken}` | `AuthResponse` |
| POST | `/auth/logout` | - | `{refreshToken}` | `null` |
| POST | `/auth/mfa/verify` | - | `{email, otp}` | `AuthResponse` |
| POST | `/auth/forgot-password` | - | `{email}` | `null` |
| POST | `/auth/reset-password` | - | `{email, otp, new_password}` | `null` |
| POST | `/auth/verify-email` | - | `{user_id, otp}` | `null` |
| POST | `/auth/resend-verification` | - | `{user_id, email}` | `null` |
| GET | `/users/me` | JWT | - | `UserProfileResponse` |
| PATCH | `/users/me` | JWT | `{full_name, phone_number}` | `UserProfileResponse` |
| POST | `/users/me/change-password` | JWT | `{current_password, new_password}` | `null` |
| GET | `/users/me/sessions` | JWT | - | `UserSessionResponse[]` |
| DELETE | `/users/me/sessions/{sessionId}` | JWT | - | `null` |
| POST | `/users/me/mfa/enable` \| `/disable` | JWT | - | `null` |
| GET | `/wallets/me` | JWT | - | `WalletResponse` |
| GET | `/wallets/{id}` | JWT | - | `WalletResponse` (chỉ xem được ví CỦA CHÍNH MÌNH — `ACCESS_DENIED` nếu không phải chủ sở hữu) |
| GET | `/wallets/lookup` | JWT | query `identifier` (email hoặc SĐT, match chính xác `User.email`/`User.phoneNumber`) | `WalletLookupResponse{wallet_id, masked_name, status}` — **endpoint mới thêm 14/09** để màn Chuyển tiền lấy được `receiver_wallet_id` thật (trước đó KHÔNG có cách nào tra cứu ví người khác — `/wallets/{id}` chặn). Chỉ trả tên đã che (`maskName()` trong `WalletService`), không trả email/SĐT/số dư người khác. |
| POST | `/wallets/me/deposits` | JWT | `{amount, idempotency_key}` | `TransactionResponse` |
| POST | `/wallets/me/withdrawals` | JWT | `{amount, idempotency_key}` | `TransactionResponse` |
| POST | `/wallets/me/transfers` | JWT | `{receiver_wallet_id, amount, idempotency_key}` | `TransactionResponse` |
| GET | `/wallets/me/transactions` | JWT | - | `TransactionResponse[]` (không filter/phân trang) |
| GET | `/wallets/transactions/{id}/ledger` | JWT | - | `LedgerEntryResponse[]` |
| GET | `/transactions/history` | JWT | query `type,status,dateFrom,dateTo,minAmount,maxAmount` + `page,size` | `PageResponse<TransactionHistoryItemResponse>` |
| POST | `/transactions/{id}/confirm-otp` | JWT | `{otp}` | `TransactionResponse` |
| GET | `/notifications` | JWT | query `unreadOnly,page,size` | `Page<NotificationResponse>` (Spring `Page`, KHÔNG phải `PageResponse` — field `content/totalElements/...` giống nhau về tên nhưng đây là serialize trực tiếp từ `org.springframework.data.domain.Page`, không đi qua record `PageResponse.java`) |
| GET | `/notifications/unread-count` | JWT | - | `{unreadCount: number}` |
| PATCH | `/notifications/{publicId}/read` | JWT | - | `NotificationResponse` |
| PATCH | `/notifications/read-all` | JWT | - | `{updatedCount: number}` |

Request field ghi dạng `snake_case` ở trên là **JSON gửi lên thật** (backend đọc bằng
`@JsonProperty`, KHÔNG đi qua lớp transform nhận — transform chỉ áp cho response). Xem type
camelCase đầy đủ tại `src/types/api.ts`.

**Sửa lại số đếm cũ**: câu trả lời trước ghi "34 endpoint" — đếm tay lại từng method trong 8
controller ra **41** (9 Auth + 7 User + 7 Wallet + 2 Transaction + 4 Notification = 29 endpoint
role USER ở bảng trên, cộng 12 endpoint ADMIN ngoài phạm vi mobile: 8 AdminUser + 2
AdminFraudReview + 2 AdminDashboard). Số "34" trước đó sai do cộng tay nhầm, không lấy từ nguồn
nào khác — bài học: số tổng nên đếm lại bằng cách liệt kê hết method rồi cộng, không tự tin số
chốt ở cuối một câu trả lời dài.

**2 điểm sai lệch ý nghĩa field, chỉ thấy khi đọc code dựng DTO (không thấy nếu chỉ đọc tên
field trong `TransactionHistoryItemResponse.java`)** — đối chiếu `TransactionService.toResponse()`
(dòng dựng `TransactionHistoryItemResponse` cho `GET /transactions/history`):

- `balanceAfter` **luôn là `null`** — code truyền cứng `null` vào đúng vị trí này, không có logic
  tính số dư sau giao dịch. Đừng dùng field này để hiện "Số dư sau GD" trên UI Lịch sử.
- `description` thực chất được gán bằng `referenceNumber` của transaction (dạng
  `"TXN-2026..."`), **không phải** mô tả/ghi chú do người dùng nhập. Nối thẳng field này làm
  phụ đề dòng giao dịch sẽ hiện mã tham chiếu thay vì text mô tả như mock hiện tại
  (`historyMock.ts` đang dùng câu mô tả tiếng Việt) — cần map lại ở FE hoặc yêu cầu sửa backend,
  không nối thẳng và giả định đúng như tên field.

## 2. `TransactionStatus` — đủ 6 giá trị thật (không phải 3-4 giá trị mock cũ)

Nguồn: `TransactionStatus.java` (enum), tone/label map tại `src/utils/mapTransactionStatus.ts`.

| Giá trị enum | Khi nào xảy ra | Tông StatusBadge | Label hiện cho user |
|---|---|---|---|
| `PENDING` | Trạng thái khởi tạo entity, gần như không lộ ra ngoài (chuyển ngay sang `PROCESSING` trước khi lưu) | `warning` | "Đang xử lý" |
| `PROCESSING` | Giao dịch đã tạo, đang trong transaction DB trước khi qua fraud/OTP check | `warning` | "Đang xử lý" |
| `PENDING_REVIEW` | Fraud check trả `CHALLENGE` — mặc định amount ≥ **10.000.000đ** (`wallet.fraud.challenge-amount`). Check này chạy TRƯỚC OTP check nên rút/chuyển >10tr luôn dừng ở đây, không bao giờ tới bước OTP. | `warning` | "Chờ duyệt" |
| `PENDING_OTP_CONFIRMATION` | Không bị fraud challenge nhưng amount ≥ **5.000.000đ** (`wallet.otp.required-amount`) — gọi `POST /transactions/{id}/confirm-otp` | `warning` | "Chờ xác thực OTP" |
| `COMPLETED` | Thành công | `success` | "Thành công" |
| `FAILED` | Thất bại (vượt hạn mức ngày, fraud `BLOCK` ≥100tr, OTP sai quá số lần...) | `danger` | "Thất bại" |

**Không nhầm với `FraudReviewStatus`** (`NOT_REQUIRED/PENDING_REVIEW/REVIEWED/CLEARED`) — đây là
enum riêng của `FraudReviewResponse`, chỉ lộ qua endpoint ADMIN (`/api/admin/fraud-assessments`),
mobile (role USER) không gọi được. Trạng thái "chờ duyệt" hiển thị cho chính chủ tài khoản phải
lấy từ `TransactionResponse.status`/`TransactionHistoryItemResponse.status`.

`src/components/TransactionListItem.tsx` (dùng ở màn Lịch sử) hiện chỉ có 2 trạng thái hiển thị
cứng (`pendingApproval: boolean` → "CHỜ DUYỆT"/"THÀNH CÔNG") — **chưa vẽ được** `FAILED`,
`PENDING_OTP_CONFIRMATION`, `PENDING`/`PROCESSING`. Cần quyết định lại UI trước khi nối API thật
cho màn Lịch sử (`TransactionRow` ở Trang chủ thì đã đủ 6 trạng thái qua `StatusBadge` + label).

## 3. Lớp API client nền tảng (đã dựng — `src/api/`)

- `client.ts` — axios instance gắn JWT qua request interceptor, response interceptor tự gọi lại
  `POST /auth/refresh` khi 401 (gộp nhiều request 401 đồng thời vào 1 lần refresh), xoá token +
  gọi `sessionExpiredHandler` khi refresh cũng fail (đăng ký handler này từ AuthContext ở phase
  Auth — chưa nối vì AuthContext/navigation logout chưa dựng).
- `caseTransform.ts` — transform snake_case → camelCase áp 1 lần trong response interceptor.
  An toàn với toàn bộ response thật hiện có: `NotificationResponse.metadata` (map tự do) chỉ chứa
  key backend tự đặt cứng dạng snake_case (`transaction_id`, `reference_number`,
  `transaction_type` — xem `NotificationService.metadata()`), nên transform áp lên đúng ý định,
  không làm hỏng dữ liệu.
- `tokenStorage.ts` — wrap `expo-secure-store` cho access + refresh token.
- `types/api.ts` — type camelCase cho toàn bộ response ở mục 1 (trừ endpoint admin, ngoài phạm
  vi mobile).

## 4. Cập nhật 14/09 — Auth + Ví/Số dư + Lịch sử + Nạp/Rút/Chuyển tiền đã nối xong

Toàn bộ mục "chưa làm" cũ (Auth, Trang chủ, Lịch sử, Nạp/Rút/Chuyển, UI 6 trạng thái) đã xong và
test end-to-end với backend thật (không phải mock). Còn lại: Thông báo (`/notifications`) chưa
nối.

### 3 bug backend thật tìm được khi test live (không unit test nào bắt được, vì không chạy qua
Postgres thật với đúng ràng buộc cột/CHECK constraint)

1. **`transactions.status` quá hẹp cho `PENDING_OTP_CONFIRMATION`** — migration V6 thêm giá trị
   này (24 ký tự) vào CHECK constraint nhưng cột vẫn `varchar(20)`, khiến MỌI giao dịch rút/chuyển
   rơi vào trạng thái chờ OTP đều lỗi 500 "value too long". Đã sửa bằng
   `V7__widen_transactions_status_column.sql` (`varchar(30)`).
2. **`AuthResponse.user.phone_number` luôn `null`** — `AuthService` (register/login/refresh/
   verifyMfa) dùng nhầm constructor 4-tham số của `UserProfileResponse` (không có phone), dù
   `phoneNumber` được lưu đúng vào DB. `GET /users/me` vẫn trả đúng phone (dùng constructor
   5-tham số). **CHƯA SỬA** — mobile hiện không hiển thị SĐT ngay sau đăng nhập/đăng ký, phải
   gọi thêm `GET /users/me` nếu cần SĐT sớm.
3. Xem mục 0 + phần trên cho các phát hiện tuần trước (casing, idempotency, description=
   referenceNumber, balanceAfter luôn null).

### Giới hạn thật của `GET /transactions/history` filter (mục nối Lịch sử)

`type`/`status` filter chỉ nhận **1 giá trị enum duy nhất** (`LedgerEntrySpecifications` dùng
`equal`, không phải `IN`) — không lọc được theo chiều tiền (không có field `direction`), không
lọc được nhiều status cùng lúc. 4/6 filter chip ở mobile (Tiền vào/Tiền ra/Nạp-Rút/Chờ duyệt) lọc
phía **client** trên dữ liệu đã tải, chỉ riêng "Chuyển tiền"/"Tất cả" mới khớp filter server thật
— nhưng để đồng nhất hành vi, mobile hiện lọc client-side cho cả 6 chip (xem `HistoryScreen.tsx`).
