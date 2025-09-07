--liquibase formatted sql

--changeset staziss:001-create-users-table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT ck_users_role CHECK (role IN ('admin', 'manager', 'courier'))
);

--changeset staziss:002-create-vehicles-table
CREATE TABLE vehicles (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(100) NOT NULL,
    license_plate VARCHAR(20) UNIQUE NOT NULL,
    max_weight DECIMAL(8,2) NOT NULL,
    max_volume DECIMAL(8,3) NOT NULL,
    CONSTRAINT ck_vehicles_max_weight CHECK (max_weight > 0),
    CONSTRAINT ck_vehicles_max_volume CHECK (max_volume > 0)
);

--changeset staziss:003-create-products-table
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    weight DECIMAL(8,3) NOT NULL,
    length DECIMAL(6,2) NOT NULL,
    width DECIMAL(6,2) NOT NULL,
    height DECIMAL(6,2) NOT NULL,
    CONSTRAINT ck_products_weight CHECK (weight > 0),
    CONSTRAINT ck_products_dimensions CHECK (length > 0 AND width > 0 AND height > 0)
);

--changeset staziss:004-create-deliveries-table
CREATE TABLE deliveries (
    id BIGSERIAL PRIMARY KEY,
    courier_id BIGINT,
    vehicle_id BIGINT,
    created_by BIGINT NOT NULL,
    delivery_date DATE NOT NULL,
    time_start TIME NOT NULL,
    time_end TIME NOT NULL,
    status VARCHAR(20) DEFAULT 'planned' NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT fk_deliveries_courier FOREIGN KEY (courier_id) REFERENCES users(id),
    CONSTRAINT fk_deliveries_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id),
    CONSTRAINT fk_deliveries_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT ck_deliveries_status CHECK (status IN ('planned', 'in_progress', 'completed', 'cancelled')),
    CONSTRAINT ck_deliveries_time_order CHECK (time_end > time_start)
);

--changeset staziss:005-create-delivery-points-table
CREATE TABLE delivery_points (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    sequence INT NOT NULL,
    address VARCHAR(500) NOT NULL,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    CONSTRAINT fk_delivery_points_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE,
    CONSTRAINT uk_delivery_points_delivery_sequence UNIQUE (delivery_id, sequence),
    CONSTRAINT ck_delivery_points_sequence CHECK (sequence > 0),
    CONSTRAINT ck_delivery_points_latitude CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT ck_delivery_points_longitude CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180))
);

--changeset staziss:006-create-delivery-products-table
CREATE TABLE delivery_products (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT DEFAULT 1 NOT NULL,
    CONSTRAINT fk_delivery_products_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_products_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_delivery_products_quantity CHECK (quantity > 0)
);

--changeset staziss:007-create-indexes
CREATE INDEX idx_deliveries_date_courier ON deliveries(delivery_date, courier_id);
CREATE INDEX idx_deliveries_status ON deliveries(status);
CREATE INDEX idx_delivery_points_delivery_id ON delivery_points(delivery_id);
CREATE INDEX idx_delivery_products_delivery_id ON delivery_products(delivery_id);
CREATE INDEX idx_users_role ON users(role);

--changeset staziss:008-insert-default-admin
INSERT INTO users (login, password_hash, name, role) VALUES 
('admin', '$2b$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewRZCnvDHb4LqQ8.', 'Системный администратор', 'admin');