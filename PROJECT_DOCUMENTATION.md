# MoneyMap — Technical Project Documentation

## 1. System Overview & Architecture

**MoneyMap** is a personal finance management web application designed to track income & expenses, import and auto-categorize bank statements, monitor category and overall budgets with automatic threshold alerts, track savings goals, and manage scheduled recurring expenses.

### Technology Stack
- **Frontend**: HTML5, Vanilla CSS3 (Glassmorphic dark design system), Vanilla JavaScript (ES6+), Chart.js (v4.4.1).
- **Backend**: Java 17 LTS, Jakarta EE 9 (Servlet 5.0), Embedded Eclipse Jetty 11.0.20.
- **Database & Persistence**: PostgreSQL with HikariCP 5.1.0 connection pooling and pure JDBC (`PreparedStatement`).
- **Serialization & Utilities**: Google Gson 2.11.0 (with custom `LocalDate` and `LocalTime` adapters), jBCrypt 0.4 for password hashing.
- **Build System**: Apache Maven 3.9+.

```
┌────────────────────────────────────────────────────────┐
│               Frontend (src/main/webapp)               │
│   HTML5 Pages  │  CSS Glassmorphism  │  api.js / app.js│
└───────────────────────────┬────────────────────────────┘
                            │ HTTP JSON / Sessions
┌───────────────────────────▼────────────────────────────┐
│          Embedded Jetty 11 (com.moneymap.Server)       │
│                  Jakarta Servlets (/api/*)             │
│    AuthServlet, ExpenseServlet, IncomeServlet, etc.   │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                  Service & DAO Layer                   │
│   AuthService, BudgetService, StatementParserService   │
│                 HikariCP Connection Pool               │
└───────────────────────────┬────────────────────────────┘
                            │ JDBC SQL
┌───────────────────────────▼────────────────────────────┐
│             PostgreSQL Database (12 Tables)            │
│   "user", admin, bank_account, category, income,       │
│   expense, budget, savings_goal, recurring_expense,    │
│   notification, import, import_transaction             │
└────────────────────────────────────────────────────────┘
```

---

## 2. Database Schema (12 Singular Tables)

The database follows a normalized relational structure:

1. **`"user"`**: User accounts (`user_id`, `name`, `email`, `password_hash`, `status`, `created_at`, `updated_at`).
2. **`admin`**: Administrative staff accounts (`admin_id`, `name`, `email`, `password_hash`, `role`, `created_at`).
3. **`bank_account`**: Linked user bank accounts (`account_id`, `user_id`, `bank_name`, `account_number`, `ifsc_code`, `account_type`, `opening_balance`, `is_primary`, `status`, `created_at`).
   - *Computed Balance*: Live balance is calculated directly in SQL as:
     `opening_balance + COALESCE(SUM(income), 0) - COALESCE(SUM(expense), 0)`.
4. **`category`**: Income and Expense categories (`category_id`, `name`, `type` ['Income'|'Expense'], `icon`, `color`, `is_system`, `user_id`, `status`).
   - Master Categories (10 Seeded): *Salary, Freelance, Investments, Food & Dining, Transportation, Groceries, Utilities, Entertainment, Shopping, Healthcare*.
5. **`income`**: Inflow records (`income_id`, `user_id`, `account_id`, `category_id`, `amount`, `income_date`, `description`, `created_at`).
6. **`expense`**: Outflow records (`expense_id`, `user_id`, `account_id`, `category_id`, `amount`, `expense_date`, `description`, `payment_method`, `created_at`).
7. **`budget`**: Spending limits (`budget_id`, `user_id`, `category_id` [NULL for overall], `month_year` [YYYY-MM], `amount`, `created_at`, `updated_at`).
8. **`savings_goal`**: Financial milestones (`goal_id`, `user_id`, `name`, `target_amount`, `current_amount`, `target_date`, `status` ['In Progress'|'Completed'|'Cancelled'], `created_at`).
9. **`recurring_expense`**: Periodic recurring bills (`recurring_id`, `user_id`, `category_id`, `account_id`, `amount`, `frequency` ['Monthly'|'Weekly'|'Yearly'], `start_date`, `next_due_date`, `status` ['Active'|'Paused'], `created_at`).
10. **`notification`**: User alerts (`notification_id`, `user_id`, `message`, `type` ['budget_alert'|'goal_alert'|'system'], `is_read`, `created_at`).
11. **`import`**: Bank statement upload sessions (`import_id`, `user_id`, `account_id`, `filename`, `total_rows`, `imported_rows`, `status` ['Staged'|'Confirmed'|'Failed'], `created_at`).
12. **`import_transaction`**: Staged statement rows (`transaction_id`, `import_id`, `txn_date`, `description`, `amount`, `type` ['debit'|'credit'], `suggested_category_id`, `status` ['Staged'|'Imported'|'Skipped']).

