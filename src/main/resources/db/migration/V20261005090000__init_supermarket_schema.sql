-- Flyway Migration V1: Initialize Supermarket Management Schema
-- Charset: utf8mb4, Engine: InnoDB

CREATE TABLE IF NOT EXISTS user (
    id VARCHAR(36) NOT NULL,
    username VARCHAR(255) NOT NULL,
    is_active BOOLEAN NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS city (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_city_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS district (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    city_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_district_code UNIQUE (code),
    CONSTRAINT fk_district_city FOREIGN KEY (city_id) REFERENCES city (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ward (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    city_id VARCHAR(36) NULL,
    district_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ward_code UNIQUE (code),
    CONSTRAINT fk_ward_city FOREIGN KEY (city_id) REFERENCES city (id) ON DELETE SET NULL,
    CONSTRAINT fk_ward_district FOREIGN KEY (district_id) REFERENCES district (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS address (
    id VARCHAR(36) NOT NULL,
    street VARCHAR(255) NULL,
    ward_id VARCHAR(36) NULL,
    district_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_address_ward FOREIGN KEY (ward_id) REFERENCES ward (id) ON DELETE SET NULL,
    CONSTRAINT fk_address_district FOREIGN KEY (district_id) REFERENCES district (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS position (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS supplier (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    more_info VARCHAR(255) NULL,
    contact_date DATETIME(6) NULL,
    address_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_supplier_address FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS warehouse (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    phone_number VARCHAR(255) NULL,
    more_info VARCHAR(255) NULL,
    establish_date DATETIME(6) NULL,
    address_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_warehouse_address FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS employee (
    id VARCHAR(36) NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NULL,
    gender VARCHAR(255) NULL,
    dob DATETIME(6) NULL,
    phone_number VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    user_id VARCHAR(36) NULL,
    position_id VARCHAR(36) NULL,
    address_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employee_code UNIQUE (code),
    CONSTRAINT fk_employee_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE SET NULL,
    CONSTRAINT fk_employee_position FOREIGN KEY (position_id) REFERENCES position (id) ON DELETE SET NULL,
    CONSTRAINT fk_employee_address FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS customer (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    gender VARCHAR(255) NULL,
    dob DATETIME(6) NULL,
    phone_number VARCHAR(255) NULL,
    email VARCHAR(255) NULL,
    accumulated_points DOUBLE NULL DEFAULT 0,
    user_id VARCHAR(36) NULL,
    address_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES user (id) ON DELETE SET NULL,
    CONSTRAINT fk_customer_address FOREIGN KEY (address_id) REFERENCES address (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS brand (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_brand_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS unit (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_unit_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_category (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_product_category_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS status_product (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_status_product_name UNIQUE (name),
    CONSTRAINT uk_status_product_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS status_invoice (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    code VARCHAR(255) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_status_invoice_name UNIQUE (name),
    CONSTRAINT uk_status_invoice_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS promotion (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    voucher_code VARCHAR(255) NULL,
    limit_voucher INT NULL,
    description VARCHAR(255) NULL,
    start_date DATETIME(6) NULL,
    end_date DATETIME(6) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NULL,
    description VARCHAR(255) NULL,
    details VARCHAR(255) NULL,
    vat DOUBLE NULL,
    warranty INT NULL,
    image VARCHAR(255) NULL,
    product_category_id VARCHAR(36) NULL,
    unit_id VARCHAR(36) NULL,
    supplier_id VARCHAR(36) NULL,
    brand_id VARCHAR(36) NULL,
    promotion_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_product_category FOREIGN KEY (product_category_id) REFERENCES product_category (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_unit FOREIGN KEY (unit_id) REFERENCES unit (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES brand (id) ON DELETE SET NULL,
    CONSTRAINT fk_product_promotion FOREIGN KEY (promotion_id) REFERENCES promotion (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS warehouse_product (
    id VARCHAR(36) NOT NULL,
    available_quantity INT NULL DEFAULT 0,
    warehouse_id VARCHAR(36) NULL,
    product_id VARCHAR(36) NULL,
    status_product_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_wp_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id) ON DELETE CASCADE,
    CONSTRAINT fk_wp_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE,
    CONSTRAINT fk_wp_status_product FOREIGN KEY (status_product_id) REFERENCES status_product (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice (
    id VARCHAR(36) NOT NULL,
    invoice_create_date DATETIME(6) NULL,
    discount DOUBLE NULL DEFAULT 0,
    total_product INT NULL DEFAULT 0,
    total_price DOUBLE NULL DEFAULT 0,
    status_invoice_id VARCHAR(36) NULL,
    customer_id VARCHAR(36) NULL,
    employee_id VARCHAR(36) NULL,
    warehouse_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_status FOREIGN KEY (status_invoice_id) REFERENCES status_invoice (id) ON DELETE SET NULL,
    CONSTRAINT fk_invoice_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE SET NULL,
    CONSTRAINT fk_invoice_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE SET NULL,
    CONSTRAINT fk_invoice_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_detail (
    id VARCHAR(36) NOT NULL,
    quantity INT NULL DEFAULT 1,
    input_price DOUBLE NULL,
    sale_price DOUBLE NULL,
    promotional_price DOUBLE NULL,
    percent_discount DOUBLE NULL,
    invoice_id VARCHAR(36) NULL,
    product_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_id_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id) ON DELETE CASCADE,
    CONSTRAINT fk_id_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_in (
    id VARCHAR(36) NOT NULL,
    total_product INT NULL DEFAULT 0,
    total_price DOUBLE NULL DEFAULT 0,
    stock_in_date DATETIME(6) NULL,
    status_invoice_id VARCHAR(36) NULL,
    supplier_id VARCHAR(36) NULL,
    warehouse_id VARCHAR(36) NULL,
    employee_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_si_status FOREIGN KEY (status_invoice_id) REFERENCES status_invoice (id) ON DELETE SET NULL,
    CONSTRAINT fk_si_supplier FOREIGN KEY (supplier_id) REFERENCES supplier (id) ON DELETE SET NULL,
    CONSTRAINT fk_si_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id) ON DELETE SET NULL,
    CONSTRAINT fk_si_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_in_detail (
    id VARCHAR(36) NOT NULL,
    quantity INT NULL DEFAULT 1,
    input_price DOUBLE NULL,
    sale_price DOUBLE NULL,
    stock_in_id VARCHAR(36) NULL,
    product_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sid_stock_in FOREIGN KEY (stock_in_id) REFERENCES stock_in (id) ON DELETE CASCADE,
    CONSTRAINT fk_sid_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_out (
    id VARCHAR(36) NOT NULL,
    total_product INT NULL DEFAULT 0,
    total_price DOUBLE NULL DEFAULT 0,
    reason VARCHAR(255) NULL,
    stock_out_date DATETIME(6) NULL,
    status_invoice_id VARCHAR(36) NULL,
    customer_id VARCHAR(36) NULL,
    employee_id VARCHAR(36) NULL,
    warehouse_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_so_status FOREIGN KEY (status_invoice_id) REFERENCES status_invoice (id) ON DELETE SET NULL,
    CONSTRAINT fk_so_customer FOREIGN KEY (customer_id) REFERENCES customer (id) ON DELETE SET NULL,
    CONSTRAINT fk_so_employee FOREIGN KEY (employee_id) REFERENCES employee (id) ON DELETE SET NULL,
    CONSTRAINT fk_so_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS stock_out_detail (
    id VARCHAR(36) NOT NULL,
    quantity INT NULL DEFAULT 1,
    input_price DOUBLE NULL,
    sale_price DOUBLE NULL,
    total_price DOUBLE NULL,
    stock_out_id VARCHAR(36) NULL,
    product_id VARCHAR(36) NULL,
    created_by VARCHAR(255) NOT NULL,
    created_date DATETIME(6) NOT NULL,
    modified_by VARCHAR(255) NULL,
    modified_date DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sod_stock_out FOREIGN KEY (stock_out_id) REFERENCES stock_out (id) ON DELETE CASCADE,
    CONSTRAINT fk_sod_product FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
