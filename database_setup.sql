-- ============================================================
--  Inventory Management System — Database Setup Script
--  Run this once in MySQL Workbench before launching the app
-- ============================================================

CREATE DATABASE IF NOT EXISTS inventory_db;
USE inventory_db;

-- Configure credentials with environment variables or a private
-- inventory-db.properties file in the project folder. See
-- inventory-db.properties.example for the property names and URL format.

-- ── Users Table ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role     ENUM('Admin', 'Staff') NOT NULL
);

ALTER TABLE users MODIFY password VARCHAR(255) NOT NULL;

-- ── Products Table ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    quantity INT          NOT NULL DEFAULT 0,
    price    DOUBLE       NOT NULL DEFAULT 0.0,
    CONSTRAINT chk_products_quantity_nonnegative CHECK (quantity >= 0)
);

SET @quantity_constraint_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'products'
      AND CONSTRAINT_NAME = 'chk_products_quantity_nonnegative'
);
SET @add_quantity_constraint = IF(
    @quantity_constraint_exists = 0,
    'ALTER TABLE products ADD CONSTRAINT chk_products_quantity_nonnegative CHECK (quantity >= 0)',
    'SELECT 1'
);
PREPARE quantity_constraint_statement FROM @add_quantity_constraint;
EXECUTE quantity_constraint_statement;
DEALLOCATE PREPARE quantity_constraint_statement;

INSERT INTO users (username, password, role) VALUES
    ('admin', 'pbkdf2_sha256$210000$RdLeCCk5F1UWim4Bn+9gMw==$2scQxuNUZuQ6xqgU4px9Ffx1xVcBbS5ePrivzhX4SYk=', 'Admin'),
    ('staff1', 'pbkdf2_sha256$210000$FWqIvPheh22StbCHNmhEXg==$gkuVNX2WzdNT0FoRASeg3rQiG2he1HWQ5uu7f95ffiI=', 'Staff')
ON DUPLICATE KEY UPDATE role = VALUES(role);

INSERT INTO products (name, quantity, price) VALUES
    ('Rice (5kg)',   100, 4.99),
    ('Cooking Oil',  50, 2.49),
    ('Sugar (1kg)',  75, 1.25),
    ('Wheat Flour',  60, 3.00),
    ('Salt (500g)',  90, 0.75);
