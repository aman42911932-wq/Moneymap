-- ==========================================================
-- MoneyMap: Personal Finance and Financial Intelligence Platform
-- PostgreSQL Database Schema (12 Tables)
-- ==========================================================

-- Drop tables if they exist (in reverse dependency order)
DROP TABLE IF EXISTS import_transaction CASCADE;
DROP TABLE IF EXISTS import CASCADE;
DROP TABLE IF EXISTS notification CASCADE;
DROP TABLE IF EXISTS recurring_expense CASCADE;
DROP TABLE IF EXISTS savings_goal CASCADE;
DROP TABLE IF EXISTS budget CASCADE;
DROP TABLE IF EXISTS expense CASCADE;
DROP TABLE IF EXISTS income CASCADE;
DROP TABLE IF EXISTS category CASCADE;
DROP TABLE IF EXISTS bank_account CASCADE;
DROP TABLE IF EXISTS admin CASCADE;
DROP TABLE IF EXISTS "user" CASCADE;

-- 1. USER TABLE
CREATE TABLE "user" (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    phone VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- 2. ADMIN TABLE
CREATE TABLE admin (
    admin_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'ADMIN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. BANK_ACCOUNT TABLE
CREATE TABLE bank_account (
    account_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL,
    account_holder_name VARCHAR(100) NOT NULL,
    account_last4 CHAR(4) NOT NULL,
    ifsc_code VARCHAR(20),
    account_type VARCHAR(50) DEFAULT 'Savings',
    opening_balance NUMERIC(12, 2) DEFAULT 0.00,
    is_primary BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. CATEGORY TABLE
CREATE TABLE category (
    category_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('income', 'expense')),
    icon VARCHAR(50) DEFAULT 'tag',
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uq_category_name_type UNIQUE (name, type)
);

-- 5. INCOME TABLE
CREATE TABLE income (
    income_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    account_id INT REFERENCES bank_account(account_id) ON DELETE SET NULL,
    category_id INT REFERENCES category(category_id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    source VARCHAR(100) NOT NULL,
    date DATE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. EXPENSE TABLE
CREATE TABLE expense (
    expense_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    account_id INT REFERENCES bank_account(account_id) ON DELETE SET NULL,
    category_id INT REFERENCES category(category_id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    date DATE NOT NULL,
    time TIME DEFAULT CURRENT_TIME,
    merchant_place VARCHAR(150),
    payment_method VARCHAR(50) DEFAULT 'UPI',
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 7. BUDGET TABLE
CREATE TABLE budget (
    budget_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    category_id INT REFERENCES category(category_id) ON DELETE SET NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_dates CHECK (end_date >= start_date)
);

-- 8. SAVINGS_GOAL TABLE
CREATE TABLE savings_goal (
    goal_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    goal_name VARCHAR(150) NOT NULL,
    target_amount NUMERIC(12, 2) NOT NULL CHECK (target_amount > 0),
    current_amount NUMERIC(12, 2) DEFAULT 0.00,
    target_date DATE,
    status VARCHAR(50) DEFAULT 'In Progress',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. RECURRING_EXPENSE TABLE
CREATE TABLE recurring_expense (
    recurring_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    category_id INT REFERENCES category(category_id) ON DELETE SET NULL,
    title VARCHAR(150) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    frequency VARCHAR(50) NOT NULL DEFAULT 'Monthly',
    start_date DATE NOT NULL,
    next_due_date DATE NOT NULL,
    status VARCHAR(50) DEFAULT 'Active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. NOTIFICATION TABLE
CREATE TABLE notification (
    notification_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    message TEXT NOT NULL,
    type VARCHAR(50) DEFAULT 'budget_alert',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 11. IMPORT TABLE
CREATE TABLE import (
    import_id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    account_id INT REFERENCES bank_account(account_id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50) DEFAULT 'CSV',
    upload_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'pending'
);

-- 12. IMPORT_TRANSACTION TABLE
CREATE TABLE import_transaction (
    transaction_id SERIAL PRIMARY KEY,
    import_id INT NOT NULL REFERENCES import(import_id) ON DELETE CASCADE,
    user_id INT NOT NULL REFERENCES "user"(user_id) ON DELETE CASCADE,
    date DATE NOT NULL,
    description TEXT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    txn_type VARCHAR(20) NOT NULL CHECK (txn_type IN ('debit', 'credit')),
    suggested_category VARCHAR(100),
    final_category_id INT REFERENCES category(category_id) ON DELETE SET NULL,
    status VARCHAR(50) DEFAULT 'pending'
);

-- ==========================================================
-- INDEXES FOR PERFORMANCE OPTIMIZATION
-- ==========================================================
CREATE INDEX idx_user_email ON "user"(email);
CREATE INDEX idx_income_user_date ON income(user_id, date);
CREATE INDEX idx_expense_user_date ON expense(user_id, date);
CREATE INDEX idx_expense_category ON expense(category_id);
CREATE INDEX idx_budget_user ON budget(user_id, start_date, end_date);
CREATE INDEX idx_import_tx_import_id ON import_transaction(import_id);
CREATE INDEX idx_notification_user ON notification(user_id, is_read);
