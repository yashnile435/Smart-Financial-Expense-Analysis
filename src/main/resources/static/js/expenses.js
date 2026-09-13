/**
 * JavaScript logic for Expense Management (Stage 4).
 * Handles authentication checks, category fetching, expense CRUD operations,
 * and dynamic search/filtering.
 */
document.addEventListener("DOMContentLoaded", () => {
    let currentUser = null;
    let availableCategories = [];
    let expenseIdToDelete = null;

    // DOM Elements
    const authUserMenu = document.getElementById("authUserMenu");
    const currentUserName = document.getElementById("currentUserName");
    const authRequiredSection = document.getElementById("authRequiredSection");
    const expenseAppSection = document.getElementById("expenseAppSection");
    const btnLogout = document.getElementById("btnLogout");

    // Auth Form Elements
    const formLogin = document.getElementById("formLogin");
    const loginEmail = document.getElementById("loginEmail");
    const loginPassword = document.getElementById("loginPassword");
    const loginAlert = document.getElementById("loginAlert");

    const formRegister = document.getElementById("formRegister");
    const regName = document.getElementById("regName");
    const regEmail = document.getElementById("regEmail");
    const regPassword = document.getElementById("regPassword");
    const registerAlert = document.getElementById("registerAlert");

    // Expense UI Elements
    const btnOpenAddExpenseModal = document.getElementById("btnOpenAddExpenseModal");
    const expenseTableBody = document.getElementById("expenseTableBody");
    const emptyState = document.getElementById("emptyState");
    const loadingState = document.getElementById("loadingState");
    const expenseCountBadge = document.getElementById("expenseCountBadge");

    // Filter Form Elements
    const filterForm = document.getElementById("filterForm");
    const filterSearch = document.getElementById("filterSearch");
    const filterCategory = document.getElementById("filterCategory");
    const filterPaymentMethod = document.getElementById("filterPaymentMethod");
    const filterStartDate = document.getElementById("filterStartDate");
    const filterEndDate = document.getElementById("filterEndDate");
    const filterMinAmount = document.getElementById("filterMinAmount");
    const filterMaxAmount = document.getElementById("filterMaxAmount");
    const btnClearFilters = document.getElementById("btnClearFilters");

    // Expense Modal Elements
    const expenseModalEl = document.getElementById("expenseModal");
    const expenseModal = new bootstrap.Modal(expenseModalEl);
    const expenseModalTitle = document.getElementById("expenseModalTitle");
    const expenseForm = document.getElementById("expenseForm");
    const modalExpenseId = document.getElementById("modalExpenseId");
    const modalAmount = document.getElementById("modalAmount");
    const modalCategory = document.getElementById("modalCategory");
    const modalDate = document.getElementById("modalDate");
    const modalPaymentMethod = document.getElementById("modalPaymentMethod");
    const modalDescription = document.getElementById("modalDescription");
    const modalAlert = document.getElementById("modalAlert");
    const btnSaveExpense = document.getElementById("btnSaveExpense");

    // Delete Modal Elements
    const deleteModalEl = document.getElementById("deleteModal");
    const deleteModal = new bootstrap.Modal(deleteModalEl);
    const btnConfirmDelete = document.getElementById("btnConfirmDelete");

    // Toast Notification
    const toastEl = document.getElementById("actionToast");
    const toastMessage = document.getElementById("toastMessage");
    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });

    function showToast(message, isSuccess = true) {
        toastMessage.textContent = message;
        toastEl.className = `toast align-items-center text-white border-0 bg-${isSuccess ? "success" : "danger"}`;
        toast.show();
    }

    /**
     * Checks if a user is currently authenticated via Spring Security session.
     */
    async function checkAuth() {
        try {
            const res = await fetch("/api/auth/me");
            if (res.ok) {
                currentUser = await res.json();
                if (currentUser.role === "ADMIN") {
                    window.location.href = "admin.html";
                    return;
                }
                currentUserName.textContent = currentUser.name || currentUser.email;
                authUserMenu.classList.remove("d-none");
                authUserMenu.classList.add("d-flex");
                authRequiredSection.classList.add("d-none");
                expenseAppSection.classList.remove("d-none");

                const adminLink = document.getElementById("navAdminLink");
                if (adminLink) {
                    adminLink.classList.add("d-none");
                }

                // Initialize categories, payment options and expenses
                await loadCategories();
                await loadPaymentOptions();
                await loadExpenses();
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
        expenseAppSection.classList.add("d-none");
        const adminLink = document.getElementById("navAdminLink");
        if (adminLink) adminLink.classList.add("d-none");
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
                    showToast("Login successful!");
                    formLogin.reset();
                    await checkAuth();
                } else {
                    loginAlert.textContent = data.message || "Invalid credentials";
                    loginAlert.classList.remove("d-none");
                }
            } catch (err) {
                loginAlert.textContent = "Could not connect to authentication server";
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
                    showToast("Account created successfully! Logging you in...");
                    // Auto-login with the newly created credentials
                    const loginRes = await fetch("/api/auth/login", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            email: regEmail.value.trim(),
                            password: regPassword.value
                        })
                    });
                    if (loginRes.ok) {
                        formRegister.reset();
                        await checkAuth();
                    } else {
                        // Switch tab to login
                        document.getElementById("login-tab").click();
                        loginEmail.value = regEmail.value;
                    }
                } else {
                    registerAlert.textContent = data.message || "Registration failed";
                    registerAlert.classList.remove("d-none");
                }
            } catch (err) {
                registerAlert.textContent = "Could not connect to registration server";
                registerAlert.classList.remove("d-none");
            }
        });
    }

    /**
     * Handles User Logout - clears session and returns to public homepage
     */
    if (btnLogout) {
        btnLogout.addEventListener("click", async () => {
            try {
                await fetch("/api/auth/logout", { method: "POST" });
            } catch (err) {}
            window.location.href = "index.html";
        });
    }

    /**
     * Loads available categories from /api/categories
     */
    async function loadCategories() {
        try {
            const res = await fetch("/api/categories");
            if (res.ok) {
                availableCategories = await res.json();

                // Populate filter dropdown
                filterCategory.innerHTML = '<option value="">All Categories</option>';
                // Populate modal dropdown
                modalCategory.innerHTML = '<option value="" disabled selected>Select category...</option>';

                availableCategories.forEach(cat => {
                    const opt1 = document.createElement("option");
                    opt1.value = cat.id;
                    opt1.textContent = cat.name;
                    filterCategory.appendChild(opt1);

                    const opt2 = document.createElement("option");
                    opt2.value = cat.id;
                    opt2.textContent = cat.name;
                    modalCategory.appendChild(opt2);
                });
            }
        } catch (err) {
            console.error("Failed to load categories", err);
        }
    }

    /**
     * Loads available active payment options from /api/payment-options
     */
    async function loadPaymentOptions() {
        try {
            const res = await fetch("/api/payment-options");
            if (res.ok) {
                const paymentOptions = await res.json();

                // Populate filter dropdown
                if (filterPaymentMethod) {
                    filterPaymentMethod.innerHTML = '<option value="">All Payment Methods</option>';
                    paymentOptions.forEach(po => {
                        const opt = document.createElement("option");
                        opt.value = po.name;
                        opt.textContent = po.name;
                        filterPaymentMethod.appendChild(opt);
                    });
                }

                // Populate modal dropdown
                if (modalPaymentMethod) {
                    modalPaymentMethod.innerHTML = '<option value="" disabled selected>Select payment method...</option>';
                    paymentOptions.forEach(po => {
                        const opt = document.createElement("option");
                        opt.value = po.name;
                        opt.textContent = po.name;
                        modalPaymentMethod.appendChild(opt);
                    });
                }
            }
        } catch (err) {
            console.error("Failed to load payment options", err);
        }
    }

    /**
     * Loads expenses from /api/expenses based on active filter criteria
     */
    async function loadExpenses() {
        loadingState.classList.remove("d-none");
        emptyState.classList.add("d-none");
        expenseTableBody.innerHTML = "";

        const params = new URLSearchParams();
        if (filterSearch.value.trim()) params.append("search", filterSearch.value.trim());
        if (filterCategory.value) params.append("categoryId", filterCategory.value);
        if (filterPaymentMethod.value) params.append("paymentMethod", filterPaymentMethod.value);
        if (filterStartDate.value) params.append("startDate", filterStartDate.value);
        if (filterEndDate.value) params.append("endDate", filterEndDate.value);
        if (filterMinAmount && filterMinAmount.value) params.append("minAmount", filterMinAmount.value);
        if (filterMaxAmount && filterMaxAmount.value) params.append("maxAmount", filterMaxAmount.value);

        try {
            const res = await fetch(`/api/expenses?${params.toString()}`);
            if (res.ok) {
                const expenses = await res.json();
                renderExpenseTable(expenses);
            } else if (res.status === 401) {
                handleUnauthenticated();
            } else {
                const err = await res.json();
                showToast(err.message || "Failed to load expenses", false);
            }
        } catch (err) {
            showToast("Error connecting to server", false);
        } finally {
            loadingState.classList.add("d-none");
        }
    }

    /**
     * Renders expenses into the table rows
     */
    function renderExpenseTable(expenses) {
        expenseTableBody.innerHTML = "";
        expenseCountBadge.textContent = `${expenses.length} Records`;

        if (expenses.length === 0) {
            emptyState.classList.remove("d-none");
            return;
        }

        emptyState.classList.add("d-none");

        expenses.forEach(exp => {
            const tr = document.createElement("tr");

            // Payment badge color
            let paymentBadgeClass = "bg-secondary-subtle text-secondary";
            if (exp.paymentMethod === "UPI") paymentBadgeClass = "bg-info-subtle text-info-emphasis";
            else if (exp.paymentMethod === "CARD") paymentBadgeClass = "bg-primary-subtle text-primary";
            else if (exp.paymentMethod === "CASH") paymentBadgeClass = "bg-success-subtle text-success";
            else if (exp.paymentMethod === "BANK_TRANSFER") paymentBadgeClass = "bg-warning-subtle text-warning-emphasis";

            const formattedAmount = Number(exp.amount).toLocaleString("en-IN", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            });

            tr.innerHTML = `
                <td class="ps-4 fw-semibold text-secondary">${exp.date}</td>
                <td>
                    <span class="badge bg-light text-dark border px-2 py-1">
                        <i class="bi bi-tag me-1 text-primary"></i>${exp.category ? exp.category.name : "Uncategorized"}
                    </span>
                </td>
                <td class="fw-bold text-dark fs-6">₹ ${formattedAmount}</td>
                <td>
                    <span class="badge ${paymentBadgeClass} px-2 py-1 rounded-pill">
                        ${exp.paymentMethod}
                    </span>
                </td>
                <td class="text-truncate text-secondary" style="max-width: 250px;">
                    ${exp.description ? escapeHtml(exp.description) : '<span class="text-muted fst-italic">No description</span>'}
                </td>
                <td class="text-end pe-4">
                    <button class="btn btn-sm btn-outline-primary rounded-circle me-1 btn-edit" title="Edit" aria-label="Edit expense" data-id="${exp.id}">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <button class="btn btn-sm btn-outline-danger rounded-circle btn-delete" title="Delete" aria-label="Delete expense" data-id="${exp.id}">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            `;

            // Attach edit event
            tr.querySelector(".btn-edit").addEventListener("click", () => openEditModal(exp));

            // Attach delete event
            tr.querySelector(".btn-delete").addEventListener("click", () => {
                expenseIdToDelete = exp.id;
                deleteModal.show();
            });

            expenseTableBody.appendChild(tr);
        });
    }

    function escapeHtml(text) {
        const div = document.createElement("div");
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Filter Actions
     */
    if (filterForm) {
        filterForm.addEventListener("submit", (e) => {
            e.preventDefault();
            loadExpenses();
        });
    }

    if (btnClearFilters) {
        btnClearFilters.addEventListener("click", () => {
            filterSearch.value = "";
            filterCategory.value = "";
            filterPaymentMethod.value = "";
            filterStartDate.value = "";
            filterEndDate.value = "";
            if (filterMinAmount) filterMinAmount.value = "";
            if (filterMaxAmount) filterMaxAmount.value = "";
            loadExpenses();
        });
    }

    /**
     * Modal Management: Open Add Expense Modal
     */
    if (btnOpenAddExpenseModal) {
        btnOpenAddExpenseModal.addEventListener("click", () => {
            expenseForm.reset();
            modalExpenseId.value = "";
            modalAlert.classList.add("d-none");
            expenseModalTitle.innerHTML = '<i class="bi bi-plus-circle me-2"></i>Add Expense';
            if (btnSaveExpense) {
                btnSaveExpense.innerHTML = '<i class="bi bi-plus-circle me-1"></i> Create Expense';
            }
            // Default date to today
            modalDate.value = new Date().toISOString().split("T")[0];
            modalPaymentMethod.value = "UPI";
            expenseModal.show();
        });
    }

    /**
     * Modal Management: Open Edit Expense Modal
     */
    function openEditModal(expense) {
        expenseForm.reset();
        modalAlert.classList.add("d-none");
        expenseModalTitle.innerHTML = '<i class="bi bi-pencil-square me-2"></i>Edit Expense';
        if (btnSaveExpense) {
            btnSaveExpense.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Update Expense';
        }

        modalExpenseId.value = expense.id;
        modalAmount.value = expense.amount;

        if (expense.category && expense.category.id) {
            const catIdStr = String(expense.category.id);
            const matchedCat = [...modalCategory.options].find(opt => opt.value === catIdStr);
            if (matchedCat) {
                modalCategory.value = catIdStr;
            } else {
                const histCatOpt = document.createElement("option");
                histCatOpt.value = catIdStr;
                histCatOpt.textContent = `${expense.category.name || "Category " + catIdStr} (Historical)`;
                modalCategory.appendChild(histCatOpt);
                modalCategory.value = catIdStr;
            }
        }

        modalDate.value = expense.date;

        // Ensure historical payment method option exists and matches case-insensitively
        if (expense.paymentMethod) {
            const pmVal = typeof expense.paymentMethod === "object" ? expense.paymentMethod.value : expense.paymentMethod;
            if (pmVal) {
                const matchedOpt = [...modalPaymentMethod.options].find(opt => opt.value.toLowerCase() === pmVal.toLowerCase());
                if (matchedOpt) {
                    modalPaymentMethod.value = matchedOpt.value;
                } else {
                    const histOpt = document.createElement("option");
                    histOpt.value = pmVal;
                    histOpt.textContent = `${pmVal} (Historical)`;
                    modalPaymentMethod.appendChild(histOpt);
                    modalPaymentMethod.value = pmVal;
                }
            }
        }
        modalDescription.value = expense.description || "";

        expenseModal.show();
    }

    if (expenseModalEl) {
        expenseModalEl.addEventListener("hidden.bs.modal", () => {
            expenseForm.reset();
            modalExpenseId.value = "";
            modalAlert.classList.add("d-none");
            expenseModalTitle.innerHTML = '<i class="bi bi-plus-circle me-2"></i>Add Expense';
            if (btnSaveExpense) {
                btnSaveExpense.innerHTML = '<i class="bi bi-plus-circle me-1"></i> Create Expense';
            }
        });
    }

    /**
     * Modal Submission: Create or Update Expense
     */
    if (expenseForm) {
        expenseForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            modalAlert.classList.add("d-none");

            const isUpdate = Boolean(modalExpenseId.value);
            const url = isUpdate ? `/api/expenses/${modalExpenseId.value}` : "/api/expenses";
            const method = isUpdate ? "PUT" : "POST";

            const payload = {
                amount: parseFloat(modalAmount.value),
                categoryId: parseInt(modalCategory.value),
                date: modalDate.value,
                paymentMethod: modalPaymentMethod.value,
                description: modalDescription.value.trim()
            };

            try {
                const res = await fetch(url, {
                    method: method,
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(payload)
                });

                const data = await res.json();
                if (res.ok) {
                    expenseModal.hide();
                    showToast(isUpdate ? "Expense updated successfully!" : "Expense created successfully!");
                    await loadExpenses();
                } else {
                    modalAlert.textContent = data.message || "Failed to save expense";
                    modalAlert.classList.remove("d-none");
                }
            } catch (err) {
                modalAlert.textContent = "Error communicating with server";
                modalAlert.classList.remove("d-none");
            }
        });
    }

    /**
     * Confirm Delete Action
     */
    if (btnConfirmDelete) {
        btnConfirmDelete.addEventListener("click", async () => {
            if (!expenseIdToDelete) return;

            try {
                const res = await fetch(`/api/expenses/${expenseIdToDelete}`, {
                    method: "DELETE"
                });

                if (res.ok) {
                    deleteModal.hide();
                    showToast("Expense deleted successfully");
                    await loadExpenses();
                } else {
                    const data = await res.json();
                    showToast(data.message || "Failed to delete expense", false);
                }
            } catch (err) {
                showToast("Error deleting expense", false);
            } finally {
                expenseIdToDelete = null;
            }
        });
    }

    // Initial check on load
    checkAuth();
});
