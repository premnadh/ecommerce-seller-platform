CREATE TABLE products (
    id UUID PRIMARY KEY,

    seller_id UUID NOT NULL,

    sku VARCHAR(64) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,

    price NUMERIC(12, 2) NOT NULL,
    currency CHAR(3) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_products_seller
        FOREIGN KEY (seller_id)
        REFERENCES seller_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_products_seller_sku
        UNIQUE (seller_id, sku),

    CONSTRAINT chk_products_price
        CHECK (price >= 0),

    CONSTRAINT chk_products_status
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),

    CONSTRAINT chk_products_currency
        CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX idx_products_seller_status
    ON products (seller_id, status);

CREATE INDEX idx_products_seller_created
    ON products (seller_id, created_at DESC);
