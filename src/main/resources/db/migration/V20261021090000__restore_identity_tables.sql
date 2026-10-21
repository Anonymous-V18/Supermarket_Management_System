-- Flyway Migration V3: Restore Identity Tables (merge identity back into the monolith)
-- Adds user.password, role, user_role, invalidated_token and seeds system roles

-- 1. Add password column to user (MySQL has no ADD COLUMN IF NOT EXISTS)
SET @has_password := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'password'
);
SET @sql := IF(@has_password = 0,
    'ALTER TABLE user ADD COLUMN password VARCHAR(255) NULL AFTER username',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Roles
CREATE TABLE IF NOT EXISTS role (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_role_name UNIQUE (name),
    CONSTRAINT uk_role_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_role (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Logged-out tokens
CREATE TABLE IF NOT EXISTS invalidated_token (
    id VARCHAR(255) NOT NULL,
    expiry_time DATETIME(6) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Seed system roles
INSERT INTO role (id, code, name, created_by, created_date) VALUES
('role-admin-0000000000000000000001', 'ADMIN', 'Administrator', 'FLYWAY', NOW(6)),
('role-employee-00000000000000000001', 'EMPLOYEE', 'Employee', 'FLYWAY', NOW(6)),
('role-storekeeper-000000000000000001', 'STOREKEEPER', 'Storekeeper', 'FLYWAY', NOW(6)),
('role-salesman-00000000000000000001', 'SALESMAN', 'Salesman', 'FLYWAY', NOW(6)),
('role-accounting-000000000000000001', 'ACCOUNTING_STAFF', 'Accounting Staff', 'FLYWAY', NOW(6)),
('role-customer-00000000000000000001', 'CUSTOMER', 'Customer', 'FLYWAY', NOW(6))
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 5. Grant ADMIN role to the seeded admin user (password is set by DatabaseInitializer on startup)
INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id FROM user u JOIN role r ON r.code = 'ADMIN'
WHERE u.username = 'admin1234';
