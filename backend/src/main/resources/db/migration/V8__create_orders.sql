CREATE TABLE orders (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    idempotency_key UUID NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    currency CHAR(3) NOT NULL,
    subtotal NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_orders_seller_id_id
        UNIQUE (seller_id, id),

    CONSTRAINT uq_orders_seller_order_number
        UNIQUE (seller_id, order_number),

    CONSTRAINT uq_orders_seller_idempotency_key
        UNIQUE (seller_id, idempotency_key),

    CONSTRAINT fk_orders_seller
        FOREIGN KEY (seller_id)
        REFERENCES seller_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_orders_status
        CHECK (
            status IN (
                'PENDING_PAYMENT',
                'CONFIRMED',
                'PAYMENT_FAILED',
                'CANCELLED'
            )
        ),

    CONSTRAINT chk_orders_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_orders_subtotal
        CHECK (subtotal >= 0),

    CONSTRAINT chk_orders_total
        CHECK (total >= 0)
);

CREATE INDEX idx_orders_seller_created
    ON orders (seller_id, created_at DESC);

CREATE INDEX idx_orders_seller_status
    ON orders (seller_id, status);
