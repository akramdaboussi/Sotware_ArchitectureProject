// ========================================
// CANVAS BACKGROUND
// ========================================
const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');

function resizeCanvas() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
}
resizeCanvas();
window.addEventListener('resize', resizeCanvas);

class Particle {
    constructor() { this.reset(); }
    reset() {
        this.x = Math.random() * canvas.width;
        this.y = Math.random() * canvas.height;
        this.size = Math.random() * 1.5 + 0.5;
        this.speedX = (Math.random() - 0.5) * 1.2;
        this.speedY = (Math.random() - 0.5) * 1.2;
        this.opacity = Math.random() * 0.25 + 0.05;
    }
    update() {
        this.x += this.speedX;
        this.y += this.speedY;
        if (this.x > canvas.width || this.x < 0) this.speedX *= -1;
        if (this.y > canvas.height || this.y < 0) this.speedY *= -1;
    }
    draw() {
        ctx.fillStyle = `rgba(148, 163, 184, ${this.opacity})`;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fill();
    }
}

const particles = Array.from({ length: 60 }, () => new Particle());

function animate() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    particles.forEach(p => { p.update(); p.draw(); });
    particles.forEach((a, i) => {
        particles.slice(i + 1).forEach(b => {
            const d = Math.hypot(a.x - b.x, a.y - b.y);
            if (d < 120) {
                ctx.strokeStyle = `rgba(100, 116, 139, ${0.1 * (1 - d / 120)})`;
                ctx.lineWidth = 0.6;
                ctx.beginPath();
                ctx.moveTo(a.x, a.y);
                ctx.lineTo(b.x, b.y);
                ctx.stroke();
            }
        });
    });
    requestAnimationFrame(animate);
}
animate();

// ========================================
// CONSTANTS & STATE
// ========================================
const AUTH_API = '/api/auth';
const ADMIN_API = '/api/admin';

let currentUser = null;
let permModalEmail = '';

const saveToken = t => localStorage.setItem('authToken', t);
const getToken = () => localStorage.getItem('authToken');
const clearToken = () => localStorage.removeItem('authToken');

// ========================================
// VIEW MANAGEMENT
// ========================================
const authContainer = document.getElementById('authContainer');
const appLayout = document.getElementById('appLayout');

const authViews = {
    login: document.getElementById('loginView'),
    register: document.getElementById('registerView'),
    checkEmail: document.getElementById('checkEmailView'),
    verifyResult: document.getElementById('verifyResultView'),
};

function showAuthView(name) {
    authContainer.style.display = 'flex';
    appLayout.style.display = 'none';
    Object.values(authViews).forEach(v => v.classList.remove('active'));
    authViews[name].classList.add('active');
}

function showApp() {
    authContainer.style.display = 'none';
    appLayout.style.display = 'flex';
}

// ========================================
// TAB NAVIGATION
// ========================================
const tabSections = {
    profile: document.getElementById('tabProfile'),
    users: document.getElementById('tabUsers'),
    permissions: document.getElementById('tabPermissions'),
    services: document.getElementById('tabServices'),
};

const navItems = document.querySelectorAll('.nav-item');

function switchTab(tabName) {
    navItems.forEach(b => b.classList.remove('active'));
    const btn = document.querySelector(`[data-tab="${tabName}"]`);
    if (btn) btn.classList.add('active');
    Object.values(tabSections).forEach(s => s.classList.remove('active'));
    if (tabSections[tabName]) tabSections[tabName].classList.add('active');
    if (tabName === 'users') loadUsersTable();
    if (tabName === 'permissions') loadPermissionsTable();
    if (tabName === 'services') setupServicesTab();
}

navItems.forEach(btn => {
    btn.addEventListener('click', () => switchTab(btn.dataset.tab));
});

// ========================================
// TOAST NOTIFICATIONS
// ========================================
const toastContainer = document.getElementById('toastContainer');

function showToast(message, type = 'info') {
    const icons = { success: '&#10003;', error: '&#10005;', info: 'i' };
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `<span class="toast-icon">${icons[type]}</span><span>${message}</span>`;
    toastContainer.appendChild(toast);
    setTimeout(() => toast.classList.add('toast-hide'), 3800);
    setTimeout(() => toast.remove(), 4200);
}

