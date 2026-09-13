/**
 * Reports & Financial Reporting Module Frontend
 * Smart Financial Expense Analysis - Stage 7
 * Financial reporting module for monthly, date-range, and category breakdowns.
 */

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

let currentUser = null;
let currentReportData = null;

// Initialize the reports application
async function initApp() {
    initDates();
    setupEventListeners();
    const authenticated = await checkAuth();
    if (authenticated) {
        await loadCategories();
        await generateReport();
    }
}

// Check session authentication status
async function checkAuth() {
    try {
        const response = await fetch('/api/auth/me', {
            method: 'GET',
            headers: { 'Accept': 'application/json' }
        });

        if (response.ok) {
            currentUser = await response.json();
            if (currentUser.role === 'ADMIN') {
                window.location.href = 'admin.html';
                return false;
            }

            document.getElementById('currentUserName').textContent = currentUser.name || 'User';
            document.getElementById('authUserMenu').classList.remove('d-none');
            document.getElementById('authUserMenu').classList.add('d-flex');
            document.getElementById('reportsViewSection').classList.remove('d-none');
            document.getElementById('authRequiredSection').classList.add('d-none');
            return true;
        } else {
            handleUnauthenticated();
            return false;
        }
    } catch (err) {
        handleUnauthenticated();
        return false;
    }
}

function handleUnauthenticated() {
    currentUser = null;
    document.getElementById('authUserMenu').classList.add('d-none');
    document.getElementById('authUserMenu').classList.remove('d-flex');
    document.getElementById('reportsViewSection').classList.add('d-none');
    document.getElementById('authRequiredSection').classList.remove('d-none');
    const adminLink = document.getElementById('navAdminLink');
    if (adminLink) adminLink.classList.add('d-none');
}

// Initialize date selectors with sensible defaults
function initDates() {
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth() + 1;

    document.getElementById('yearInput').value = year;
    document.getElementById('monthSelect').value = month;

    // Start date = 1st of current month, End date = today
    const firstDay = new Date(year, today.getMonth(), 1);
    document.getElementById('startDateInput').value = formatDateIso(firstDay);
    document.getElementById('endDateInput').value = formatDateIso(today);
}

function formatDateIso(d) {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
}

// Load expense categories into filter dropdown
async function loadCategories() {
    try {
        const response = await fetch('/api/categories', { credentials: 'include' });
        if (response.ok) {
            const categories = await response.json();
            const select = document.getElementById('categoryFilterSelect');
            select.innerHTML = '<option value="">All Categories</option>';
            categories.forEach(cat => {
                const opt = document.createElement('option');
                opt.value = cat.id;
                opt.textContent = cat.name;
                select.appendChild(opt);
            });
        }
    } catch (e) {
        console.warn('Failed to load categories', e);
    }
}

// Setup form and button listeners
function setupEventListeners() {
    // Logout listener
    document.getElementById('btnLogout').addEventListener('click', async () => {
        try {
            await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' });
            window.location.href = 'index.html';
        } catch (e) {
            window.location.href = 'index.html';
        }
    });

    // Report type change toggles filter visibility
    document.getElementById('reportTypeSelect').addEventListener('change', handleReportTypeChange);

    // Form submit triggers report generation
    document.getElementById('reportFilterForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        await generateReport();
    });

    // Print button
    document.getElementById('btnPrintReport').addEventListener('click', () => {
        window.print();
    });

    // CSV export button
    document.getElementById('btnExportCsv').addEventListener('click', () => {
        exportReport('csv');
    });

    // PDF export button
    document.getElementById('btnExportPdf').addEventListener('click', () => {
        exportReport('pdf');
    });
}

function handleReportTypeChange() {
    const reportType = document.getElementById('reportTypeSelect').value;
    const monthCol = document.getElementById('monthFilterCol');
    const yearCol = document.getElementById('yearFilterCol');
    const startCol = document.getElementById('startDateFilterCol');
    const endCol = document.getElementById('endDateFilterCol');
    const catCol = document.getElementById('categoryFilterCol');
    const pmCol = document.getElementById('paymentMethodFilterCol');

    // Default: hide date pickers, show month/year
    if (reportType === 'MONTHLY' || reportType === 'BUDGET_VS_ACTUAL') {
        monthCol.classList.remove('d-none');
        yearCol.classList.remove('d-none');
        startCol.classList.add('d-none');
        endCol.classList.add('d-none');
    } else {
        monthCol.classList.add('d-none');
        yearCol.classList.add('d-none');
        startCol.classList.remove('d-none');
        endCol.classList.remove('d-none');
    }

    if (reportType === 'BUDGET_VS_ACTUAL') {
        catCol.classList.add('d-none');
        pmCol.classList.add('d-none');
    } else if (reportType === 'CATEGORY') {
        catCol.classList.add('d-none');
        pmCol.classList.remove('d-none');
    } else if (reportType === 'PAYMENT_METHOD') {
        catCol.classList.remove('d-none');
        pmCol.classList.add('d-none');
    } else {
        catCol.classList.remove('d-none');
        pmCol.classList.remove('d-none');
    }
}

