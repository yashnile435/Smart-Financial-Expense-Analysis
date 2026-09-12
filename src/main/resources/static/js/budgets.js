/**
 * JavaScript logic for Budget Management (Stage 5).
 * Handles budget CRUD operations, deterministic spending/utilization calculations,
 * ownership enforcement, and responsive UI components.
 */
document.addEventListener("DOMContentLoaded", () => {
    let currentUser = null;
    let allBudgets = [];
    let budgetIdToDelete = null;

    const MONTH_NAMES = [
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];

    // Navigation & Auth Elements
    const authUserMenu = document.getElementById("authUserMenu");
    const currentUserName = document.getElementById("currentUserName");
    const authRequiredSection = document.getElementById("authRequiredSection");
    const budgetAppSection = document.getElementById("budgetAppSection");
    const btnLogout = document.getElementById("btnLogout");

    // Auth Forms
    const formLogin = document.getElementById("formLogin");
    const loginEmail = document.getElementById("loginEmail");
    const loginPassword = document.getElementById("loginPassword");
    const loginAlert = document.getElementById("loginAlert");

    const formRegister = document.getElementById("formRegister");
    const regName = document.getElementById("regName");
    const regEmail = document.getElementById("regEmail");
    const regPassword = document.getElementById("regPassword");
    const registerAlert = document.getElementById("registerAlert");

    // Current Month Summary Elements
    const currentMonthTitle = document.getElementById("currentMonthTitle");
    const currentMonthStatusBadge = document.getElementById("currentMonthStatusBadge");
    const currentBudgetAmount = document.getElementById("currentBudgetAmount");
    const currentSpentAmount = document.getElementById("currentSpentAmount");
    const currentRemainingAmount = document.getElementById("currentRemainingAmount");
    const currentUtilizationPercent = document.getElementById("currentUtilizationPercent");
    const currentProgressBar = document.getElementById("currentProgressBar");
    const progressBarLabel = document.getElementById("progressBarLabel");
    const noCurrentBudgetNotice = document.getElementById("noCurrentBudgetNotice");

    // Budget Form Elements
    const budgetForm = document.getElementById("budgetForm");
    const formCardTitle = document.getElementById("formCardTitle");
    const formBudgetId = document.getElementById("formBudgetId");
    const budgetMonth = document.getElementById("budgetMonth");
    const budgetYear = document.getElementById("budgetYear");
    const budgetAmount = document.getElementById("budgetAmount");
    const formAlert = document.getElementById("formAlert");
    const btnResetForm = document.getElementById("btnResetForm");
    const btnCancelEdit = document.getElementById("btnCancelEdit");

    // Table & History Elements
    const budgetTableBody = document.getElementById("budgetTableBody");
    const budgetCountBadge = document.getElementById("budgetCountBadge");
    const emptyBudgetState = document.getElementById("emptyBudgetState");
    const loadingBudgetState = document.getElementById("loadingBudgetState");

    // View Modal Elements
    const viewBudgetModalEl = document.getElementById("viewBudgetModal");
    const viewBudgetModal = new bootstrap.Modal(viewBudgetModalEl);
    const viewModalMonthYear = document.getElementById("viewModalMonthYear");
    const viewModalStatusBadge = document.getElementById("viewModalStatusBadge");
    const viewModalBudget = document.getElementById("viewModalBudget");
    const viewModalSpent = document.getElementById("viewModalSpent");
    const viewModalRemaining = document.getElementById("viewModalRemaining");
    const viewModalUtilization = document.getElementById("viewModalUtilization");
    const viewModalBarLabel = document.getElementById("viewModalBarLabel");
    const viewModalProgressBar = document.getElementById("viewModalProgressBar");

    // Delete Modal Elements
    const deleteBudgetModalEl = document.getElementById("deleteBudgetModal");
    const deleteBudgetModal = new bootstrap.Modal(deleteBudgetModalEl);
    const btnConfirmDeleteBudget = document.getElementById("btnConfirmDeleteBudget");

    // Toast Notification
    const toastEl = document.getElementById("actionToast");
    const toastMessage = document.getElementById("toastMessage");
    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });

    function showToast(message, isSuccess = true) {
        toastMessage.textContent = message;
        toastEl.className = `toast align-items-center text-white border-0 bg-${isSuccess ? "success" : "danger"}`;
        toast.show();
    }

    function formatCurrency(amount) {
        const num = Number(amount || 0);
        return "₹" + num.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function getStatusBadgeConfig(status) {
        switch (status) {
            case "UNDER_BUDGET":
                return { text: "UNDER BUDGET", className: "bg-success text-white" };
            case "NEAR_LIMIT":
                return { text: "NEAR LIMIT", className: "bg-warning text-dark" };
            case "OVER_BUDGET":
                return { text: "OVER BUDGET", className: "bg-danger text-white" };
            default:
                return { text: status || "N/A", className: "bg-secondary text-white" };
        }
    }

    /**
     * Checks user authentication state.
     */
    async function checkAuth() {
        try {
            const res = await fetch("/api/auth/me");
            if (res.ok) {
                currentUser = await res.json();
                currentUserName.textContent = currentUser.name || currentUser.email;
                authUserMenu.classList.remove("d-none");
                authUserMenu.classList.add("d-flex");
                authRequiredSection.classList.add("d-none");
                budgetAppSection.classList.remove("d-none");

                // Initialize default month/year in form to current date
                const now = new Date();
                budgetMonth.value = String(now.getMonth() + 1);
                budgetYear.value = String(now.getFullYear());

                await loadCurrentMonthBudget();
                await loadAllBudgets();
            } else {
                handleUnauthenticated();
            }
        } catch (e) {
            handleUnauthenticated();
        }
    }

    function handleUnauthenticated() {
        currentUser = null;
        authUserMenu.classList.add("d-none");
        authUserMenu.classList.remove("d-flex");
        authRequiredSection.classList.remove("d-none");
        budgetAppSection.classList.add("d-none");
    }

    /**
     * Handles User Login
     */
    if (formLogin) {
        formLogin.addEventListener("submit", async (e) => {
            e.preventDefault();
            loginAlert.classList.add("d-none");

            try {
                const res = await fetch("/api/auth/login", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        email: loginEmail.value.trim(),
                        password: loginPassword.value
                    })
                });
                const data = await res.json();
                if (res.ok && data.success) {
                    showToast("Login successful! Welcome back.");
                    await checkAuth();
                } else {
                    loginAlert.textContent = data.message || "Invalid email or password";
                    loginAlert.classList.remove("d-none");
                }
            } catch (err) {
                loginAlert.textContent = "Network error while connecting to server.";
                loginAlert.classList.remove("d-none");
            }
        });
    }

    /**
     * Handles User Registration
     */
    if (formRegister) {
        formRegister.addEventListener("submit", async (e) => {
            e.preventDefault();
            registerAlert.classList.add("d-none");

            try {
                const res = await fetch("/api/auth/register", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        name: regName.value.trim(),
                        email: regEmail.value.trim(),
                        password: regPassword.value
                    })
                });
                const data = await res.json();
                if (res.ok && data.success) {
                    showToast("Account created! Logging in...");
                    // Auto login after registration
                    const loginRes = await fetch("/api/auth/login", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            email: regEmail.value.trim(),
                            password: regPassword.value
                        })
                    });
                    if (loginRes.ok) {
                        await checkAuth();
                    } else {
                        window.location.reload();
                    }
                } else {
                    registerAlert.textContent = data.message || "Registration failed";
                    registerAlert.classList.remove("d-none");
                }
            } catch (err) {
                registerAlert.textContent = "Network error during registration.";
                registerAlert.classList.remove("d-none");
            }
        });
    }

    /**
     * Handles Logout
     */
    if (btnLogout) {
        btnLogout.addEventListener("click", async () => {
            try {
                await fetch("/api/auth/logout", { method: "POST" });
                showToast("Logged out successfully");
                handleUnauthenticated();
            } catch (err) {
                handleUnauthenticated();
            }
        });
    }

    /**
     * Loads the current month's budget details and updates the overview card & progress bar.
     */
    async function loadCurrentMonthBudget() {
        const now = new Date();
        const curMonth = now.getMonth() + 1;
        const curYear = now.getFullYear();
        const monthName = MONTH_NAMES[curMonth] || "Current Month";

        try {
            const res = await fetch("/api/budgets/current");
            if (res.ok) {
                const budget = await res.json();
                renderCurrentMonthOverview(budget);
            } else if (res.status === 404) {
                // No budget configured for this month
                renderEmptyCurrentMonth(monthName, curYear);
            } else {
                renderEmptyCurrentMonth(monthName, curYear);
            }
        } catch (e) {
            renderEmptyCurrentMonth(monthName, curYear);
        }
    }

    function renderCurrentMonthOverview(b) {
        const monthName = MONTH_NAMES[b.month] || b.month;
        currentMonthTitle.textContent = `${monthName} ${b.year}`;

        const statusConfig = getStatusBadgeConfig(b.status);
        currentMonthStatusBadge.textContent = statusConfig.text;
        currentMonthStatusBadge.className = `badge px-3 py-2 rounded-pill fs-6 fw-bold ${statusConfig.className}`;

        currentBudgetAmount.textContent = formatCurrency(b.budgetAmount);
        currentSpentAmount.textContent = formatCurrency(b.totalExpenses);

        const remNum = Number(b.remainingAmount);
        currentRemainingAmount.textContent = formatCurrency(remNum);
        if (remNum < 0) {
            currentRemainingAmount.className = "fw-bold text-danger mb-0";
        } else {
            currentRemainingAmount.className = "fw-bold text-success mb-0";
        }

        const utilNum = Number(b.utilizationPercentage || 0);
        currentUtilizationPercent.textContent = `${utilNum.toFixed(1)}%`;

        // Progress bar capped at 100% visually
        const visualWidth = Math.min(Math.max(utilNum, 0), 100);
        currentProgressBar.style.width = `${visualWidth}%`;
        currentProgressBar.setAttribute("aria-valuenow", visualWidth);
        progressBarLabel.textContent = `${utilNum.toFixed(1)}% Used (Actual)`;

        // Color bar based on status
        if (b.status === "OVER_BUDGET") {
            currentProgressBar.className = "progress-bar progress-bar-striped progress-bar-animated bg-danger";
        } else if (b.status === "NEAR_LIMIT") {
            currentProgressBar.className = "progress-bar progress-bar-striped progress-bar-animated bg-warning";
        } else {
            currentProgressBar.className = "progress-bar progress-bar-striped progress-bar-animated bg-success";
        }

        noCurrentBudgetNotice.classList.add("d-none");
    }

    function renderEmptyCurrentMonth(monthName, year) {
        currentMonthTitle.textContent = `${monthName} ${year}`;
        currentMonthStatusBadge.textContent = "NOT SET";
        currentMonthStatusBadge.className = "badge bg-secondary text-white px-3 py-2 rounded-pill fs-6 fw-bold";
        currentBudgetAmount.textContent = "₹0.00";
        currentSpentAmount.textContent = "₹0.00";
        currentRemainingAmount.textContent = "₹0.00";
        currentRemainingAmount.className = "fw-bold text-muted mb-0";
        currentUtilizationPercent.textContent = "0.0%";

        currentProgressBar.style.width = "0%";
        currentProgressBar.className = "progress-bar bg-secondary";
        progressBarLabel.textContent = "0.0% Used";

        noCurrentBudgetNotice.classList.remove("d-none");
    }

    /**
     * Loads all historical budgets for the authenticated user.
     */
    async function loadAllBudgets() {
        loadingBudgetState.classList.remove("d-none");
        emptyBudgetState.classList.add("d-none");
        budgetTableBody.innerHTML = "";

        try {
            const res = await fetch("/api/budgets/all");
            if (res.ok) {
                allBudgets = await res.json();
                renderBudgetTable(allBudgets);
            } else {
                showToast("Failed to load budgets", false);
            }
        } catch (err) {
            showToast("Network error loading budgets", false);
        } finally {
            loadingBudgetState.classList.add("d-none");
        }
    }

    function renderBudgetTable(budgets) {
        budgetCountBadge.textContent = `${budgets.length} Record${budgets.length === 1 ? "" : "s"}`;

        if (!budgets || budgets.length === 0) {
            emptyBudgetState.classList.remove("d-none");
            budgetTableBody.innerHTML = "";
            return;
        }

        emptyBudgetState.classList.add("d-none");
        budgetTableBody.innerHTML = budgets.map(b => {
            const monthName = MONTH_NAMES[b.month] || b.month;
            const statusConfig = getStatusBadgeConfig(b.status);
            const remNum = Number(b.remainingAmount);
            const remClass = remNum < 0 ? "text-danger fw-bold" : "text-success fw-bold";

            return `
                <tr>
                    <td class="ps-4 fw-semibold text-dark">
                        ${monthName} <span class="text-muted small">(${b.month})</span>
                    </td>
                    <td class="fw-semibold text-secondary">${b.year}</td>
                    <td class="fw-semibold text-dark">${formatCurrency(b.budgetAmount)}</td>
                    <td class="text-danger fw-semibold">${formatCurrency(b.totalExpenses)}</td>
                    <td class="${remClass}">${formatCurrency(remNum)}</td>
                    <td>
                        <span class="badge ${statusConfig.className} px-2 py-1 rounded-pill small">
                            ${statusConfig.text}
                        </span>
                        <div class="text-muted" style="font-size: 0.75rem;">${Number(b.utilizationPercentage || 0).toFixed(1)}%</div>
                    </td>
                    <td class="text-end pe-4">
                        <div class="btn-group btn-group-sm" role="group">
                            <button class="btn btn-outline-info rounded-pill px-2 py-1 me-1 btn-view" data-id="${b.id}" title="View Details">
                                <i class="bi bi-eye"></i> View
                            </button>
                            <button class="btn btn-outline-primary rounded-pill px-2 py-1 me-1 btn-edit" data-id="${b.id}" title="Edit Budget">
                                <i class="bi bi-pencil"></i> Edit
                            </button>
                            <button class="btn btn-outline-danger rounded-pill px-2 py-1 btn-delete" data-id="${b.id}" title="Delete Budget">
                                <i class="bi bi-trash"></i>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join("");

        // Attach action listeners
        budgetTableBody.querySelectorAll(".btn-view").forEach(btn => {
            btn.addEventListener("click", () => handleViewBudget(Number(btn.dataset.id)));
        });
        budgetTableBody.querySelectorAll(".btn-edit").forEach(btn => {
            btn.addEventListener("click", () => handleEditBudget(Number(btn.dataset.id)));
        });
        budgetTableBody.querySelectorAll(".btn-delete").forEach(btn => {
            btn.addEventListener("click", () => handleDeleteClick(Number(btn.dataset.id)));
        });
    }

    /**
     * Handles Save / Update Budget Form Submission.
     */
    if (budgetForm) {
        budgetForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            formAlert.classList.add("d-none");

            const monthVal = parseInt(budgetMonth.value);
            const yearVal = parseInt(budgetYear.value);
            const amountVal = parseFloat(budgetAmount.value);

            if (!monthVal || monthVal < 1 || monthVal > 12) {
                formAlert.textContent = "Please select a valid month (1-12)";
                formAlert.classList.remove("d-none");
                return;
            }
            if (!yearVal || yearVal < 2000) {
                formAlert.textContent = "Please enter a valid year (2000 or later)";
                formAlert.classList.remove("d-none");
                return;
            }
            if (!amountVal || amountVal <= 0) {
                formAlert.textContent = "Amount must be greater than zero";
                formAlert.classList.remove("d-none");
                return;
            }

            const payload = {
                month: monthVal,
                year: yearVal,
                amount: amountVal
            };

            const isEdit = Boolean(formBudgetId.value);
            const url = isEdit ? `/api/budgets/${formBudgetId.value}` : "/api/budgets";
            const method = isEdit ? "PUT" : "POST";

            try {
                const res = await fetch(url, {
                    method: method,
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();

                if (res.ok) {
                    showToast(isEdit ? "Budget updated successfully!" : "Budget created successfully!");
                    resetBudgetForm();
                    await loadCurrentMonthBudget();
                    await loadAllBudgets();
                } else {
                    formAlert.textContent = data.message || "Failed to save budget";
                    formAlert.classList.remove("d-none");
                }
            } catch (err) {
                formAlert.textContent = "Network error while saving budget";
                formAlert.classList.remove("d-none");
            }
        });
    }

    function resetBudgetForm() {
        formBudgetId.value = "";
        formAlert.classList.add("d-none");
        formCardTitle.innerHTML = '<i class="bi bi-pencil-square text-primary me-2"></i>Create or Update Budget';
        btnCancelEdit.classList.add("d-none");

        const now = new Date();
        budgetMonth.value = String(now.getMonth() + 1);
        budgetYear.value = String(now.getFullYear());
        budgetAmount.value = "";
    }

    if (btnResetForm) {
        btnResetForm.addEventListener("click", resetBudgetForm);
    }
    if (btnCancelEdit) {
        btnCancelEdit.addEventListener("click", resetBudgetForm);
    }

    /**
     * Pre-fills the form for editing an existing budget.
     */
    function handleEditBudget(id) {
        const budget = allBudgets.find(b => b.id === id);
        if (!budget) return;

        formBudgetId.value = budget.id;
        budgetMonth.value = String(budget.month);
        budgetYear.value = String(budget.year);
        budgetAmount.value = budget.budgetAmount;

        formCardTitle.innerHTML = `<i class="bi bi-pencil-fill text-warning me-2"></i>Update Budget (${MONTH_NAMES[budget.month]} ${budget.year})`;
        btnCancelEdit.classList.remove("d-none");
        formAlert.classList.add("d-none");

        // Smooth scroll to form
        document.getElementById("budgetFormCard").scrollIntoView({ behavior: "smooth" });
    }

    /**
     * Shows modal with detailed budget and utilization calculation breakdown.
     */
    function handleViewBudget(id) {
        const budget = allBudgets.find(b => b.id === id);
        if (!budget) return;

        const monthName = MONTH_NAMES[budget.month] || budget.month;
        viewModalMonthYear.textContent = `${monthName} ${budget.year}`;

        const statusConfig = getStatusBadgeConfig(budget.status);
        viewModalStatusBadge.textContent = statusConfig.text;
        viewModalStatusBadge.className = `badge px-3 py-2 rounded-pill fs-6 ${statusConfig.className}`;

        viewModalBudget.textContent = formatCurrency(budget.budgetAmount);
        viewModalSpent.textContent = formatCurrency(budget.totalExpenses);

        const remNum = Number(budget.remainingAmount);
        viewModalRemaining.textContent = formatCurrency(remNum);
        viewModalRemaining.className = remNum < 0 ? "fw-bold fs-5 text-danger" : "fw-bold fs-5 text-success";

        const utilNum = Number(budget.utilizationPercentage || 0);
        viewModalUtilization.textContent = `${utilNum.toFixed(2)}%`;
        viewModalBarLabel.textContent = `${utilNum.toFixed(2)}% (Capped at 100% in bar)`;

        const visualPercent = Math.min(Math.max(utilNum, 0), 100);
        viewModalProgressBar.style.width = `${visualPercent}%`;
        if (budget.status === "OVER_BUDGET") {
            viewModalProgressBar.className = "progress-bar bg-danger";
        } else if (budget.status === "NEAR_LIMIT") {
            viewModalProgressBar.className = "progress-bar bg-warning";
        } else {
            viewModalProgressBar.className = "progress-bar bg-success";
        }

        viewBudgetModal.show();
    }

    /**
     * Prompts confirmation modal before deleting.
     */
    function handleDeleteClick(id) {
        budgetIdToDelete = id;
        deleteBudgetModal.show();
    }

    if (btnConfirmDeleteBudget) {
        btnConfirmDeleteBudget.addEventListener("click", async () => {
            if (!budgetIdToDelete) return;

            try {
                const res = await fetch(`/api/budgets/${budgetIdToDelete}`, {
                    method: "DELETE"
                });
                const data = await res.json();
                deleteBudgetModal.hide();

                if (res.ok && data.success) {
                    showToast("Budget deleted successfully");
                    await loadCurrentMonthBudget();
                    await loadAllBudgets();
                } else {
                    showToast(data.message || "Failed to delete budget", false);
                }
            } catch (err) {
                deleteBudgetModal.hide();
                showToast("Network error while deleting budget", false);
            } finally {
                budgetIdToDelete = null;
            }
        });
    }

    // Initial load
    checkAuth();
});
