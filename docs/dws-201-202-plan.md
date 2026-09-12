# DWS-201 & DWS-202 — Kế hoạch hoàn thành chi tiết

Sau DWS-204 (Monitoring), đây là bước tiếp theo theo đúng thứ tự ưu tiên đã đề xuất: bổ sung phần còn thiếu cho 2 Story đã có code (Quản lý user & Audit log) — rủi ro cao nhất hiện tại vì đây là quyền admin nhưng gần như chưa có test.

## DWS-201 — Quản lý người dùng cho Admin (4 subtask còn lại)

### DWS-208 — Unit test `AdminUserService`

File mới: `src/test/java/com/digitalwallet/service/AdminUserServiceTest.java`, theo đúng convention đang dùng (`@ExtendWith(MockitoExtension.class)`, `@Mock` field cho repository, dựng service bằng constructor thủ công, AssertJ + Mockito `verify`).

Cần mock: `UserRepository`, `UserSessionRepository`, `AuditLogRepository`, `AuditService`.

Test case tối thiểu:
1. `listUsers_noEmailFilter_delegatesToFindAll` — gọi `userRepository.findAll(pageable)`, không gọi `findByEmailContainingIgnoreCase`.
2. `listUsers_withEmailFilter_delegatesToFindByEmailContaining` — email non-blank → gọi đúng method, verify email đã `trim()`.
3. `getUser_found_returnsResponse` / `getUser_notFound_throwsResourceNotFound` (`ErrorCode.RESOURCE_NOT_FOUND`).
4. `updateRole_success_incrementsTokenVersionAndRevokesSessions` — verify `target.incrementTokenVersion()` được gọi (kiểm tra qua `user.getTokenVersion()` tăng lên), `userSessionRepository.revokeActiveByUserId(...)` được gọi, `auditService.log(...)` được gọi với `AuditAction.USER_ROLE_CHANGED`.
5. `updateStatus_deactivate_revokesSessions` — khi `active=false`, verify `revokeActiveByUserId` được gọi.
6. `updateStatus_activate_doesNotRevokeSessions` — khi `active=true`, verify `revokeActiveByUserId` **không** được gọi (đúng logic hiện tại: chỉ revoke khi khoá, không revoke khi mở khoá).
7. `updateRole_userNotFound_throwsResourceNotFoundAndDoesNotAudit` — verify `auditService` không bị gọi khi user không tồn tại.
8. `listAuditLogs_noUserId_delegatesToFindAll`, `listAuditLogs_withUserId_delegatesToFindByResourceTypeAndResourceId`.

Đây là service có quyền cao (đổi role/khoá user) — nên test kỹ cả nhánh audit không bị bỏ sót khi update thành công.

### DWS-209 — Controller test `AdminUserController`

File mới: `src/test/java/com/digitalwallet/api/controller/AdminUserControllerTest.java`. Vì controller có `@PreAuthorize("hasRole('ADMIN')")` ở class-level, cần test **có bật security filter** (khác với `AuthControllerTest` hiện tại dùng `@AutoConfigureMockMvc(addFilters = false)` — ở đây phải để `addFilters` mặc định `true` và dùng `@WithMockUser(roles = "ADMIN")` / `@WithMockUser(roles = "USER")` từ `spring-security-test` để giả lập authority, vì method security (`@EnableMethodSecurity`) chỉ kích hoạt khi có `Authentication` trong `SecurityContext`).

Cần thêm dependency `spring-security-test` (scope test) vào `pom.xml` nếu chưa có — kiểm tra lại, hiện `pom.xml` không có dependency này, cần bổ sung:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Test case:
1. `listUsers_asAdmin_returns200` (`@WithMockUser(roles = "ADMIN")`).
2. `listUsers_asUser_returns403` (`@WithMockUser(roles = "USER")`).
3. `listUsers_noAuth_returns401`.
4. `updateRole_asAdmin_returns200AndCallsService`.
5. `listAuditLogs_asAdmin_returns200`.

### DWS-210 — API admin xem/thu hồi session của 1 user

Thêm 2 endpoint mới vào `AdminUserController`:

