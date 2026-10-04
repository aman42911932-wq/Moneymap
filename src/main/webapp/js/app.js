/**
 * Common Application UI Controller for MoneyMap
 */

// Escape HTML utility to prevent XSS injection
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

document.addEventListener('DOMContentLoaded', async () => {
    await initUserSession();
    initNotificationBell();
    highlightActiveNav();
});

async function initUserSession() {
    const isLoginPage = window.location.pathname.endsWith('index.html') || window.location.pathname === '/' || window.location.pathname === '';
    if (isLoginPage) return;

    try {
        const res = await API.getMe();
        if (res && res.success && res.data) {
            const user = res.data;
            API.setCurrentUser(user);
            populateUserUI(user);

            // Admin gate check
            const isAdminPage = window.location.pathname.endsWith('admin.html');
            if (isAdminPage && user.role !== 'ADMIN' && user.role !== 'SUPER_ADMIN') {
                alert('Access Denied: Administrator role required.');
                window.location.href = 'dashboard.html';
            }
        } else {
            API.clearCurrentUser();
            window.location.href = 'index.html';
        }
    } catch (e) {
        API.clearCurrentUser();
        window.location.href = 'index.html';
    }
}

function populateUserUI(user) {
    const nameEl = document.getElementById('sidebarUserName');
    const roleEl = document.getElementById('sidebarUserRole');
    const avatarEl = document.getElementById('sidebarUserAvatar');

    if (user && nameEl) {
        nameEl.textContent = user.name || "User";
        if (roleEl) roleEl.textContent = user.role === 'SUPER_ADMIN' || user.role === 'ADMIN' ? 'Administrator' : 'Registered User';
        if (avatarEl) avatarEl.textContent = (user.name || "U").charAt(0).toUpperCase();
    }
}

async function handleLogout() {
    try {
        await API.logout();
    } catch (ignored) {}
    API.clearCurrentUser();
    window.location.href = 'index.html';
}

async function initNotificationBell() {
    const badgeEl = document.getElementById('notifBadge');
    if (!badgeEl) return;

    try {
        const res = await API.getUnreadCount();
        if (res && res.success && res.data && res.data.unreadCount > 0) {
            badgeEl.textContent = res.data.unreadCount;
            badgeEl.style.display = 'block';
        } else {
            badgeEl.style.display = 'none';
        }
    } catch (e) {
        badgeEl.style.display = 'none';
    }
}

function highlightActiveNav() {
    const currentPath = window.location.pathname.split('/').pop() || 'dashboard.html';
    const navLinks = document.querySelectorAll('.nav-item a');
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPath || (currentPath === '' && href === 'dashboard.html')) {
            link.parentElement.classList.add('active');
        } else {
            link.parentElement.classList.remove('active');
        }
    });
}

function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('active');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('active');
}

window.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal-overlay')) {
        e.target.classList.remove('active');
    }
});

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        maximumFractionDigits: 2
    }).format(amount || 0);
}
