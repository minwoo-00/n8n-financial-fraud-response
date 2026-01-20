const navButtons = document.querySelectorAll('.nav-btn');
const sections = document.querySelectorAll('.section');
const authState = document.getElementById('auth-state');
const currentUser = document.getElementById('current-user');

const loginForm = document.getElementById('login-form');
const transferForm = document.getElementById('transfer-form');
const logoutForm = document.getElementById('logout-form');

const loginStatus = document.getElementById('login-status');
const transferStatus = document.getElementById('transfer-status');
const logoutStatus = document.getElementById('logout-status');

const AUTH_STORAGE_KEY = 'fds-auth';

function getAuthState() {
    const raw = window.sessionStorage.getItem(AUTH_STORAGE_KEY);
    if (!raw) {
        return null;
    }

    try {
        return JSON.parse(raw);
    } catch (error) {
        return null;
    }
}

function setAuthState(state) {
    if (!state) {
        window.sessionStorage.removeItem(AUTH_STORAGE_KEY);
        authState?.setAttribute('data-authenticated', 'false');
        if (currentUser) {
            currentUser.textContent = '';
        }
        return;
    }

    window.sessionStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(state));
    authState?.setAttribute('data-authenticated', 'true');
    if (currentUser) {
        currentUser.textContent = `User ${state.userId}`;
    }
}

function updateNavAvailability(isAuthenticated) {
    navButtons.forEach((button) => {
        const section = button.dataset.section;
        if (section === 'login') {
            return;
        }

        if (isAuthenticated) {
            button.classList.remove('disabled');
            button.removeAttribute('disabled');
        } else {
            button.classList.add('disabled');
            button.setAttribute('disabled', 'disabled');
        }
    });
}

function setActiveSection(target) {
    sections.forEach((section) => {
        section.classList.toggle('active', section.id === `${target}-section`);
    });

    navButtons.forEach((button) => {
        button.classList.toggle('active', button.dataset.section === target);
    });
}

function ensureAuthenticatedSection() {
    const state = getAuthState();
    if (!state) {
        setActiveSection('login');
    }
}

navButtons.forEach((button) => {
    button.addEventListener('click', () => {
        if (button.hasAttribute('disabled')) {
            return;
        }
        setActiveSection(button.dataset.section);
    });
});

async function sendRequest(url, payload, statusElement) {
    statusElement.classList.remove('success', 'error', 'info');
    statusElement.textContent = 'Processing request...';
    statusElement.classList.add('visible', 'info');

    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
        });

        const responseText = await response.text();

        if (!response.ok) {
            throw new Error(responseText || 'Request failed');
        }

        statusElement.classList.remove('info');
        statusElement.classList.add('success');
        statusElement.textContent = responseText || 'Success';
        return true;
    } catch (error) {
        statusElement.classList.remove('info');
        statusElement.classList.add('error');
        statusElement.textContent = error.message || 'Request failed';
        return false;
    }
}

loginForm?.addEventListener('submit', async (event) => {
    event.preventDefault();

    const userId = loginForm.querySelector('#login-userId').value.trim();
    const password = loginForm.querySelector('#login-password').value;
    const country = loginForm.querySelector('#login-country').value;

    const payload = {
        userId,
        password,
        country,
    };

    const success = await sendRequest('/auth/login', payload, loginStatus);
    if (success) {
        setAuthState({ userId, country });
        updateNavAvailability(true);
        setActiveSection('transfer');
    }
});

transferForm?.addEventListener('submit', (event) => {
    event.preventDefault();

    const state = getAuthState();
    if (!state) {
        updateNavAvailability(false);
        setActiveSection('login');
        return;
    }

    const payload = {
        userId: state.userId,
        country: state.country,
        amount: Number(transferForm.querySelector('#transfer-amount').value),
    };

    sendRequest('/transfer', payload, transferStatus);
});

logoutForm?.addEventListener('submit', async (event) => {
    event.preventDefault();

    const state = getAuthState();
    if (!state) {
        updateNavAvailability(false);
        setActiveSection('login');
        return;
    }

    const payload = {
        userId: state.userId,
        country: state.country,
        password: 'logout',
    };

    const success = await sendRequest('/auth/logout', payload, logoutStatus);
    if (success) {
        setAuthState(null);
        updateNavAvailability(false);
        setActiveSection('login');
    }
});

const existingState = getAuthState();
updateNavAvailability(Boolean(existingState));
if (existingState) {
    authState?.setAttribute('data-authenticated', 'true');
    if (currentUser) {
        currentUser.textContent = `User ${existingState.userId}`;
    }
}
ensureAuthenticatedSection();