```java
@GetMapping("/users/{userId}/sessions")
public ResponseEntity<ApiResponse<List<UserSessionResponse>>> listUserSessions(
        @PathVariable UUID userId, HttpServletRequest httpRequest) {
    UUID requestId = RequestIds.getUuid(httpRequest);
    return ResponseEntity.ok(ApiResponse.success(
            requestId.toString(), Instant.now(), adminUserService.listUserSessions(userId)));
}

@DeleteMapping("/users/{userId}/sessions/{sessionId}")
public ResponseEntity<ApiResponse<Void>> revokeUserSession(
        @CurrentUser User admin, @PathVariable UUID userId, @PathVariable UUID sessionId,
        HttpServletRequest httpRequest) {
    UUID requestId = RequestIds.getUuid(httpRequest);
    adminUserService.revokeUserSession(admin, userId, sessionId, httpRequest, requestId);
    return ResponseEntity.ok(ApiResponse.success(requestId.toString(), Instant.now(), null));
}
```

Thêm vào `AdminUserService`:

```java
public List<UserSessionResponse> listUserSessions(UUID userId) {
    findUser(userId); // đảm bảo user tồn tại, ném RESOURCE_NOT_FOUND nếu không
    return userSessionRepository.findActiveByUserId(userId, Instant.now())
            .stream().map(UserSessionResponse::new).toList();
}

@Transactional
public void revokeUserSession(User admin, UUID userId, UUID sessionId,
        HttpServletRequest request, UUID requestId) {
    User target = findUser(userId);
    UserSession session = userSessionRepository.findById(sessionId)
            .filter(s -> s.getUser().getId().equals(userId))
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    session.revoke();
    userSessionRepository.save(session);
    audit(admin, AuditAction.SESSION_REVOKED, target, request, requestId);
}
```

Lưu ý: `UserSessionRepository` hiện chưa có `findById` filter theo user — dùng cách trên (`findById` + `filter`) để tránh phải thêm query mới, đơn giản và đủ dùng ở quy mô hiện tại. Cần import `UserSession`, `List`, `UserSessionResponse`, `AuditAction.SESSION_REVOKED` (enum đã có sẵn, dùng cho user tự revoke session của mình ở `UserService` — verify lại `UserService.revokeSession` để dùng đúng action, tránh tạo action mới trùng ý nghĩa).

### DWS-211 — Whitelist field cho `Pageable.sort`

Vấn đề: `Pageable pageable` inject trực tiếp từ query param `?sort=` — Spring Data cho phép client truyền bất kỳ tên property nào, nếu không khớp field JPA sẽ lỗi 500 (property không tồn tại) thay vì 400 rõ ràng, và về nguyên tắc không nên tin tưởng input này 100%.

Cách làm gọn nhất — thêm 1 utility validate trong `AdminUserService` trước khi query:

```java
private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of("createdAt", "email", "fullName", "role");

private Pageable sanitize(Pageable pageable) {
    List<Sort.Order> validOrders = pageable.getSort().stream()
            .filter(order -> ALLOWED_USER_SORT_FIELDS.contains(order.getProperty()))
            .toList();
    Sort sort = validOrders.isEmpty() ? Sort.by(Sort.Direction.DESC, "createdAt") : Sort.by(validOrders);
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
}
```

Gọi `sanitize(pageable)` ở đầu `listUsers` và `listAuditLogs` (whitelist audit log riêng: `createdAt`, `action`). Đơn giản, không cần thêm class riêng.

## DWS-202 — Audit Log nâng cao cho Admin (4 subtask còn lại)

### DWS-213 — Mở rộng `AuditLogRepository` bằng Specification

Thêm vào `AuditLogRepository`:
```java
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, UUID resourceId, Pageable pageable);
}
```

File mới `AuditLogSpecifications.java` (package `domain.repository` hoặc `service`, theo convention hiện tại đặt cạnh entity/repository):
```java
public final class AuditLogSpecifications {
    private AuditLogSpecifications() {}

    public static Specification<AuditLog> filter(AuditAction action, AuditActorType actorType,
            String resourceType, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (action != null) predicates.add(cb.equal(root.get("actionType"), action));
            if (actorType != null) predicates.add(cb.equal(root.get("actorType"), actorType));
            if (resourceType != null) predicates.add(cb.equal(root.get("resourceType"), resourceType));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```
