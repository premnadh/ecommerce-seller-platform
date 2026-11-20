CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    order_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'MOCK',
    status VARCHAR(20) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,
    provider_reference VARCHAR(100),
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_payment_attempts_order
        FOREIGN KEY (seller_id, order_id)
        REFERENCES orders(seller_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_payment_attempts_operation
        CHECK (operation IN ('PAYMENT', 'REFUND')),

    CONSTRAINT chk_payment_attempts_provider
        CHECK (provider IN ('MOCK')),

    CONSTRAINT chk_payment_attempts_status
        CHECK (status IN ('INITIATED', 'SUCCEEDED', 'FAILED')),

    CONSTRAINT chk_payment_attempts_amount
        CHECK (amount >= 0),

    CONSTRAINT chk_payment_attempts_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_payment_attempts_order
    ON payment_attempts (seller_id, order_id, created_at DESC);

CREATE UNIQUE INDEX uq_payment_attempts_provider_reference
    ON payment_attempts (provider, provider_reference)
    WHERE provider_reference IS NOT NULL;