// Generate the selected report
async function generateReport() {
    const reportType = document.getElementById('reportTypeSelect').value;
    const btn = document.getElementById('btnGenerateReport');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Generating...';

    try {
        if (reportType === 'MONTHLY') {
            await fetchMonthlyReport();
        } else if (reportType === 'DATE_RANGE') {
            await fetchDateRangeReport();
        } else if (reportType === 'CATEGORY') {
            await fetchCategoryOnlyReport();
        } else if (reportType === 'PAYMENT_METHOD') {
            await fetchPaymentMethodOnlyReport();
        } else if (reportType === 'BUDGET_VS_ACTUAL') {
            await fetchBudgetVsActualOnlyReport();
        }
    } catch (err) {
        showToast(err.message || 'Error generating report', true);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-arrow-clockwise me-1"></i> Generate Report';
    }
}

async function fetchMonthlyReport() {
    const month = document.getElementById('monthSelect').value;
    const year = document.getElementById('yearInput').value;
    const catId = document.getElementById('categoryFilterSelect').value;
    const pm = document.getElementById('paymentMethodFilterSelect').value;

    let url = `/api/reports/monthly?month=${month}&year=${year}`;
    if (catId) url += `&categoryId=${catId}`;
    if (pm) url += `&paymentMethod=${pm}`;

    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || 'Failed to fetch monthly report');
    }

    currentReportData = await res.json();
    renderComprehensiveReport(currentReportData, true);
}

async function fetchDateRangeReport() {
    const startDate = document.getElementById('startDateInput').value;
    const endDate = document.getElementById('endDateInput').value;
    const catId = document.getElementById('categoryFilterSelect').value;
    const pm = document.getElementById('paymentMethodFilterSelect').value;

    if (!startDate || !endDate) {
        throw new Error('Please specify both start date and end date');
    }
    if (startDate > endDate) {
        throw new Error('Start date cannot be after end date');
    }

    let url = `/api/reports/date-range?startDate=${startDate}&endDate=${endDate}`;
    if (catId) url += `&categoryId=${catId}`;
    if (pm) url += `&paymentMethod=${pm}`;

    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || 'Failed to fetch date range report');
    }

    currentReportData = await res.json();
    renderComprehensiveReport(currentReportData, false);
}

async function fetchCategoryOnlyReport() {
    const startDate = document.getElementById('startDateInput').value;
    const endDate = document.getElementById('endDateInput').value;

    if (!startDate || !endDate) {
        throw new Error('Please specify both start date and end date');
    }
    if (startDate > endDate) {
        throw new Error('Start date cannot be after end date');
    }

    const res = await fetch(`/api/reports/category?startDate=${startDate}&endDate=${endDate}`, { credentials: 'include' });
    if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || 'Failed to fetch category report');
    }

    const categories = await res.json();
    renderCategoryOnlyView(categories, startDate, endDate);
}

async function fetchPaymentMethodOnlyReport() {
    const startDate = document.getElementById('startDateInput').value;
    const endDate = document.getElementById('endDateInput').value;

    if (!startDate || !endDate) {
        throw new Error('Please specify both start date and end date');
    }
    if (startDate > endDate) {
        throw new Error('Start date cannot be after end date');
    }

    const res = await fetch(`/api/reports/payment-method?startDate=${startDate}&endDate=${endDate}`, { credentials: 'include' });
    if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || 'Failed to fetch payment method report');
    }

    const methods = await res.json();
    renderPaymentMethodOnlyView(methods, startDate, endDate);
}

