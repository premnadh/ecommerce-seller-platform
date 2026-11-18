-- Allow inventory records to reference products
-- using both seller_id and product_id.
ALTER TABLE products
    ADD CONSTRAINT uq_products_seller_id_id
    UNIQUE (seller_id, id);

-- Current inventory state for each product.
CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,

    seller_id UUID NOT NULL,
    product_id UUID NOT NULL,

    quantity_on_hand INTEGER NOT NULL DEFAULT 0,
    quantity_reserved INTEGER NOT NULL DEFAULT 0,
    reorder_level INTEGER NOT NULL DEFAULT 5,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_inventory_product
        UNIQUE (product_id),

    CONSTRAINT fk_inventory_seller
        FOREIGN KEY (seller_id)
        REFERENCES seller_accounts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_inventory_product_seller
        FOREIGN KEY (seller_id, product_id)
        REFERENCES products(seller_id, id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_inventory_quantity_on_hand
        CHECK (quantity_on_hand >= 0),

    CONSTRAINT chk_inventory_quantity_reserved
        CHECK (
            quantity_reserved >= 0
            AND quantity_reserved <= quantity_on_hand
        ),

    CONSTRAINT chk_inventory_reorder_level
        CHECK (reorder_level >= 0),

    CONSTRAINT chk_inventory_version
        CHECK (version >= 0)
);

CREATE INDEX idx_inventory_seller
    ON inventory_items(seller_id);

CREATE INDEX idx_inventory_low_stock
    ON inventory_items(seller_id, quantity_on_hand);
