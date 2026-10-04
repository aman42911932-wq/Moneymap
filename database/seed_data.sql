-- ==========================================================
-- MoneyMap: Initial Seed Data
-- ==========================================================

-- 1. DEFAULT CATEGORIES (Expense Categories)
INSERT INTO category (name, type, icon, is_active) VALUES
('Groceries', 'expense', 'shopping-cart', TRUE),
('Food & Dining', 'expense', 'utensils', TRUE),
('Rent & Housing', 'expense', 'home', TRUE),
('Utilities & Bills', 'expense', 'bolt', TRUE),
('Transportation', 'expense', 'car', TRUE),
('Shopping', 'expense', 'bag-shopping', TRUE),
('Entertainment', 'expense', 'film', TRUE),
('Healthcare', 'expense', 'heart-pulse', TRUE),
('Education', 'expense', 'graduation-cap', TRUE),
('Travel', 'expense', 'plane', TRUE),
('Personal Care', 'expense', 'spa', TRUE),
('Miscellaneous', 'expense', 'circle-question', TRUE);

-- DEFAULT CATEGORIES (Income Categories)
INSERT INTO category (name, type, icon, is_active) VALUES
('Salary', 'income', 'briefcase', TRUE),
('Freelance & Consulting', 'income', 'laptop-code', TRUE),
('Investments & Dividends', 'income', 'chart-line', TRUE),
('Rental Income', 'income', 'building', TRUE),
('Gifts & Grants', 'income', 'gift', TRUE),
('Other Income', 'income', 'wallet', TRUE);

-- 2. DEMO ADMINISTRATOR (Password: admin123 -> BCrypt hashed)
INSERT INTO admin (name, email, password_hash, role) VALUES
('System Administrator', 'admin@moneymap.com', '$2a$12$JgLyEakajJQAHYDG6pxW2.M.4rsMznzn9MP92A1Fn0kw1Nf2eD09q', 'SUPER_ADMIN');

-- 3. DEMO USER (Matching Object Diagram: Asha Rao, Password: user123 -> BCrypt hashed)
INSERT INTO "user" (user_id, name, email, phone, password_hash, is_active) VALUES
(1, 'Asha Rao', 'asha.rao@example.com', '+919876543210', '$2a$12$1Rxy7fFHsL9a2UQ.Aq86teEsl9O4bn0AgMHbNA0v2sfJzNU2z33v6', TRUE);

-- Synchronize sequence
SELECT setval('user_user_id_seq', (SELECT MAX(user_id) FROM "user"));

-- 4. DEMO BANK ACCOUNT (Matching Object Diagram: SBI, last 4821, Opening Balance 25,000)
INSERT INTO bank_account (account_id, user_id, bank_name, account_holder_name, account_last4, ifsc_code, account_type, opening_balance, is_primary, is_active) VALUES
(1, 1, 'State Bank of India', 'Asha Rao', '4821', 'SBIN0001234', 'Savings', 25000.00, TRUE, TRUE);

SELECT setval('bank_account_account_id_seq', (SELECT MAX(account_id) FROM bank_account));

-- 5. DEMO INCOME (Matching Object Diagram: Salary 50,000)
INSERT INTO income (user_id, account_id, category_id, amount, source, date, description) VALUES
(1, 1, 13, 50000.00, 'Salary', '2026-09-30', 'Monthly Software Engineer Salary');

-- 6. DEMO EXPENSE (Matching Object Diagram: Groceries 850 via UPI)
INSERT INTO expense (user_id, account_id, category_id, amount, date, time, merchant_place, payment_method, description) VALUES
(1, 1, 1, 850.00, '2026-10-01', '14:30:00', 'Supermarket', 'UPI', 'Weekly grocery essentials'),
(1, 1, 2, 420.00, '2026-10-01', '19:15:00', 'Swiggy', 'UPI', 'Dinner order'),
(1, 1, 5, 250.00, '2026-10-02', '09:00:00', 'Uber', 'UPI', 'Ride to office');

-- 7. DEMO BUDGET (Matching Object Diagram: 8,000 for October)
INSERT INTO budget (user_id, category_id, amount, start_date, end_date) VALUES
(1, 1, 8000.00, '2026-10-01', '2026-10-31'),
(1, 2, 5000.00, '2026-10-01', '2026-10-31');

-- 8. DEMO SAVINGS GOAL (Matching Object Diagram: Laptop, target 60,000, current 18,000)
INSERT INTO savings_goal (user_id, goal_name, target_amount, current_amount, target_date, status) VALUES
(1, 'Laptop', 60000.00, 18000.00, '2026-12-31', 'In Progress'),
(1, 'Emergency Fund', 100000.00, 45000.00, '2027-06-30', 'In Progress');

-- 9. DEMO RECURRING EXPENSE
INSERT INTO recurring_expense (user_id, category_id, title, amount, frequency, start_date, next_due_date, status) VALUES
(1, 4, 'Internet Broadband', 999.00, 'Monthly', '2026-09-01', '2026-10-10', 'Active'),
(1, 7, 'Netflix Subscription', 649.00, 'Monthly', '2026-09-15', '2026-10-15', 'Active');

-- 10. DEMO NOTIFICATION
INSERT INTO notification (user_id, message, type, is_read) VALUES
(1, 'Welcome to MoneyMap! Your personal finance hub is ready.', 'system', TRUE),
(1, 'Reminder: Internet Broadband bill of ₹999 is due on Oct 10.', 'bill_reminder', FALSE);