// ========================================
// CONFIRM MODAL
// ========================================
const confirmModal = document.getElementById('confirmModal');
const modalTitle = document.getElementById('modalTitle');
const modalMessage = document.getElementById('modalMessage');
const modalConfirmBtn = document.getElementById('modalConfirmBtn');
const modalCancelBtn = document.getElementById('modalCancelBtn');

function showConfirmModal({ title, message, confirmText = 'Confirm', onConfirm }) {
    modalTitle.textContent = title;
    modalMessage.textContent = message;
    modalConfirmBtn.textContent = confirmText;
    confirmModal.style.display = 'flex';

    const cleanup = () => {
        modalConfirmBtn.removeEventListener('click', doConfirm);
        modalCancelBtn.removeEventListener('click', doCancel);
    };
    const doConfirm = async () => { confirmModal.style.display = 'none'; cleanup(); await onConfirm(); };
    const doCancel = () => { confirmModal.style.display = 'none'; cleanup(); };

    modalConfirmBtn.addEventListener('click', doConfirm);
    modalCancelBtn.addEventListener('click', doCancel);
}

// ========================================
// PERMISSION MODAL (per user)
// ========================================
const permissionModal = document.getElementById('permissionModal');
const permModalUser = document.getElementById('permModalUser');
const permCurrentList = document.getElementById('permCurrentList');
const permInputField = document.getElementById('permInputField');
const permAddBtn = document.getElementById('permAddBtn');
const permModalCloseBtn = document.getElementById('permModalCloseBtn');

function openPermissionModal(user) {
    permModalEmail = user.email;
    permModalUser.textContent = `${user.firstName} ${user.lastName}  —  ${user.email}`;
    renderPermList(user.permissions || []);
    permInputField.value = '';
    permissionModal.style.display = 'flex';
}

function renderPermList(perms) {
    permCurrentList.innerHTML = '';
    if (!perms.length) {
        permCurrentList.innerHTML = '<p style="color:var(--text-sub);font-size:0.83rem;">No permissions assigned.</p>';
        return;
    }
    perms.forEach(p => {
        const row = document.createElement('div');
        row.className = 'perm-list-item';
        row.id = `pli-${p}`;
        row.innerHTML = `<span>${p}</span>
            <button class="btn-icon" onclick="handleRemovePerm('${p}', this)">&#10005;</button>`;
        permCurrentList.appendChild(row);
    });
}

permModalCloseBtn.addEventListener('click', () => {
    permissionModal.style.display = 'none';
    loadUsersTable();
    loadPermissionsTable();
});

permAddBtn.addEventListener('click', async () => {
    const perm = permInputField.value.trim().toUpperCase();
    if (!perm) return;
    permAddBtn.disabled = true;
    try {
        await apiAddPermission(permModalEmail, perm);
        showToast(`Permission "${perm}" added.`, 'success');
        permInputField.value = '';
        const row = document.createElement('div');
        row.className = 'perm-list-item';
        row.id = `pli-${perm}`;
        row.innerHTML = `<span>${perm}</span>
            <button class="btn-icon" onclick="handleRemovePerm('${perm}', this)">&#10005;</button>`;
        if (permCurrentList.querySelector('p')) permCurrentList.innerHTML = '';
        permCurrentList.appendChild(row);
    } catch (e) {
        showToast(e.message, 'error');
    } finally {
        permAddBtn.disabled = false;
    }
});

async function handleRemovePerm(perm, btn) {
    btn.disabled = true;
    try {
        await apiRemovePermission(permModalEmail, perm);
        showToast(`Permission "${perm}" removed.`, 'success');
        document.getElementById(`pli-${perm}`)?.remove();
        if (!permCurrentList.children.length) {
            permCurrentList.innerHTML = '<p style="color:var(--text-sub);font-size:0.83rem;">No permissions assigned.</p>';
        }
    } catch (e) {
        showToast(e.message, 'error');
        btn.disabled = false;
    }
}

