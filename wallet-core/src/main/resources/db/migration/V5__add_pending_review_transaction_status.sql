alter table transactions drop constraint chk_transactions_status;

alter table transactions add constraint chk_transactions_status
    check (status in ('PENDING', 'PROCESSING', 'PENDING_REVIEW', 'COMPLETED', 'FAILED'));