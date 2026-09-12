create table users (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    email varchar(254) not null,
    full_name varchar(100) not null,
    phone_number varchar(255),
    password_hash varchar(255) not null,
    role varchar(20) not null,
    email_verified_at timestamptz,
    is_active boolean not null default true,
    failed_login_attempts integer not null default 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    constraint uq_users_email unique (email),
    constraint uq_users_phone_number unique (phone_number),
    constraint chk_users_role check (role in ('USER', 'ADMIN', 'SUPPORT'))
);

create table wallets (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    user_id uuid not null,
    currency varchar(3) not null default 'VND',
    balance numeric(18, 2) not null default 0,
    status varchar(30) not null default 'PENDING_VERIFICATION',
    constraint uq_wallets_user_currency unique (user_id, currency),
    constraint fk_wallet_user foreign key (user_id) references users(id) on delete restrict,
    constraint chk_wallets_status check (status in ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'FROZEN')),
    constraint chk_wallets_balance_non_negative check (balance >= 0)
);

create table transactions (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    reference_number varchar(255) not null,
    sender_wallet_id uuid,
    receiver_wallet_id uuid,
    related_transaction_id uuid,
    transaction_type varchar(20) not null,
    amount numeric(18, 2) not null,
    status varchar(20) not null default 'PENDING',
    fraud_score numeric(5, 2),
    fraud_decision varchar(20),
    idempotency_key varchar(255),
    completed_at timestamptz,
    failed_at timestamptz,
    constraint uq_transactions_reference_number unique (reference_number),
    constraint uq_transactions_idempotency_key unique (idempotency_key),
    constraint fk_transactions_sender_wallet foreign key (sender_wallet_id) references wallets(id),
    constraint fk_transactions_receiver_wallet foreign key (receiver_wallet_id) references wallets(id),
    constraint fk_transactions_related_transaction foreign key (related_transaction_id) references transactions(id),
    constraint chk_transactions_type check (transaction_type in ('DEPOSIT', 'WITHDRAW', 'TRANSFER', 'REVERSAL')),
    constraint chk_transactions_status check (status in ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    constraint chk_transactions_fraud_decision check (fraud_decision is null or fraud_decision in ('ALLOW', 'CHALLENGE', 'BLOCK')),
    constraint chk_transactions_amount_positive check (amount > 0),
    constraint chk_transactions_fraud_score_range check (fraud_score is null or (fraud_score >= 0 and fraud_score <= 100))
);

create table fraud_assessments (
    id uuid primary key,
    created_at timestamptz not null default now(),
    transaction_id uuid not null,
    risk_score numeric(5, 2) not null,
    rule_score numeric(5, 2) not null,
    ai_anomaly_score numeric(5, 2) not null,
    model_version varchar(255) not null,
    triggered_rules jsonb,
    decision varchar(20) not null,
    review_status varchar(20) not null default 'NOT_REQUIRED',
    reviewed_by uuid,
    review_action varchar(20),
    review_note text,
    reviewed_at timestamptz,
    constraint uq_fraud_assessments_transaction_id unique (transaction_id),
    constraint fk_fraud_assessments_transaction foreign key (transaction_id) references transactions(id),
    constraint fk_fraud_assessments_reviewed_by foreign key (reviewed_by) references users(id),
    constraint chk_fraud_assessments_decision check (decision in ('ALLOW', 'CHALLENGE', 'BLOCK')),
    constraint chk_fraud_assessments_review_status check (review_status in ('NOT_REQUIRED', 'PENDING_REVIEW', 'REVIEWED', 'CLEARED')),
    constraint chk_fraud_assessments_review_action check (review_action is null or review_action in ('APPROVED', 'REJECTED')),
    constraint chk_fraud_assessments_risk_score_range check (risk_score >= 0 and risk_score <= 100),
    constraint chk_fraud_assessments_rule_score_range check (rule_score >= 0 and rule_score <= 100),
    constraint chk_fraud_assessments_ai_score_range check (ai_anomaly_score >= 0 and ai_anomaly_score <= 100)
);

create table ledger_entries (
    id uuid primary key,
    created_at timestamptz not null default now(),
    transaction_id uuid not null,
    wallet_id uuid,
    account_type varchar(20) not null,
    debit_amount numeric(18, 2) not null default 0,
    credit_amount numeric(18, 2) not null default 0,
    constraint fk_ledger_entries_transaction foreign key (transaction_id) references transactions(id),
    constraint fk_ledger_entries_wallet foreign key (wallet_id) references wallets(id),
    constraint chk_ledger_entries_account_type check (account_type in ('USER_WALLET', 'CASH_ACCOUNT')),
    constraint chk_ledger_entries_debit_non_negative check (debit_amount >= 0),
    constraint chk_ledger_entries_credit_non_negative check (credit_amount >= 0),
    constraint chk_ledger_entries_single_sided check (
        (debit_amount = 0 and credit_amount > 0) or
        (debit_amount > 0 and credit_amount = 0)
    )
);

create table notifications (
    id uuid primary key,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    public_id uuid not null,
    user_id uuid not null,
    type varchar(40) not null,
    title varchar(150) not null,
    message text not null,
    metadata jsonb,
    is_read boolean not null default false,
    read_at timestamptz,
    constraint uq_notifications_public_id unique (public_id),
    constraint fk_notifications_user foreign key (user_id) references users(id),
    constraint chk_notifications_type check (type in (
        'TRANSACTION_SENT',
        'TRANSACTION_RECEIVED',
        'WITHDRAWAL_APPROVED',
        'WITHDRAWAL_REJECTED',
        'FRAUD_ALERT',
        'SYSTEM_ANNOUNCEMENT'
    ))
);

create table audit_logs (
    id uuid primary key,
    created_at timestamptz not null default now(),
    actor_id uuid,
    actor_type varchar(20) not null,
    action varchar(255) not null,
    resource_type varchar(255) not null,
    resource_id uuid not null,
    before_state jsonb,
    after_state jsonb,
    ip_address varchar(255),
    user_agent text,
    request_id uuid,
    constraint fk_audit_logs_actor foreign key (actor_id) references users(id),
    constraint chk_audit_logs_actor_type check (actor_type in ('USER', 'ADMIN', 'SYSTEM'))
);

create index idx_wallets_user_id on wallets(user_id);
create index idx_transactions_sender_wallet_id on transactions(sender_wallet_id);
create index idx_transactions_receiver_wallet_id on transactions(receiver_wallet_id);
create index idx_transactions_related_transaction_id on transactions(related_transaction_id);
create index idx_transactions_status on transactions(status);
create index idx_ledger_entries_transaction_id on ledger_entries(transaction_id);
create index idx_ledger_entries_wallet_id on ledger_entries(wallet_id);
create index idx_notifications_user_id on notifications(user_id);
create index idx_notifications_user_id_is_read on notifications(user_id, is_read);
create index idx_notifications_public_id on notifications(public_id);
create index idx_audit_logs_actor_id on audit_logs(actor_id);
create index idx_audit_logs_resource on audit_logs(resource_type, resource_id);
create index idx_audit_logs_request_id on audit_logs(request_id);
create index idx_fraud_assessments_reviewed_by on fraud_assessments(reviewed_by);
