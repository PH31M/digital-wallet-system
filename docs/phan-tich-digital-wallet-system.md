# Phân tích dự án digital-wallet-system (wallet-core)

Ngày phân tích: 01/08/2026. Branch hiện tại: `DWS-70-audit-service-log`, đã đồng bộ với `origin`, nhưng **working tree đang có rất nhiều thay đổi chưa commit** — nghĩa là phần lớn hệ thống mô tả dưới đây là code bạn đang viết dở, chưa đưa lên PR.

## 1. Bức tranh tổng thể: đã làm gì, đang làm gì

Lịch sử git chỉ có 13 commit trên `main`, dừng ở mốc "DWS-70 audit service log" (24/7). Nhưng thư mục làm việc hiện có đầy đủ code cho: xác thực đa lớp (JWT + refresh rotation + MFA email OTP + quản lý session), toàn bộ nghiệp vụ ví (nạp/rút/chuyển tiền với sổ cái kép), chấm điểm gian lận theo luật, giới hạn hạn mức ngày, audit log, và một cụm API quản trị (admin). Tức là bạn đã tiến rất xa so với commit cuối cùng — phần này chưa được đưa vào Jira ticket/PR mới, có thể bạn đang làm ticket kế tiếp (quản lý user, session, MFA, fraud, ledger) nhưng chưa chia nhỏ ra commit.

Cụ thể theo trạng thái git:

**Đã commit (nằm trên nhánh, đã qua PR):** cấu trúc monorepo, Dockerfile/docker-compose/Makefile cho môi trường dev, schema V1 (users, wallets, transactions, fraud_assessments, ledger_entries, notifications, audit_logs), và một bản đầu của audit service (AuditLog, ApiResponse chuẩn hoá, GlobalExceptionHandler, ErrorCode).

**Đang làm, chưa commit (untracked/modified):** toàn bộ luồng đăng ký/đăng nhập có email OTP verify, quên/đổi mật khẩu, MFA qua OTP email, refresh token xoay vòng + phát hiện reuse, quản lý phiên đăng nhập (user_sessions, migration V3), rate limiting theo Redis, WalletService với nạp/rút/chuyển tiền có khoá pessimistic + idempotency key, FraudService chấm điểm theo ngưỡng số tiền, TransactionLimitService giới hạn tổng giao dịch/ngày, NotificationService, AdminUserController để admin đổi role/khoá user và xem audit log, cùng bộ test khá đầy đủ (2638 dòng test, 15 file).

Nói cách khác: bạn đã làm xong "lõi nghiệp vụ ví điện tử" ở mức production-grade khá tốt, chỉ còn thiếu bước dọn dẹp và commit theo từng ticket nhỏ (Jira DWS-xx) để đúng quy trình bạn đang theo (mỗi ticket một PR).

## 2. Kiến trúc tổng thể

Đây là một Spring Boot 3+ (dùng Jakarta Persistence, chuẩn bị cho Spring Boot 4) backend theo kiến trúc layered truyền thống, package gốc `com.digitalwallet`:

```
api/        -> REST layer: controller, DTO request/response
common/     -> response envelope dùng chung (ApiResponse, ErrorInfo), request metadata (requestId, ip, user-agent)
config/     -> Spring config: Security, JWT, Redis, Async, WebSocket, WebMvc, RequestId filter, TimeConfig
domain/     -> entity JPA, enum, repository (Spring Data JPA)
exception/  -> exception nghiệp vụ + GlobalExceptionHandler (advice) ánh xạ sang ErrorCode
security/   -> JWT filter, blacklist, rate-limit filter, CustomUserDetailsService, @CurrentUser resolver
service/    -> business logic (Auth, User, Wallet, Fraud, Audit, Notification, Otp, Email, TransactionLimit, AdminUser)
util/       -> ReferenceNumberGenerator, IdempotencyService
```

