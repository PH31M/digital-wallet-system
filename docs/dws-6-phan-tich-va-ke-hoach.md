> **Lưu ý về file này**: đây là bản kế hoạch làm việc (working plan) do Claude tổng hợp dựa trên phân tích code thực tế trong `wallet-core/` và cấu trúc Jira hiện có (đối chiếu với cách các epic DWS-2, DWS-3 đã được breakdown). File này bổ sung cho `docs/phan-tich-digital-wallet-system.md` (bản phân tích tổng thể ngày 01/08/2026), tập trung riêng vào epic **DWS-6 – Admin & Monitoring**.

# DWS-6 — Admin & Monitoring: Phân tích & Kế hoạch hoàn thành

**Epic:** DWS-6 — *"Admin dashboard, audit logs, user management"*
**Trạng thái Jira hiện tại:** To Do, **0 Story/Subtask con** (epic rỗng, chưa được breakdown trong Jira dù code đã có một phần).

## 1. Hiện trạng — đã làm gì, còn thiếu gì

Khác với các epic khác (DWS-2, DWS-3...) đã được chia nhỏ đầy đủ thành Story/Subtask trong Jira, **DWS-6 chưa hề được breakdown** — nhưng code thực tế trong `wallet-core` đã âm thầm triển khai một phần đáng kể của nó (giống pattern đã ghi nhận trong `phan-tich-digital-wallet-system.md`: code đi trước, Jira ticket theo sau).

### 1.1. Đã có sẵn trong code (đã commit, chạy được)

| Thành phần | File | Ghi chú |
|---|---|---|
| Quản lý user (admin) | `AdminUserController`, `AdminUserService` | List user (filter email, phân trang), xem chi tiết, đổi role, khoá/mở khoá (`isActive`). Mỗi hành động đều tăng `tokenVersion` + revoke session — đăng xuất user ngay lập tức. |
| Audit log — ghi | `AuditLog` entity, `AuditService.log()`, `AuditLogRepository` | Ghi nhận actor/action/resource/IP/user-agent/requestId. Được gọi rải khắp hệ thống (auth, transaction, admin action). |
| Audit log — đọc (cơ bản) | `AdminUserController.listAuditLogs` | Chỉ filter được theo `userId` (qua `findByResourceTypeAndResourceId("USER", userId, ...)`), hoặc lấy tất cả — **chưa filter theo action, actor, khoảng thời gian**. |
| Duyệt gian lận (admin) | `AdminFraudReviewController`, `AdminFraudReviewService`, `AdminFraudReviewServiceTest` | List assessment theo `reviewStatus`, admin approve/reject — đã có test service. |

### 1.2. Chưa có (gap thật, cần làm mới)

| Thành phần | Hiện trạng |
|---|---|
| **Admin Dashboard / Overview API** | Không tồn tại. Không có controller/service/DTO nào tên `Dashboard`. |
| **System Monitoring (health, metrics)** | `spring-boot-starter-actuator` **chưa có trong `pom.xml`**. `SecurityConfig` đã permit `/actuator/health` nhưng endpoint đó **chưa thực sự tồn tại** vì thiếu dependency — đây là một lỗ hổng cấu hình cần vá trước. Không có metrics (Micrometer/Prometheus). |
| **Audit log filter nâng cao** | Chưa filter theo `actionType`, `actorType`, khoảng thời gian `from`/`to`. Chưa có export. |
| **Test cho Admin User** | Không có `AdminUserServiceTest` hay controller test nào — đây là service có quyền cao (đổi role, khoá user) nhưng chưa được test. |
| **Test cho Admin Fraud Review Controller** | Có test cho service, chưa có test cho controller (kiểm tra `@PreAuthorize`, request/response). |
| **Quản lý session của user (góc nhìn admin)** | Admin chưa có API xem/thu hồi session của một user cụ thể (khác với API user tự quản lý session của chính mình đã có ở `UserController`). |
| **Đối chiếu Jira ↔ Git** | Phần code đã có (mục 1.1) được commit dồn trong các commit không rõ ràng ("DWS-70", "v3") — chưa gắn với ticket Jira tương ứng để dễ review/trace. |

## 2. Đề xuất breakdown (Story → Subtask) cho DWS-6

Áp dụng đúng pattern đang dùng trong project (Epic → Story → Subtask, tiếng Việt, subtask dạng hành động cụ thể).

### Story 1 — Quản lý người dùng cho Admin *(đã code phần lớn, cần hoàn thiện)*
1. *(Done — retroactive)* `AdminUserController` + `AdminUserService`: list/get/đổi role/khoá-mở user
2. Unit test `AdminUserService` (đổi role, khoá/mở, list, get, các case lỗi)
3. Controller test `AdminUserController` (chặn non-admin bằng `@PreAuthorize`, đúng format `ApiResponse`)
4. API cho admin xem danh sách session đang hoạt động của 1 user + thu hồi 1 session cụ thể
5. Whitelist field cho `Pageable.sort` (tránh client truyền `sort=` tuỳ ý gây lỗi hoặc property injection)

