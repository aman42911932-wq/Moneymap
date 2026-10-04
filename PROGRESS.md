# MoneyMap Project Progress & Verification Report

## 1. Project Overview & Decisions Applied
- **Database Schema**: 12 singular-named PostgreSQL tables (`"user"`, `admin`, `bank_account`, `category`, `income`, `expense`, `budget`, `savings_goal`, `recurring_expense`, `notification`, `import`, `import_transaction`). Added `CONSTRAINT uq_category_name_type UNIQUE (name, type)` to `category`.
- **BCrypt Password Hashes**: Configured $2a$12 hashes for `admin@moneymap.com` (`admin123`) and `asha.rao@example.com` (`user123`).
- **Servlet Stack**: Java 17, Jakarta Servlets (Servlet 5.0), Embedded Eclipse Jetty 11 (`11.0.20`), single unified connection pool provider `com.moneymap.config.DBConnection` (HikariCP).
- **Frontend Architecture**: Standardized REST API client (`api.js`) returning normalized `{success, data, error}` envelope; removed silent mock fallbacks; fixed syntax issues in `index.html`.

---

## 2. Completed Phase Status

| Phase | Description | Status | Verification Proof |
|---|---|---|---|
| **Phase 0: Ground Truth & Fixes** | Syntax check on HTML pages, scripts, schema table name consistency | ✅ Done | `node scripts/check_scripts.js` passes all 9 HTML files + JS scripts |
| **Phase 1: Foundation** | pom.xml (Jetty 11 + Jakarta Servlet 5.0), single DBConnection class, JsonUtil ISO date adapters, schema UNIQUE constraint | ✅ Done | `mvn clean compile` succeeds with zero errors |
| **Phase 2: api.js & Contract** | Normalized `{success, data, error}`, offline mode restricted strictly to `?offline=1`, routes aligned with servlets | ✅ Done | Aligned with `API_CONTRACT.md` |
| **Phase 3: Auth & Security** | User & admin login separation, session fixation protection, active user checks on `requireAuth` | ✅ Done | `BaseServlet.java` & `AuthServlet.java` reject deactivated users |
| **Phase 4: CSV Import Pipeline** | Multi-row staging, keyword word-boundary category regex rules, row delete/edit before confirm, single-confirm check | ✅ Done | `StatementParserService.java`, `ImportServlet.java`, `ImportDAO.java` |
| **Phase 5: Core UI & Dashboards** | Live computed bank account balances, budget utilization & alerts, goals, recurring bills, escapeHtml protection | ✅ Done | Handled across all 8 webapp views |
| **Phase 6: Documentation & Tests** | `API_CONTRACT.md`, `README.md`, `PROJECT_DOCUMENTATION.md`, and automated `scripts/smoke_test.js` (27/27 PASS) | ✅ Done | Node.js integration suite with manual cookie tracking |

---

## 3. Files Created, Modified & Deleted

### Created:
- `scripts/check_scripts.js`: Automated syntax checker for inline HTML scripts and standalone JS files.
- `scripts/smoke_test.js`: 27-point end-to-end integration smoke test suite (cross-platform Node.js).
- `scripts/smoke_test.sh`: Bash wrapper test suite.
- `PROGRESS.md`: This progress tracking document.

### Modified:
- `database/schema.sql`: Added `CONSTRAINT uq_category_name_type UNIQUE (name, type)` to `category` table.
- `database/seed_data.sql`: Updated BCrypt password hashes for admin and demo user.
- `src/main/java/com/moneymap/config/DBConnection.java`: Added system property / env variable reading for database credentials.
- `src/main/webapp/index.html`: Fixed `.auth-tab.active` CSS syntax and tab switching.
- `src/main/webapp/js/api.js`: Restricted `isOffline()` strictly to `?offline=1`.
- `src/main/java/com/moneymap/servlet/BaseServlet.java`: Added active user check to `requireAuth`.
- `src/main/java/com/moneymap/servlet/AuthServlet.java`: Added active user verification to `/api/me`.

### Deleted:
- `scripts/smoke_test.sh`: Removed bash test file in favor of cross-platform Node.js test runner.

---

## 4. Execution Step Log
- Item 1: DONE - Removed hard-coded DB password fallback in DBConnection.java, requiring DB_PASSWORD env/system property, and deleted scripts/smoke_test.sh.
- Item 2: DONE - Removed all mockHandler and mock storage from api.js, verified empty states for charts and stats in dashboard.html and admin.html.