Hạ tầng: PostgreSQL (Flyway migration, `ddl-auto: validate` — nghĩa là schema chỉ được thay đổi qua migration, không cho Hibernate tự sinh, đây là thực hành đúng cho hệ thống tài chính), Redis (OTP, token blacklist, rate limit), SMTP mail (OTP email), JWT (thư viện `jjwt`), WebSocket (đã cấu hình nhưng chưa thấy nơi publish message — có thể dành cho thông báo real-time sau này). Có cả biến cấu hình MoMo (partner-code, access-key...) trong `application.yml` nhưng chưa thấy `MomoService` nào — đây là phần bạn có kế hoạch làm (tích hợp cổng thanh toán) nhưng chưa bắt đầu code.

## 3. Luồng Authentication — chi tiết từng bước

### 3.1 Đăng ký (`POST /api/auth/register`)

`AuthController.register` nhận `RegisterRequest`, gọi `AuthService.register` trong 1 transaction:

1. Chuẩn hoá email (`trim().toLowerCase()`), kiểm tra trùng bằng `existsByEmail` — ném `EmailAlreadyExistsException` nếu trùng.
2. Tạo `User` với `role = USER`, `isActive = true`, mật khẩu được hash bằng `BCryptPasswordEncoder(strength=12)`.
3. Tạo luôn một `Wallet` (VND, balance = 0) với trạng thái `PENDING_VERIFICATION` — ví chưa dùng được cho tới khi xác minh email.
4. Sinh access token + refresh token (`JwtTokenProvider`), lưu session (`UserSession`) gắn với `jti` của refresh token.
5. Ghi audit log `USER_REGISTERED`.
6. Gửi OTP đăng ký **sau khi transaction commit thành công** — đây là điểm hay: dùng `TransactionSynchronizationManager.registerSynchronization(afterCommit)` để đảm bảo không gửi email nếu transaction DB rollback (tránh gửi OTP cho user chưa thực sự tồn tại trong DB).
7. Trả về `AuthResponse` gồm access/refresh token + thông tin profile.

Điểm học được: tách "hành động DB" và "side-effect ngoài DB (gửi email)" bằng transaction synchronization là pattern chuẩn để tránh rò rỉ hiệu ứng phụ khi rollback.

### 3.2 Xác minh email (`POST /api/auth/verify-email`, `/resend-verification`)

OTP được sinh và lưu ở Redis thông qua `OtpService` (không lưu OTP thô — dùng SHA-256 hash trước khi lưu, TTL 10 phút). `EmailOtpService.sendRegistrationOtp` chạy `@Async` (không chặn luồng request). Khi verify: kiểm tra số lần thử sai (`hasExceededRegistrationAttempts`, tối đa 3 lần) trước khi gọi `verifyRegistrationOtp` — verify dùng một **Lua script chạy atomic trong Redis** (`VERIFY_AND_DELETE_SCRIPT`) để so sánh OTP và xoá key trong một bước, tránh race condition khi 2 request verify cùng lúc. Nếu đúng: `user.markAsVerified()` set `emailVerifiedAt`, đồng thời kích hoạt ví (`wallet.activate()` chuyển từ `PENDING_VERIFICATION` sang `ACTIVE`). Resend có giới hạn theo cả user (`MAX_RESEND_PER_HOUR=3`) và theo IP (`MAX_RESEND_PER_IP_PER_HOUR=10`) để chống spam.

### 3.3 Đăng nhập (`POST /api/auth/login`)

1. Tìm user theo email, nếu không có → `INVALID_CREDENTIALS` (không tiết lộ user có tồn tại hay không — chống user enumeration).
2. Kiểm tra `user.isCurrentlyLocked()` — nếu `lockedUntil` còn hiệu lực thì chặn ngay (`ACCOUNT_LOCKED`).
3. So khớp password bằng BCrypt. Sai thì `recordFailedLogin()` tăng `failedLoginAttempts`, tới khi đạt `MAX_FAILED_LOGIN_ATTEMPTS` thì khoá tài khoản `LOCK_DURATION_MINUTES` (khoá tạm, tự mở khi hết hạn) — đồng thời ghi audit `USER_LOGIN_FAILED`.
4. Đúng password: `recordSuccessfulLogin()` reset bộ đếm và set `lastLoginAt`.
5. Nếu user bật MFA (`mfaEnabled=true`): gửi OTP MFA qua email, trả `AuthResponse` với `mfaRequired=true` và **không phát token** — client phải gọi tiếp `/api/auth/mfa/verify`.
6. Nếu không bật MFA: phát access + refresh token, tạo session, ghi audit `USER_LOGIN_SUCCESS`.

