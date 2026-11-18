-- Prevent the same order from reserving or releasing
-- stock more than once for the same seller and product.

CREATE UNIQUE INDEX uq_inventory_movement_operation
    ON inventory_movements (
        seller_id,
        product_id,
        movement_type,
        reference_id
    )
    WHERE reference_id IS NOT NULL
      AND movement_type IN ('RESERVATION', 'RELEASE');
