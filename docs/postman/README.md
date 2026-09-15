# Postman Collection (DWS-157/159/160)

Collection được sinh tự động từ OpenAPI spec (springdoc-openapi), không maintain tay.

## Import

1. Chạy backend (`docker compose up` hoặc `mvn spring-boot:run`).
2. Xác nhận spec chạy được: `http://localhost:8089/v3/api-docs` (cổng host thật theo
   `docker-compose.yml`, container dùng 8080).
3. Trong Postman: **Import → Link** → dán URL trên → Import.
4. Postman tự sinh 1 collection với toàn bộ endpoint từ `@RestController`.

## Environment (DWS-159)

Tạo 1 Environment mới với các biến:

| Variable | Value |
|---|---|
| `base_url` | `http://localhost:8089` |
| `jwt_token` | (dán access token sau khi login qua `/api/auth/login`) |

Set header `Authorization: Bearer {{jwt_token}}` ở collection level (hoặc từng request cần
auth).

## Export (DWS-160)

Sau khi import + set environment: **Collection → Export** (Collection v2.1) và **Environment
→ Export**, lưu 2 file JSON vào thư mục này để đính kèm/chia sẻ khi cần.

## Đồng bộ lại khi code đổi

Vì collection sinh từ `/v3/api-docs`, chỉ cần Import → Link lại (hoặc bấm "Resync" nếu đã
liên kết) để cập nhật — không cần sửa tay từng request.