### 3.4 MFA verify (`POST /api/auth/mfa/verify`)

Kiểm tra `hasExceededMfaAttempts` trước, verify OTP theo purpose `"mfa"`, nếu đúng mới phát token + tạo session — logic tương tự OTP đăng ký nhưng namespace Redis riêng.

### 3.5 Refresh token — cơ chế chống tái sử dụng (token reuse detection)

Đây là phần thiết kế bảo mật đáng chú ý nhất:

- Access token và refresh token đều là JWT ký HMAC (`HS256`), nhưng chỉ refresh token có `jti` (id duy nhất) và claim `tokenVersion`.
- Khi refresh: nếu refresh token đó **đã nằm trong blacklist Redis** (tức là đã được dùng để refresh 1 lần trước đó rồi) → hệ thống hiểu đây là dấu hiệu token bị đánh cắp và dùng lại (replay attack). Phản ứng: tăng `tokenVersion` của user (làm mọi token cũ, kể cả các token còn hiệu lực khác, bị vô hiệu vì so `tokenVersion` không khớp), ghi audit `SECURITY_ALERT`, ném `TOKEN_REUSE_DETECTED`.
- Nếu token hợp lệ và chưa bị dùng: so `tokenVersion` trong token với `tokenVersion` hiện tại của user trong DB — không khớp thì coi như hết hạn (đây chính là cơ chế "logout everywhere" — đổi mật khẩu hoặc admin đổi role/khoá user đều gọi `incrementTokenVersion()` khiến mọi refresh token cũ bị vô hiệu ngay cả khi chưa hết TTL).
- Sau khi xác thực hợp lệ: đưa token cũ vào blacklist (TTL = thời gian còn lại của token), phát access + refresh token mới, và **xoay vòng session** (`rotateSession`): tìm `UserSession` theo `jti` cũ, đánh dấu `revoke()`, tạo session mới. Đây là "refresh token rotation" chuẩn — mỗi refresh token chỉ dùng được đúng 1 lần.

### 3.6 Logout, quên/đổi mật khẩu, quản lý session

`logout`: idempotent (token hỏng/hết hạn vẫn trả thành công), blacklist cả refresh token lẫn access token hiện tại, revoke session tương ứng, ghi audit.

`forgot-password` / `reset-password`: giống luồng OTP, nhưng khi đổi mật khẩu thành công sẽ gọi `revokeAllSessions` — đăng xuất toàn bộ thiết bị đang đăng nhập, một biện pháp bảo mật đúng chuẩn.

`UserController` cho phép người dùng tự xem danh sách session đang hoạt động (`GET /me/sessions`) và tự thu hồi một session cụ thể (`DELETE /me/sessions/{id}`) — tức là bạn đã làm tính năng kiểu "quản lý thiết bị đăng nhập" giống các app ngân hàng/Google.

### 3.7 Lớp filter bảo vệ mọi request

`JwtAuthenticationFilter` (OncePerRequestFilter) chạy trước `UsernamePasswordAuthenticationFilter`: lấy Bearer token, kiểm tra hợp lệ + đúng loại "access" + **không nằm trong blacklist** + user chưa được authenticate ở bước nào khác, rồi mới set `SecurityContextHolder`. `SecurityConfig` bật CSRF off (hợp lý vì API thuần JWT, stateless), CORS whitelist origin, session policy STATELESS, và chỉ permit `/api/auth/**` và `/actuator/health`, còn lại đều yêu cầu authenticated. `RateLimitFilter` là một filter riêng dùng Redis `INCR` + `EXPIRE` theo cửa sổ thời gian cố định (fixed window counter), phân nhóm theo path: `/api/auth/**` giới hạn chặt hơn (20/phút mặc định), `/api/wallets/me/**` (POST) giới hạn riêng cho thao tác tiền (30/phút), còn lại dùng limit mặc định (120/phút).

