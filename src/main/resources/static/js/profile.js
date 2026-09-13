/**
 * Frontend JavaScript for User Profile and Account Settings.
 */
document.addEventListener('DOMContentLoaded', () => {
    let currentUser = null;

    const authRequiredSection = document.getElementById('authRequiredSection');
    const profileContentSection = document.getElementById('profileContentSection');
    const authUserMenu = document.getElementById('authUserMenu');
    const currentUserName = document.getElementById('currentUserName');
    const navAdminLink = document.getElementById('navAdminLink');
    const btnLogout = document.getElementById('btnLogout');

    const displayProfileName = document.getElementById('displayProfileName');
    const displayProfileEmail = document.getElementById('displayProfileEmail');
    const displayProfileRole = document.getElementById('displayProfileRole');
    const displayProfileStatus = document.getElementById('displayProfileStatus');

    const updateProfileForm = document.getElementById('updateProfileForm');
    const profileNameInput = document.getElementById('profileNameInput');
    const profileEmailInput = document.getElementById('profileEmailInput');

    const changePasswordForm = document.getElementById('changePasswordForm');
    const currentPasswordInput = document.getElementById('currentPasswordInput');
    const newPasswordInput = document.getElementById('newPasswordInput');
    const confirmPasswordInput = document.getElementById('confirmPasswordInput');

    // Initialize
    checkAuth();

    if (btnLogout) {
        btnLogout.addEventListener('click', handleLogout);
    }

    if (updateProfileForm) {
        updateProfileForm.addEventListener('submit', handleUpdateProfile);
    }

    if (changePasswordForm) {
        changePasswordForm.addEventListener('submit', handleChangePassword);
    }

    function checkAuth() {
        fetch('/api/auth/me')
            .then(res => {
                if (res.ok) return res.json();
                throw new Error('Not authenticated');
            })
            .then(user => {
                currentUser = user;
                if (currentUserName) currentUserName.textContent = user.name;
                if (authUserMenu) authUserMenu.classList.remove('d-none');
                if (authUserMenu) authUserMenu.classList.add('d-flex');

                if (user.role === 'ADMIN') {
                    document.querySelectorAll('.navbar-nav .nav-item a').forEach(el => {
                        const href = el.getAttribute('href');
                        if (href && href !== 'profile.html' && el.id !== 'navAdminLink') {
                            const li = el.closest('.nav-item');
                            if (li) li.classList.add('d-none');
                        }
                    });
                    if (navAdminLink) {
                        navAdminLink.classList.remove('d-none');
                    }
                }

                if (authRequiredSection) authRequiredSection.classList.add('d-none');
                if (profileContentSection) profileContentSection.classList.remove('d-none');

                loadProfile();
            })
            .catch(() => {
                if (authRequiredSection) authRequiredSection.classList.remove('d-none');
                if (profileContentSection) profileContentSection.classList.add('d-none');
                if (authUserMenu) authUserMenu.classList.add('d-none');
            });
    }

    function loadProfile() {
        fetch('/api/profile')
            .then(res => {
                if (!res.ok) throw new Error('Failed to load profile');
                return res.json();
            })
            .then(profile => {
                if (displayProfileName) displayProfileName.textContent = profile.name;
                if (displayProfileEmail) displayProfileEmail.textContent = profile.email;
                if (displayProfileRole) displayProfileRole.textContent = profile.role;
                if (displayProfileStatus) {
                    displayProfileStatus.textContent = profile.enabled ? 'Active' : 'Disabled';
                    displayProfileStatus.className = `badge ${profile.enabled ? 'bg-success-subtle text-success border-success' : 'bg-danger-subtle text-danger border-danger'} border rounded-pill px-3 py-1`;
                }

                if (profileNameInput) profileNameInput.value = profile.name;
                if (profileEmailInput) profileEmailInput.value = profile.email;
            })
            .catch(err => {
                showToast(err.message, 'danger');
            });
    }

    function handleUpdateProfile(e) {
        e.preventDefault();
        const name = profileNameInput.value.trim();
        if (!name) {
            showToast('Name cannot be blank', 'warning');
            return;
        }

        fetch('/api/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name })
        })
        .then(res => {
            if (res.ok) return res.json();
            return res.json().then(data => { throw new Error(data.message || 'Failed to update profile'); });
        })
        .then(updated => {
            if (currentUserName) currentUserName.textContent = updated.name;
            if (displayProfileName) displayProfileName.textContent = updated.name;
            showToast('Profile name updated successfully!', 'success');
        })
        .catch(err => {
            showToast(err.message, 'danger');
        });
    }

    function handleChangePassword(e) {
        e.preventDefault();
        const currentPassword = currentPasswordInput.value;
        const newPassword = newPasswordInput.value;
        const confirmPassword = confirmPasswordInput.value;

        if (!currentPassword) {
            showToast('Current password is required', 'warning');
            return;
        }

        if (!newPassword || newPassword.length < 6) {
            showToast('New password must be at least 6 characters', 'warning');
            return;
        }

        if (newPassword !== confirmPassword) {
            showToast('New password and confirmation do not match', 'warning');
            return;
        }

        fetch('/api/profile/password', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                currentPassword,
                newPassword,
                confirmPassword
            })
        })
        .then(res => {
            if (res.ok) return res.json();
            return res.json().then(data => { throw new Error(data.message || 'Failed to change password'); });
        })
        .then(() => {
            changePasswordForm.reset();
            showToast('Password updated successfully!', 'success');
        })
        .catch(err => {
            showToast(err.message, 'danger');
        });
    }

    function handleLogout() {
        fetch('/api/auth/logout', { method: 'POST' })
            .finally(() => {
                window.location.href = 'index.html';
            });
    }

    function showToast(message, type = 'info') {
        const toastEl = document.getElementById('actionToast');
        const toastMsg = document.getElementById('toastMessage');
        if (!toastEl || !toastMsg) return;

        toastEl.className = `toast align-items-center text-white border-0 bg-${type}`;
        toastMsg.textContent = message;

        const toast = new bootstrap.Toast(toastEl, { delay: 3500 });
        toast.show();
    }
});
