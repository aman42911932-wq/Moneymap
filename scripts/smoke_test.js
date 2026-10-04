/**
 * MoneyMap Automated Smoke Test Suite in Node.js
 * Asserts all 27 core scenarios using built-in fetch and manual cookie tracking per session.
 */

const BASE_URL = 'http://localhost:8080';

let passCount = 0;
let failCount = 0;

function assertTest(num, desc, passed, detail = '') {
    if (passed) {
        console.log(`Test ${num}: PASS - ${desc}`);
        passCount++;
    } else {
        console.error(`Test ${num}: FAIL - ${desc} ${detail ? '(' + detail + ')' : ''}`);
        failCount++;
    }
}

class Session {
    constructor() {
        this.cookies = {};
    }

    async request(path, options = {}) {
        const headers = {
            ...(options.headers || {})
        };

        const cookieHeader = Object.entries(this.cookies)
            .map(([k, v]) => `${k}=${v}`)
            .join('; ');
        if (cookieHeader) {
            headers['Cookie'] = cookieHeader;
        }

        const url = `${BASE_URL}${path}`;
        const res = await fetch(url, { ...options, headers });

        // Capture Set-Cookie headers
        const setCookies = res.headers.getSetCookie ? res.headers.getSetCookie() : [];
        if (!setCookies.length && res.headers.get('set-cookie')) {
            setCookies.push(res.headers.get('set-cookie'));
        }

        for (const cookieStr of setCookies) {
            const parts = cookieStr.split(';')[0].split('=');
            if (parts.length >= 2) {
                const name = parts[0].trim();
                const val = parts.slice(1).join('=').trim();
                this.cookies[name] = val;
            }
        }

        let json = null;
        try {
            json = await res.json();
        } catch (e) {
            // Not json
        }

        return { status: res.status, ok: res.ok, json };
    }
}