## 4. Luồng Wallet — nạp/rút/chuyển tiền và sổ cái kép (double-entry ledger)

Đây là phần thể hiện tư duy hệ thống tài chính rõ nhất trong dự án.

### 4.1 Idempotency

Mọi thao tác tiền (`deposit`, `withdraw`, `transfer`) đều nhận `idempotencyKey` từ client. Trước khi làm gì, service tra `transactionRepository.findByIdempotencyKey` — nếu đã tồn tại giao dịch với key đó thì trả thẳng kết quả cũ, không xử lý lại. Đây là kỹ thuật bắt buộc trong hệ thống thanh toán: client có thể gọi lại API do mất mạng/timeout mà không sợ bị trừ tiền 2 lần. Cột `idempotency_key` trong DB có `unique constraint` — đảm bảo an toàn ngay cả khi có race condition ở tầng ứng dụng.

### 4.2 Khoá bi quan (pessimistic locking) chống race condition

`walletRepository.findByIdForUpdate` (dùng `SELECT ... FOR UPDATE`) khoá dòng ví trước khi đọc/ghi số dư — đảm bảo hai giao dịch cùng lúc trên một ví không thể đọc số dư cũ rồi cùng ghi đè (lost update). Với `transfer` (đụng tới 2 ví), bạn **sort ví theo UUID trước khi khoá theo thứ tự cố định** (`walletIds.stream().sorted()`), đây chính xác là kỹ thuật chuẩn để **tránh deadlock**: nếu không sort, giao dịch A khoá ví X trước Y trong khi giao dịch B khoá Y trước X cùng lúc sẽ gây deadlock kinh điển.

### 4.3 Trạng thái giao dịch và fraud check nằm giữa luồng

Thứ tự xử lý (lấy `deposit` làm ví dụ, các luồng khác tương tự):

1. Check idempotency.
2. Khoá ví, kiểm tra ví đang `ACTIVE` (không thì `WalletFrozenException`).
3. Tạo `Transaction` (status mặc định `PENDING`), gán `referenceNumber` (qua `ReferenceNumberGenerator`).
4. Kiểm tra hạn mức ngày (`assertWithinDailyLimit`) — nếu vượt, set `transaction.fail()`, lưu, ghi audit `TRANSACTION_FAILED`, gửi notification thất bại, rồi mới ném `DailyLimitExceededException`. Chú ý: transaction vẫn được lưu vào DB ở trạng thái `FAILED` (không rollback) nhờ `@Transactional(noRollbackFor = BusinessException.class)` — đây là chủ đích: bạn muốn giữ lại **lịch sử** mọi lần thử giao dịch, kể cả thất bại, phục vụ audit/fraud investigation sau này, thay vì để Spring rollback xoá sạch record.
5. Set status `PROCESSING`, lưu.
6. `enforceFraudDecision`: gọi `FraudService.assess`. Nếu `BLOCK` → set `FAILED`, ghi audit `FRAUD_BLOCKED`, gửi cảnh báo, ném lỗi (transaction giữ nguyên trong DB ở trạng thái FAILED). Nếu `CHALLENGE` → hiện tại chỉ ghi audit + gửi notification cảnh báo rồi **vẫn cho tiếp tục xử lý** (chưa có bước yêu cầu xác thực bổ sung thực sự — đây là điểm còn dang dở, xem mục 6).
7. Cộng/trừ số dư ví (`wallet.credit`/`wallet.debit`), lưu ví.
8. Ghi 2 dòng `LedgerEntry` — **đây là sổ cái kép**: mỗi giao dịch luôn sinh ra ít nhất 2 bút toán đối ứng (một bên debit, một bên credit), tổng debit luôn bằng tổng credit. Ví dụ nạp tiền: 1 dòng debit `CASH_ACCOUNT` (tiền vào hệ thống từ bên ngoài) + 1 dòng credit `USER_WALLET` (ví user tăng). Với `transfer`: debit ví người gửi + credit ví người nhận, không đụng `CASH_ACCOUNT` vì tiền không rời khỏi hệ thống. Ràng buộc `chk_ledger_entries_single_sided` ở DB (V1 migration) đảm bảo mỗi dòng ledger chỉ có 1 trong 2 cột debit/credit khác 0 — đúng chuẩn kế toán double-entry.
9. `transaction.complete()`, lưu, ghi audit hoàn tất, gửi notification thành công.

