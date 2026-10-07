CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    country VARCHAR(2) NOT NULL,
    ip_address VARCHAR(45),
    card_attempts INTEGER NOT NULL,
    email_age_days INTEGER,
    status VARCHAR(20) NOT NULL,
    risk_score INTEGER,
    risk_level VARCHAR(20),
    recommended_action VARCHAR(20),
    triggered_rules VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT uk_transactions_transaction_id UNIQUE (transaction_id)
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox_events (published_at, created_at);
