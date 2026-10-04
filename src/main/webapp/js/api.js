/**
 * MoneyMap Universal API Client
 * Clean RESTful interface for MoneyMap Jakarta Servlets.
 */
const API = {
    baseUrl: '/api',

    getCurrentUser() {
        const stored = localStorage.getItem('mm_user');
        return stored ? JSON.parse(stored) : null;
    },

    setCurrentUser(user) {
        localStorage.setItem('mm_user', JSON.stringify(user));
    },

    clearCurrentUser() {
        localStorage.removeItem('mm_user');
    },

    async request(endpoint, options = {}) {
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {})
        };

        try {
            const resp = await fetch(`${this.baseUrl}${endpoint}`, {
                ...options,
                headers
            });

            let json;
            try {
                json = await resp.json();
            } catch (err) {
                json = null;
            }

            if (resp.ok && json) {
                return {
                    success: json.success !== undefined ? json.success : true,
                    data: json.data !== undefined ? json.data : json,
                    error: json.error || null,
                    message: json.message || null
                };
            } else {
                return {
                    success: false,
                    data: null,
                    error: (json && json.error) ? json.error : `HTTP ${resp.status}: ${resp.statusText || 'Request failed'}`
                };
            }
        } catch (e) {
            return {
                success: false,
                data: null,
                error: e.message || 'Network error: Server is unreachable.'
            };
        }
    },

    // ==========================================
    // AUTHENTICATION
    // ==========================================
    async login(email, password) {
        return this.request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
    },

    async adminLogin(email, password) {
        return this.request('/auth/admin-login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
    },

    async register(name, email, phone, password) {
        return this.request('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ name, email, phone, password })
        });
    },

    async logout() {
        const res = await this.request('/auth/logout', { method: 'POST' });
        this.clearCurrentUser();
        return res;
    },

    async getMe() {
        return this.request('/me');
    },

    // ==========================================
    // TRANSACTIONS (EXPENSES & INCOME)
    // ==========================================
    async getExpenses(filters = {}) {
        const params = new URLSearchParams(filters);
        return this.request(`/expenses?${params.toString()}`);
    },

    async addExpense(expense) {
        return this.request('/expenses', {
            method: 'POST',
            body: JSON.stringify(expense)
        });
    },

    async deleteExpense(id) {
        return this.request(`/expenses/${id}`, { method: 'DELETE' });
    },

    async getIncomes(filters = {}) {
        const params = new URLSearchParams(filters);
        return this.request(`/income?${params.toString()}`);
    },

    async addIncome(income) {
        return this.request('/income', {
            method: 'POST',
            body: JSON.stringify(income)
        });
    },

    async deleteIncome(id) {
        return this.request(`/income/${id}`, { method: 'DELETE' });
    },

    // ==========================================
    // REPORTS & ANALYTICS
    // ==========================================
    async getSummary(startDate, endDate) {
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        return this.request(`/reports/summary?${params.toString()}`);
    },

    async getReportsByCategory(startDate, endDate) {
        const params = new URLSearchParams();
        if (startDate) params.append('startDate', startDate);
        if (endDate) params.append('endDate', endDate);
        return this.request(`/reports/by-category?${params.toString()}`);
    },

    async getMonthlyComparison(year) {
        const params = new URLSearchParams();
        if (year) params.append('year', year);
        return this.request(`/reports/monthly-comparison?${params.toString()}`);
    },

    async getInsights() {
        return this.request('/reports/insights');
    },

    // ==========================================
    // BANK ACCOUNTS
    // ==========================================
    async getBankAccounts() {
        return this.request('/accounts');
    },

    async addBankAccount(account) {
        return this.request('/accounts', {
            method: 'POST',
            body: JSON.stringify(account)
        });
    },

    async setPrimaryAccount(accountId) {
        return this.request(`/accounts/${accountId}/primary`, { method: 'PUT' });
    },

    // ==========================================
    // BUDGETS
    // ==========================================
    async getBudgets() {
        return this.request('/budgets');
    },

    async addBudget(budget) {
        return this.request('/budgets', {
            method: 'POST',
            body: JSON.stringify(budget)
        });
    },

    // ==========================================
    // SAVINGS GOALS
    // ==========================================
    async getSavingsGoals() {
        return this.request('/goals');
    },

    async addSavingsGoal(goal) {
        return this.request('/goals', {
            method: 'POST',
            body: JSON.stringify(goal)
        });
    },

    async contributeGoal(goalId, amount) {
        return this.request(`/goals/${goalId}/contribute`, {
            method: 'POST',
            body: JSON.stringify({ goalId, amount })
        });
    },

    // ==========================================
    // RECURRING COMMITMENTS
    // ==========================================
    async getRecurring() {
        return this.request('/recurring');
    },

    async addRecurring(rec) {
        return this.request('/recurring', {
            method: 'POST',
            body: JSON.stringify(rec)
        });
    },

    // ==========================================
    // CATEGORIES
    // ==========================================
    async getCategories(type) {
        const params = new URLSearchParams();
        if (type) params.append('type', type);
        return this.request(`/categories?${params.toString()}`);
    },

    async addCategory(cat) {
        return this.request('/categories', {
            method: 'POST',
            body: JSON.stringify(cat)
        });
    },

    // ==========================================
    // NOTIFICATIONS
    // ==========================================
    async getNotifications() {
        return this.request('/notifications');
    },

    async getUnreadCount() {
        return this.request('/notifications/count');
    },

    async markNotificationRead(id) {
        return this.request(`/notifications/${id}/read`, { method: 'PUT' });
    },

    // ==========================================
    // STATEMENT IMPORT PIPELINE
    // ==========================================
    async uploadStatement(fileName, fileContent, accountId) {
        return this.request('/imports/upload', {
            method: 'POST',
            body: JSON.stringify({ fileName, fileContent, accountId })
        });
    },

    async getStagedTransactions(importId) {
        return this.request(`/imports/${importId}/transactions`);
    },

    async updateImportCategory(transactionId, categoryId, categoryName) {
        return this.request(`/imports/transactions/${transactionId}/category`, {
            method: 'PUT',
            body: JSON.stringify({ transactionId, categoryId, categoryName })
        });
    },

    async deleteImportTransaction(transactionId) {
        return this.request(`/imports/transactions/${transactionId}`, {
            method: 'DELETE'
        });
    },

    async confirmImport(importId) {
        return this.request(`/imports/${importId}/confirm`, {
            method: 'POST',
            body: JSON.stringify({ importId })
        });
    },

    // ==========================================
    // ADMIN ENDPOINTS
    // ==========================================
    async getAdminUsers() {
        return this.request('/admin/users');
    },

    async toggleUserStatus(userId, isActive) {
        return this.request(`/admin/users/${userId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ isActive })
        });
    },

    async getAdminCategories() {
        return this.request('/admin/categories');
    },

    async toggleCategoryStatus(categoryId, isActive) {
        return this.request(`/admin/categories/${categoryId}/status`, {
            method: 'PUT',
            body: JSON.stringify({ isActive })
        });
    },

    async createAdminCategory(cat) {
        return this.request('/admin/categories', {
            method: 'POST',
            body: JSON.stringify(cat)
        });
    },

    async getAdminStats() {
        return this.request('/admin/stats');
    }
};
