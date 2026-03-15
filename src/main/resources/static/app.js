const API_URL = ''; // Nginx routes everything directly, so /api, /a/api, /b/api work directly from root

let state = {
    token: localStorage.getItem('token'),
    user: null,
    claims: null,
    projects: [],
    currentProject: null,
    tasks: []
};

// DOM Elements
const views = {
    auth: document.getElementById('auth-view'),
    app: document.getElementById('app-view')
};
const forms = {
    login: document.getElementById('login-form'),
    register: document.getElementById('register-form'),
    project: document.getElementById('form-project'),
    task: document.getElementById('form-task')
};
const modals = {
    project: document.getElementById('modal-project'),
    task: document.getElementById('modal-task')
};
const ui = {
    authMessage: document.getElementById('auth-message'),
    userName: document.getElementById('user-name'),
    userRole: document.getElementById('user-role'),
    userAvatar: document.getElementById('user-avatar'),
    projectList: document.getElementById('project-list'),
    currentProjectTitle: document.getElementById('current-project-title'),
    projectActions: document.getElementById('project-actions'),
    kanbanBoard: document.getElementById('kanban-board'),
    noProjectSelected: document.getElementById('no-project-selected'),
    listTodo: document.getElementById('list-todo'),
    listInProgress: document.getElementById('list-inprogress'),
    listDone: document.getElementById('list-done'),
    counts: {
        todo: document.getElementById('count-todo'),
        inprogress: document.getElementById('count-inprogress'),
        done: document.getElementById('count-done')
    },
    btnDeleteProject: document.getElementById('btn-delete-project')
};

// --- Initialization ---

async function init() {
    setupEventListeners();
    if (state.token) {
        parseToken();
        const me = await fetchUser();
        if (me) {
            showView('app');
            loadWorkspace();
        } else {
            logout();
        }
    } else {
        showView('auth');
    }
}

function parseToken() {
    try {
        const payload = state.token.split('.')[1];
        state.claims = JSON.parse(atob(payload));
        
        // Show/hide admin specific features based on permissions
        const hasAdminRights = state.claims.permissions?.includes('ADMIN') || state.claims.permissions?.includes('MANAGE_USERS');
        if (hasAdminRights) {
            ui.btnDeleteProject.classList.remove('hidden');
        } else {
            ui.btnDeleteProject.classList.add('hidden');
        }
    } catch(e) {
        console.error("Failed to parse token", e);
    }
}

// --- Navigation & UI ---

function showView(viewName) {
    Object.values(views).forEach(v => v.classList.add('hidden'));
    views[viewName].classList.remove('hidden');
    if (viewName === 'app') {
        const initial = state.claims?.sub ? state.claims.sub.charAt(0).toUpperCase() : 'U';
        ui.userAvatar.innerText = initial;
        ui.userName.innerText = state.user?.firstName ? `${state.user.firstName} ${state.user.lastName}` : (state.claims?.sub || 'User');
        ui.userRole.innerText = state.claims?.permissions?.includes('ADMIN') ? 'Admin' : 'Member';
    }
}

function showModal(modalName) {
    modals[modalName].classList.remove('hidden');
}

function hideModals() {
    Object.values(modals).forEach(m => m.classList.add('hidden'));
}

function showAuthMessage(msg, isError = false) {
    ui.authMessage.textContent = msg;
    ui.authMessage.className = `alert ${isError ? 'alert-danger' : 'alert-success'}`;
    ui.authMessage.classList.remove('hidden');
    setTimeout(() => ui.authMessage.classList.add('hidden'), 5000);
}

// --- API Calls ---