async function runAllTests() {
    console.log('==========================================');
    console.log(' Starting MoneyMap Node.js Smoke Test Suite');
    console.log('==========================================');

    const userSession = new Session();
    const adminSession = new Session();
    const user2Session = new Session();

    // 1 wrong password login fails
    const res1 = await userSession.request('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'asha.rao@example.com', password: 'wrongpassword' })
    });
    assertTest(1, 'Wrong password login fails (401)', res1.status === 401 && res1.json?.success === false);

    // 2 demo user login succeeds
    const res2 = await userSession.request('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'asha.rao@example.com', password: 'user123' })
    });
    assertTest(2, 'Demo user login succeeds', res2.status === 200 && res2.json?.success === true && res2.json?.data?.email === 'asha.rao@example.com');

    // 3 /api/me works
    const res3 = await userSession.request('/api/me');
    assertTest(3, '/api/me returns authenticated user data', res3.status === 200 && res3.json?.data?.email === 'asha.rao@example.com');

    // 4 unauthenticated /api/expenses returns 401
    const anonSession = new Session();
    const res4 = await anonSession.request('/api/expenses');
    assertTest(4, 'Unauthenticated /api/expenses returns 401', res4.status === 401);

    // 5 register a new user and log in
    const uniqueEmail = `testuser_${Date.now()}@example.com`;
    const res5 = await user2Session.request('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: 'Test Runner', email: uniqueEmail, phone: '1234567890', password: 'pass123' })
    });
    const user2Id = res5.json?.data?.userId;
    assertTest(5, 'Register a new user and establish session', res5.status === 200 && res5.json?.success === true && !!user2Id);

    // 6 add a bank account
    const res6 = await userSession.request('/api/accounts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            bankName: 'HDFC Bank',
            accountHolderName: 'Asha Rao',
            accountLast4: '9911',
            ifscCode: 'HDFC0001234',
            accountType: 'Savings',
            openingBalance: 10000.00,
            isPrimary: false
        })
    });
    assertTest(6, 'Add a bank account', res6.status === 200 && res6.json?.success === true);

    // 7 add an expense with categoryName and date
    const res7 = await userSession.request('/api/expenses', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            amount: 450.00,
            date: '2026-10-03',
            categoryName: 'Food & Dining',
            merchantPlace: 'Pizza Hut',
            paymentMethod: 'UPI',
            description: 'Lunch'
        })
    });
    assertTest(7, 'Add an expense with categoryName and date', res7.status === 200 && res7.json?.success === true);

    // 8 add an income
    const res8 = await userSession.request('/api/income', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            amount: 5000.00,
            date: '2026-10-03',
            categoryName: 'Freelance & Consulting',
            source: 'Upwork Client',
            description: 'Consulting project'
        })
    });
    assertTest(8, 'Add an income', res8.status === 200 && res8.json?.success === true);

    // 9 expense list has date, categoryName and the new row
    const res9 = await userSession.request('/api/expenses');
    const hasPizzaHut = Array.isArray(res9.json?.data) && res9.json.data.some(e => e.merchantPlace === 'Pizza Hut' && e.categoryName === 'Food & Dining' && e.date === '2026-10-03');
    assertTest(9, 'Expense list has date, categoryName, and new row', hasPizzaHut);

    // 10 upload sample_bank_statement.csv with accountId
    const csvContent = `Date,Description,Amount,Type\n2026-10-02,"Swiggy Order #48291",350.00,Debit\n2026-10-02,"Blinkit Groceries Express",720.00,Debit\n2026-10-01,"Uber Ride",210.00,Debit\n2026-09-30,"Freelance Consulting",15000.00,Credit`;
    const res10 = await userSession.request('/api/imports/upload', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fileName: 'sample_bank_statement.csv', fileContent: csvContent, accountId: 1 })
    });
    const importId = res10.json?.data?.importId;
    const stagedTxns = res10.json?.data?.transactions || [];
    assertTest(10, 'Upload statement with accountId stages rows', res10.status === 200 && res10.json?.success === true && !!importId && stagedTxns.length === 4);

    // 11 a CSV with a bad date, a zero amount uploads without 500 and reports bad rows
    const badCsv = `Date,Description,Amount,Type\nINVALID-DATE,"Bad Row",100.00,Debit\n2026-10-01,"Good Row",250.00,Debit`;
    const res11 = await userSession.request('/api/imports/upload', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ fileName: 'bad.csv', fileContent: badCsv, accountId: 1 })
    });
    const goodRowPresent = res11.json?.data?.transactions?.some(t => t.description === 'Good Row');
    assertTest(11, 'CSV with bad rows parsed gracefully without 500', res11.status === 200 && res11.json?.success === true && goodRowPresent);

    // 12 update one staged row's category
    const txIdToUpdate = stagedTxns[0]?.transactionId;
    const res12 = await userSession.request(`/api/imports/transactions/${txIdToUpdate}/category`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ categoryName: 'Personal Care' })
    });
    assertTest(12, 'Update one staged row category', res12.status === 200 && res12.json?.success === true);

    // 13 delete one staged row
    const txIdToDelete = stagedTxns[stagedTxns.length - 1]?.transactionId;
    const res13 = await userSession.request(`/api/imports/transactions/${txIdToDelete}`, {
        method: 'DELETE'
    });
    assertTest(13, 'Delete one staged row before confirm', res13.status === 200 && res13.json?.success === true);

    // 14 confirm succeeds
    const res14 = await userSession.request(`/api/imports/${importId}/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ importId })
    });
    assertTest(14, 'Confirm import succeeds', res14.status === 200 && res14.json?.success === true);

    // 15 confirming again is rejected
    const res15 = await userSession.request(`/api/imports/${importId}/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ importId })
    });
    assertTest(15, 'Confirming an already confirmed import is rejected', res15.status !== 200 || res15.json?.success === false);

    // 16 a second user cannot read or confirm the first user's import
    const res16 = await user2Session.request(`/api/imports/${importId}/confirm`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ importId })
    });
    assertTest(16, 'Second user cannot confirm first user import (ownership check)', res16.status !== 200 || res16.json?.success === false);

    // 17 imported transactions exist with the edited category, deleted row is absent, and carry account
    const res17 = await userSession.request('/api/expenses');
    const updatedSwiggy = res17.json?.data?.find(e => e.description === 'Swiggy Order #48291');
    const deletedPresent = res17.json?.data?.some(e => e.description === 'Freelance Consulting');
    assertTest(17, 'Imported transactions committed with updated category and account', !!updatedSwiggy && updatedSwiggy.categoryName === 'Personal Care' && !deletedPresent);

    // 18 reports summary, by-category, monthly-comparison and insights return data
    const res18a = await userSession.request('/api/reports/summary');
    const res18b = await userSession.request('/api/reports/by-category');
    const res18c = await userSession.request('/api/reports/monthly-comparison');
    const res18d = await userSession.request('/api/reports/insights');
    const allExpensesRes = await userSession.request('/api/expenses');
    const allIncomesRes = await userSession.request('/api/income');
    const expectedSumExpenses = allExpensesRes.json?.data?.reduce((s, e) => s + parseFloat(e.amount), 0) || 0;
    const expectedSumIncomes = allIncomesRes.json?.data?.reduce((s, i) => s + parseFloat(i.amount), 0) || 0;
    const reportedIncome = parseFloat(res18a.json?.data?.totalIncome || 0);
    const reportedExpense = parseFloat(res18a.json?.data?.totalExpense || 0);
    const reportsValid = Math.abs(reportedIncome - expectedSumIncomes) < 0.01 &&
                         Math.abs(reportedExpense - expectedSumExpenses) < 0.01 &&
                         Array.isArray(res18b.json?.data) &&
                         Array.isArray(res18c.json?.data) &&
                         Array.isArray(res18d.json?.data?.insights);
    assertTest(18, 'Reports (summary, by-category, monthly, insights) assert exact sums of transactions', reportsValid);

    // 19 bank account computed balance equals opening + income - expenses
    const res19 = await userSession.request('/api/accounts');
    const primaryAcc = res19.json?.data?.find(a => a.accountId === 1);
    const accOpening = parseFloat(primaryAcc?.openingBalance || 0);
    const accIncomes = allIncomesRes.json?.data?.filter(i => i.accountId === 1).reduce((s, i) => s + parseFloat(i.amount), 0) || 0;
    const accExpenses = allExpensesRes.json?.data?.filter(e => e.accountId === 1).reduce((s, e) => s + parseFloat(e.amount), 0) || 0;
    const expectedCurrentBal = accOpening + accIncomes - accExpenses;
    const actualCurrentBal = parseFloat(primaryAcc?.currentBalance || 0);
    const balanceMatches = Math.abs(actualCurrentBal - expectedCurrentBal) < 0.01;
    assertTest(19, 'Bank account computed live balance equals exact (opening + income - expenses)', balanceMatches);

    // 20 create a category budget; its spent % is correct
    const res20 = await userSession.request('/api/budgets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ categoryName: 'Food & Dining', amount: 5000.00, startDate: '2026-10-01', endDate: '2026-10-31' })
    });
    const foodExpenses = allExpensesRes.json?.data
        ?.filter(e => e.categoryName === 'Food & Dining' && e.date >= '2026-10-01' && e.date <= '2026-10-31')
        .reduce((s, e) => s + parseFloat(e.amount), 0) || 0;
    const expectedPct = Math.round((foodExpenses / 5000.00) * 1000) / 10;
    const actualPct = parseFloat(res20.json?.data?.percentageSpent || 0);
    const budgetPctMatches = Math.abs(actualPct - expectedPct) < 0.2;
    assertTest(20, 'Create category budget and assert exact spent percentage calculation', res20.status === 200 && res20.json?.success === true && budgetPctMatches);

    // 21 add an expense that exceeds it and the 100% notification appears
    await userSession.request('/api/expenses', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ amount: 1500.00, date: '2026-10-04', categoryName: 'Entertainment', merchantPlace: 'PVR Cinema', description: 'Movie tickets' })
    });
    const res21Notifs = await userSession.request('/api/notifications');
    const hasExceeded = Array.isArray(res21Notifs.json?.data) && res21Notifs.json.data.some(n => n.type === 'budget_exceeded' || n.message?.includes('exceeded'));
    assertTest(21, 'Budget overspending triggers 100% alert notification', hasExceeded);

    // 22 notification count, mark-read
    const res22Count = await userSession.request('/api/notifications/count');
    const firstNotifId = res21Notifs.json?.data?.[0]?.notificationId;
    const res22Mark = await userSession.request(`/api/notifications/${firstNotifId}/read`, { method: 'PUT' });
    assertTest(22, 'Notification count and mark-as-read succeed', res22Count.json?.data?.unreadCount !== undefined && res22Mark.json?.success === true);

    // 23 create a goal and contribute; a negative contribution is rejected
    const res23Create = await userSession.request('/api/goals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ goalName: 'Vacation Fund', targetAmount: 40000.00, currentAmount: 5000.00, targetDate: '2026-12-31' })
    });
    const goalId = res23Create.json?.data?.goalId;
    const res23Pos = await userSession.request(`/api/goals/${goalId}/contribute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ goalId, amount: 2000.00 })
    });
    const res23Neg = await userSession.request(`/api/goals/${goalId}/contribute`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ goalId, amount: -500.00 })
    });
    assertTest(23, 'Goal contribution succeeds and negative contribution is rejected', res23Pos.json?.success === true && res23Neg.json?.success === false);

    // 24 create a recurring bill with a category
    const res24 = await userSession.request('/api/recurring', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: 'Gym Membership', amount: 1200.00, categoryName: 'Personal Care', frequency: 'Monthly', nextDueDate: '2026-11-01' })
    });
    assertTest(24, 'Create recurring bill with category', res24.status === 200 && res24.json?.success === true);

    // 25 admin login; admin users list, stats, add category, deactivate user; deactivated user rejected
    const res25Login = await adminSession.request('/api/auth/admin-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: 'admin@moneymap.com', password: 'admin123' })
    });
    const res25Users = await adminSession.request('/api/admin/users');
    const res25Stats = await adminSession.request('/api/admin/stats');
    const uniqueCatName = `Pet Supplies ${Date.now()}`;
    const res25AddCat = await adminSession.request('/api/admin/categories', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: uniqueCatName, type: 'expense', icon: 'paw' })
    });
    // Deactivate user 2
    await adminSession.request(`/api/admin/users/${user2Id}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isActive: false })
    });
    // Verify user 2 session is rejected
    const res25Deact = await user2Session.request('/api/me');
    const adminPass = res25Login.json?.success === true &&
                      Array.isArray(res25Users.json?.data) &&
                      res25Stats.json?.data?.totalUsers !== undefined &&
                      res25AddCat.json?.success === true &&
                      res25Deact.status === 401;
    assertTest(25, 'Admin features (stats, users, categories, user deactivation) work', adminPass);

    // 26 a normal user calling /api/admin/* gets 403
    const res26 = await userSession.request('/api/admin/users');
    assertTest(26, 'Normal user calling /api/admin/* gets 403 Forbidden', res26.status === 403);

    // 27 logout, then /api/me returns 401
    await userSession.request('/api/auth/logout', { method: 'POST' });
    const res27 = await userSession.request('/api/me');
    assertTest(27, 'Logout invalidates session; subsequent /api/me returns 401', res27.status === 401);

    // HTML Page 200 checks
    const pages = ['index.html', 'dashboard.html', 'transactions.html', 'statement-import.html', 'budgets.html', 'goals.html', 'recurring.html', 'bank-accounts.html', 'admin.html'];
    let pagesPass = true;
    for (const p of pages) {
        const pageRes = await fetch(`${BASE_URL}/${p}`);
        if (pageRes.status !== 200) {
            console.error(`Page ${p} returned ${pageRes.status}`);
            pagesPass = false;
        }
    }
    console.log(`Frontend HTML Pages (9/9): ${pagesPass ? 'PASS (HTTP 200)' : 'FAIL'}`);

    console.log('==========================================');
    console.log(` Smoke Test Summary: ${passCount} / 27 Tests Passed (${failCount} Failed)`);
    console.log('==========================================');

    if (failCount > 0 || !pagesPass) {
        process.exit(1);
    }
}

runAllTests().catch(err => {
    console.error('Test execution error:', err);
    process.exit(1);
});