async function fetchBudgetVsActualOnlyReport() {
    const month = document.getElementById('monthSelect').value;
    const year = document.getElementById('yearInput').value;

    const res = await fetch(`/api/reports/budget-vs-actual?month=${month}&year=${year}`, { credentials: 'include' });
    if (!res.ok) {
        const errorData = await res.json();
        throw new Error(errorData.message || 'Failed to fetch budget vs actual comparison');
    }

    const comparison = await res.json();
    renderBudgetVsActualOnlyView(comparison, month, year);
}

// Render full comprehensive report (monthly or custom date range)
function renderComprehensiveReport(data, isMonthly) {
    const summary = data.summary || {};
    const title = summary.reportTitle || 'Financial Report';
    const period = `Period: ${summary.startDate || '-'} to ${summary.endDate || '-'}`;

    document.getElementById('displayReportTitle').textContent = title;
    document.getElementById('displayReportPeriod').textContent = period;
    document.getElementById('printReportTitle').textContent = title;
    document.getElementById('printReportPeriod').textContent = period;

    // Show all sections
    document.getElementById('summaryCardsRow').classList.remove('d-none');
    document.getElementById('breakdownsRow').classList.remove('d-none');
    document.getElementById('categoryBreakdownCol').classList.remove('d-none');
    document.getElementById('paymentMethodBreakdownCol').classList.remove('d-none');
    document.getElementById('expenseTableCard').classList.remove('d-none');

    // Populate Summary Cards
    document.getElementById('statTotalExpenses').textContent = formatInr(summary.totalExpenses);
    document.getElementById('statExpenseCount').textContent = summary.expenseCount || 0;
    document.getElementById('statAverageExpense').textContent = formatInr(summary.averageExpense);
    document.getElementById('statHighestExpense').textContent = formatInr(summary.highestExpense);
    document.getElementById('statLowestExpense').textContent = formatInr(summary.lowestExpense);

    // Budget Comparison
    const budgetCard = document.getElementById('budgetComparisonCard');
    if (data.budgetComparison) {
        budgetCard.classList.remove('d-none');
        renderBudgetComparison(data.budgetComparison);
    } else {
        budgetCard.classList.add('d-none');
    }

    // Category Breakdown
    renderCategoryTable(data.categoryBreakdown || []);

    // Payment Method Breakdown
    renderPaymentMethodTable(data.paymentMethodBreakdown || []);

    // Expense Details Table
    renderExpensesTable(data.expenses || []);
}

function renderCategoryOnlyView(categories, startDate, endDate) {
    const title = `Category Spending Report (${startDate} to ${endDate})`;
    const period = `Period: ${startDate} to ${endDate}`;
    document.getElementById('displayReportTitle').textContent = title;
    document.getElementById('displayReportPeriod').textContent = period;
    document.getElementById('printReportTitle').textContent = title;
    document.getElementById('printReportPeriod').textContent = period;

    // Calculate total from categories
    let total = 0;
    let count = 0;
    categories.forEach(c => {
        total += parseFloat(c.totalAmount || 0);
        count += parseInt(c.expenseCount || 0);
    });

    document.getElementById('statTotalExpenses').textContent = formatInr(total);
    document.getElementById('statExpenseCount').textContent = count;
    document.getElementById('statAverageExpense').textContent = count > 0 ? formatInr(total / count) : '₹0.00';
    document.getElementById('statHighestExpense').textContent = '-';
    document.getElementById('statLowestExpense').textContent = '-';

    document.getElementById('summaryCardsRow').classList.remove('d-none');
    document.getElementById('budgetComparisonCard').classList.add('d-none');
    document.getElementById('breakdownsRow').classList.remove('d-none');
    document.getElementById('categoryBreakdownCol').classList.remove('d-none');
    document.getElementById('paymentMethodBreakdownCol').classList.add('d-none');
    document.getElementById('expenseTableCard').classList.add('d-none');

    renderCategoryTable(categories);
}

