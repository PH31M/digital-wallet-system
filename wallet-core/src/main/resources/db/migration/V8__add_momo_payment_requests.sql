create table momo_payment_requests (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    transaction_id uuid not null,
    momo_order_id varchar(255) not null,
    momo_request_id varchar(255) not null,
    momo_trans_id varchar(255),
    request_type varchar(20) not null,
    status varchar(20) not null default 'PENDING',
    raw_response text,
    constraint uq_momo_payment_requests_momo_order_id unique (momo_order_id),
    constraint fk_momo_payment_requests_transaction foreign key (transaction_id) references transactions(id),
    constraint chk_momo_payment_requests_request_type check (request_type in ('captureWallet', 'payWithATM')),
    constraint chk_momo_payment_requests_status check (status in ('PENDING', 'CONFIRMED', 'FAILED'))
);

create index idx_momo_payment_requests_transaction_id on momo_payment_requests(transaction_id);
