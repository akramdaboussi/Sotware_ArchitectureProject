// ========================================
// CANVAS PARTICLE ANIMATION
// ========================================

const canvas = document.getElementById('canvas');
const ctx = canvas.getContext('2d');

// Set canvas size
function resizeCanvas() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
}
resizeCanvas();
window.addEventListener('resize', resizeCanvas);

// Particle class
class Particle {
    constructor() {
        this.x = Math.random() * canvas.width;
        this.y = Math.random() * canvas.height;
        this.size = Math.random() * 3 + 1;
        this.speedX = (Math.random() - 0.5) * 2;
        this.speedY = (Math.random() - 0.5) * 2;
        this.opacity = Math.random() * 0.5 + 0.3;
    }

    update() {
        this.x += this.speedX;
        this.y += this.speedY;

        // Wrap around screen
        if (this.x > canvas.width) this.x = 0;
        if (this.x < 0) this.x = canvas.width;
        if (this.y > canvas.height) this.y = 0;
        if (this.y < 0) this.y = canvas.height;
    }

    draw() {
        ctx.fillStyle = `rgba(255, 255, 255, ${this.opacity})`;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fill();
    }
}

// Create particles
const particles = [];
const particleCount = 100;

for (let i = 0; i < particleCount; i++) {
    particles.push(new Particle());
}

// Animation loop
function animate() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    particles.forEach(particle => {
        particle.update();
        particle.draw();
    });

    // Draw connections
    particles.forEach((particleA, indexA) => {
        particles.slice(indexA + 1).forEach(particleB => {
            const dx = particleA.x - particleB.x;
            const dy = particleA.y - particleB.y;
            const distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 120) {
                ctx.strokeStyle = `rgba(255, 255, 255, ${0.15 * (1 - distance / 120)})`;
                ctx.lineWidth = 1;
                ctx.beginPath();
                ctx.moveTo(particleA.x, particleA.y);
                ctx.lineTo(particleB.x, particleB.y);
                ctx.stroke();
            }
        });
    });

    requestAnimationFrame(animate);
}

animate();

// ========================================
// API BASE URL
// ========================================

const API_BASE = 'http://localhost:8080/api/auth';

// ========================================
// VIEW MANAGEMENT
// ========================================

const views = {
    login: document.getElementById('loginView'),
    register: document.getElementById('registerView'),
    checkEmail: document.getElementById('checkEmailView'),
    verifyResult: document.getElementById('verifyResultView'),
    dashboard: document.getElementById('dashboardView')
};

function showView(viewName) {
    Object.values(views).forEach(view => view.classList.remove('active'));
    views[viewName].classList.add('active');
}

// ========================================
// LOCAL STORAGE
// ========================================

function saveToken(token) {
    localStorage.setItem('authToken', token);
}

function getToken() {
    return localStorage.getItem('authToken');
}

function clearToken() {
    localStorage.removeItem('authToken');
}

// ========================================
// ERROR HANDLING
// ========================================

function showError(elementId, message) {
    const errorEl = document.getElementById(elementId);
    errorEl.textContent = message;
    errorEl.classList.add('show');
    setTimeout(() => errorEl.classList.remove('show'), 5000);
}

function clearErrors() {
    document.querySelectorAll('.error-message').forEach(el => {
        el.classList.remove('show');
        el.textContent = '';
    });
}

// ========================================
// AUTHENTICATION API CALLS
// ========================================

async function register(firstName, lastName, email, password, phoneNumber) {
    const response = await fetch(`${API_BASE}/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            firstName,
            lastName,
            email,
            password,
            phoneNumber
        })
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Registration failed');
    }

    return data;
}

async function login(email, password) {
    const response = await fetch(`${API_BASE}/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ email, password })
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Login failed');
    }

    return data;
}

async function getCurrentUser(token) {
    const response = await fetch(`${API_BASE}/me`, {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Failed to get user info');
    }

    return data;
}

async function logout(token) {
    const response = await fetch(`${API_BASE}/logout`, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    const data = await response.json();

    if (!response.ok) {
        throw new Error(data.error || 'Logout failed');
    }

    return data;
}

// ========================================
// EMAIL VERIFICATION API
// ========================================

async function verifyEmailToken(tokenId, t) {
    const response = await fetch(`${API_BASE}/verify?tokenId=${encodeURIComponent(tokenId)}&t=${encodeURIComponent(t)}`);
    const data = await response.json();
    return { ok: response.ok, data };
}

// ========================================
// FORM HANDLERS
// ========================================

// Login Form
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const data = await login(email, password);
        saveToken(data.token);
        loadDashboard();
    } catch (error) {
        showError('loginError', error.message);
    }
});

