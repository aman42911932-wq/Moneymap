# MoneyMap — RESTful API Contract & Specification

This document details the standardized REST API contract for the MoneyMap application.
All responses are formatted using a standard JSON envelope:

```json
{
  "success": true,
  "data": { ... },
  "message": "Optional success message",
  "error": null
}
```

In case of error:

```json
{
  "success": false,
  "data": null,
  "error": "Descriptive error message"
}
```

---

## 1. Authentication (`/api/auth` & `/api/me`)

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| `POST` | `/api/auth/login` | User login (creates session) | `{"email": "...", "password": "..."}` |
| `POST` | `/api/auth/admin-login` | Admin login (creates session) | `{"email": "...", "password": "..."}` |
| `POST` | `/api/auth/register` | User registration | `{"name": "...", "email": "...", "phone": "...", "password": "..."}` |
| `POST` | `/api/auth/logout` | Invalidate current session | *None* |
| `GET` | `/api/me` | Get current authenticated user profile | *None* |

---

## 2. Transactions (Expenses & Income)

| Method | Endpoint | Description | Parameters / Body |
|---|---|---|---|
| `GET` | `/api/expenses` | List expenses (with categoryName & bankName) | `?startDate=...&endDate=...&categoryId=...` |
| `POST` | `/api/expenses` | Add an expense (checks budgets & alerts) | `{"amount": 420.00, "date": "2026-10-01", "categoryName": "Food & Dining", "merchantPlace": "Swiggy", "paymentMethod": "UPI", "accountId": 1, "description": "Dinner"}` |
| `DELETE`| `/api/expenses/{id}` | Delete an expense | Path parameter `{id}` |
| `GET` | `/api/income` | List incomes (with categoryName & bankName) | `?startDate=...&endDate=...&categoryId=...` |
| `POST` | `/api/income` | Add an income | `{"amount": 50000.00, "date": "2026-09-30", "source": "Salary", "categoryName": "Salary", "accountId": 1, "description": "Monthly Salary"}` |
| `DELETE`| `/api/income/{id}` | Delete an income | Path parameter `{id}` |

---

## 3. Reports & Analytics (`/api/reports`)

| Method | Endpoint | Description | Query Parameters |
|---|---|---|---|
| `GET` | `/api/reports/summary` | KPI totals: totalIncome, totalExpense, netSavings, savingsRate, counts | `?startDate=...&endDate=...` |
| `GET` | `/api/reports/by-category` | Category-wise expense breakdown with percentages and icons | `?startDate=...&endDate=...` |
| `GET` | `/api/reports/monthly-comparison` | Monthly income vs expense over last 6 months for Chart.js | `?year=2026` |
| `GET` | `/api/reports/insights` | Smart financial insights & top spending categories | *None* |

---

## 4. Bank Accounts (`/api/accounts`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `GET` | `/api/accounts` | List accounts with calculated `currentBalance` | *None* |
| `POST` | `/api/accounts` | Add a bank account | `{"bankName": "HDFC", "accountHolderName": "...", "accountLast4": "1234", "ifscCode": "HDFC0001", "openingBalance": 10000.00, "isPrimary": false}` |
| `PUT` | `/api/accounts/{id}/primary`| Designate account as primary | Path parameter `{id}` |

---

## 5. Budgets (`/api/budgets`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `GET` | `/api/budgets` | List budgets with `currentSpent` and `percentageSpent` | *None* |
| `POST` | `/api/budgets` | Upsert a budget (overall or per category) | `{"categoryName": "Groceries", "amount": 8000.00, "startDate": "2026-10-01", "endDate": "2026-10-31"}` |

---

## 6. Savings Goals (`/api/goals`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `GET` | `/api/goals` | List savings goals and milestone percentages | *None* |
| `POST` | `/api/goals` | Create a savings goal | `{"goalName": "Laptop", "targetAmount": 60000.00, "currentAmount": 18000.00, "targetDate": "2026-12-31"}` |
| `POST` | `/api/goals/{id}/contribute` | Add contribution to a savings goal | `{"goalId": 1, "amount": 2000.00}` |

---

## 7. Recurring Commitments (`/api/recurring`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `GET` | `/api/recurring` | List scheduled recurring bills and next due date | *None* |
| `POST` | `/api/recurring` | Add a recurring expense | `{"title": "Internet", "amount": 999.00, "categoryName": "Utilities & Bills", "frequency": "Monthly", "nextDueDate": "2026-10-10"}` |

---

## 8. Master Categories (`/api/categories`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `GET` | `/api/categories` | List active categories | `?type=expense` or `?type=income` |
| `POST` | `/api/categories` | Create custom category | `{"name": "...", "type": "expense", "icon": "tag"}` |

---

## 9. Notifications (`/api/notifications`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `GET` | `/api/notifications` | List user notifications | *None* |
| `GET` | `/api/notifications/count`| Get unread notifications count | *None* |
| `PUT` | `/api/notifications/{id}/read`| Mark notification as read | Path parameter `{id}` |

---

## 10. Statement Import Pipeline (`/api/imports`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `POST` | `/api/imports/upload` | Upload & parse CSV statement, stages rows | `{"fileName": "statement.csv", "fileContent": "...", "accountId": 1}` |
| `GET` | `/api/imports/{id}/transactions`| Get staged transactions for import review | Path parameter `{id}` |
| `PUT` | `/api/imports/transactions/{id}/category`| Update category on staged transaction | `{"transactionId": 1, "categoryId": 2, "categoryName": "Food & Dining"}` |
| `DELETE`| `/api/imports/transactions/{id}`| Remove staged transaction before confirmation | Path parameter `{id}` |
| `POST` | `/api/imports/{id}/confirm` | Confirm staged rows into Income & Expense tables | `{"importId": 1}` |

---

## 11. Administration Console (`/api/admin`)

| Method | Endpoint | Description | Body / Parameters |
|---|---|---|---|
| `GET` | `/api/admin/users` | List all users and account statuses | *None* (Requires ADMIN role) |
| `PUT` | `/api/admin/users/{id}/status` | Activate or deactivate a user | `{"isActive": false}` |
| `GET` | `/api/admin/categories`| List all master categories | *None* |
| `PUT` | `/api/admin/categories/{id}/status`| Enable or disable category | `{"isActive": false}` |
| `POST` | `/api/admin/categories` | Create a new master category | `{"name": "...", "type": "expense", "icon": "tag"}` |
| `GET` | `/api/admin/stats` | System KPI totals (users, transactions, categories) | *None* |
