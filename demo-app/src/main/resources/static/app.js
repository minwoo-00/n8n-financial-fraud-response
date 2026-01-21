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
        return { success: true, data: responseText };
    } catch (error) {
        statusElement.classList.remove('info');
        statusElement.classList.add('error');
        statusElement.textContent = error.message || 'Request failed';
        return { success: false, error: error.message };
    }
}

// 추가 인증 모달 표시 함수
function showVerificationModal(originalPayload, statusElement) {
    const modal = document.createElement('div');
    modal.className = 'verification-modal';
    modal.innerHTML = `
        <div class="verification-content">
            <h3>보안 확인</h3>
            <p>추가 인증이 필요합니다. ID와 비밀번호를 다시 입력해주세요.</p>
            <form id="verification-form" class="form">
                <div class="form-group">
                    <label class="form-label">User ID</label>
                    <input type="text" id="verify-userId" class="form-input" readonly value="${originalPayload.userId}">
                </div>
                <div class="form-group">
                    <label class="form-label">Password</label>
                    <input type="password" id="verify-password" class="form-input" placeholder="비밀번호 입력" required autocomplete="off">
                </div>
                <div class="button-group">
                    <button type="submit" class="form-submit">확인</button>
                    <button type="button" class="cancel-btn form-submit">취소</button>
                </div>
            </form>
            <div id="verification-status" class="status" role="status"></div>
        </div>
    `;

    document.body.appendChild(modal);

    const form = modal.querySelector('#verification-form');
    const cancelBtn = modal.querySelector('.cancel-btn');
    const verificationStatus = modal.querySelector('#verification-status');

    // 취소 버튼
    cancelBtn.addEventListener('click', () => {
        document.body.removeChild(modal);
        statusElement.classList.remove('info', 'success');
        statusElement.classList.add('error');
        statusElement.textContent = '송금이 취소되었습니다.';
    });

    // 인증 폼 제출
    form.addEventListener('submit', async (event) => {
        event.preventDefault();

        const userId = form.querySelector('#verify-userId').value;
        const password = form.querySelector('#verify-password').value;

        // 비밀번호 확인
        const loginPayload = { userId, password, country: originalPayload.country };

        // 로그인 검증 요청
        verificationStatus.classList.remove('success', 'error', 'info');
        verificationStatus.textContent = '인증 확인 중...';
        verificationStatus.classList.add('visible', 'info');

        try {
            const response = await fetch('/auth/verify', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(loginPayload),
            });

            const responseText = await response.text();

            if (!response.ok || responseText !== 'VERIFIED') {
                // 인증 실패 - 모달 유지하고 에러 표시
                verificationStatus.classList.remove('info');
                verificationStatus.classList.add('error');
                verificationStatus.textContent = '인증 실패: 비밀번호를 확인해주세요.';

                // 비밀번호 필드 초기화
                form.querySelector('#verify-password').value = '';
                form.querySelector('#verify-password').focus();

                return; // 모달 닫지 않고 종료
            }

            // 인증 성공 - 모달 닫고 verified=true로 재송금
            document.body.removeChild(modal);

            const verifiedPayload = {
                ...originalPayload,
                verified: true
            };

            await sendTransferRequest(verifiedPayload, statusElement);

        } catch (error) {
            verificationStatus.classList.remove('info');
            verificationStatus.classList.add('error');
            verificationStatus.textContent = '인증 실패: ' + (error.message || '오류가 발생했습니다.');

            // 비밀번호 필드 초기화
            form.querySelector('#verify-password').value = '';
            form.querySelector('#verify-password').focus();
        }
    });
}

// 송금 요청 함수 (재사용 가능)
async function sendTransferRequest(payload, statusElement) {
    statusElement.classList.remove('success', 'error', 'info');
    statusElement.textContent = 'Processing transfer...';
    statusElement.classList.add('visible', 'info');

    try {
        const params = new URLSearchParams();
        params.append('userId', payload.userId);
        params.append('amount', payload.amount);
        if (payload.country) params.append('country', payload.country);
        if (payload.verified) params.append('verified', 'true');

        const response = await fetch(`/api/transfer?${params.toString()}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.status === 'VERIFICATION_REQUIRED') {
            // 추가 인증 필요
            statusElement.classList.remove('info');
            statusElement.classList.add('info');
            statusElement.textContent = result.message;

            // 인증 모달 표시
            showVerificationModal(payload, statusElement);
            return;
        }

        if (result.status === 'BLOCKED') {
            // 계정 차단
            statusElement.classList.remove('info');
            statusElement.classList.add('error');
            statusElement.textContent = result.message;
            return;
        }

        if (result.status === 'SUCCESS') {
            // 송금 성공
            statusElement.classList.remove('info');
            statusElement.classList.add('success');
            statusElement.innerHTML = `송금이 완료되었습니다<br>금액: ${result.amount.toLocaleString()}원<br>은행: ${result.toBank}`;
            return;
        }

        // 기타 오류
        throw new Error(result.message || 'Transfer failed');

    } catch (error) {
        statusElement.classList.remove('info');
        statusElement.classList.add('error');
        statusElement.textContent = error.message || 'Transfer failed';
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

    // 상태 초기화
    loginStatus.classList.remove('success', 'error', 'info', 'login-failure');
    loginStatus.textContent = 'Processing request...';
    loginStatus.classList.add('visible', 'info');

    try {
        const response = await fetch('/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
        });

        const responseText = await response.text();

        if (responseText === 'LOGIN_SUCCESS') {
            loginStatus.classList.remove('info');
            loginStatus.classList.add('success');
            loginStatus.textContent = 'LOGIN_SUCCESS';
            setAuthState({ userId, country });
            updateNavAvailability(true);
            setActiveSection('transfer');
        } else if (responseText === 'LOGIN_FAILURE') {
            loginStatus.classList.remove('info', 'success', 'error');
            loginStatus.classList.add('login-failure', 'visible');
            loginStatus.textContent = 'LOGIN_FAILURE';
        } else if (responseText.includes('BLOCKED')) {
            loginStatus.classList.remove('info', 'success');
            loginStatus.classList.add('error');
            loginStatus.textContent = '계정이 차단되었습니다.';
        } else {
            loginStatus.classList.remove('info');
            loginStatus.classList.add('error');
            loginStatus.textContent = responseText || 'Login failed';
        }
    } catch (error) {
        loginStatus.classList.remove('info');
        loginStatus.classList.add('error');
        loginStatus.textContent = error.message || 'Login failed';
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
        verified: false
    };

    sendTransferRequest(payload, transferStatus);
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

    const result = await sendRequest('/auth/logout', payload, logoutStatus);
    if (result.success) {
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