// ========================================
// API HELPERS
// ========================================
async function apiFetch(url, options = {}) {
    const res = await fetch(url, options);
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || data.message || `Request failed (${res.status})`);
    return data;
}

function authHeaders() {
    return { 'Authorization': `Bearer ${getToken()}`, 'Content-Type': 'application/json' };
}

async function apiRegister(firstName, lastName, email, password, phoneNumber) {
    return apiFetch(`${AUTH_API}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ firstName, lastName, email, password, phoneNumber }),
    });
}

async function apiLogin(email, password) {
    return apiFetch(`${AUTH_API}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
    });
}

async function apiGetMe() { return apiFetch(`${AUTH_API}/me`, { headers: authHeaders() }); }
async function apiLogout() { return apiFetch(`${AUTH_API}/logout`, { method: 'POST', headers: authHeaders() }); }
async function apiDeleteAccount() { return apiFetch(`${AUTH_API}/me`, { method: 'DELETE', headers: authHeaders() }); }
async function apiGetAllUsers() { return apiFetch(`${ADMIN_API}/users`, { headers: authHeaders() }); }

async function apiVerifyEmail(tokenId, t) {
    const res = await fetch(`${AUTH_API}/verify?tokenId=${encodeURIComponent(tokenId)}&t=${encodeURIComponent(t)}`);
    const data = await res.json().catch(() => ({}));
    return { ok: res.ok, data };
}

async function apiAddPermission(email, permission) {
    return apiFetch(`${ADMIN_API}/add-permission`, {
        method: 'POST', headers: authHeaders(),
        body: JSON.stringify({ email, permission }),
    });
}

async function apiRemovePermission(email, permission) {
    return apiFetch(`${ADMIN_API}/remove-permission`, {
        method: 'POST', headers: authHeaders(),
        body: JSON.stringify({ email, permission }),
    });
}

async function apiDeleteUser(id) {
    return apiFetch(`${ADMIN_API}/users/${id}`, { method: 'DELETE', headers: authHeaders() });
}

// ========================================
// AUTH FORM HANDLERS
// ========================================

// Login
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('loginBtn');
    btn.disabled = true; btn.textContent = 'Signing in...';
    clearError('loginError');
    try {
        const data = await apiLogin(
            document.getElementById('loginEmail').value,
            document.getElementById('loginPassword').value
        );
        saveToken(data.token);
        await loadApp();
    } catch (err) {
        showError('loginError', err.message);
    } finally {
        btn.disabled = false; btn.textContent = 'Sign In';
    }
});

// Register
document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('registerBtn');
    btn.disabled = true; btn.textContent = 'Creating account...';
    clearError('registerError');
    try {
        const data = await apiRegister(
            document.getElementById('regFirstName').value,
            document.getElementById('regLastName').value,
            document.getElementById('regEmail').value,
            document.getElementById('regPassword').value,
            document.getElementById('regPhone').value
        );
        document.getElementById('registeredEmail').textContent = data.email;
        showAuthView('checkEmail');
    } catch (err) {
        showError('registerError', err.message);
    } finally {
        btn.disabled = false; btn.textContent = 'Create Account';
    }
});

// Logout
document.getElementById('logoutBtn').addEventListener('click', async () => {
    try { await apiLogout(); } catch (_) { }
    clearToken(); currentUser = null;
    showAuthView('login');
});

// Nav toggles
document.getElementById('showRegister').addEventListener('click', e => { e.preventDefault(); showAuthView('register'); });
document.getElementById('showLogin').addEventListener('click', e => { e.preventDefault(); showAuthView('login'); });
document.getElementById('backToLoginBtn').addEventListener('click', () => showAuthView('login'));
document.getElementById('verifyToLoginBtn').addEventListener('click', () => showAuthView('login'));

// Delete account
document.getElementById('deleteAccountBtn').addEventListener('click', () => {
    showConfirmModal({
        title: 'Delete Account',
        message: 'This will permanently delete your account and all associated data. This action cannot be undone.',
        confirmText: 'Delete',
        onConfirm: async () => {
            try {
                await apiDeleteAccount();
                clearToken(); currentUser = null;
                showAuthView('login');
                showToast('Account deleted.', 'success');
            } catch (err) {
                showToast(err.message, 'error');
            }
        }
    });
});