Với `transfer`, còn có bước validate người nhận không phải chính người gửi, và kiểm tra quyền sở hữu ví gửi (`ensureOwned`) trước khi khoá.

`getLedgerEntries` cho phép cả người gửi lẫn người nhận xem chi tiết bút toán của một giao dịch (kiểm tra `isParticipant`), phù hợp với việc đối soát/tra cứu minh bạch.

### 4.4 Chấm điểm gian lận (FraudService) — hiện tại còn đơn giản

Logic hiện tại chỉ là rule theo ngưỡng số tiền tuyệt đối: `amount >= challengeAmount` (10,000,000đ mặc định) → risk score 70, quyết định `CHALLENGE`; `amount >= blockAmount` (100,000,000đ) → risk score 100, `BLOCK`. Có sẵn field `aiAnomalyScore` và `modelVersion = "rules-v1"` trong entity `FraudAssessment` — cho thấy bạn đã thiết kế chỗ trống để sau này gắn model ML/AI thực sự, hiện tại mới là placeholder rule-based (đúng như tên "rules-v1").

### 4.5 Giới hạn hạn mức ngày (TransactionLimitService)

Tính tổng tiền đã di chuyển trong ngày (theo timezone cấu hình, mặc định UTC) bằng truy vấn `sumMoneyMovementForWallet` trong khoảng `[đầu ngày, đầu ngày mai)`, cộng thêm số tiền giao dịch hiện tại, so với `dailyLimit` (mặc định 50,000,000đ). Với `transfer`, hàm này được gọi cho **cả 2 ví** (sender và receiver) — nghĩa là ví nhận tiền cũng bị tính vào hạn mức nhận tiền trong ngày.

## 5. Audit log & Admin

`AuditService.log` khá đơn giản — ghi 1 dòng `AuditLog` (actor, loại actor USER/ADMIN/SYSTEM, action enum, loại+id resource bị tác động, IP, user-agent, requestId liên kết với request gốc). Được gọi rải khắp: đăng ký, đăng nhập thành công/thất bại, đổi mật khẩu, bật/tắt MFA, thu hồi session, mọi giao dịch tiền (kể cả thất bại/bị chặn gian lận), và các hành động admin (đổi role, khoá/mở user).

`AdminUserController` (`/api/admin/**`, bảo vệ bằng `@PreAuthorize("hasRole('ADMIN')")`) cho phép: liệt kê/tìm user theo email (phân trang), xem chi tiết 1 user, đổi role, khoá/mở user (`isActive`), và xem audit log toàn hệ thống hoặc theo user. Điểm hay: mỗi khi admin đổi role hoặc khoá user, hệ thống gọi `incrementTokenVersion()` + `revokeActiveByUserId` để **đăng xuất ngay lập tức** mọi phiên hiện có của user đó — tránh trường hợp user bị khoá quyền nhưng token cũ vẫn dùng được cho tới khi hết hạn.

## 6. Cơ sở dữ liệu — 3 migration Flyway

`V1__init_schema.sql`: toàn bộ schema gốc — `users`, `wallets`, `transactions`, `fraud_assessments`, `ledger_entries`, `notifications`, `audit_logs`, cùng index và các CHECK constraint chặt (role hợp lệ, status hợp lệ, balance không âm, amount dương, fraud score trong [0,100], ledger single-sided...). Việc đẩy validation nghiệp vụ xuống tận DB constraint (không chỉ ở code Java) là thực hành tốt cho hệ thống tài chính — chống được cả lỗi do code Java có bug lẫn thao tác tay trực tiếp vào DB.

`V2__add_user_token_version.sql`: thêm cột `token_version` — nền tảng cho cơ chế "logout everywhere" đã phân tích ở mục 3.5.