const getHeaders = () => ({
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${state.token}`
});

async function fetchUser() {
    try {
        const res = await fetch('/api/auth/me', { headers: getHeaders() });
        if (res.ok) {
            state.user = await res.json();
            return state.user;
        }
    } catch(e) { console.error(e); }
    return null;
}

async function loadWorkspace() {
    try {
        const res = await fetch('/a/api/projects', { headers: getHeaders() });
        if (res.ok) {
            state.projects = await res.json();
            renderProjects();
        }
    } catch(e) { console.error("Error loading projects", e); }
}

async function selectProject(projectId) {
    state.currentProject = state.projects.find(p => p.id === projectId) || null;
    renderProjects(); // to update active state
    
    if (state.currentProject) {
        ui.noProjectSelected.style.display = 'none';
        ui.kanbanBoard.style.display = 'grid';
        ui.projectActions.style.display = 'flex';
        ui.currentProjectTitle.innerText = state.currentProject.name;
        await loadTasks(projectId);
    } else {
        ui.noProjectSelected.style.display = 'flex';
        ui.kanbanBoard.style.display = 'none';
        ui.projectActions.style.display = 'none';
        ui.currentProjectTitle.innerText = 'Select a Project';
    }
}

async function loadTasks(projectId) {
    try {
        const res = await fetch(`/b/api/tasks?projectId=${projectId}`, { headers: getHeaders() });
        if (res.ok) {
            state.tasks = await res.json();
            renderKanban();
        }
    } catch(e) { console.error("Error loading tasks", e); }
}

// --- Event Listeners ---

function setupEventListeners() {
    // Auth Toggles
    document.getElementById('go-to-register').addEventListener('click', (e) => {
        e.preventDefault();
        forms.login.classList.add('hidden');
        forms.register.classList.remove('hidden');
    });
    
    document.getElementById('go-to-login').addEventListener('click', (e) => {
        e.preventDefault();
        forms.register.classList.add('hidden');
        forms.login.classList.remove('hidden');
    });

    // Auth Submit
    forms.login.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            email: document.getElementById('login-email').value,
            password: document.getElementById('login-password').value
        };
        try {
            const res = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (res.ok && data.token) {
                state.token = data.token;
                localStorage.setItem('token', state.token);
                parseToken();
                state.user = await fetchUser();
                showView('app');
                loadWorkspace();
            } else {
                showAuthMessage(data.message || 'Login failed', true);
            }
        } catch(err) {
            showAuthMessage('Network error', true);
        }
    });

    forms.register.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            firstName: document.getElementById('reg-firstName').value,
            lastName: document.getElementById('reg-lastName').value,
            email: document.getElementById('reg-email').value,
            password: document.getElementById('reg-password').value,
            phoneNumber: document.getElementById('reg-phone').value
        };
        try {
            const res = await fetch('/api/auth/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            const data = await res.json();
            if (res.ok) {
                showAuthMessage('Registration successful! Check your email to verify your account.');
                forms.register.classList.add('hidden');
                forms.login.classList.remove('hidden');
            } else {
                showAuthMessage(data.message || 'Registration failed', true);
            }
        } catch(err) {
            showAuthMessage('Network error', true);
        }
    });

    // Basic App Navigation
    document.getElementById('btn-logout').addEventListener('click', logout);
    
    // Modals
    document.getElementById('btn-new-project').addEventListener('click', () => {
        forms.project.reset();
        showModal('project');
    });
    document.getElementById('btn-add-task').addEventListener('click', () => {
        forms.task.reset();
        showModal('task');
    });
    
    document.querySelectorAll('.btn-close, .btn-cancel').forEach(btn => {
        btn.addEventListener('click', hideModals);
    });

    // Forms inside App
    forms.project.addEventListener('submit', async (e) => {
        e.preventDefault();
        const pName = document.getElementById('proj-name').value;
        const pDesc = document.getElementById('proj-desc').value;
        try {
            const res = await fetch('/a/api/projects', {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify({ name: pName, description: pDesc })
            });
            if (res.ok) {
                const newProject = await res.json();
                state.projects.push(newProject);
                hideModals();
                selectProject(newProject.id);
            }
        } catch(err) { console.error(err); }
    });

    forms.task.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!state.currentProject) return;
        
        const payload = {
            title: document.getElementById('task-title').value,
            description: document.getElementById('task-desc').value,
            status: document.getElementById('task-status').value,
            projectId: state.currentProject.id
        };
        try {
            const res = await fetch('/b/api/tasks', {
                method: 'POST',
                headers: getHeaders(),
                body: JSON.stringify(payload)
            });
            if (res.ok) {
                const newTask = await res.json();
                state.tasks.push(newTask);
                hideModals();
                renderKanban();
            }
        } catch(err) { console.error(err); }
    });
    
    // Delete Project (Admin only)
    ui.btnDeleteProject.addEventListener('click', async () => {
        if (!state.currentProject) return;
        if(confirm(`Are you sure you want to delete "${state.currentProject.name}"?`)) {
            // Note: service-a doesn't have a DELETE endpoint in the spec, but we'd call it here.
            alert('Delete action simulated (Requires DELETE /a/api/projects/{id} implemented on Backend)');
        }
    });
}

function logout() {
    localStorage.removeItem('token');
    state.token = null;
    state.user = null;
    state.claims = null;
    showView('auth');
}

// --- Render Functions ---

function renderProjects() {
    ui.projectList.innerHTML = '';
    state.projects.forEach(p => {
        const li = document.createElement('li');
        li.innerHTML = `<i class="fa-solid fa-folder"></i> ${p.name}`;
        if (state.currentProject && state.currentProject.id === p.id) {
            li.classList.add('active');
        }
        li.addEventListener('click', () => selectProject(p.id));
        ui.projectList.appendChild(li);
    });
}

function renderKanban() {
    ui.listTodo.innerHTML = '';
    ui.listInProgress.innerHTML = '';
    ui.listDone.innerHTML = '';
    
    let counts = { TODO: 0, IN_PROGRESS: 0, DONE: 0 };
    
    state.tasks.forEach(task => {
        counts[task.status] = (counts[task.status] || 0) + 1;
        
        const card = document.createElement('div');
        card.className = 'task-card';
        card.innerHTML = `
            <h4>${task.title}</h4>
            ${task.description ? `<p>${task.description}</p>` : ''}
            <div class="task-footer">
                <span><i class="fa-regular fa-clock"></i> ${new Date(task.createdAt || Date.now()).toLocaleDateString()}</span>
                ${renderStatusDropdown(task)}
            </div>
        `;
        
        // Setup dropdown change listener to move tasks
        const select = card.querySelector('select');
        select.addEventListener('change', (e) => updateTaskStatus(task.id, e.target.value));
        
        if (task.status === 'TODO') ui.listTodo.appendChild(card);
        else if (task.status === 'IN_PROGRESS') ui.listInProgress.appendChild(card);
        else if (task.status === 'DONE') ui.listDone.appendChild(card);
        else ui.listTodo.appendChild(card); // fallback
    });
    
    ui.counts.todo.innerText = counts.TODO;
    ui.counts.inprogress.innerText = counts.IN_PROGRESS;
    ui.counts.done.innerText = counts.DONE;
}

function renderStatusDropdown(task) {
    const isAdmin = state.claims?.permissions?.includes('ADMIN') || state.claims?.permissions?.includes('MANAGE_USERS');
    
    return `
    <select class="status-selector" style="background:transparent; border:none; color:var(--text-muted); font-size: 0.75rem; cursor:pointer;" ${!isAdmin && false ? 'disabled' : ''}>
        <option value="TODO" ${task.status === 'TODO' ? 'selected' : ''}>To Do</option>
        <option value="IN_PROGRESS" ${task.status === 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
        <option value="DONE" ${task.status === 'DONE' ? 'selected' : ''}>Done</option>
    </select>
    `;
}

async function updateTaskStatus(taskId, newStatus) {
    try {
        const res = await fetch(`/b/api/tasks/${taskId}/status`, {
            method: 'PUT',
            headers: getHeaders(),
            body: JSON.stringify({ status: newStatus })
        });
        if (res.ok) {
            const updatedTask = await res.json();
            const index = state.tasks.findIndex(t => t.id === taskId);
            if (index !== -1) {
                state.tasks[index] = updatedTask;
                renderKanban();
            }
        }
    } catch(e) { console.error("Could not update status", e); }
}

// Bootstrap
document.addEventListener('DOMContentLoaded', init);
