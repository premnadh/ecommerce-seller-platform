-- Ensure inventory can be referenced by seller and product together.
ALTER TABLE inventory_items
    ADD CONSTRAINT uq_inventory_seller_product
    UNIQUE (seller_id, product_id);

-- Audit history of every inventory change.
CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,

    seller_id UUID NOT NULL,
    product_id UUID NOT NULL,

    movement_type VARCHAR(30) NOT NULL,

    quantity_on_hand_delta INTEGER NOT NULL DEFAULT 0,
    quantity_reserved_delta INTEGER NOT NULL DEFAULT 0,

    reason VARCHAR(255),
    reference_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movement_inventory
        FOREIGN KEY (seller_id, product_id)
        REFERENCES inventory_items(seller_id, product_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_inventory_movement_type
        CHECK (
            movement_type IN (
                'INITIAL_STOCK',
                'STOCK_ADJUSTMENT',
                'RESERVATION',
                'RELEASE'
            )
        ),

    CONSTRAINT chk_inventory_movement_delta
        CHECK (
            quantity_on_hand_delta <> 0
            OR quantity_reserved_delta <> 0
        )
);

CREATE INDEX idx_inventory_movements_history
    ON inventory_movements(seller_id, product_id, created_at DESC);
