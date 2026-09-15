-- V6 thêm 'PENDING_OTP_CONFIRMATION' (24 ký tự) vào CHECK constraint của transactions.status,
-- nhưng chưa mở rộng cột — vẫn là varchar(20), ngắn hơn chính giá trị vừa cho phép. Hệ quả:
-- mọi giao dịch rút/chuyển tiền rơi vào PENDING_OTP_CONFIRMATION (>= wallet.otp.required-amount)
-- sẽ luôn lỗi "value too long for type character varying(20)" khi Hibernate cố lưu, tức là
-- toàn bộ nhánh xác nhận OTP của rút tiền/chuyển tiền bị vỡ ở tầng DB — phát hiện khi test
-- thật luồng rút tiền 7.000.000đ (nằm trong khoảng bắt buộc OTP) qua Postgres thật.
alter table transactions alter column status type varchar(30);
