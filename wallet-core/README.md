# Wallet Core Module

## Cấu trúc thư mục chính

- `src/main/java/com/digitalwallet/`
  - `WalletCoreApplication.java` - entry point của ứng dụng Spring Boot.
  - `config/` - cấu hình Spring Boot, security, JWT, Redis, async và WebSocket.
  - `security/` - các lớp liên quan đến JWT và authentication filter.
  - `domain/` - business domain, entity, enum, repository và service.
  - `api/` - REST layer, controller, DTO request/response và exception advice.
  - `constant/` - hằng số dùng chung.
  - `exception/` - custom exception.
  - `util/` - class tiện ích dùng trong toàn bộ ứng dụng.
  - `websocket/` - xử lý WebSocket và event publishing.

- `src/main/resources/`
  - `application.properties` - cấu hình Spring Boot mặc định.
  - `db/migration/` - Flyway SQL migration.

## Luồng hoạt động chính

1. `WalletCoreApplication` khởi chạy Spring Boot và scan tất cả component trong `com.digitalwallet`.
2. `SecurityConfig` định nghĩa rule bảo mật chung cho toàn bộ API.
3. `JwtConfig` đọc cấu hình JWT từ properties/yaml.
4. `RedisConfig` cấu hình kết nối đến Redis.
5. `AsyncConfig` cung cấp thread pool cho các tác vụ bất đồng bộ.
6. `WebSocketConfig` đăng ký endpoint STOMP và broker nội bộ.
7. `AuthController` xử lý login/register.
8. `WalletController` cung cấp endpoint lấy thông tin ví.
9. Các entity trong `domain/entity` biểu diễn bảng dữ liệu.
10. Repository trong `domain/repository` thực hiện truy vấn database.
11. `exception/` định nghĩa lỗi nghiệp vụ, ví dụ `InsufficientBalanceException`, `WalletFrozenException`.
12. `util/` chứa helper dùng chung như `ReferenceNumberGenerator` và `IdempotencyService`.

## Hướng dẫn dùng file mới

- Nếu muốn thêm endpoint mới, tạo thêm controller trong `api/controller` và DTO tương ứng trong `api/dto/request` hoặc `api/dto/response`.
- Nếu cần thêm nghiệp vụ mới, tạo service trong `domain/service` và dùng repository tương ứng.
- Nếu thêm bảng mới, tạo entity trong `domain/entity` và migration SQL trong `src/main/resources/db/migration`.
- Nếu cần thay đổi luật bảo mật, chỉnh `SecurityConfig`.
- Nếu cần cấu hình thêm JWT, thay đổi `JwtConfig` và cấu hình trong `application.properties`.

## Ghi chú

- Package gốc phải là `com.digitalwallet` để Spring Boot scan đủ các component.
- Đã dùng Jakarta Persistence (`jakarta.persistence.*`) cho Spring Boot 4.
- `application.properties` hiện chỉ cấu hình tên ứng dụng. Các cấu hình database, JWT, Redis cần thêm khi triển khai thực tế.
