# DWS-204 — System Monitoring: Health & Metrics — Kế hoạch hoàn thành chi tiết

**Story:** DWS-204 (con của Epic DWS-6 — Admin & Monitoring)
**6 Subtask:** DWS-223 → DWS-228

## 1. Xác nhận lại gap thật (đọc trực tiếp code)

Đã kiểm tra các file liên quan, xác nhận:

- `wallet-core/pom.xml`: **không có** `spring-boot-starter-actuator` trong dependencies (chỉ có web, data-jpa, data-redis(-reactive), security, validation, websocket, flyway, mail, jjwt, postgresql). Spring Boot parent version `3.3.2`.
- `SecurityConfig.java` dòng 61: `.requestMatchers("/api/auth/**", "/actuator/health", "/ws").permitAll()` — đã permit `/actuator/health` **trước khi endpoint đó tồn tại**. Đây là gap cấu hình thật: nếu thêm actuator mà không rà lại, `/actuator/health` sẽ public hoàn toàn (đúng ý đồ), nhưng các endpoint actuator khác (`/actuator/env`, `/actuator/beans`, `/actuator/metrics`...) sẽ rơi vào `.anyRequest().authenticated()` — tức là **yêu cầu JWT của user thường cũng vào được**, không riêng ADMIN. Cần siết lại thành yêu cầu role ADMIN.
- `RedisConfig.java`: chỉ khai báo `StringRedisTemplate`, không có `RedisConnectionFactory` custom — nghĩa là Spring Boot tự autoconfigure factory từ `spring.data.redis.*` trong `application.yml`. Khi thêm actuator, Spring Boot Health autoconfiguration **sẽ tự động thêm `RedisHealthIndicator`** vì đã có `spring-boot-starter-data-redis` + `RedisConnectionFactory` bean sẵn — **không cần viết custom HealthIndicator cho Redis theo cách thủ công**, chỉ cần verify nó hoạt động đúng.
- `application.yml`: chưa có block `management:` nào cả.
- `docker-compose.yml`: `postgres` và `redis` đã có `healthcheck`, nhưng **service `backend` (wallet-core) chưa có healthcheck nào** — cơ hội tốt để nối với `/actuator/health` sau khi có.
- `logback-spring.xml`: log pattern hiện tại đã có `requestId` — không cần đổi khi thêm actuator.

## 2. Kế hoạch chi tiết theo từng Subtask

### DWS-223 — Thêm `spring-boot-starter-actuator` vào `pom.xml`

Thêm vào `<dependencies>` trong `wallet-core/pom.xml`, ngay sau `spring-boot-starter-validation` (giữ nhóm "starter" liền nhau):

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
    <version>3.3.2</version>
</dependency>
```

Không cần thêm gì khác ở bước này — chỉ thêm dependency là `/actuator/health` đã tự có (mặc định Spring Boot expose sẵn `health` qua web).

### DWS-224 — Cấu hình `application.yml` cho actuator

Thêm block `management:` vào `application.yml` (đặt sau block `spring:`, trước `jwt:`):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: when-authorized
      probes:
        enabled: true
  health:
    redis:
      enabled: true
    db:
      enabled: true
```

Lưu ý:
- `include: health,info,metrics` — tối thiểu, **không** thêm `env`, `beans`, `configprops`, `shutdown`... để tránh lộ thông tin hệ thống dù đã bảo vệ bằng ADMIN (nguyên tắc defense-in-depth).
- `show-details: when-authorized` — chỉ user đã authenticate với role phù hợp mới thấy chi tiết từng component (DB, Redis...), người ngoài gọi `/actuator/health` không xác thực chỉ thấy `{"status":"UP"}` — khớp với việc endpoint này đang `permitAll`.
- `probes.enabled: true` — thêm `/actuator/health/liveness` và `/actuator/health/readiness`, hữu ích nếu sau này deploy K8s.

### DWS-225 — Bảo vệ `/actuator/**` (trừ `health`) bằng role ADMIN trong `SecurityConfig`

Sửa `SecurityConfig.java`, dòng 59-62. Hiện tại:

```java
.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/auth/**", "/actuator/health", "/ws").permitAll()
        .anyRequest().authenticated())
```

Sửa thành:

```java
.authorizeHttpRequests(authorize -> authorize
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/api/auth/**", "/actuator/health", "/actuator/health/**", "/ws").permitAll()
        .requestMatchers("/actuator/**").hasRole("ADMIN")
        .anyRequest().authenticated())
```

Thứ tự matcher quan trọng trong Spring Security — rule cụ thể hơn (`/actuator/health`) phải đứng **trước** rule tổng quát hơn (`/actuator/**`), Spring Security áp dụng rule đầu tiên khớp path.

Có 1 điểm cần quyết định: hiện `/actuator/**` không dùng JWT filter riêng biệt nào khác — nó vẫn đi qua `JwtAuthenticationFilter` như mọi request khác, nên `hasRole("ADMIN")` hoạt động bình thường (dựa trên authority set từ JWT). Không cần filter riêng.

