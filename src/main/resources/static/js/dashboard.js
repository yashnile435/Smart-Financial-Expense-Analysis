/**
 * JavaScript logic for Financial Dashboard & Analytics (Stage 6).
 * Handles summary aggregation rendering, Chart.js visualizations,
 * rule-based financial insights, and session authentication.
 */
document.addEventListener("DOMContentLoaded", () => {
    let currentUser = null;
    let categoryChartInstance = null;
    let paymentMethodChartInstance = null;
    let trendChartInstance = null;

    const MONTH_NAMES = [
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    ];

    // Auth & Navigation Elements
    const authUserMenu = document.getElementById("authUserMenu");
    const currentUserName = document.getElementById("currentUserName");
    const authRequiredSection = document.getElementById("authRequiredSection");
    const dashboardAppSection = document.getElementById("dashboardAppSection");
    const btnLogout = document.getElementById("btnLogout");
    const btnRefreshDashboard = document.getElementById("btnRefreshDashboard");

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

    // Dashboard View Elements
    const dashboardLoading = document.getElementById("dashboardLoading");
    const dashboardContent = document.getElementById("dashboardContent");

    // KPI Elements
    const kpiTotalExpenses = document.getElementById("kpiTotalExpenses");
    const kpiExpenseCount = document.getElementById("kpiExpenseCount");
    const kpiThisMonthExpenses = document.getElementById("kpiThisMonthExpenses");
    const kpiThisMonthCount = document.getElementById("kpiThisMonthCount");
    const kpiAverageExpense = document.getElementById("kpiAverageExpense");
    const kpiHighestExpense = document.getElementById("kpiHighestExpense");

    // Budget Card Elements
    const budgetCardMonthTitle = document.getElementById("budgetCardMonthTitle");
    const budgetCardStatusBadge = document.getElementById("budgetCardStatusBadge");
    const budgetCardBudget = document.getElementById("budgetCardBudget");
    const budgetCardSpent = document.getElementById("budgetCardSpent");
    const budgetCardRemaining = document.getElementById("budgetCardRemaining");
    const budgetCardUtilization = document.getElementById("budgetCardUtilization");
    const budgetCardProgressBar = document.getElementById("budgetCardProgressBar");
    const budgetCardBarLabel = document.getElementById("budgetCardBarLabel");
    const noBudgetWarning = document.getElementById("noBudgetWarning");

    // Insights & Tables
    const insightsContainer = document.getElementById("insightsContainer");
    const recentExpensesTableBody = document.getElementById("recentExpensesTableBody");
    const emptyRecentState = document.getElementById("emptyRecentState");

    // Chart Empty States
    const categoryChartEmpty = document.getElementById("categoryChartEmpty");
    const paymentMethodChartEmpty = document.getElementById("paymentMethodChartEmpty");

    // Toast
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

    /**
     * Checks if user is authenticated.
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
                dashboardAppSection.classList.remove("d-none");

                const adminLink = document.getElementById("navAdminLink");
                if (adminLink) {
                    adminLink.classList.add("d-none");
                }

                await loadDashboard();
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
        dashboardAppSection.classList.add("d-none");
        const adminLink = document.getElementById("navAdminLink");
        if (adminLink) adminLink.classList.add("d-none");

        // If URL hash requests register tab, switch to it
        if (window.location.hash === "#register" || window.location.hash === "#tabRegister") {
            const registerTabBtn = document.getElementById("register-tab");
            if (registerTabBtn) {
                const tab = new bootstrap.Tab(registerTabBtn);
                tab.show();
            }
        }
    }

    /**
     * Login handler
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
                    if (data.role === "ADMIN") {
                        window.location.href = "admin.html";
                        return;
                    }
                    showToast("Login successful! Loading dashboard...");
                    await checkAuth();
                } else {
                    loginAlert.textContent = data.message || "Invalid email or password";
                    loginAlert.classList.remove("d-none");
                }
            } catch (err) {
                loginAlert.textContent = "Network error connecting to server.";
                loginAlert.classList.remove("d-none");
            }
        });
    }

    /**
     * Register handler
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
     * Logout handler - clears session and returns to public homepage
     */
    if (btnLogout) {
        btnLogout.addEventListener("click", async () => {
            try {
                await fetch("/api/auth/logout", { method: "POST" });
            } catch (err) {}
            window.location.href = "index.html";
        });
    }

    if (btnRefreshDashboard) {
        btnRefreshDashboard.addEventListener("click", () => {
            loadDashboard();
            showToast("Refreshing financial metrics...");
        });
    }

    /**
     * Fetches dashboard summary and populates metrics, charts, insights, and transactions.
     */
    async function loadDashboard() {
        dashboardLoading.classList.remove("d-none");
        dashboardContent.classList.add("d-none");

        try {
            const res = await fetch("/api/dashboard/summary");
            if (res.ok) {
                const data = await res.json();
                renderDashboard(data);
                dashboardLoading.classList.add("d-none");
                dashboardContent.classList.remove("d-none");

                // Stage 9: Load advanced dashboard components
                loadBudgetAlerts();
                loadSpendingComparison();
                loadGoalsSnapshot();
                loadRecurringSnapshot();
            } else if (res.status === 401) {
                handleUnauthenticated();
            } else {
                showToast("Failed to fetch dashboard summary", false);
            }
        } catch (err) {
            showToast("Network error while loading dashboard", false);
        }
    }

    async function loadBudgetAlerts() {
        const alertBanner = document.getElementById("budgetAlertBanner");
        if (!alertBanner) return;

        try {
            const res = await fetch("/api/budgets/alerts");
            if (!res.ok) return;
            const alerts = await res.json();
            const criticalAlerts = alerts.filter(a => a.alertLevel === "WARNING" || a.alertLevel === "CRITICAL_WARNING" || a.alertLevel === "EXCEEDED");

            if (criticalAlerts.length > 0) {
                alertBanner.innerHTML = criticalAlerts.map(a => {
                    let alertClass = "alert-warning";
                    let iconClass = "bi-exclamation-triangle-fill text-warning";
                    if (a.alertLevel === "EXCEEDED") {
                        alertClass = "alert-danger";
                        iconClass = "bi-exclamation-octagon-fill text-danger";
                    }
                    return `
                        <div class="alert ${alertClass} shadow-sm border-0 rounded-4 d-flex align-items-center mb-2 p-3">
                            <i class="bi ${iconClass} fs-3 me-3"></i>
                            <div class="flex-grow-1">
                                <div class="fw-bold">${a.alertLevel.replace('_', ' ')} (${a.month}/${a.year})</div>
                                <div class="small">${a.message}</div>
                            </div>
                            <a href="budgets.html" class="btn btn-sm btn-outline-dark rounded-pill px-3">Manage Budget</a>
                        </div>`;
                }).join("");
                alertBanner.classList.remove("d-none");
            } else {
                alertBanner.classList.add("d-none");
            }
        } catch (e) {
            console.error("Failed to load budget alerts", e);
        }
    }

    async function loadSpendingComparison() {
        try {
            const res = await fetch("/api/analytics/comparison");
            if (!res.ok) return;
            const comp = await res.json();

            const compCurrentTotal = document.getElementById("compCurrentTotal");
            const compPreviousTotal = document.getElementById("compPreviousTotal");
            const compDifference = document.getElementById("compDifference");
            const compPctChange = document.getElementById("compPctChange");
            const comparisonDirectionBadge = document.getElementById("comparisonDirectionBadge");
            const comparisonSubtitle = document.getElementById("comparisonSubtitle");
            const compMonthlyAvg = document.getElementById("compMonthlyAvg");
            const compAvgDiff = document.getElementById("compAvgDiff");

            if (compCurrentTotal) compCurrentTotal.textContent = formatCurrency(comp.currentPeriodTotal);
            if (compPreviousTotal) compPreviousTotal.textContent = formatCurrency(comp.comparisonPeriodTotal);
            if (compDifference) {
                compDifference.textContent = formatCurrency(Math.abs(comp.difference));
                if (comp.direction === "INCREASED") {
                    compDifference.className = "fw-bold mb-0 mt-1 text-danger";
                } else if (comp.direction === "DECREASED") {
                    compDifference.className = "fw-bold mb-0 mt-1 text-success";
                } else {
                    compDifference.className = "fw-bold mb-0 mt-1 text-secondary";
                }
            }
            if (compPctChange) {
                compPctChange.textContent = `${comp.percentageChange.toFixed(1)}% ${comp.direction.toLowerCase()}`;
                compPctChange.className = comp.direction === "INCREASED" ? "fw-semibold text-danger" : (comp.direction === "DECREASED" ? "fw-semibold text-success" : "fw-semibold text-secondary");
            }
            if (comparisonDirectionBadge) {
                comparisonDirectionBadge.textContent = comp.direction;
                comparisonDirectionBadge.className = `badge rounded-pill px-3 py-1 ${comp.direction === 'INCREASED' ? 'bg-danger-subtle text-danger border border-danger' : (comp.direction === 'DECREASED' ? 'bg-success-subtle text-success border border-success' : 'bg-secondary-subtle text-secondary')}`;
            }
            if (comparisonSubtitle) {
                comparisonSubtitle.textContent = `Month ${comp.currentMonth}/${comp.currentYear} vs Month ${comp.comparisonMonth}/${comp.comparisonYear}`;
            }
            if (compMonthlyAvg) compMonthlyAvg.textContent = formatCurrency(comp.monthlyAverage);
            if (compAvgDiff) {
                const diffVal = parseFloat(comp.currentVsAverageDifference || 0);
                const prefix = diffVal >= 0 ? "+" : "";
                compAvgDiff.textContent = `${prefix}${formatCurrency(diffVal)} vs 6-mo avg`;
                compAvgDiff.className = diffVal > 0 ? "text-danger fw-semibold" : "text-success fw-semibold";
            }
        } catch (e) {
            console.error("Failed to load spending comparison", e);
        }
    }

    async function loadGoalsSnapshot() {
        try {
            const res = await fetch("/api/goals");
            if (!res.ok) return;
            const goals = await res.json();
            let totalSaved = 0;
            let activeCount = 0;
            goals.forEach(g => {
                totalSaved += parseFloat(g.currentAmount || 0);
                if (g.status === "ACTIVE") activeCount++;
            });

            const dashGoalsSaved = document.getElementById("dashGoalsSaved");
            const dashGoalsCount = document.getElementById("dashGoalsCount");
            if (dashGoalsSaved) dashGoalsSaved.textContent = formatCurrency(totalSaved);
            if (dashGoalsCount) dashGoalsCount.textContent = `${activeCount} Active Goal(s) of ${goals.length}`;
        } catch (e) {
            console.error("Failed to load goals snapshot", e);
        }
    }

    async function loadRecurringSnapshot() {
        try {
            const res = await fetch("/api/recurring-expenses");
            if (!res.ok) return;
            const items = await res.json();
            let totalCommitment = 0;
            let activeCount = 0;
            items.forEach(i => {
                if (i.active) {
                    activeCount++;
                    totalCommitment += parseFloat(i.amount || 0);
                }
            });

            const dashRecurringCount = document.getElementById("dashRecurringCount");
            const dashRecurringCommitment = document.getElementById("dashRecurringCommitment");
            if (dashRecurringCount) dashRecurringCount.textContent = activeCount;
            if (dashRecurringCommitment) dashRecurringCommitment.textContent = `${formatCurrency(totalCommitment)} Commitment`;
        } catch (e) {
            console.error("Failed to load recurring snapshot", e);
        }
    }

    function renderDashboard(data) {
        // 1. Top KPI Metrics
        kpiTotalExpenses.textContent = formatCurrency(data.totalExpenses);
        kpiExpenseCount.textContent = `${data.expenseCount} Total Transaction${data.expenseCount === 1 ? "" : "s"}`;

        kpiThisMonthExpenses.textContent = formatCurrency(data.currentMonthExpenses);
        kpiThisMonthCount.textContent = `${data.currentMonthExpenseCount} Transaction${data.currentMonthExpenseCount === 1 ? "" : "s"}`;

        kpiAverageExpense.textContent = formatCurrency(data.averageExpense);
        kpiHighestExpense.textContent = formatCurrency(data.highestExpense);

        // 2. Current Month Budget Card
        const monthName = MONTH_NAMES[data.currentMonth] || `Month ${data.currentMonth}`;
        budgetCardMonthTitle.textContent = `${monthName} ${data.currentYear}`;

        if (data.currentMonthBudget !== null && data.currentMonthBudget !== undefined) {
            noBudgetWarning.classList.add("d-none");

            budgetCardBudget.textContent = formatCurrency(data.currentMonthBudget);
            budgetCardSpent.textContent = formatCurrency(data.currentMonthExpenses);

            const remaining = Number(data.currentMonthRemaining || 0);
            budgetCardRemaining.textContent = formatCurrency(remaining);
            budgetCardRemaining.className = remaining < 0 ? "fw-bold text-danger mb-0" : "fw-bold text-success mb-0";

            const util = Number(data.currentMonthUtilization || 0);
            budgetCardUtilization.textContent = `${util.toFixed(1)}%`;
            budgetCardBarLabel.textContent = `${util.toFixed(1)}% Used (Actual)`;

            // Visual bar capped at 100%
            const visualPercent = Math.min(Math.max(util, 0), 100);
            budgetCardProgressBar.style.width = `${visualPercent}%`;
            budgetCardProgressBar.setAttribute("aria-valuenow", visualPercent);

            // Status Badge & Bar color
            if (data.currentMonthBudgetStatus === "OVER_BUDGET") {
                budgetCardStatusBadge.textContent = "OVER BUDGET";
                budgetCardStatusBadge.className = "badge bg-danger text-white px-3 py-2 rounded-pill fs-6 fw-bold";
                budgetCardProgressBar.className = "progress-bar progress-bar-striped progress-bar-animated bg-danger";
            } else if (data.currentMonthBudgetStatus === "NEAR_LIMIT") {
                budgetCardStatusBadge.textContent = "NEAR LIMIT";
                budgetCardStatusBadge.className = "badge bg-warning text-dark px-3 py-2 rounded-pill fs-6 fw-bold";
                budgetCardProgressBar.className = "progress-bar progress-bar-striped progress-bar-animated bg-warning";
            } else {
                budgetCardStatusBadge.textContent = "UNDER BUDGET";
                budgetCardStatusBadge.className = "badge bg-success text-white px-3 py-2 rounded-pill fs-6 fw-bold";
                budgetCardProgressBar.className = "progress-bar progress-bar-striped progress-bar-animated bg-success";
            }
        } else {
            noBudgetWarning.classList.remove("d-none");
            budgetCardBudget.textContent = "₹0.00";
            budgetCardSpent.textContent = formatCurrency(data.currentMonthExpenses);
            budgetCardRemaining.textContent = "₹0.00";
            budgetCardRemaining.className = "fw-bold text-muted mb-0";
            budgetCardUtilization.textContent = "0.0%";
            budgetCardStatusBadge.textContent = "NOT CONFIGURED";
            budgetCardStatusBadge.className = "badge bg-secondary text-white px-3 py-2 rounded-pill fs-6 fw-bold";
            budgetCardProgressBar.style.width = "0%";
            budgetCardProgressBar.className = "progress-bar bg-secondary";
            budgetCardBarLabel.textContent = "No Budget Configured";
        }

        // 3. Financial Insights
        renderInsights(data.financialInsights);

        // 4. Charts
        renderCategoryChart(data.categoryBreakdown);
        renderPaymentMethodChart(data.paymentMethodBreakdown);
        renderTrendChart(data.monthlyTrend);

        // 5. Recent Expenses Table
        renderRecentExpenses(data.recentExpenses);
    }

    function renderInsights(insights) {
        if (!insights || insights.length === 0) {
            insightsContainer.innerHTML = `
                <div class="col-12">
                    <div class="alert alert-light border text-muted small mb-0 rounded-3">
                        <i class="bi bi-info-circle me-1"></i> No insights available yet.
                    </div>
                </div>
            `;
            return;
        }

        insightsContainer.innerHTML = insights.map(i => {
            let iconClass = "bi-info-circle-fill text-primary";
            let alertClass = "alert-primary border-primary-subtle";

            switch (i.type) {
                case "OVER_BUDGET":
                    iconClass = "bi-exclamation-octagon-fill text-danger";
                    alertClass = "alert-danger border-danger-subtle";
                    break;
                case "NEAR_LIMIT":
                    iconClass = "bi-exclamation-triangle-fill text-warning";
                    alertClass = "alert-warning border-warning-subtle";
                    break;
                case "UNDER_BUDGET":
                    iconClass = "bi-check-circle-fill text-success";
                    alertClass = "alert-success border-success-subtle";
                    break;
                case "CATEGORY_CONCENTRATION":
                    iconClass = "bi-pie-chart-fill text-info";
                    alertClass = "alert-info border-info-subtle";
                    break;
                case "SPENDING_INCREASED":
                    iconClass = "bi-graph-up-arrow text-danger";
                    alertClass = "alert-danger border-danger-subtle";
                    break;
                case "SPENDING_REDUCED":
                    iconClass = "bi-graph-down-arrow text-success";
                    alertClass = "alert-success border-success-subtle";
                    break;
                case "NO_DATA":
                    iconClass = "bi-clock-history text-secondary";
                    alertClass = "alert-secondary border-secondary-subtle";
                    break;
            }

            return `
                <div class="col-md-6 col-lg-4">
                    <div class="alert ${alertClass} d-flex align-items-start gap-3 p-3 rounded-4 mb-0 h-100">
                        <i class="bi ${iconClass} fs-3 flex-shrink-0"></i>
                        <div>
                            <div class="fw-bold small mb-1">${i.title}</div>
                            <div class="small opacity-75">${i.message}</div>
                        </div>
                    </div>
                </div>
            `;
        }).join("");
    }

    const PALETTE = [
        "#7C5CFC", "#9B7CFD", "#6845EC", "#B8A3FF", "#4E29D4",
        "#20c997", "#ffc107", "#fd7e14", "#d63384", "#0dcaf0"
    ];

    function renderCategoryChart(breakdown) {
        const ctx = document.getElementById("categoryChart");
        if (categoryChartInstance) {
            categoryChartInstance.destroy();
        }

        if (!breakdown || breakdown.length === 0) {
            ctx.classList.add("d-none");
            categoryChartEmpty.classList.remove("d-none");
            return;
        }

        ctx.classList.remove("d-none");
        categoryChartEmpty.classList.add("d-none");

        const labels = breakdown.map(c => `${c.categoryName} (${c.percentage}%)`);
        const values = breakdown.map(c => Number(c.totalAmount));

        categoryChartInstance = new Chart(ctx, {
            type: "doughnut",
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: PALETTE.slice(0, values.length),
                    borderWidth: 2,
                    borderColor: "#ffffff"
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: "bottom",
                        labels: { boxWidth: 12, font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                return ` ${ctx.label}: ₹${ctx.raw.toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
                            }
                        }
                    }
                }
            }
        });
    }

    function renderPaymentMethodChart(breakdown) {
        const ctx = document.getElementById("paymentMethodChart");
        if (paymentMethodChartInstance) {
            paymentMethodChartInstance.destroy();
        }

        if (!breakdown || breakdown.length === 0) {
            ctx.classList.add("d-none");
            paymentMethodChartEmpty.classList.remove("d-none");
            return;
        }

        ctx.classList.remove("d-none");
        paymentMethodChartEmpty.classList.add("d-none");

        const labels = breakdown.map(p => `${p.paymentMethod} (${p.percentage}%)`);
        const values = breakdown.map(p => Number(p.totalAmount));

        paymentMethodChartInstance = new Chart(ctx, {
            type: "doughnut",
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: ["#7C5CFC", "#9B7CFD", "#6845EC", "#B8A3FF", "#4E29D4"],
                    borderWidth: 2,
                    borderColor: "#ffffff"
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: "bottom",
                        labels: { boxWidth: 12, font: { size: 11 } }
                    },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                return ` ${ctx.label}: ₹${ctx.raw.toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
                            }
                        }
                    }
                }
            }
        });
    }

    function renderTrendChart(trend) {
        const ctx = document.getElementById("trendChart");
        if (trendChartInstance) {
            trendChartInstance.destroy();
        }

        if (!trend || trend.length === 0) return;

        const labels = trend.map(m => {
            const shortName = MONTH_NAMES[m.month] ? MONTH_NAMES[m.month].substring(0, 3) : m.month;
            return `${shortName} ${m.year}`;
        });
        const values = trend.map(m => Number(m.totalAmount));

        trendChartInstance = new Chart(ctx, {
            type: "line",
            data: {
                labels: labels,
                datasets: [{
                    label: "Monthly Expenditure (₹)",
                    data: values,
                    borderColor: "#7C5CFC",
                    backgroundColor: "rgba(124, 92, 252, 0.12)",
                    borderWidth: 3,
                    fill: true,
                    tension: 0.35,
                    pointBackgroundColor: "#7C5CFC",
                    pointRadius: 5,
                    pointHoverRadius: 7
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            callback: function (value) {
                                return "₹" + value.toLocaleString("en-IN");
                            }
                        },
                        grid: { color: "rgba(0, 0, 0, 0.05)" }
                    },
                    x: {
                        grid: { display: false }
                    }
                },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: function (ctx) {
                                return ` Spending: ₹${ctx.raw.toLocaleString("en-IN", { minimumFractionDigits: 2 })}`;
                            }
                        }
                    }
                }
            }
        });
    }

    function renderRecentExpenses(recents) {
        if (!recents || recents.length === 0) {
            emptyRecentState.classList.remove("d-none");
            recentExpensesTableBody.innerHTML = "";
            return;
        }

        emptyRecentState.classList.add("d-none");
        recentExpensesTableBody.innerHTML = recents.map(r => `
            <tr>
                <td class="ps-4 text-secondary small fw-semibold">${r.date}</td>
                <td>
                    <span class="badge bg-light text-dark border px-2 py-1 rounded-pill">
                        ${r.categoryName || "General"}
                    </span>
                </td>
                <td class="fw-bold text-dark">${formatCurrency(r.amount)}</td>
                <td>
                    <span class="badge bg-secondary-subtle text-secondary px-2 py-1 rounded-pill small">
                        ${r.paymentMethod}
                    </span>
                </td>
                <td class="text-muted small">${r.description || "—"}</td>
            </tr>
        `).join("");
    }

    // Initialize authentication check
    checkAuth();
});