function renderPaymentMethodOnlyView(methods, startDate, endDate) {
    const title = `Payment Method Report (${startDate} to ${endDate})`;
    const period = `Period: ${startDate} to ${endDate}`;
    document.getElementById('displayReportTitle').textContent = title;
    document.getElementById('displayReportPeriod').textContent = period;
    document.getElementById('printReportTitle').textContent = title;
    document.getElementById('printReportPeriod').textContent = period;

    let total = 0;
    let count = 0;
    methods.forEach(m => {
        total += parseFloat(m.totalAmount || 0);
        count += parseInt(m.expenseCount || 0);
    });

    document.getElementById('statTotalExpenses').textContent = formatInr(total);
    document.getElementById('statExpenseCount').textContent = count;
    document.getElementById('statAverageExpense').textContent = count > 0 ? formatInr(total / count) : '₹0.00';
    document.getElementById('statHighestExpense').textContent = '-';
    document.getElementById('statLowestExpense').textContent = '-';

    document.getElementById('summaryCardsRow').classList.remove('d-none');
    document.getElementById('budgetComparisonCard').classList.add('d-none');
    document.getElementById('breakdownsRow').classList.remove('d-none');
    document.getElementById('categoryBreakdownCol').classList.add('d-none');
    document.getElementById('paymentMethodBreakdownCol').classList.remove('d-none');
    document.getElementById('expenseTableCard').classList.add('d-none');

    renderPaymentMethodTable(methods);
}

function renderBudgetVsActualOnlyView(b, month, year) {
    const title = `Budget vs Actual Report - Month ${month}/${year}`;
    const period = `Period: ${year}-${String(month).padStart(2, '0')}`;
    document.getElementById('displayReportTitle').textContent = title;
    document.getElementById('displayReportPeriod').textContent = period;
    document.getElementById('printReportTitle').textContent = title;
    document.getElementById('printReportPeriod').textContent = period;

    document.getElementById('summaryCardsRow').classList.add('d-none');
    document.getElementById('breakdownsRow').classList.add('d-none');
    document.getElementById('expenseTableCard').classList.add('d-none');

    const budgetCard = document.getElementById('budgetComparisonCard');
    budgetCard.classList.remove('d-none');
    renderBudgetComparison(b);
}

function renderBudgetComparison(b) {
    const statusBadge = document.getElementById('budgetStatusBadge');
    const amountVal = document.getElementById('budgetAmountVal');
    const actualVal = document.getElementById('budgetActualVal');
    const remainingVal = document.getElementById('budgetRemainingVal');
    const utilVal = document.getElementById('budgetUtilizationVal');
    const progressBar = document.getElementById('budgetProgressBar');

    if (b.budgetConfigured) {
        statusBadge.textContent = b.status || 'UNDER_BUDGET';
        statusBadge.className = 'badge px-3 py-1 rounded-pill ' + getStatusBadgeClass(b.status);

        amountVal.textContent = formatInr(b.budgetAmount);
        actualVal.textContent = formatInr(b.actualExpense);
        remainingVal.textContent = formatInr(b.remainingAmount);

        const util = parseFloat(b.utilizationPercentage || 0);
        utilVal.textContent = util.toFixed(2) + '%';

        const cappedWidth = Math.min(Math.max(0, util), 100);
        progressBar.style.width = cappedWidth + '%';
        progressBar.className = 'progress-bar ' + getProgressBarClass(b.status);

        if (b.remainingAmount < 0) {
            remainingVal.className = 'fs-5 fw-bold text-danger mt-1';
        } else {
            remainingVal.className = 'fs-5 fw-bold text-success mt-1';
        }
    } else {
        statusBadge.textContent = 'NOT CONFIGURED';
        statusBadge.className = 'badge bg-secondary px-3 py-1 rounded-pill';

        amountVal.textContent = 'Not Set';
        actualVal.textContent = formatInr(b.actualExpense);
        remainingVal.textContent = 'N/A';
        remainingVal.className = 'fs-5 fw-bold text-muted mt-1';
        utilVal.textContent = 'N/A';
        progressBar.style.width = '0%';
    }
}

function getStatusBadgeClass(status) {
    if (status === 'UNDER_BUDGET') return 'bg-success';
    if (status === 'NEAR_LIMIT') return 'bg-warning text-dark';
    if (status === 'OVER_BUDGET') return 'bg-danger';
    return 'bg-secondary';
}

function getProgressBarClass(status) {
    if (status === 'UNDER_BUDGET') return 'bg-success';
    if (status === 'NEAR_LIMIT') return 'bg-warning';
    if (status === 'OVER_BUDGET') return 'bg-danger';
    return 'bg-secondary';
}

