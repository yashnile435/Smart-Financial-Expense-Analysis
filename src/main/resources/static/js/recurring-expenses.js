/**
 * Frontend JavaScript for Scheduled Recurring Expenses.
 */
document.addEventListener('DOMContentLoaded', () => {
    let currentUser = null;
    let createModalInstance = null;

    const authRequiredSection = document.getElementById('authRequiredSection');
    const recurringContentSection = document.getElementById('recurringContentSection');
    const authUserMenu = document.getElementById('authUserMenu');
    const currentUserName = document.getElementById('currentUserName');
    const navAdminLink = document.getElementById('navAdminLink');
    const btnLogout = document.getElementById('btnLogout');

    const kpiTotalRecurring = document.getElementById('kpiTotalRecurring');
    const kpiActiveRecurring = document.getElementById('kpiActiveRecurring');
    const kpiTotalCommitment = document.getElementById('kpiTotalCommitment');
    const recurringTableBody = document.getElementById('recurringTableBody');

    const btnProcessDue = document.getElementById('btnProcessDue');
    const createRecurringForm = document.getElementById('createRecurringForm');
    const recurringCategory = document.getElementById('recurringCategory');
    const recurringStartDate = document.getElementById('recurringStartDate');

    const modalEl = document.getElementById('createRecurringModal');
    if (modalEl) createModalInstance = new bootstrap.Modal(modalEl);

    // Initialize
    checkAuth();

    if (btnLogout) {
        btnLogout.addEventListener('click', handleLogout);
    }

    if (btnProcessDue) {
        btnProcessDue.addEventListener('click', handleProcessDue);
    }

    if (createRecurringForm) {
        createRecurringForm.addEventListener('submit', handleCreateRecurring);
    }

    // Set today as default start date
    if (recurringStartDate) {
        recurringStartDate.value = new Date().toISOString().split('T')[0];
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
                    window.location.href = 'admin.html';
                    return;
                }

                if (authRequiredSection) authRequiredSection.classList.add('d-none');
                if (recurringContentSection) recurringContentSection.classList.remove('d-none');

                loadCategories();
                loadPaymentOptions();
                loadRecurringExpenses();
            })
            .catch(() => {
                if (authRequiredSection) authRequiredSection.classList.remove('d-none');
                if (recurringContentSection) recurringContentSection.classList.add('d-none');
                if (authUserMenu) authUserMenu.classList.add('d-none');
            });
    }

    function loadCategories() {
        fetch('/api/categories')
            .then(res => res.json())
            .then(categories => {
                if (!recurringCategory) return;
                recurringCategory.innerHTML = '<option value="" disabled selected>Select category</option>' +
                    categories.map(c => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join('');
            })
            .catch(err => console.error('Failed to load categories', err));
    }

    function loadPaymentOptions() {
        fetch('/api/payment-options')
            .then(res => res.json())
            .then(options => {
                const select = document.getElementById('recurringPaymentMethod');
                if (!select) return;
                select.innerHTML = '<option value="" disabled selected>Select payment method</option>' +
                    options.map(opt => `<option value="${escapeHtml(opt.name)}">${escapeHtml(opt.name)}</option>`).join('');
            })
            .catch(err => console.error('Failed to load payment options', err));
    }

    function loadRecurringExpenses() {
        fetch('/api/recurring-expenses')
            .then(res => {
                if (!res.ok) throw new Error('Failed to load recurring expenses');
                return res.json();
            })
            .then(items => {
                renderTable(items);
            })
            .catch(err => {
                showToast(err.message, 'danger');
                if (recurringTableBody) {
                    recurringTableBody.innerHTML = `
                        <tr>
                            <td colspan="8" class="text-center py-5 text-muted">
                                <i class="bi bi-exclamation-triangle fs-2 text-warning d-block mb-2"></i>
                                Failed to load recurring schedules.
                            </td>
                        </tr>`;
                }
            });
    }

    function renderTable(items) {
        let totalCommitment = 0;
        let activeCount = 0;

        items.forEach(i => {
            if (i.active) {
                activeCount++;
                totalCommitment += parseFloat(i.amount || 0);
            }
        });

        if (kpiTotalRecurring) kpiTotalRecurring.textContent = items.length;
        if (kpiActiveRecurring) kpiActiveRecurring.textContent = activeCount;
        if (kpiTotalCommitment) kpiTotalCommitment.textContent = formatCurrency(totalCommitment);

        if (!recurringTableBody) return;

        if (items.length === 0) {
            recurringTableBody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center py-5 text-muted">
                        <i class="bi bi-arrow-repeat fs-1 text-secondary opacity-50 d-block mb-2"></i>
                        <h5 class="fw-bold">No Scheduled Recurring Expenses</h5>
                        <p class="small text-muted mb-0">Add subscriptions, bills, or regular rent to track them automatically.</p>
                    </td>
                </tr>`;
            return;
        }

        const today = new Date().toISOString().split('T')[0];

        recurringTableBody.innerHTML = items.map(i => {
            const isDue = i.active && i.nextOccurrence && i.nextOccurrence <= today;
            const dueBadge = isDue ? '<span class="badge bg-warning text-dark ms-1">Due Now</span>' : '';
            const statusBadge = i.active 
                ? '<span class="badge bg-success-subtle text-success border border-success px-2 py-1 rounded-pill">Active</span>'
                : '<span class="badge bg-secondary-subtle text-secondary border border-secondary px-2 py-1 rounded-pill">Inactive</span>';

            return `
                <tr>
                    <td><span class="badge bg-light text-dark border px-2 py-1">${escapeHtml(i.categoryName)}</span></td>
                    <td class="fw-semibold text-dark">${escapeHtml(i.description || 'No description')}</td>
                    <td class="fw-bold text-dark">${formatCurrency(i.amount)}</td>
                    <td><span class="badge bg-primary-subtle text-primary border border-primary">${i.frequency}</span></td>
                    <td><small class="text-muted">${i.paymentMethod}</small></td>
                    <td>
                        <span class="fw-semibold">${i.nextOccurrence || 'N/A'}</span>
                        ${dueBadge}
                    </td>
                    <td>
                        <button class="btn btn-sm btn-link p-0 text-decoration-none btn-toggle-status" data-id="${i.id}" aria-label="Toggle recurring schedule status">
                            ${statusBadge}
                        </button>
                    </td>
                    <td class="text-end">
                        <div class="d-flex justify-content-end gap-2">
                            ${isDue ? `
                                <button class="btn btn-sm btn-success rounded-pill px-3 fw-semibold btn-generate" data-id="${i.id}" title="Generate expense occurrence now" aria-label="Generate expense occurrence now">
                                    <i class="bi bi-check-circle me-1"></i> Generate
                                </button>` : ''}
                            <button class="btn btn-sm btn-outline-danger rounded-circle btn-delete-recurring" data-id="${i.id}" title="Delete Schedule" aria-label="Delete recurring schedule">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>`;
        }).join('');

        // Event listeners
        document.querySelectorAll('.btn-toggle-status').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                toggleStatus(id);
            });
        });

        document.querySelectorAll('.btn-generate').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                generateDue(id);
            });
        });

        document.querySelectorAll('.btn-delete-recurring').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (confirm('Are you sure you want to delete this recurring expense schedule?')) {
                    deleteRecurring(id);
                }
            });
        });
    }

    function handleCreateRecurring(e) {
        e.preventDefault();
        const categoryId = recurringCategory.value;
        const amount = document.getElementById('recurringAmount').value;
        const description = document.getElementById('recurringDescription').value.trim();
        const frequency = document.getElementById('recurringFrequency').value;
        const paymentMethod = document.getElementById('recurringPaymentMethod').value;
        const startDate = recurringStartDate.value;
        const endDate = document.getElementById('recurringEndDate').value || null;

        const payload = {
            categoryId: parseInt(categoryId),
            amount: parseFloat(amount),
            description: description || null,
            frequency,
            paymentMethod,
            startDate,
            endDate
        };

        fetch('/api/recurring-expenses', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => {
            if (res.ok) return res.json();
            return res.json().then(data => { throw new Error(data.message || 'Failed to create recurring expense'); });
        })
        .then(() => {
            if (createModalInstance) createModalInstance.hide();
            createRecurringForm.reset();
            if (recurringStartDate) recurringStartDate.value = new Date().toISOString().split('T')[0];
            showToast('Recurring expense schedule created successfully!', 'success');
            loadRecurringExpenses();
        })
        .catch(err => {
            showToast(err.message, 'danger');
        });
    }

    function handleProcessDue() {
        btnProcessDue.disabled = true;
        btnProcessDue.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Processing...';

        fetch('/api/recurring-expenses/process-due', { method: 'POST' })
            .then(res => {
                if (res.ok) return res.json();
                return res.json().then(data => { throw new Error(data.message || 'Failed to process due expenses'); });
            })
            .then(generated => {
                const count = generated.length;
                if (count > 0) {
                    showToast(`Successfully generated ${count} due expense transaction(s)!`, 'success');
                } else {
                    showToast('No recurring expenses are currently due for processing.', 'info');
                }
                loadRecurringExpenses();
            })
            .catch(err => {
                showToast(err.message, 'danger');
            })
            .finally(() => {
                btnProcessDue.disabled = false;
                btnProcessDue.innerHTML = '<i class="bi bi-play-circle me-1"></i> Process Due Now';
            });
    }

    function generateDue(id) {
        fetch(`/api/recurring-expenses/${id}/generate`, { method: 'POST' })
            .then(res => {
                if (res.ok) return res.json();
                return res.json().then(data => { throw new Error(data.message || 'Failed to generate expense'); });
            })
            .then(() => {
                showToast('Expense occurrence generated successfully!', 'success');
                loadRecurringExpenses();
            })
            .catch(err => {
                showToast(err.message, 'danger');
            });
    }

    function toggleStatus(id) {
        fetch(`/api/recurring-expenses/${id}/status`, { method: 'PUT' })
            .then(res => {
                if (res.ok) return res.json();
                return res.json().then(data => { throw new Error(data.message || 'Failed to update status'); });
            })
            .then(updated => {
                showToast(`Schedule ${updated.active ? 'activated' : 'deactivated'}`, 'info');
                loadRecurringExpenses();
            })
            .catch(err => {
                showToast(err.message, 'danger');
            });
    }

    function deleteRecurring(id) {
        fetch(`/api/recurring-expenses/${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) return res.json();
                return res.json().then(data => { throw new Error(data.message || 'Failed to delete schedule'); });
            })
            .then(() => {
                showToast('Recurring expense deleted successfully', 'info');
                loadRecurringExpenses();
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

    function formatCurrency(val) {
        const num = parseFloat(val || 0);
        return '₹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;')
                  .replace(/</g, '&lt;')
                  .replace(/>/g, '&gt;')
                  .replace(/"/g, '&quot;')
                  .replace(/'/g, '&#039;');
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