// ========================================
// USERS TAB — CREATE (collapsible)
// ========================================
document.getElementById('createUserToggle').addEventListener('click', () => {
    const body = document.getElementById('createUserBody');
    const arrow = document.getElementById('createUserArrow');
    const open = body.style.display === 'block';
    body.style.display = open ? 'none' : 'block';
    arrow.innerHTML = open ? '&#9660;' : '&#9650;';
});

document.getElementById('createUserForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const btn = document.getElementById('createUserBtn');
    btn.disabled = true; btn.textContent = 'Creating...';
    clearError('createUserError');
    try {
        await apiRegister(
            document.getElementById('cuFirstName').value,
            document.getElementById('cuLastName').value,
            document.getElementById('cuEmail').value,
            document.getElementById('cuPassword').value,
            document.getElementById('cuPhone').value
        );
        showToast('User created. They must verify their email.', 'success');
        document.getElementById('createUserForm').reset();
        document.getElementById('createUserBody').style.display = 'none';
        document.getElementById('createUserArrow').innerHTML = '&#9660;';
        loadUsersTable();
    } catch (err) {
        showError('createUserError', err.message);
    } finally {
        btn.disabled = false; btn.textContent = 'Create User';
    }
});

document.getElementById('refreshUsersBtn').addEventListener('click', loadUsersTable);

// ========================================
// USERS TABLE — READ + DELETE
// ========================================
async function loadUsersTable() {
    const tbody = document.getElementById('usersTableBody');
    tbody.innerHTML = `<tr><td colspan="6" class="table-empty">Loading...</td></tr>`;
    try {
        const users = await apiGetAllUsers();
        const badge = document.getElementById('usersCountBadge');
        if (badge) badge.textContent = users.length;
        document.getElementById('userCount').textContent = `${users.length} user${users.length !== 1 ? 's' : ''}`;
        renderUsersTable(users);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="table-empty err">${err.message}</td></tr>`;
    }
}

function renderUsersTable(users) {
    const tbody = document.getElementById('usersTableBody');
    if (!users.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="table-empty">No users found.</td></tr>`;
        return;
    }
    tbody.innerHTML = users.map(u => {
        const perms = (u.permissions || [])
            .map(p => `<span class="badge ${p === 'ADMIN' ? 'badge-admin' : 'badge-perm'}">${p}</span>`)
            .join('') || '<span style="color:var(--text-sub);font-size:0.78rem;">None</span>';

        const status = u.enabled
            ? `<span class="badge badge-active">Active</span>`
            : `<span class="badge badge-inactive">Inactive</span>`;

        const isSelf = currentUser && u.email === currentUser.email;

        return `<tr>
            <td class="cell-id">${u.id}</td>
            <td class="cell-name">${u.firstName} ${u.lastName}</td>
            <td class="cell-email">${u.email}</td>
            <td>${status}</td>
            <td><div class="perm-tags">${perms}</div></td>
            <td style="text-align:right;">
                ${isSelf
                ? '<span class="badge badge-perm">You</span>'
                : `<button class="btn-icon" title="Delete user" onclick="confirmDeleteUser(${u.id},'${u.email}')">&#10005;</button>`
            }
            </td>
        </tr>`;
    }).join('');
}

function confirmDeleteUser(id, email) {
    showConfirmModal({
        title: 'Delete User',
        message: `This will permanently delete "${email}". This action cannot be undone.`,
        confirmText: 'Delete',
        onConfirm: async () => {
            try {
                await apiDeleteUser(id);
                showToast(`User "${email}" deleted.`, 'success');
                loadUsersTable();
            } catch (err) {
                showToast(err.message, 'error');
            }
        }
    });
}

// ========================================
// PERMISSIONS TAB
// ========================================
document.getElementById('refreshPermBtn').addEventListener('click', loadPermissionsTable);

document.getElementById('quickPermForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const email = document.getElementById('qpEmail').value.trim();
    const perm = document.getElementById('qpPermission').value.trim().toUpperCase();
    const btn = document.getElementById('qpAddBtn');
    btn.disabled = true;
    clearError('quickPermMsg');
    try {
        await apiAddPermission(email, perm);
        showToast(`Permission "${perm}" added to ${email}.`, 'success');
        document.getElementById('qpPermission').value = '';
        loadPermissionsTable();
    } catch (err) {
        showError('quickPermMsg', err.message);
    } finally {
        btn.disabled = false;
    }
});

document.getElementById('qpRemoveBtn').addEventListener('click', async () => {
    const email = document.getElementById('qpEmail').value.trim();
    const perm = document.getElementById('qpPermission').value.trim().toUpperCase();
    if (!email || !perm) { showError('quickPermMsg', 'Email and permission are required.'); return; }
    const btn = document.getElementById('qpRemoveBtn');
    btn.disabled = true;
    clearError('quickPermMsg');
    try {
        await apiRemovePermission(email, perm);
        showToast(`Permission "${perm}" removed from ${email}.`, 'success');
        document.getElementById('qpPermission').value = '';
        loadPermissionsTable();
    } catch (err) {
        showError('quickPermMsg', err.message);
    } finally {
        btn.disabled = false;
    }
});

async function loadPermissionsTable() {
    const tbody = document.getElementById('permTableBody');
    tbody.innerHTML = `<tr><td colspan="4" class="table-empty">Loading...</td></tr>`;
    try {
        const users = await apiGetAllUsers();
        document.getElementById('permUserCount').textContent = `${users.length} user${users.length !== 1 ? 's' : ''}`;
        renderPermissionsTable(users);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="table-empty err">${err.message}</td></tr>`;
    }
}