// Register Form — now shows "Check Your Email" instead of going to dashboard
document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    clearErrors();

    const firstName = document.getElementById('regFirstName').value;
    const lastName = document.getElementById('regLastName').value;
    const email = document.getElementById('regEmail').value;
    const password = document.getElementById('regPassword').value;
    const phoneNumber = document.getElementById('regPhone').value;

    try {
        const data = await register(firstName, lastName, email, password, phoneNumber);
        saveToken(data.token);

        // Show the "Check Your Email" screen
        document.getElementById('registeredEmail').textContent = email;
        showView('checkEmail');
    } catch (error) {
        showError('registerError', error.message);
    }
});

// Logout Button
document.getElementById('logoutBtn').addEventListener('click', async () => {
    const token = getToken();
    if (token) {
        try {
            await logout(token);
        } catch (error) {
            console.error('Logout error:', error);
        }
    }
    clearToken();
    showView('login');
});

// Toggle between login and register
document.getElementById('showRegister').addEventListener('click', (e) => {
    e.preventDefault();
    clearErrors();
    showView('register');
});

document.getElementById('showLogin').addEventListener('click', (e) => {
    e.preventDefault();
    clearErrors();
    showView('login');
});

// Back to Login from Check Email screen
document.getElementById('backToLoginBtn').addEventListener('click', () => {
    showView('login');
});

// Back to Login from Verify Result screen
document.getElementById('verifyToLoginBtn').addEventListener('click', () => {
    showView('login');
});

// ========================================
// DASHBOARD
// ========================================

async function loadDashboard() {
    const token = getToken();
    if (!token) {
        showView('login');
        return;
    }

    try {
        const user = await getCurrentUser(token);

        // Display user info with verification badge
        const userInfoEl = document.getElementById('userInfo');
        const verifiedBadge = user.verified
            ? '<span class="badge badge-verified">✅ Verified</span>'
            : '<span class="badge badge-unverified">⏳ Not Verified — check your email!</span>';

        userInfoEl.innerHTML = `
            <p><strong>Name:</strong> ${user.firstName} ${user.lastName}</p>
            <p><strong>Email:</strong> ${user.email} ${verifiedBadge}</p>
            <p><strong>Phone:</strong> ${user.phoneNumber || 'Not provided'}</p>
            <p><strong>Status:</strong> <span style="color: #10b981">${user.enabled ? 'Active' : 'Inactive'}</span></p>
        `;

        showView('dashboard');
    } catch (error) {
        console.error('Error loading dashboard:', error);
        clearToken();
        showView('login');
    }
}

// ========================================
// EMAIL VERIFICATION LINK HANDLER
// ========================================

async function handleVerificationLink() {
    const params = new URLSearchParams(window.location.search);
    const tokenId = params.get('tokenId');
    const t = params.get('t');

    // Check if the current page was loaded with verification params
    if (tokenId && t) {
        const result = await verifyEmailToken(tokenId, t);

        const iconEl = document.getElementById('verifyIcon');
        const titleEl = document.getElementById('verifyTitle');
        const messageEl = document.getElementById('verifyMessage');

        if (result.ok) {
            iconEl.textContent = '✅';
            iconEl.classList.add('success');
            titleEl.textContent = 'Email Verified!';
            messageEl.textContent = 'Your account has been confirmed. You can now log in.';
        } else {
            iconEl.textContent = '❌';
            iconEl.classList.add('error');
            titleEl.textContent = 'Verification Failed';
            messageEl.textContent = result.data.error || 'The link is invalid or expired.';
        }

        showView('verifyResult');

        // Clean the URL so refreshing doesn't re-trigger
        window.history.replaceState({}, document.title, '/');
        return true; // verification was handled
    }
    return false; // no verification params
}

// ========================================
// INITIALIZATION
// ========================================

window.addEventListener('DOMContentLoaded', async () => {
    // First, check if the page was loaded with a verification link
    const wasVerification = await handleVerificationLink();
    if (wasVerification) return;

    // Otherwise, check if user is already logged in
    const token = getToken();
    if (token) {
        loadDashboard();
    } else {
        showView('login');
    }
});