### Story 2 — Audit Log nâng cao cho Admin *(đã có bản cơ bản, cần mở rộng)*
1. *(Done — retroactive)* `AuditLog` entity + `AuditService.log()` + list cơ bản theo `userId`
2. Mở rộng `AuditLogRepository` bằng `Specification`: filter theo `actionType`, `actorType`, `resourceType`, khoảng thời gian `from`/`to`
3. API `GET /api/admin/audit-logs` với đầy đủ filter + sort mặc định theo `createdAt desc`
4. Unit/integration test cho query audit log nâng cao
5. *(Optional)* Export audit log ra CSV theo khoảng thời gian

### Story 3 — Admin Dashboard / System Overview API *(chưa làm)*
1. Thiết kế `AdminDashboardResponse` DTO: tổng số user, user mới (ngày/tuần), tổng giao dịch theo status, volume tiền theo ngày, số fraud đang `PENDING_REVIEW`, tỷ lệ giao dịch fail
2. Query aggregation trong `UserRepository`/`TransactionRepository`/`FraudAssessmentRepository` (count/sum có điều kiện)
3. `AdminDashboardService` tổng hợp dữ liệu
4. `AdminDashboardController`: `GET /api/admin/dashboard/overview`
5. Cache kết quả dashboard trong Redis (TTL ngắn 30–60s) để tránh query nặng lặp lại
6. Unit test `AdminDashboardService`

### Story 4 — System Monitoring: Health & Metrics *(chưa làm, thiếu cả dependency)*
1. Thêm `spring-boot-starter-actuator` vào `pom.xml`
2. Cấu hình `application.yml`: `management.endpoints.web.exposure.include`, `management.endpoint.health.show-details`
3. Bảo vệ `/actuator/**` (trừ `/actuator/health`) bằng role `ADMIN` trong `SecurityConfig` — hiện tại chỉ mới permitAll `/actuator/health`
4. Custom `HealthIndicator` cho Redis nếu Spring Boot không tự thêm khi dùng `spring-boot-starter-data-redis`
5. *(Optional)* Thêm `micrometer-registry-prometheus`, expose `/actuator/prometheus`, bổ sung service Prometheus/Grafana vào `docker-compose.yml`
6. Test: `/actuator/health` trả `200 UP` khi DB + Redis khả dụng

### Story 5 — Admin duyệt Fraud Assessment *(đã code, cần bổ sung test)*
1. *(Done — retroactive)* `AdminFraudReviewController` + `AdminFraudReviewService` + service test
2. Controller test cho `AdminFraudReviewController` (permission + luồng review)

### Story 6 — Dọn dẹp quy trình Git/Jira cho DWS-6
1. Tách code admin đã viết (mục 1.1) thành các PR riêng tương ứng từng Subtask ở trên, để giữ lịch sử git rõ ràng
2. Cập nhật Jira: đánh Done cho Subtask tương ứng phần đã có sẵn, gắn link PR/commit

## 3. Thứ tự thực hiện đề xuất

1. **Story 4 (Monitoring)** trước — vì đang có lỗ hổng cấu hình thật (`/actuator/health` được permit nhưng không tồn tại), nên vá sớm.
2. **Story 1 & 2** — bổ sung test cho phần đã code (rủi ro cao nhất vì đây là quyền admin, thiếu test là gap bảo mật).
3. **Story 3 (Dashboard)** — phần giá trị nghiệp vụ mới, làm sau khi nền tảng ổn định.
4. **Story 5** — bổ sung test còn thiếu, nhanh, có thể làm song song.
5. **Story 6** — dọn dẹp, làm cuối hoặc xen kẽ khi tách PR.

## 4. Rủi ro/điểm cần chú ý khi triển khai

- `/actuator/health` hiện được `permitAll` trong `SecurityConfig` nhưng chưa có actuator dependency — nếu thêm actuator mà không rà lại rule, các endpoint khác như `/actuator/env`, `/actuator/beans` có thể vô tình bị lộ nếu cấu hình `exposure.include` quá rộng. Cần include tối thiểu (`health,info,metrics`) và bảo vệ bằng ADMIN role, trừ `health`.
- Admin đổi role/khoá user đã đúng chuẩn (increment `tokenVersion` + revoke session) — khi làm thêm "xem/thu hồi session theo admin" (Story 1.4), tái dùng lại `UserSessionRepository.revokeActiveByUserId` đã có, tránh viết logic trùng.
- Dashboard aggregation (Story 3) nên dùng query native/JPQL tổng hợp (COUNT/SUM) thay vì load toàn bộ entity vào memory rồi tính bằng Java — quan trọng khi dữ liệu transaction lớn dần.
