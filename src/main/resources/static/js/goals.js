/**
 * Frontend JavaScript for Savings Goals Management.
 */
document.addEventListener('DOMContentLoaded', () => {
    let currentUser = null;
    let createGoalModalInstance = null;
    let contributionModalInstance = null;

    const authRequiredSection = document.getElementById('authRequiredSection');
    const goalsContentSection = document.getElementById('goalsContentSection');
    const authUserMenu = document.getElementById('authUserMenu');
    const currentUserName = document.getElementById('currentUserName');
    const navAdminLink = document.getElementById('navAdminLink');
    const btnLogout = document.getElementById('btnLogout');

    const kpiTotalGoals = document.getElementById('kpiTotalGoals');
    const kpiActiveGoals = document.getElementById('kpiActiveGoals');
    const kpiTotalTarget = document.getElementById('kpiTotalTarget');
    const kpiTotalSaved = document.getElementById('kpiTotalSaved');
    const goalsListContainer = document.getElementById('goalsListContainer');

    const createGoalForm = document.getElementById('createGoalForm');
    const contributionForm = document.getElementById('contributionForm');
    const contribGoalId = document.getElementById('contribGoalId');
    const contribGoalName = document.getElementById('contribGoalName');
    const contribGoalProgress = document.getElementById('contribGoalProgress');
    const contribAmount = document.getElementById('contribAmount');

    const modalEl1 = document.getElementById('createGoalModal');
    if (modalEl1) createGoalModalInstance = new bootstrap.Modal(modalEl1);

    const modalEl2 = document.getElementById('contributionModal');
    if (modalEl2) contributionModalInstance = new bootstrap.Modal(modalEl2);

    // Initialize
    checkAuth();

    if (btnLogout) {
        btnLogout.addEventListener('click', handleLogout);
    }

    if (createGoalForm) {
        createGoalForm.addEventListener('submit', handleCreateGoal);
    }

    if (contributionForm) {
        contributionForm.addEventListener('submit', handleAddContribution);
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
                if (goalsContentSection) goalsContentSection.classList.remove('d-none');

                loadGoals();
            })
            .catch(() => {
                if (authRequiredSection) authRequiredSection.classList.remove('d-none');
                if (goalsContentSection) goalsContentSection.classList.add('d-none');
                if (authUserMenu) authUserMenu.classList.add('d-none');
            });
    }

    function loadGoals() {
        fetch('/api/goals')
            .then(res => {
                if (!res.ok) throw new Error('Failed to load goals');
                return res.json();
            })
            .then(goals => {
                renderGoals(goals);
            })
            .catch(err => {
                showToast(err.message, 'danger');
                if (goalsListContainer) {
                    goalsListContainer.innerHTML = `
                        <div class="col-12 text-center py-5 text-muted">
                            <i class="bi bi-exclamation-triangle fs-2 text-warning d-block mb-2"></i>
                            <div>Failed to load savings goals. Please try again.</div>
                        </div>`;
                }
            });
    }

    function renderGoals(goals) {
        let totalTarget = 0;
        let totalSaved = 0;
        let activeCount = 0;

        goals.forEach(g => {
            totalTarget += parseFloat(g.targetAmount || 0);
            totalSaved += parseFloat(g.currentAmount || 0);
            if (g.status === 'ACTIVE') activeCount++;
        });

        if (kpiTotalGoals) kpiTotalGoals.textContent = goals.length;
        if (kpiActiveGoals) kpiActiveGoals.textContent = activeCount;
        if (kpiTotalTarget) kpiTotalTarget.textContent = formatCurrency(totalTarget);
        if (kpiTotalSaved) kpiTotalSaved.textContent = formatCurrency(totalSaved);

        if (!goalsListContainer) return;

        if (goals.length === 0) {
            goalsListContainer.innerHTML = `
                <div class="col-12 text-center py-5 text-muted">
                    <i class="bi bi-trophy fs-1 text-secondary opacity-50 d-block mb-2"></i>
                    <h5 class="fw-bold">No Savings Goals Yet</h5>
                    <p class="small text-muted mb-3">Create your first goal to begin saving towards your target.</p>
                    <button class="btn btn-sm btn-primary rounded-pill px-4" data-bs-toggle="modal" data-bs-target="#createGoalModal">
                        <i class="bi bi-plus-lg me-1"></i> Create Goal
                    </button>
                </div>`;
            return;
        }

        goalsListContainer.innerHTML = goals.map(g => {
            const progress = parseFloat(g.progressPercentage || 0);
            const isCompleted = g.status === 'COMPLETED';
            const badgeClass = isCompleted ? 'bg-success' : (g.status === 'ACTIVE' ? 'bg-primary' : 'bg-secondary');
            const barClass = isCompleted ? 'bg-success' : 'bg-primary';

            return `
                <div class="col-md-6 col-lg-4">
                    <div class="card h-100 border-0 shadow-sm rounded-4 overflow-hidden">
                        <div class="card-body p-4 d-flex flex-column">
                            <div class="d-flex justify-content-between align-items-start mb-2">
                                <h5 class="fw-bold text-dark mb-0">${escapeHtml(g.name)}</h5>
                                <span class="badge ${badgeClass} rounded-pill px-3 py-1 small">${g.status}</span>
                            </div>
                            <p class="text-muted small mb-3 flex-grow-1">${g.description ? escapeHtml(g.description) : 'No description provided.'}</p>
                            
                            <div class="mb-3">
                                <div class="d-flex justify-content-between text-muted small mb-1">
                                    <span>Saved: <strong class="text-dark">${formatCurrency(g.currentAmount)}</strong></span>
                                    <span>Target: <strong class="text-dark">${formatCurrency(g.targetAmount)}</strong></span>
                                </div>
                                <div class="progress rounded-pill" style="height: 10px;">
                                    <div class="progress-bar ${barClass}" role="progressbar" style="width: ${Math.min(100, progress)}%" aria-valuenow="${progress}" aria-valuemin="0" aria-valuemax="100"></div>
                                </div>
                                <div class="d-flex justify-content-between text-muted small mt-1">
                                    <span>${progress.toFixed(1)}% completed</span>
                                    <span>Remaining: <strong>${formatCurrency(g.remainingAmount)}</strong></span>
                                </div>
                            </div>

                            <div class="d-flex align-items-center justify-content-between text-muted small border-top pt-3 mt-auto">
                                <span><i class="bi bi-calendar-event me-1"></i> ${g.targetDate || 'No date'}</span>
                                <div class="d-flex gap-2">
                                    ${!isCompleted && g.status !== 'CANCELLED' ? `
                                        <button class="btn btn-sm btn-outline-success rounded-pill px-3 fw-semibold btn-contribute"
                                            data-id="${g.id}" data-name="${escapeHtml(g.name)}" data-current="${g.currentAmount}" data-target="${g.targetAmount}" data-progress="${progress}">
                                            <i class="bi bi-plus-circle me-1"></i> Deposit
                                        </button>` : ''}
                                    <button class="btn btn-sm btn-outline-danger rounded-circle btn-delete-goal" data-id="${g.id}" title="Delete Goal" aria-label="Delete Goal">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>`;
        }).join('');

        // Attach event listeners
        document.querySelectorAll('.btn-contribute').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const target = e.currentTarget;
                const id = target.getAttribute('data-id');
                const name = target.getAttribute('data-name');
                const cur = target.getAttribute('data-current');
                const tgt = target.getAttribute('data-target');
                const pct = target.getAttribute('data-progress');

                if (contribGoalId) contribGoalId.value = id;
                if (contribGoalName) contribGoalName.textContent = name;
                if (contribGoalProgress) contribGoalProgress.textContent = `Progress: ${formatCurrency(cur)} / ${formatCurrency(tgt)} (${parseFloat(pct).toFixed(1)}%)`;
                if (contribAmount) contribAmount.value = '';

                if (contributionModalInstance) contributionModalInstance.show();
            });
        });

        document.querySelectorAll('.btn-delete-goal').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                if (confirm('Are you sure you want to delete this savings goal?')) {
                    deleteGoal(id);
                }
            });
        });
    }

    function handleCreateGoal(e) {
        e.preventDefault();
        const name = document.getElementById('goalName').value.trim();
        const description = document.getElementById('goalDescription').value.trim();
        const targetAmount = document.getElementById('goalTargetAmount').value;
        const currentAmount = document.getElementById('goalCurrentAmount').value || '0.00';
        const targetDate = document.getElementById('goalTargetDate').value;

        const payload = {
            name,
            description: description || null,
            targetAmount: parseFloat(targetAmount),
            currentAmount: parseFloat(currentAmount),
            targetDate
        };

        fetch('/api/goals', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(res => {
            if (res.ok) return res.json();
            return res.json().then(data => { throw new Error(data.message || 'Failed to create goal'); });
        })
        .then(() => {
            if (createGoalModalInstance) createGoalModalInstance.hide();
            createGoalForm.reset();
            showToast('Savings goal created successfully!', 'success');
            loadGoals();
        })
        .catch(err => {
            showToast(err.message, 'danger');
        });
    }

    function handleAddContribution(e) {
        e.preventDefault();
        const id = contribGoalId.value;
        const amount = parseFloat(contribAmount.value);

        if (!amount || amount <= 0) {
            showToast('Please enter a positive contribution amount', 'warning');
            return;
        }

        fetch(`/api/goals/${id}/contributions`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount })
        })
        .then(res => {
            if (res.ok) return res.json();
            return res.json().then(data => { throw new Error(data.message || 'Failed to deposit contribution'); });
        })
        .then(updatedGoal => {
            if (contributionModalInstance) contributionModalInstance.hide();
            contributionForm.reset();
            const msg = updatedGoal.status === 'COMPLETED'
                ? 'Congratulations! You have completed your savings goal!'
                : 'Contribution deposited successfully!';
            showToast(msg, 'success');
            loadGoals();
        })
        .catch(err => {
            showToast(err.message, 'danger');
        });
    }

    function deleteGoal(id) {
        fetch(`/api/goals/${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) return res.json();
                return res.json().then(data => { throw new Error(data.message || 'Failed to delete goal'); });
            })
            .then(() => {
                showToast('Savings goal deleted successfully', 'info');
                loadGoals();
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