`V3__add_user_sessions_and_mfa.sql`: thêm cột `mfa_enabled` và bảng `user_sessions` — nền tảng cho MFA và quản lý phiên đăng nhập ở mục 3.6/3.7.

## 7. Kiểm thử (test)

Có 15 file test, tổng 2638 dòng — tập trung nặng nhất vào `AuthServiceTest` (633 dòng, bao phủ toàn bộ các nhánh: đăng ký trùng email, khoá tài khoản, token reuse, rotation, MFA...) và `WalletServiceTest` (304 dòng). Ngoài ra có test cho JWT provider/filter, token blacklist, rate limit filter, request-id filter, global exception handler, validator mật khẩu mạnh, fraud service, transaction limit, audit service, user service. Đây là mức độ test khá tốt cho một dự án học tập/side-project — đặc biệt phần security và luồng tiền được test kỹ, đúng là hai chỗ "không được phép sai" trong hệ thống ví điện tử.

## 8. Đánh giá & những điểm còn dang dở đáng chú ý để bạn học tiếp

Một số quan sát khách quan, không phải để chê mà để bạn biết mình đang đứng ở đâu:

Cơ chế `CHALLENGE` trong fraud hiện mới dừng ở "ghi log + gửi cảnh báo" chứ chưa thực sự chặn giao dịch lại để chờ xác thực bổ sung (ví dụ OTP xác nhận giao dịch) — giao dịch vẫn được xử lý tiếp ngay sau đó. Nếu mục tiêu là "review thủ công trước khi hoàn tất", cần thêm bước giữ giao dịch ở trạng thái chờ và một API cho admin duyệt (đã có sẵn field `reviewStatus`, `reviewedBy`, `reviewAction` trong bảng `fraud_assessments` — cho thấy bạn có dự tính làm nhưng chưa nối luồng).

Chưa thấy `TransactionController`/`TransactionService` độc lập — toàn bộ nghiệp vụ giao dịch hiện nằm trong `WalletService`, điều này ổn cho quy mô hiện tại nhưng đáng cân nhắc tách khi luồng phức tạp thêm (ví dụ khi làm reversal/hoàn tiền, đã có sẵn cột `related_transaction_id` chờ dùng cho tính năng này).

Có cấu hình MoMo trong `application.yml` nhưng chưa có code tích hợp cổng thanh toán tương ứng — rõ ràng là việc tiếp theo trong roadmap.

Về mặt quy trình: khối lượng công việc lớn (auth, wallet, fraud, audit, admin, tests — hàng chục file) đang nằm chung trong một working tree chưa commit. Nên cân nhắc chia nhỏ thành các commit/PR theo từng ticket Jira (ví dụ DWS-71 MFA, DWS-72 Wallet transfer, DWS-73 Fraud rules...) thay vì gộp hết, để giữ lịch sử git dễ review và dễ revert nếu cần — đúng tinh thần bạn đã làm ở các ticket trước (DWS-34, DWS-24...).

## 9. Tóm tắt luồng end-to-end (để hình dung tổng thể)

Người dùng đăng ký → nhận OTP qua email → xác minh → ví được kích hoạt → đăng nhập (có thể qua thêm bước MFA nếu bật) → nhận cặp access/refresh token, hệ thống lưu session → dùng access token gọi API ví (được `JwtAuthenticationFilter` xác thực trên mỗi request, đồng thời bị `RateLimitFilter` giới hạn tần suất) → thực hiện nạp/rút/chuyển tiền: khoá ví → kiểm tra hạn mức ngày → chấm điểm gian lận → cập nhật số dư → ghi 2 bút toán sổ cái đối ứng → hoàn tất giao dịch → ghi audit log + gửi notification. Song song, mọi hành động nhạy cảm (đăng nhập, đổi mật khẩu, giao dịch, hành động admin) đều để lại dấu vết trong `audit_logs`, và access token hết hạn sau 15 phút (mặc định) trong khi refresh token sống 7 ngày nhưng chỉ dùng được đúng 1 lần rồi tự động bị thay thế.