### DWS-226 — Custom HealthIndicator cho Redis

**Cập nhật so với mô tả subtask ban đầu**: sau khi đọc `RedisConfig.java`, Spring Boot **đã tự động cung cấp `RedisHealthIndicator`** khi có actuator + `spring-boot-starter-data-redis` + `RedisConnectionFactory` bean (tất cả đã có sẵn). Không cần viết class `HealthIndicator` mới từ đầu.

Việc cần làm ở subtask này thu hẹp lại thành:
1. Set `management.health.redis.enabled: true` (đã làm ở DWS-224).
2. Verify bằng tay: tắt Redis (`docker compose stop redis`), gọi `/actuator/health`, xác nhận response trả `status: DOWN` với `components.redis.status: DOWN`.
3. Nếu muốn thông tin chi tiết hơn mặc định (ví dụ đo latency PING), có thể viết thêm 1 `HealthIndicator` bổ sung tên riêng (không trùng `redis`), ví dụ `RedisLatencyHealthIndicator` dùng `StringRedisTemplate.execute(RedisConnection::ping)` — nhưng đây là **nice-to-have**, không phải bắt buộc để đóng subtask.

### DWS-227 — (Optional) `micrometer-registry-prometheus` + Prometheus/Grafana trong docker-compose

Thêm dependency:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Thêm `prometheus` vào `management.endpoints.web.exposure.include` (application.yml):

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Thêm service vào `docker-compose.yml` (khối mới, cạnh `mailhog`):

```yaml
  prometheus:
    image: prom/prometheus:latest
    container_name: wallet-prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    networks:
      - wallet-network
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: wallet-grafana
    ports:
      - "3001:3000"
    networks:
      - wallet-network
    restart: unless-stopped
```

Cần thêm file `prometheus.yml` ở root `wallet-core/` với scrape config trỏ vào `backend:8080/actuator/prometheus`. Vì đây là subtask Optional, có thể làm sau, không chặn các subtask khác.

### DWS-228 — Test `/actuator/health` trả 200 UP

Viết integration test mới, ví dụ `src/test/java/com/digitalwallet/config/ActuatorHealthTest.java`, dùng `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate`, tương tự cách các test hiện tại tổ chức (xem `WebSocketNotificationIntegrationTest` để theo pattern context loading có sẵn — cần Postgres/Redis test container hoặc profile test đang dùng, kiểm tra `src/test/resources` xem đã có config test DB/Redis chưa trước khi viết test này, tránh trùng lặp thiết lập).

Test case tối thiểu:
1. Gọi `GET /actuator/health` không có Authorization header → `200`, body `{"status":"UP"}` (không chi tiết vì chưa authorized).
2. Gọi `GET /actuator/health` với JWT thường (role USER) → vẫn `200 UP` nhưng không thấy `components` chi tiết (do `show-details: when-authorized` và user không phải ADMIN).
3. Gọi `GET /actuator/metrics` không có JWT → `401/403` (đang bị chặn bởi rule `/actuator/**` hasRole ADMIN).
4. Gọi `GET /actuator/metrics` với JWT role ADMIN → `200`.

## 3. Thứ tự thực hiện đề xuất (dependency giữa các subtask)

1. DWS-223 (thêm dependency) — bắt buộc làm trước tiên, không dependency nào chạy được nếu thiếu jar.
2. DWS-224 (cấu hình yml) — làm ngay sau, cần trước khi test.
3. DWS-225 (bảo vệ SecurityConfig) — làm cùng lúc hoặc ngay sau 224, **quan trọng**: nếu bỏ qua bước này, mọi user đã login (không cần ADMIN) sẽ gọi được `/actuator/metrics`, `/actuator/info` — rò rỉ thông tin hệ thống.
4. DWS-226 (verify Redis health) — làm sau khi 223-225 xong, chỉ cần verify + optional latency indicator.
5. DWS-228 (test) — làm cuối, sau khi toàn bộ cấu hình ổn định, để test phản ánh đúng hành vi cuối cùng.
6. DWS-227 (Prometheus/Grafana, Optional) — có thể làm bất cứ lúc nào sau DWS-224, không block gì khác, ưu tiên thấp nhất.

## 4. Rủi ro cần lưu ý

- Nếu `show-details` để `always` thay vì `when-authorized`, `/actuator/health` public sẽ lộ chi tiết trạng thái DB/Redis cho bất kỳ ai không cần đăng nhập — nên giữ `when-authorized`.
- Thứ tự `requestMatchers` trong `SecurityConfig` rất dễ sai (rule tổng quát che rule cụ thể) — cần test thủ công lại toàn bộ nhóm endpoint `/actuator/*` sau khi sửa, không chỉ riêng route mới thêm.
- `docker-compose.yml` hiện chưa có `healthcheck` cho service `backend` — sau khi có `/actuator/health`, nên bổ sung luôn (không nằm trong 6 subtask hiện tại nhưng là quick win liên quan, có thể thêm là subtask thứ 7 nếu muốn):

```yaml
  backend:
    ...
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
```