function renderPermissionsTable(users) {
    const tbody = document.getElementById('permTableBody');
    if (!users.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="table-empty">No users found.</td></tr>`;
        return;
    }
    tbody.innerHTML = users.map(u => {
        const perms = (u.permissions || [])
            .map(p => `<span class="badge ${p === 'ADMIN' ? 'badge-admin' : 'badge-perm'}">${p}</span>`)
            .join('') || '<span style="color:var(--text-sub);font-size:0.78rem;">None</span>';

        return `<tr>
            <td class="cell-name">${u.firstName} ${u.lastName}</td>
            <td class="cell-email">${u.email}</td>
            <td><div class="perm-tags">${perms}</div></td>
            <td style="text-align:right;">
                <button class="btn-ghost-sm" onclick='openPermissionModal(${JSON.stringify(u)})'>Manage</button>
            </td>
        </tr>`;
    }).join('');
}

// ========================================
// PROFILE RENDERING
// ========================================
function renderProfile() {
    if (!currentUser) return;
    const u = currentUser;
    const initials = ((u.firstName?.[0] || '') + (u.lastName?.[0] || '')).toUpperCase() || '?';

    document.getElementById('profileAvatar').textContent = initials;
    document.getElementById('profileFullName').textContent = `${u.firstName} ${u.lastName}`;
    document.getElementById('sidebarUserMini').textContent = u.email;

    document.getElementById('profileVerifiedBadge').innerHTML = u.verified
        ? `<span class="badge badge-verified">Verified</span>`
        : `<span class="badge badge-pending">Pending Verification</span>`;

    document.getElementById('profileInfoRows').innerHTML = `
        <div class="info-row">
            <span class="info-label">Email</span>
            <span class="info-value">${u.email}</span>
        </div>
        <div class="info-row">
            <span class="info-label">Phone</span>
            <span class="info-value">${u.phoneNumber || '&mdash;'}</span>
        </div>
        <div class="info-row">
            <span class="info-label">Status</span>
            <span class="info-value">${u.enabled
            ? '<span class="badge badge-active">Active</span>'
            : '<span class="badge badge-inactive">Inactive</span>'}</span>
        </div>
    `;

    const permsEl = document.getElementById('profilePermissions');
    if (!u.permissions || !u.permissions.length) {
        permsEl.innerHTML = '<span style="color:var(--text-sub);font-size:0.83rem;">No permissions assigned.</span>';
    } else {
        permsEl.innerHTML = u.permissions
            .map(p => `<span class="badge ${p === 'ADMIN' ? 'badge-admin' : 'badge-perm'}">${p}</span>`)
            .join('');
    }
}

// ========================================
// LOAD APP (after login)
// ========================================
async function loadApp() {
    try {
        currentUser = await apiGetMe();
        console.log('[AUTH] User:', currentUser.email, '| Permissions:', currentUser.permissions);
        renderProfile();
        showApp();
        switchTab('profile');

        const isAdmin = Array.isArray(currentUser.permissions) &&
            currentUser.permissions.includes('ADMIN');
        console.log('[AUTH] isAdmin:', isAdmin);

        const showEl = (id, displayType) => {
            const el = document.getElementById(id);
            if (el) el.style.display = isAdmin ? displayType : 'none';
        };
        showEl('labelManagement', 'block');
        showEl('navUsers', 'flex');
        showEl('navPermissions', 'flex');

    } catch (err) {
        console.error('[AUTH] loadApp error:', err);
        clearToken();
        showAuthView('login');
    }
}

// ========================================
// EMAIL VERIFICATION
// ========================================
async function handleVerificationLink() {
    const params = new URLSearchParams(window.location.search);
    const tokenId = params.get('tokenId');
    const t = params.get('t');
    if (!tokenId || !t) return false;

    const result = await apiVerifyEmail(tokenId, t);
    const icon = document.getElementById('verifyIcon');
    icon.textContent = result.ok ? 'OK' : '!';
    icon.className = `status-icon ${result.ok ? 'status-icon--success' : 'status-icon--error'}`;

    document.getElementById('verifyTitle').textContent = result.ok ? 'Email Verified' : 'Verification Failed';
    document.getElementById('verifyMessage').textContent = result.ok
        ? 'Your account is confirmed. You can now sign in.'
        : (result.data.error || 'The verification link is invalid or has expired.');

    if (result.ok && result.data.token) saveToken(result.data.token);
    showAuthView('verifyResult');
    window.history.replaceState({}, document.title, '/');
    return true;
}

// ========================================
// ERROR HELPERS
// ========================================
function showError(id, msg) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = msg;
    el.classList.add('show');
    setTimeout(() => el.classList.remove('show'), 6000);
}

function clearError(id) {
    const el = document.getElementById(id);
    if (!el) return;
    el.textContent = '';
    el.classList.remove('show');
}

// ========================================
// INIT
// ========================================
window.addEventListener('DOMContentLoaded', async () => {
    authContainer.style.display = 'flex';   // default show auth
    const wasVerification = await handleVerificationLink();
    if (wasVerification) return;
    const token = getToken();
    if (token) {
        await loadApp();
    } else {
        showAuthView('login');
    }
});

// ========================================
// SERVICES A & B (via Nginx)
// ========================================

function setupServicesTab() {
    // Only bind once
    if (document.getElementById('callServiceAHello').__bound) return;
    document.getElementById('callServiceAHello').__bound = true;

    const callService = async (url, resultId) => {
        const resultEl = document.getElementById(resultId);
        resultEl.textContent = 'Loading...';
        try {
            const res = await fetch(url, {
                headers: { 'Authorization': `Bearer ${getToken()}` }
            });
            if (!res.ok) {
                resultEl.textContent = `❌ ${res.status} ${res.statusText}`;
                return;
            }
            const data = await res.json();
            resultEl.textContent = JSON.stringify(data, null, 2);
        } catch (err) {
            resultEl.textContent = `❌ Network error: ${err.message}\n\nMake sure you are running the full Docker stack:\ndocker-compose up --build`;
        }
    };

    document.getElementById('callServiceAHello').addEventListener('click', () =>
        callService('/a/hello', 'serviceAResult'));
    document.getElementById('callServiceAData').addEventListener('click', () =>
        callService('/a/data', 'serviceAResult'));
    document.getElementById('callServiceBHello').addEventListener('click', () =>
        callService('/b/hello', 'serviceBResult'));
    document.getElementById('callServiceBData').addEventListener('click', () =>
        callService('/b/data', 'serviceBResult'));
}