Lưu ý field trong entity là `createdAt` (kế thừa từ `BaseEntity`) — kiểm tra tên chính xác trong `BaseEntity` trước khi dùng string literal (tránh lỗi runtime do sai tên field, không bắt được lúc compile với Specification kiểu string-based).

### DWS-214 — API `GET /api/admin/audit-logs` với đầy đủ filter

Sửa `AdminUserService.listAuditLogs` (hoặc tách riêng `AdminAuditLogService` nếu muốn giữ `AdminUserService` gọn — quyết định: giữ trong `AdminUserService` vì đang nhỏ, tách khi phình to):

```java
public Page<AuditLogResponse> listAuditLogs(UUID userId, AuditAction action, AuditActorType actorType,
        Instant from, Instant to, Pageable pageable) {
    Pageable sorted = sanitizeAuditSort(pageable);
    if (userId != null) {
        return auditLogRepository.findByResourceTypeAndResourceId("USER", userId, sorted)
                .map(AuditLogResponse::new);
    }
    Specification<AuditLog> spec = AuditLogSpecifications.filter(action, actorType, null, from, to);
    return auditLogRepository.findAll(spec, sorted).map(AuditLogResponse::new);
}
```

Sửa controller thêm param:
```java
@GetMapping("/audit-logs")
public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> listAuditLogs(
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) AuditAction action,
        @RequestParam(required = false) AuditActorType actorType,
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        Pageable pageable, HttpServletRequest httpRequest) { ... }
```
Spring tự bind `Instant` từ query param ISO-8601 (`2026-08-01T00:00:00Z`) không cần converter thêm, đã kiểm tra Spring Boot 3.3 hỗ trợ mặc định.

### DWS-215 — Test cho query audit log nâng cao

- `AdminUserServiceTest` bổ sung case: filter theo `action`/`actorType`/khoảng thời gian, verify `auditLogRepository.findAll(any(Specification.class), any(Pageable.class))` được gọi khi `userId == null`.
- Test riêng cho `AuditLogSpecifications` nếu muốn kiểm tra predicate build đúng — cân nhắc `@DataJpaTest` với H2 in-memory để test Specification chạy thật (hiện project dùng Postgres cho migration, H2 không chạy được Flyway script Postgres-specific — nếu dùng `@DataJpaTest` cần `@AutoConfigureTestDatabase(replace = NONE)` trỏ Postgres test thật, hoặc bỏ qua Flyway trong test này bằng `@TestPropertySource(properties = "spring.flyway.enabled=false")` + `spring.jpa.hibernate.ddl-auto=create-drop` cho riêng test đó). Vì đây là subtask phụ, có thể để mức unit test qua mock là đủ, không bắt buộc phải chạy thật với DB.

### DWS-216 — (Optional) Export audit log ra CSV

```java
@GetMapping("/audit-logs/export")
public void exportAuditLogs(
        @RequestParam(required = false) Instant from,
        @RequestParam(required = false) Instant to,
        HttpServletResponse response) throws IOException {
    response.setContentType("text/csv");
    response.setHeader("Content-Disposition", "attachment; filename=audit-logs.csv");
    adminUserService.exportAuditLogsCsv(from, to, response.getWriter());
}
```
Service ghi từng dòng bằng `PrintWriter`, dùng `Pageable` phân trang nội bộ (ví dụ 500 dòng/lần) để tránh load hết vào memory nếu bảng lớn. Vì Optional, có thể làm sau cùng.

## Thứ tự thực hiện đề xuất

1. DWS-208 (unit test AdminUserService) — không cần sửa code sản phẩm, làm ngay được, giá trị cao nhất (đóng gap bảo mật/quyền admin thiếu test).
2. DWS-213 (Specification) → DWS-214 (API filter) — làm cùng nhau vì phụ thuộc trực tiếp.
3. DWS-209 (controller test) — làm sau khi có `spring-security-test` dependency, test luôn cả 2 endpoint mới ở DWS-210 nếu đã xong.
4. DWS-210 (session API) — có thể làm song song với DWS-213/214, độc lập.
5. DWS-211 (sort whitelist) — nhỏ, làm xen bất cứ lúc nào, không phụ thuộc.
6. DWS-215 (test audit filter) — sau DWS-213/214.
7. DWS-216 (export CSV, optional) — cuối cùng.
