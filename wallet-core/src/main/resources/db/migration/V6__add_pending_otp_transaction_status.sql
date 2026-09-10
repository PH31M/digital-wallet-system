alter table transactions drop constraint chk_transactions_status;

alter table transactions add constraint chk_transactions_status
    check (status in ('PENDING', 'PROCESSING', 'PENDING_REVIEW', 'PENDING_OTP_CONFIRMATION', 'COMPLETED', 'FAILED'));
