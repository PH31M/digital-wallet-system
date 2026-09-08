## Jira ticket
<!-- Ví dụ: DWS-XX -->
Closes DWS-

## Mô tả thay đổi
<!-- Tóm tắt ngắn gọn thay đổi này làm gì và tại sao -->

## Loại thay đổi
- [ ] Feature mới
- [ ] Bug fix
- [ ] Refactor / cleanup (không đổi behavior)
- [ ] Test
- [ ] Docs / cấu hình / CI

## Checklist trước khi merge
- [ ] Đã chạy `mvn test` (hoặc `make test`) local và pass
- [ ] Đã tự review lại diff của chính mình
- [ ] Đã cập nhật/migration Flyway nếu có thay đổi schema
- [ ] Đã cập nhật Jira ticket tương ứng sau khi merge
- [ ] Không có secret/credential nào bị commit nhầm (.env, key, token...)

## Ghi chú thêm cho reviewer (nếu có)
<!-- Rủi ro cần lưu ý, phần khó review, chỗ cần test kỹ thêm... -->
