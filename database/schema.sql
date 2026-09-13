-- ===================================================================
-- Smart Financial Expense Analysis
-- Database Schema & Master Data Seed Script
-- ===================================================================

CREATE DATABASE IF NOT EXISTS smart_financial_expense
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_financial_expense;

-- -------------------------------------------------------------------
-- 1. Users Table
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 2. Categories Table
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    CONSTRAINT uk_categories_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 3. Payment Options Master Table
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_options_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 4. Expenses Table
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    date DATE NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    INDEX idx_expenses_user_date (user_id, date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 5. Budgets Table
-- -------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS budgets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    month INT NOT NULL,
    year INT NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_budget_user_month_year UNIQUE (user_id, month, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -------------------------------------------------------------------
-- 6. Default Expense Categories Seed
-- -------------------------------------------------------------------
INSERT INTO categories (name, description)
SELECT 'Food', 'Groceries, dining out, restaurants, and food delivery'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Food');

INSERT INTO categories (name, description)
SELECT 'Travel', 'Flights, trains, fuel, taxis, and public transit'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Travel');

INSERT INTO categories (name, description)
SELECT 'Shopping', 'Clothing, electronics, household goods, and personal items'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Shopping');

INSERT INTO categories (name, description)
SELECT 'Bills', 'Electricity, water, internet, phone, and utility bills'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Bills');

INSERT INTO categories (name, description)
SELECT 'Education', 'Tuition, courses, books, and learning resources'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Education');

INSERT INTO categories (name, description)
SELECT 'Entertainment', 'Movies, streaming subscriptions, games, and events'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Entertainment');

INSERT INTO categories (name, description)
SELECT 'Health', 'Medical expenses, pharmacy, insurance, and fitness'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Health');

INSERT INTO categories (name, description)
SELECT 'Rent', 'Monthly apartment or house rent and lease payments'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Rent');

INSERT INTO categories (name, description)
SELECT 'Utilities', 'Gas, water, power, maintenance, and municipal services'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Utilities');

INSERT INTO categories (name, description)
SELECT 'Other', 'Miscellaneous and uncategorized expenses'
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Other');

-- -------------------------------------------------------------------
-- 7. Default Payment Options Seed
-- -------------------------------------------------------------------
INSERT INTO payment_options (name, description, active)
SELECT 'Cash', 'Physical currency and cash transactions', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_options WHERE name = 'Cash');

INSERT INTO payment_options (name, description, active)
SELECT 'UPI', 'Instant unified payments interface (GPay, PhonePe, Paytm, etc.)', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_options WHERE name = 'UPI');

INSERT INTO payment_options (name, description, active)
SELECT 'Credit Card', 'Major credit card networks (Visa, MasterCard, Amex, RuPay)', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_options WHERE name = 'Credit Card');

INSERT INTO payment_options (name, description, active)
SELECT 'Debit Card', 'Bank-issued debit and ATM cards', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_options WHERE name = 'Debit Card');

INSERT INTO payment_options (name, description, active)
SELECT 'Net Banking', 'Direct online net banking portal transfers', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_options WHERE name = 'Net Banking');

INSERT INTO payment_options (name, description, active)
SELECT 'Bank Transfer', 'NEFT, RTGS, and IMPS wire transfers', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_options WHERE name = 'Bank Transfer');

INSERT INTO payment_options (name, description, active)
SELECT 'Other', 'Gift cards, vouchers, wallets, and other payment channels', TRUE
WHERE NOT EXISTS (SELECT 1 FROM payment_options WHERE name = 'Other');

-- -------------------------------------------------------------------
-- 8. Default Administrator Seed
-- Credentials: admin@example.com / Admin@123
-- Password stored strictly as secure BCrypt hash ($2a$10$...)
-- -------------------------------------------------------------------
INSERT INTO users (name, email, password, role, enabled)
SELECT 'Administrator', 'admin@example.com', '$2a$10$QOVBux01SNAs1.XY0cW5HehVIQ78jh5Lnq81gyFEtKJJ5Vtqqki22', 'ADMIN', TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@example.com'
);
