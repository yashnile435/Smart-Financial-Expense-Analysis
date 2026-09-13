/**
 * Admin Management Frontend Logic
 * Smart Financial Expense Analysis - Stage 8
 */

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

let currentUser = null;
let addCategoryModal = null;
let editCategoryModal = null;
let addPaymentOptionModal = null;
let editPaymentOptionModal = null;

async function initApp() {
    setupModals();
    setupEventListeners();
    const authorized = await checkAuthAndRole();
    if (authorized) {
        await loadAllAdminData();
    }
}

function setupModals() {
    const addCatEl = document.getElementById('addCategoryModal');
    if (addCatEl) {
        addCategoryModal = new bootstrap.Modal(addCatEl);
    }
    const editCatEl = document.getElementById('editCategoryModal');
    if (editCatEl) {
        editCategoryModal = new bootstrap.Modal(editCatEl);
    }
    const addPoEl = document.getElementById('addPaymentOptionModal');
    if (addPoEl) {
        addPaymentOptionModal = new bootstrap.Modal(addPoEl);
    }
    const editPoEl = document.getElementById('editPaymentOptionModal');
    if (editPoEl) {
        editPaymentOptionModal = new bootstrap.Modal(editPoEl);
    }
}

function setupEventListeners() {
    // Logout
    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) {
        btnLogout.addEventListener('click', handleLogout);
    }

    // Refresh all
    const btnRefresh = document.getElementById('btnRefreshAll');
    if (btnRefresh) {
        btnRefresh.addEventListener('click', () => {
            loadAllAdminData();
            showToast('Admin data refreshed', true);
        });
    }

    // User Search
    const btnSearchUsers = document.getElementById('btnSearchUsers');
    if (btnSearchUsers) {
        btnSearchUsers.addEventListener('click', () => {
            const query = document.getElementById('userSearchInput').value.trim();
            loadUsers(query);
        });
    }

    const userSearchInput = document.getElementById('userSearchInput');
    if (userSearchInput) {
        userSearchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                loadUsers(userSearchInput.value.trim());
            }
        });
    }

    const btnResetSearch = document.getElementById('btnResetSearch');
    if (btnResetSearch) {
        btnResetSearch.addEventListener('click', () => {
            document.getElementById('userSearchInput').value = '';
            loadUsers('');
        });
    }

    // Add Category Form
    const addCategoryForm = document.getElementById('addCategoryForm');
    if (addCategoryForm) {
        addCategoryForm.addEventListener('submit', handleAddCategory);
    }

    // Edit Category Form
    const editCategoryForm = document.getElementById('editCategoryForm');
    if (editCategoryForm) {
        editCategoryForm.addEventListener('submit', handleEditCategory);
    }

    // Add Payment Option Form
    const addPaymentOptionForm = document.getElementById('addPaymentOptionForm');
    if (addPaymentOptionForm) {
        addPaymentOptionForm.addEventListener('submit', handleAddPaymentOption);
    }

    // Edit Payment Option Form
    const editPaymentOptionForm = document.getElementById('editPaymentOptionForm');
    if (editPaymentOptionForm) {
        editPaymentOptionForm.addEventListener('submit', handleEditPaymentOption);
    }
}

// Check session authentication and ROLE_ADMIN authorization
async function checkAuthAndRole() {
    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (!response.ok) {
            showUnauthenticated();
            return false;
        }

        currentUser = await response.json();

        // Check if role is ADMIN
        if (currentUser.role !== 'ADMIN') {
            showAccessDenied();
            return false;
        }

        // Successfully authorized as ADMIN
        showAdminDashboard();
        return true;
    } catch (err) {
        showUnauthenticated();
        return false;
    }
}

function showUnauthenticated() {
    document.getElementById('authRequiredSection').classList.remove('d-none');
    document.getElementById('accessDeniedSection').classList.add('d-none');
    document.getElementById('adminMainSection').classList.add('d-none');
    document.getElementById('authUserMenu').classList.add('d-none');
    document.getElementById('authUserMenu').classList.remove('d-flex');
}

function showAccessDenied() {
    document.getElementById('authRequiredSection').classList.add('d-none');
    document.getElementById('accessDeniedSection').classList.remove('d-none');
    document.getElementById('adminMainSection').classList.add('d-none');
    document.getElementById('authUserMenu').classList.remove('d-none');
    document.getElementById('authUserMenu').classList.add('d-flex');
    document.getElementById('currentUserName').textContent = currentUser.name || 'User';
}

