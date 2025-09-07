--liquibase formatted sql

--changeset staziss:014-update-admin-password
-- Update admin password to "admin123" - BCrypt hash generated with rounds 12
UPDATE users 
SET password_hash = '$2a$10$z1azzGeYiaHewbX.R5XQb.9WzRldo.ER6S749OswSTtGh.E.FORSG'
WHERE login = 'admin';