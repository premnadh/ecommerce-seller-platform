CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(64) NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    quantity INTEGER NOT NULL,
    line_total NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_order_items_order_product
        UNIQUE (order_id, product_id),

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (seller_id, order_id)
        REFERENCES orders(seller_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (seller_id, product_id)
        REFERENCES products(seller_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_order_items_unit_price
        CHECK (unit_price >= 0),

    CONSTRAINT chk_order_items_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_order_items_line_total
        CHECK (
            line_total >= 0
            AND line_total = unit_price * quantity
        )
);

CREATE INDEX idx_order_items_order
    ON order_items (seller_id, order_id);

CREATE INDEX idx_order_items_product
    ON order_items (seller_id, product_id);