function showAdminDashboard() {
    document.getElementById('authRequiredSection').classList.add('d-none');
    document.getElementById('accessDeniedSection').classList.add('d-none');
    document.getElementById('adminMainSection').classList.remove('d-none');
    document.getElementById('authUserMenu').classList.remove('d-none');
    document.getElementById('authUserMenu').classList.add('d-flex');
    document.getElementById('currentUserName').textContent = currentUser.name || 'Admin';
}

async function handleLogout() {
    try {
        await fetch('/api/auth/logout', { method: 'POST' });
    } finally {
        window.location.href = 'index.html';
    }
}

// Load all admin modules
async function loadAllAdminData() {
    await Promise.all([
        loadSummaryStats(),
        loadUsers(),
        loadCategories(),
        loadPaymentOptions(),
        loadFinancialOverview(),
        loadBudgets()
    ]);
}

// ===================================================================
// System Overview Summary
// ===================================================================

async function loadSummaryStats() {
    try {
        const [sumRes, expSumRes] = await Promise.all([
            fetch('/api/admin/summary'),
            fetch('/api/admin/expenses/summary')
        ]);

        if (sumRes.ok) {
            const data = await sumRes.json();
            document.getElementById('statTotalUsers').textContent = data.totalUsers ?? 0;
            document.getElementById('statActiveUsers').textContent = `Active: ${data.activeUsers ?? 0}`;
            document.getElementById('statTotalExpenses').textContent = data.totalExpenses ?? 0;
            document.getElementById('statCurrentMonthAmount').textContent = `This Month: ${formatInr(data.currentMonthExpenseAmount)}`;
            document.getElementById('statTotalExpenseAmount').textContent = formatInr(data.totalExpenseAmount);
            document.getElementById('statTotalBudgets').textContent = data.totalBudgets ?? 0;
            document.getElementById('statTotalBudgetAmount').textContent = `Allocated: ${formatInr(data.totalBudgetAmount)}`;
        }

        if (expSumRes.ok) {
            const expData = await expSumRes.json();
            document.getElementById('statAvgExpense').textContent = `Avg: ${formatInr(expData.averageExpense)}`;
            document.getElementById('finAvgExpense').textContent = formatInr(expData.averageExpense);
            document.getElementById('finMaxExpense').textContent = formatInr(expData.highestExpense);
            document.getElementById('finCurrentMonth').textContent = formatInr(expData.currentMonthExpenseAmount);
        }
    } catch (err) {
        console.error('Error loading summary stats:', err);
    }
}

// ===================================================================
// User Management
// ===================================================================

async function loadUsers(search = '') {
    const tbody = document.getElementById('userTableBody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Loading users...</td></tr>';

    try {
        let url = '/api/admin/users';
        if (search) {
            url += `?search=${encodeURIComponent(search)}`;
        }

        const res = await fetch(url);
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-danger">Failed to load users</td></tr>';
            return;
        }

        const users = await res.json();
        if (users.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No users found matching your criteria.</td></tr>';
            return;
        }

        tbody.innerHTML = users.map(user => {
            const isSelf = currentUser && user.id === currentUser.id;
            const statusBadge = user.enabled
                ? '<span class="badge bg-success-subtle text-success border border-success px-2 py-1">Active</span>'
                : '<span class="badge bg-secondary-subtle text-secondary border border-secondary px-2 py-1">Disabled</span>';

            const roleBadge = user.role === 'ADMIN'
                ? '<span class="badge bg-danger-subtle text-danger border border-danger px-2 py-1">ADMIN</span>'
                : '<span class="badge bg-primary-subtle text-primary border border-primary px-2 py-1">USER</span>';

            const statusBtnText = user.enabled ? 'Disable' : 'Enable';
            const statusBtnClass = user.enabled ? 'btn-outline-danger' : 'btn-outline-success';
            const nextStatus = !user.enabled;

            const nextRole = user.role === 'ADMIN' ? 'USER' : 'ADMIN';
            const roleBtnText = user.role === 'ADMIN' ? 'Demote to USER' : 'Promote to ADMIN';
            const roleBtnClass = user.role === 'ADMIN' ? 'btn-outline-warning' : 'btn-outline-primary';

            return `
                <tr>
                    <td class="fw-semibold text-secondary">#${user.id}</td>
                    <td class="fw-semibold text-dark">
                        ${escapeHtml(user.name)}
                        ${isSelf ? '<span class="badge bg-info-subtle text-info ms-1">You</span>' : ''}
                    </td>
                    <td class="text-secondary">${escapeHtml(user.email)}</td>
                    <td>${roleBadge}</td>
                    <td>${statusBadge}</td>
                    <td class="text-end">
                        <div class="btn-group btn-group-sm">
                            <button class="btn ${statusBtnClass}" 
                                onclick="handleToggleStatus(${user.id}, ${nextStatus}, '${escapeHtml(user.name)}')"
                                ${isSelf ? 'disabled title="Cannot disable your own account"' : ''}>
                                ${statusBtnText}
                            </button>
                            <button class="btn ${roleBtnClass}"
                                onclick="handleChangeRole(${user.id}, '${nextRole}', '${escapeHtml(user.name)}')"
                                ${isSelf ? 'disabled title="Cannot demote your own account"' : ''}>
                                ${roleBtnText}
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-danger">Error loading users</td></tr>';
    }
}

async function handleToggleStatus(userId, enabled, userName) {
    const action = enabled ? 'enable' : 'disable';
    if (!confirm(`Are you sure you want to ${action} account for "${userName}"?`)) {
        return;
    }

    try {
        const res = await fetch(`/api/admin/users/${userId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ enabled: enabled })
        });

        const data = await res.json();
        if (res.ok) {
            showToast(`User "${userName}" ${enabled ? 'enabled' : 'disabled'} successfully.`, true);
            const search = document.getElementById('userSearchInput').value.trim();
            loadUsers(search);
            loadSummaryStats();
        } else {
            showToast(data.message || 'Failed to update user status.', false);
        }
    } catch (err) {
        showToast('Error updating user status.', false);
    }
}

