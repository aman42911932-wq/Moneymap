# MoneyMap — Personal Finance & Financial Intelligence Platform

MoneyMap is a centralized personal finance management system designed to track income & expenses, import bank statements with rule-based auto-categorization, manage budgets with overspending alerts, track savings goals, and manage recurring commitments.

---

## 🛠️ Technology Architecture

- **Backend:** Java 17 Jakarta Servlets (Servlet 5.0), Embedded Eclipse Jetty 11, JDBC PreparedStatements, Session Management, HikariCP, Gson, BCrypt
- **Database:** PostgreSQL (12 Relational Singular Tables, Foreign Key Constraints, Indexes, Seed Data)
- **Frontend:** HTML5, CSS3 (Glassmorphic Financial Dashboard Theme), Modern JavaScript (ES6+), Chart.js Data Visualizations
- **Build System:** Apache Maven 3.9+

---

## 📁 Project Structure

```text
MONEYMAP PROJECT/
├── database/
│   ├── schema.sql                 # 12 PostgreSQL tables ("user", bank_account, expense, etc.)
│   └── seed_data.sql              # Master categories (10), demo users with BCrypt hashes
│
├── src/main/java/com/moneymap/
│   ├── Server.java                # Embedded Jetty 11 Server Runner
│   ├── config/
│   │   └── DBConnection.java      # PostgreSQL HikariCP Connection Pool Provider
│   ├── model/                     # 12 POJO Entity Classes (User, BankAccount, Expense, etc.)
│   ├── dao/                       # Data Access Objects with computed balances & budget upsert
│   ├── service/                   # Business Logic (StatementParserService, AuthService, BudgetService)
│   ├── servlet/                   # Jakarta Servlets (/api/auth, /api/expenses, /api/reports, etc.)
│   └── util/                      # JsonUtil (LocalDate/Time TypeAdapters), BCryptGenerator
│
├── src/main/webapp/
│   ├── index.html                 # Login & Registration Portal
│   ├── dashboard.html             # Overview Dashboard with KPI cards & Chart.js
│   ├── transactions.html          # Manual Entry, Search, Filtering, and Deletion
│   ├── statement-import.html      # Staged Bank Statement Upload & Review Table
│   ├── budgets.html               # Live Budget Utilization Bars & Alert Badges
│   ├── goals.html                 # Savings Milestones & Contribution Tracker
│   ├── recurring.html             # Scheduled Recurring Bills & Next Due Tracker
│   ├── bank-accounts.html         # Linked Bank Cards & Computed Balance
│   ├── admin.html                 # Administrator User & Category Console
│   ├── css/
│   │   └── styles.css             # Glassmorphic Responsive Stylesheet
│   └── js/
│       ├── api.js                 # Standardized REST API client ({success, data, error})
│       └── app.js                 # Session manager, escapeHtml, live notification bell
│
├── API_CONTRACT.md                # Standardized REST API specification
├── PROJECT_DOCUMENTATION.md       # Full technical architecture and system guide
├── sample_bank_statement.csv      # Ready-to-test bank statement file
└── pom.xml                        # Maven configuration with Java 17, Jetty 11 & PostgreSQL JDBC
```

---

## 🚀 How to Run the Application

### 1. Initialize the PostgreSQL Database
Open your terminal or `psql`:
```bash
# Create database
psql -U postgres -c "CREATE DATABASE moneymap;"

# Run the 12-table schema
psql -U postgres -d moneymap -f database/schema.sql

# Insert master categories and demo seed data (BCrypt hashed passwords)
psql -U postgres -d moneymap -f database/seed_data.sql
```

### 2. Configure Database Credentials (if needed)
In [`src/main/java/com/moneymap/config/DBConnection.java`](src/main/java/com/moneymap/config/DBConnection.java) or via environment variables:
- `DB_URL`: `jdbc:postgresql://localhost:5432/moneymap`
- `DB_USER`: `postgres`
- `DB_PASSWORD`: `your_password`

### 3. Compile & Start the Embedded Java Server
```bash
mvn clean compile exec:java
```

The server will start at:  
👉 **`http://localhost:8080/index.html`**

---

## 🔑 Demo Credentials

| Role | Email | Password | Purpose |
|---|---|---|---|
| **Registered User** | `asha.rao@example.com` | `user123` | Full personal finance portal |
| **Administrator** | `admin@moneymap.com` | `admin123` | User directory & master categories |

---

## 🌟 Key Features

1. **Statement Import & Staging Pipeline**:
   - Upload CSV statements or test with `sample_bank_statement.csv`.
   - Accurate regex word-boundary auto-categorization without substring false-positives (`Swiggy` $\rightarrow$ *Food & Dining*, `Blinkit` $\rightarrow$ *Groceries*, `Uber` $\rightarrow$ *Transportation*, `Freelance` $\rightarrow$ *Freelance*).
   - In-line category editing and row deletion before confirmation.
   - Debits committed to `expense`, credits to `income`, and real-time budget overspending notifications triggered.
2. **Budgets & Overspending Alerts**:
   - Category-wise and overall monthly spending limits with percentage utilization bars.
   - Real-time notification checks at 80% (warning) and 100% (exceeded).
3. **Savings Goals & Contributions**:
   - Milestone tracking with progress percentage and interactive deposit contributions.
4. **Bank Account Management**:
   - Computed live current balance: `opening_balance + total_income - total_expenses`.
   - 1-click primary account designation.
5. **Admin Portal**:
   - Manage registered user statuses (Activate / Deactivate) and custom/master categories.
