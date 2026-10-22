-- ============================================================
--  Inventory Management System — Database Setup Script
--  Run this once in MySQL Workbench before launching the app
-- ============================================================

CREATE DATABASE IF NOT EXISTS inventory_db;
USE inventory_db;

-- ── Users Table ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role     ENUM('Admin', 'Staff', 'Customer') NOT NULL
);

-- ── Products Table ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS products (
    id       INT AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    quantity INT          NOT NULL DEFAULT 0,
    price    DOUBLE       NOT NULL DEFAULT 0.0
);

-- ── Sample Users ─────────────────────────────────────────────────────────────
-- Change passwords before using in production
INSERT INTO users (username, password, role) VALUES
    ('admin',     'admin123',  'Admin'),
    ('staff1',    'staff123',  'Staff'),
    ('customer1', 'cust123',   'Customer')
ON DUPLICATE KEY UPDATE password = VALUES(password), role = VALUES(role);

-- ── Sample Products (optional demo data) ─────────────────────────────────────
INSERT INTO products (name, quantity, price) VALUES
    ('Rice (5kg)',   100, 4.99),
    ('Cooking Oil',  50, 2.49),
    ('Sugar (1kg)',  75, 1.25),
    ('Wheat Flour',  60, 3.00),
    ('Salt (500g)',  90, 0.75);