async function handleChangeRole(userId, newRole, userName) {
    if (!confirm(`Are you sure you want to change role for "${userName}" to ${newRole}?`)) {
        return;
    }

    try {
        const res = await fetch(`/api/admin/users/${userId}/role`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ role: newRole })
        });

        const data = await res.json();
        if (res.ok) {
            showToast(`Role for "${userName}" updated to ${newRole}.`, true);
            const search = document.getElementById('userSearchInput').value.trim();
            loadUsers(search);
        } else {
            showToast(data.message || 'Failed to update user role.', false);
        }
    } catch (err) {
        showToast('Error updating user role.', false);
    }
}

// ===================================================================
// Category Management
// ===================================================================

async function loadCategories() {
    const tbody = document.getElementById('categoryTableBody');
    tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">Loading categories...</td></tr>';

    try {
        const res = await fetch('/api/admin/categories');
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-danger">Failed to load categories</td></tr>';
            return;
        }

        const categories = await res.json();
        if (categories.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">No categories available.</td></tr>';
            return;
        }

        tbody.innerHTML = categories.map(cat => {
            return `
                <tr>
                    <td class="fw-semibold text-secondary">#${cat.id}</td>
                    <td class="fw-bold text-dark">${escapeHtml(cat.name)}</td>
                    <td class="text-secondary small">${cat.description ? escapeHtml(cat.description) : '<span class="text-muted fst-italic">No description</span>'}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary me-1" 
                            onclick="openEditCategoryModal(${cat.id}, '${escapeHtml(cat.name)}', '${escapeHtml(cat.description || '')}')">
                            <i class="bi bi-pencil-square"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-outline-danger" 
                            onclick="handleDeleteCategory(${cat.id}, '${escapeHtml(cat.name)}')">
                            <i class="bi bi-trash"></i> Delete
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-danger">Error loading categories</td></tr>';
    }
}

async function handleAddCategory(e) {
    e.preventDefault();
    const name = document.getElementById('newCategoryName').value.trim();
    const description = document.getElementById('newCategoryDesc').value.trim();

    if (!name) {
        showToast('Category name cannot be blank', false);
        return;
    }

    try {
        const res = await fetch('/api/admin/categories', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ name, description })
        });

        const data = await res.json();
        if (res.ok) {
            showToast(`Category "${data.name}" created successfully.`, true);
            document.getElementById('addCategoryForm').reset();
            if (addCategoryModal) addCategoryModal.hide();
            loadCategories();
            loadFinancialOverview();
        } else {
            showToast(data.message || 'Failed to create category.', false);
        }
    } catch (err) {
        showToast('Error creating category.', false);
    }
}

function openEditCategoryModal(id, name, description) {
    document.getElementById('editCategoryId').value = id;
    document.getElementById('editCategoryName').value = name;
    document.getElementById('editCategoryDesc').value = description;
    if (editCategoryModal) editCategoryModal.show();
}

async function handleEditCategory(e) {
    e.preventDefault();
    const id = document.getElementById('editCategoryId').value;
    const name = document.getElementById('editCategoryName').value.trim();
    const description = document.getElementById('editCategoryDesc').value.trim();

    if (!name) {
        showToast('Category name cannot be blank', false);
        return;
    }

    try {
        const res = await fetch(`/api/admin/categories/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ name, description })
        });

        const data = await res.json();
        if (res.ok) {
            showToast(`Category "${data.name}" updated successfully.`, true);
            if (editCategoryModal) editCategoryModal.hide();
            loadCategories();
            loadFinancialOverview();
        } else {
            showToast(data.message || 'Failed to update category.', false);
        }
    } catch (err) {
        showToast('Error updating category.', false);
    }
}

