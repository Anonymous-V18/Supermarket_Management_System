-- Flyway Migration V2: Seed Supermarket Master Data
-- Idempotent inserts with ON DUPLICATE KEY UPDATE / INSERT IGNORE

-- 1. Seed Positions
INSERT INTO position (id, name, created_by, created_date) VALUES
('pos-manager-000000000000000000001', 'Quản lý chi nhánh', 'FLYWAY', NOW(6)),
('pos-storekeeper-00000000000000001', 'Nhân viên quản lý kho', 'FLYWAY', NOW(6)),
('pos-salesman-00000000000000000001', 'Nhân viên thu ngân bán hàng', 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. Seed Status Invoice
INSERT INTO status_invoice (id, code, name, created_by, created_date) VALUES
('st-inv-pending-000000000000000001', 'PENDING', 'Chờ xử lý', 'FLYWAY', NOW(6)),
('st-inv-completed-0000000000000001', 'COMPLETED', 'Đã hoàn thành', 'FLYWAY', NOW(6)),
('st-inv-cancelled-0000000000000001', 'CANCELLED', 'Đã hủy', 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 3. Seed Status Product
INSERT INTO status_product (id, code, name, created_by, created_date) VALUES
('st-prod-active-000000000000000001', 'ACTIVE', 'Đang kinh doanh', 'FLYWAY', NOW(6)),
('st-prod-inactive-000000000000001', 'INACTIVE', 'Ngừng kinh doanh', 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 4. Seed Units
INSERT INTO unit (id, code, name, created_by, created_date) VALUES
('unit-cai-000000000000000000000001', 'CAI', 'Cái', 'FLYWAY', NOW(6)),
('unit-hop-000000000000000000000001', 'HOP', 'Hộp', 'FLYWAY', NOW(6)),
('unit-chai-00000000000000000000001', 'CHAI', 'Chai', 'FLYWAY', NOW(6)),
('unit-kg-0000000000000000000000001', 'KG', 'Kilogram', 'FLYWAY', NOW(6)),
('unit-goi-000000000000000000000001', 'GOI', 'Gói', 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 5. Seed Product Categories
INSERT INTO product_category (id, code, name, created_by, created_date) VALUES
('cat-tp-00000000000000000000000001', 'THUC_PHAM', 'Thực phẩm & Đồ uống', 'FLYWAY', NOW(6)),
('cat-dg-00000000000000000000000001', 'DO_GIA_DUNG', 'Đồ gia dụng & Tiện ích', 'FLYWAY', NOW(6)),
('cat-mp-00000000000000000000000001', 'MY_PHAM', 'Hóa mỹ phẩm & Chăm sóc cá nhân', 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 6. Seed Admin User & Default Employee
-- IAM identity handles credentials; local user stores projection
INSERT INTO user (id, username, is_active, created_by, created_date) VALUES
('usr-admin-sp-00000000000000000001', 'admin1234', TRUE, 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE is_active = VALUES(is_active);

-- Create Default Administrator Employee Profile
INSERT INTO employee (id, code, name, gender, dob, phone_number, email, user_id, position_id, created_by, created_date) VALUES
('emp-admin-00000000000000000000001', 'EMP_ADMIN', 'System Administrator', 'Nam', '1995-01-01 00:00:00.000000', '0999999999', 'admin@supermarket.com', 'usr-admin-sp-00000000000000000001', 'pos-manager-000000000000000000001', 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);