function renderCategoryTable(categories) {
    const tbody = document.getElementById('categoryTableBody');
    const countBadge = document.getElementById('categoryCountBadge');
    tbody.innerHTML = '';

    countBadge.textContent = `${categories.length} categories`;

    if (!categories || categories.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">No category data for this period</td></tr>';
        return;
    }

    categories.forEach(cat => {
        const tr = document.createElement('tr');
        const pct = parseFloat(cat.percentage || 0);
        tr.innerHTML = `
            <td class="ps-3 fw-semibold">${escapeHtml(cat.categoryName)}</td>
            <td class="text-center">${cat.expenseCount || 0}</td>
            <td class="text-end fw-bold text-primary">${formatInr(cat.totalAmount)}</td>
            <td class="text-end pe-3">
                <div class="d-flex align-items-center justify-content-end gap-2">
                    <span class="small">${pct.toFixed(2)}%</span>
                    <div class="progress flex-grow-1" style="max-width: 60px; height: 6px;">
                        <div class="progress-bar bg-primary" role="progressbar" style="width: ${Math.min(pct, 100)}%;"></div>
                    </div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function renderPaymentMethodTable(methods) {
    const tbody = document.getElementById('paymentMethodTableBody');
    const countBadge = document.getElementById('paymentCountBadge');
    tbody.innerHTML = '';

    countBadge.textContent = `${methods.length} methods`;

    if (!methods || methods.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">No payment method data for this period</td></tr>';
        return;
    }

    methods.forEach(m => {
        const tr = document.createElement('tr');
        const pct = parseFloat(m.percentage || 0);
        tr.innerHTML = `
            <td class="ps-3 fw-semibold">${escapeHtml(m.paymentMethod)}</td>
            <td class="text-center">${m.expenseCount || 0}</td>
            <td class="text-end fw-bold text-primary">${formatInr(m.totalAmount)}</td>
            <td class="text-end pe-3">
                <div class="d-flex align-items-center justify-content-end gap-2">
                    <span class="small">${pct.toFixed(2)}%</span>
                    <div class="progress flex-grow-1" style="max-width: 60px; height: 6px;">
                        <div class="progress-bar bg-info" role="progressbar" style="width: ${Math.min(pct, 100)}%;"></div>
                    </div>
                </div>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function renderExpensesTable(expenses) {
    const tbody = document.getElementById('expenseReportTableBody');
    const totalBadge = document.getElementById('transactionTotalBadge');
    tbody.innerHTML = '';

    totalBadge.textContent = `${expenses.length} Records`;

    if (!expenses || expenses.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4">No transactions found for this period</td></tr>';
        return;
    }

    expenses.forEach(e => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="ps-3">${escapeHtml(e.date)}</td>
            <td><span class="badge bg-light text-dark border">${escapeHtml(e.category)}</span></td>
            <td>${escapeHtml(e.description || '-')}</td>
            <td><span class="badge bg-primary-subtle text-primary border border-primary px-2">${escapeHtml(e.paymentMethod)}</span></td>
            <td class="text-end pe-3 fw-bold">${formatInr(e.amount)}</td>
        `;
        tbody.appendChild(tr);
    });
}

// Trigger CSV or PDF file export
function exportReport(format) {
    const reportType = document.getElementById('reportTypeSelect').value;
    let query = '';

    if (reportType === 'MONTHLY' || reportType === 'BUDGET_VS_ACTUAL') {
        const month = document.getElementById('monthSelect').value;
        const year = document.getElementById('yearInput').value;
        query = `month=${month}&year=${year}`;
    } else {
        const startDate = document.getElementById('startDateInput').value;
        const endDate = document.getElementById('endDateInput').value;
        if (!startDate || !endDate) {
            showToast('Start date and end date are required to export', true);
            return;
        }
        query = `startDate=${startDate}&endDate=${endDate}`;
    }

    const catId = document.getElementById('categoryFilterSelect').value;
    const pm = document.getElementById('paymentMethodFilterSelect').value;
    if (catId) query += `&categoryId=${catId}`;
    if (pm) query += `&paymentMethod=${pm}`;

    const endpoint = `/api/reports/export/${format}?${query}`;
    window.location.href = endpoint;
}

// Format numbers in Indian Rupee format (INR)
function formatInr(val) {
    if (val === null || val === undefined || isNaN(val)) {
        return '₹0.00';
    }
    const num = parseFloat(val);
    return '₹' + num.toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
}

function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function showToast(message, isError = false) {
    const toastEl = document.getElementById('actionToast');
    const toastMsg = document.getElementById('toastMessage');
    toastEl.className = 'toast align-items-center text-white border-0 ' + (isError ? 'bg-danger' : 'bg-success');
    toastMsg.textContent = message;
    const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
    toast.show();
}