async function handleDeleteCategory(id, name) {
    if (!confirm(`Are you sure you want to delete category "${name}"?\nNote: If any expenses are currently using this category, deletion will be rejected.`)) {
        return;
    }

    try {
        const res = await fetch(`/api/admin/categories/${id}`, {
            method: 'DELETE',
            headers: { 'Accept': 'application/json' }
        });

        const data = await res.json();
        if (res.ok) {
            showToast(`Category "${name}" deleted successfully.`, true);
            loadCategories();
            loadFinancialOverview();
        } else {
            // Displays 409 conflict message cleanly
            showToast(data.message || 'Cannot delete category.', false);
        }
    } catch (err) {
        showToast('Error deleting category.', false);
    }
}

// ===================================================================
// Payment Options Management
// ===================================================================

async function loadPaymentOptions() {
    const tbody = document.getElementById('paymentOptionsTableBody');
    if (!tbody) return;
    tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div> Loading payment options...</td></tr>';

    try {
        const res = await fetch('/api/admin/payment-options');
        if (!res.ok) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-danger">Failed to load payment options</td></tr>';
            return;
        }

        const options = await res.json();
        if (options.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted">No payment options found.</td></tr>';
            return;
        }

        tbody.innerHTML = options.map(po => {
            const statusBadge = po.active 
                ? '<span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill px-2 py-1"><i class="bi bi-check-circle me-1"></i>Active</span>'
                : '<span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle rounded-pill px-2 py-1"><i class="bi bi-pause-circle me-1"></i>Inactive</span>';
            const toggleText = po.active ? 'Deactivate' : 'Activate';
            const toggleIcon = po.active ? 'bi-pause-fill' : 'bi-play-fill';
            const toggleClass = po.active ? 'btn-outline-warning' : 'btn-outline-success';

            return `
                <tr>
                    <td class="fw-semibold text-secondary">#${po.id}</td>
                    <td class="fw-bold text-dark">${escapeHtml(po.name)}</td>
                    <td class="text-secondary small">${po.description ? escapeHtml(po.description) : '<span class="text-muted fst-italic">No description</span>'}</td>
                    <td>${statusBadge}</td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary me-1" 
                            onclick="openEditPaymentOptionModal(${po.id}, '${escapeHtml(po.name)}', '${escapeHtml(po.description || '')}', ${po.active})" title="Edit Option">
                            <i class="bi bi-pencil-square"></i> Edit
                        </button>
                        <button class="btn btn-sm ${toggleClass} me-1" 
                            onclick="handleTogglePaymentOption(${po.id}, ${!po.active}, '${escapeHtml(po.name)}')" title="${toggleText}">
                            <i class="bi ${toggleIcon}"></i> ${toggleText}
                        </button>
                        <button class="btn btn-sm btn-outline-danger" 
                            onclick="handleDeletePaymentOption(${po.id}, '${escapeHtml(po.name)}')" title="Delete Option">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                </tr>
            `;
        }).join('');
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-danger">Error loading payment options</td></tr>';
    }
}

