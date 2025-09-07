--liquibase formatted sql

--changeset staziss:009-update-delivery-points-remove-address
ALTER TABLE delivery_points DROP COLUMN IF EXISTS address;

--changeset staziss:009a-update-delivery-points-latitude-not-null
ALTER TABLE delivery_points ALTER COLUMN latitude SET NOT NULL;

--changeset staziss:009b-update-delivery-points-longitude-not-null
ALTER TABLE delivery_points ALTER COLUMN longitude SET NOT NULL;

--changeset staziss:010-drop-old-delivery-products-table
DROP TABLE IF EXISTS delivery_products;

--changeset staziss:011-create-delivery-point-products-table
CREATE TABLE delivery_point_products (
    id BIGSERIAL PRIMARY KEY,
    delivery_point_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    CONSTRAINT fk_delivery_point_products_delivery_point FOREIGN KEY (delivery_point_id) REFERENCES delivery_points(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_point_products_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_delivery_point_products_quantity CHECK (quantity > 0)
);

--changeset staziss:012-create-new-indexes
CREATE INDEX idx_delivery_point_products_delivery_point_id ON delivery_point_products(delivery_point_id);
CREATE INDEX idx_delivery_point_products_product_id ON delivery_point_products(product_id);

--changeset staziss:013-drop-old-indexes
DROP INDEX IF EXISTS idx_delivery_products_delivery_id;