---

## 3. Security & Session Management

- **Password Storage**: All passwords hashed using `BCrypt` with standard work factor 10.
- **Authentication**: HTTP Session (`HttpSession`) maintains `user_id`, `admin_id`, and `role`.
  - User Login: `/api/auth/login`
  - Admin Login: `/api/auth/admin-login`
  - Logout: `/api/auth/logout` (invalidates session on server and clears browser state).
  - Session Verification: `/api/me` checks session validity and returns authenticated identity.
- **XSS Sanitization**: Frontend uses `escapeHtml()` utility function across all dynamic HTML insertions.

---

## 4. REST API Endpoint Summary

All responses conform to the standard response envelope:
```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

| Method | Route | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authenticate standard user |
| `POST` | `/api/auth/admin-login` | Authenticate administrator |
| `POST` | `/api/auth/register` | Register new user |
| `POST` | `/api/auth/logout` | Invalidate current session |
| `GET` | `/api/me` | Current authenticated session info |
| `GET` | `/api/expenses` | List user expenses (optional month/category filter) |
| `POST` | `/api/expenses` | Create an expense |
| `DELETE` | `/api/expenses?id={id}` | Delete an expense |
| `GET` | `/api/income` | List user income (optional month filter) |
| `POST` | `/api/income` | Create an income entry |
| `DELETE` | `/api/income?id={id}` | Delete an income entry |
| `GET` | `/api/reports/summary` | KPI cards (total income, expense, balance, savings rate) |
| `GET` | `/api/reports/by-category` | Category spending breakdown for Chart.js |
| `GET` | `/api/reports/monthly-comparison` | Monthly income vs expense trends |
| `GET` | `/api/reports/insights` | Financial insights and savings tips |
| `GET` | `/api/accounts` | List linked bank accounts with live computed balance |
| `POST` | `/api/accounts` | Link a new bank account |
| `PUT` | `/api/accounts/{id}/primary` | Designate primary account |
| `GET` | `/api/budgets` | List budgets with computed `currentSpent` and `percentageSpent` |
| `POST` | `/api/budgets` | Upsert category or overall budget |
| `GET` | `/api/goals` | List user savings goals |
| `POST` | `/api/goals` | Create savings goal |
| `POST` | `/api/goals/{id}/contribute` | Deposit contribution towards goal |
| `GET` | `/api/recurring` | List recurring bills |
| `POST` | `/api/recurring` | Create recurring bill |
| `POST` | `/api/imports/upload` | Upload CSV and return staged transactions |
| `PUT` | `/api/imports/{id}/transactions/{txId}` | Update staged row category |
| `DELETE` | `/api/imports/{id}/transactions/{txId}` | Remove staged row |
| `POST` | `/api/imports/{id}/confirm` | Commit staged rows to income/expense |
| `GET` | `/api/notifications` | List user notifications |
| `GET` | `/api/notifications/count` | Unread notifications count |
| `PUT` | `/api/notifications/{id}/read` | Mark notification as read |
| `GET` | `/api/admin/users` | Admin: List all registered users |
| `PUT` | `/api/admin/users/{id}/toggle-status` | Admin: Activate/deactivate user |
| `GET` | `/api/admin/categories` | Admin: List categories with toggle status |
| `POST` | `/api/admin/categories` | Admin: Add master category |
| `GET` | `/api/admin/stats` | Admin: Platform statistics |

---

## 5. Bank Statement CSV Import Flow

1. **Upload**: User uploads a standard CSV file (or tests with `sample_bank_statement.csv`).
2. **Rule-based Auto-Categorization**: `StatementParserService` applies word-boundary regular expressions (`\b(swiggy|zomato)\b`, `\b(blinkit|dmart)\b`, `\b(uber|ola)\b`, `\b(freelance|upwork)\b`, etc.) to prevent false-positive substring matches.
3. **Staged Review**: User can modify suggested categories or remove individual rows.
4. **Confirmation**:
   - Debits are inserted into `expense` table.
   - Credits are inserted into `income` table.
   - Associated `account_id` is linked.
   - Budget threshold checks trigger automatically (creating notifications if $\ge 80\%$ or $\ge 100\%$).
   - Import session marked as `Confirmed`.

---

## 6. Setup & Execution

### Prerequisites
- Java Development Kit (JDK) 17+
- Apache Maven 3.9+
- PostgreSQL 13+

### Step-by-Step Run
```bash
# 1. Initialize PostgreSQL database
psql -U postgres -c "CREATE DATABASE moneymap;"
psql -U postgres -d moneymap -f database/schema.sql
psql -U postgres -d moneymap -f database/seed_data.sql

# 2. Build and run the embedded Jetty server
mvn clean compile exec:java
```

Access the application in any modern web browser at:  
👉 **`http://localhost:8080/index.html`**