async function handleAddPaymentOption(e) {
    e.preventDefault();
    const name = document.getElementById('newPaymentOptionName').value.trim();
    const description = document.getElementById('newPaymentOptionDesc').value.trim();
    const active = document.getElementById('newPaymentOptionActive').checked;
    const alertEl = document.getElementById('addPaymentOptionAlert');
    if (alertEl) alertEl.classList.add('d-none');

    if (!name) {
        if (alertEl) {
            alertEl.textContent = 'Payment option name cannot be blank';
            alertEl.classList.remove('d-none');
        }
        return;
    }

    try {
        const res = await fetch('/api/admin/payment-options', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ name, description, active })
        });

        const data = await res.json();
        if (res.ok) {
            showToast(`Payment option "${data.name}" created successfully.`, true);
            document.getElementById('addPaymentOptionForm').reset();
            document.getElementById('newPaymentOptionActive').checked = true;
            if (addPaymentOptionModal) addPaymentOptionModal.hide();
            loadPaymentOptions();
        } else {
            if (alertEl) {
                alertEl.textContent = data.message || 'Failed to create payment option.';
                alertEl.classList.remove('d-none');
            } else {
                showToast(data.message || 'Failed to create payment option.', false);
            }
        }
    } catch (err) {
        if (alertEl) {
            alertEl.textContent = 'Error communicating with server.';
            alertEl.classList.remove('d-none');
        }
    }
}

function openEditPaymentOptionModal(id, name, description, active) {
    document.getElementById('editPaymentOptionId').value = id;
    document.getElementById('editPaymentOptionName').value = name;
    document.getElementById('editPaymentOptionDesc').value = description || '';
    document.getElementById('editPaymentOptionActive').checked = !!active;
    const alertEl = document.getElementById('editPaymentOptionAlert');
    if (alertEl) alertEl.classList.add('d-none');
    if (editPaymentOptionModal) editPaymentOptionModal.show();
}

async function handleEditPaymentOption(e) {
    e.preventDefault();
    const id = document.getElementById('editPaymentOptionId').value;
    const name = document.getElementById('editPaymentOptionName').value.trim();
    const description = document.getElementById('editPaymentOptionDesc').value.trim();
    const active = document.getElementById('editPaymentOptionActive').checked;
    const alertEl = document.getElementById('editPaymentOptionAlert');
    if (alertEl) alertEl.classList.add('d-none');

    if (!name) {
        if (alertEl) {
            alertEl.textContent = 'Payment option name cannot be blank';
            alertEl.classList.remove('d-none');
        }
        return;
    }

    try {
        const res = await fetch(`/api/admin/payment-options/${id}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ name, description, active })
        });

        const data = await res.json();
        if (res.ok) {
            showToast(`Payment option "${data.name}" updated successfully.`, true);
            if (editPaymentOptionModal) editPaymentOptionModal.hide();
            loadPaymentOptions();
        } else {
            if (alertEl) {
                alertEl.textContent = data.message || 'Failed to update payment option.';
                alertEl.classList.remove('d-none');
            } else {
                showToast(data.message || 'Failed to update payment option.', false);
            }
        }
    } catch (err) {
        if (alertEl) {
            alertEl.textContent = 'Error communicating with server.';
            alertEl.classList.remove('d-none');
        }
    }
}

async function handleTogglePaymentOption(id, active, name) {
    try {
        const res = await fetch(`/api/admin/payment-options/${id}/status?active=${active}`, {
            method: 'PATCH',
            headers: { 'Accept': 'application/json' }
        });
        const data = await res.json();
        if (res.ok) {
            showToast(`Payment option "${name}" is now ${active ? 'Active' : 'Inactive'}.`, true);
            loadPaymentOptions();
        } else {
            showToast(data.message || 'Failed to update status.', false);
        }
    } catch (err) {
        showToast('Error updating payment option status.', false);
    }
}

async function handleDeletePaymentOption(id, name) {
    if (!confirm(`Are you sure you want to delete payment option "${name}"?\n(If it is referenced by existing transactions, it will be safely deactivated instead of deleted.)`)) {
        return;
    }

    try {
        const res = await fetch(`/api/admin/payment-options/${id}`, {
            method: 'DELETE',
            headers: { 'Accept': 'application/json' }
        });
        const data = await res.json();
        if (res.ok) {
            showToast(data.message || `Payment option "${name}" processed.`, true);
            loadPaymentOptions();
        } else {
            showToast(data.message || 'Failed to delete payment option.', false);
        }
    } catch (err) {
        showToast('Error deleting payment option.', false);
    }
}

// Global window bindings for HTML onclick attributes
window.openEditPaymentOptionModal = openEditPaymentOptionModal;
window.handleTogglePaymentOption = handleTogglePaymentOption;
window.handleDeletePaymentOption = handleDeletePaymentOption;

// ===================================================================
// Financial Overview & Analytics
// ===================================================================

async function loadFinancialOverview() {
    try {
        const [catRes, pmRes, monthRes] = await Promise.all([
            fetch('/api/admin/analytics/categories'),
            fetch('/api/admin/analytics/payment-methods'),
            fetch('/api/admin/analytics/monthly?months=6')
        ]);

        // 1. Categories
        const catTbody = document.getElementById('categoryAnalyticsBody');
        if (catRes.ok) {
            const list = await catRes.json();
            if (list.length === 0) {
                catTbody.innerHTML = '<tr><td colspan="4" class="text-center py-3 text-muted">No expense transactions recorded yet.</td></tr>';
            } else {
                catTbody.innerHTML = list.map(item => `
                    <tr>
                        <td class="fw-semibold text-dark">${escapeHtml(item.categoryName)}</td>
                        <td class="text-end fw-bold">${formatInr(item.totalAmount)}</td>
                        <td class="text-center text-secondary">${item.transactionCount}</td>
                        <td class="text-end">
                            <span class="badge bg-primary-subtle text-primary">${item.percentage}%</span>
                        </td>
                    </tr>
                `).join('');
            }
        }

        // 2. Payment Methods
        const pmTbody = document.getElementById('paymentMethodAnalyticsBody');
        if (pmRes.ok) {
            const pmList = await pmRes.json();
            if (pmList.length === 0) {
                pmTbody.innerHTML = '<tr><td colspan="4" class="text-center py-3 text-muted">No expense transactions recorded yet.</td></tr>';
            } else {
                pmTbody.innerHTML = pmList.map(item => `
                    <tr>
                        <td class="fw-semibold text-dark">${escapeHtml(item.paymentMethod)}</td>
                        <td class="text-end fw-bold">${formatInr(item.totalAmount)}</td>
                        <td class="text-center text-secondary">${item.transactionCount}</td>
                        <td class="text-end">
                            <span class="badge bg-info-subtle text-info">${item.percentage}%</span>
                        </td>
                    </tr>
                `).join('');
            }
        }

        // 3. Monthly Trend
        const monthTbody = document.getElementById('monthlyAnalyticsBody');
        if (monthRes.ok) {
            const mList = await monthRes.json();
            if (mList.length === 0) {
                monthTbody.innerHTML = '<tr><td colspan="3" class="text-center py-3 text-muted">No monthly records found.</td></tr>';
            } else {
                monthTbody.innerHTML = mList.map(item => `
                    <tr>
                        <td class="fw-bold text-dark">${item.monthName} ${item.year}</td>
                        <td class="text-end fw-semibold text-primary">${formatInr(item.totalAmount)}</td>
                        <td class="text-center text-secondary">${item.transactionCount}</td>
                    </tr>
                `).join('');
            }
        }
    } catch (err) {
        console.error('Error loading financial analytics:', err);
    }
}

// ===================================================================
// Budget Overview
// ===================================================================

async function loadBudgetOverview() {
    try {
        const res = await fetch('/api/admin/budgets/summary');
        if (!res.ok) return;

        const data = await res.json();
        document.getElementById('budgetUnderCount').textContent = data.budgetsUnderBudget ?? 0;
        document.getElementById('budgetNearCount').textContent = data.budgetsNearLimit ?? 0;
        document.getElementById('budgetOverCount').textContent = data.budgetsOverBudget ?? 0;
        document.getElementById('budgetTotalConfigured').textContent = data.totalBudgets ?? 0;
        document.getElementById('budgetAverageTarget').textContent = formatInr(data.averageBudgetAmount);
    } catch (err) {
        console.error('Error loading budget overview:', err);
    }
}

// ===================================================================
// Helper Utilities
// ===================================================================

function formatInr(amount) {
    if (amount === null || amount === undefined) {
        return 'INR 0.00';
    }
    const num = Number(amount);
    if (isNaN(num)) {
        return 'INR ' + amount;
    }
    return 'INR ' + num.toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function showToast(message, isSuccess = true) {
    const toastEl = document.getElementById('actionToast');
    const toastMsg = document.getElementById('toastMessage');
    if (!toastEl || !toastMsg) return;

    toastMsg.textContent = message;
    toastEl.classList.remove('bg-success', 'bg-danger');
    toastEl.classList.add(isSuccess ? 'bg-success' : 'bg-danger');

    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
    toast.show();
}
