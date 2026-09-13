/**
 * Smart Financial Expense Analysis
 * Homepage Script: Handles live backend health check and authentication state.
 */
document.addEventListener("DOMContentLoaded", () => {
    const indicator = document.getElementById("statusIndicator");
    const statusText = document.getElementById("statusText");
    const btnRefresh = document.getElementById("btnRefreshHealth");

    /**
     * Pings /api/health to verify backend connectivity and MySQL database status.
     */
    async function checkBackendHealth() {
        if (!indicator || !statusText) return;

        indicator.className = "status-indicator status-checking";
        statusText.textContent = "Checking backend connection...";
        statusText.className = "fw-semibold small text-secondary";

        try {
            const response = await fetch("/api/health");
            if (response.ok) {
                const data = await response.json();
                indicator.className = "status-indicator status-online";
                statusText.textContent = "Backend is ONLINE (" + data.status + ")";
                statusText.className = "fw-semibold small text-success";
            } else {
                throw new Error("HTTP error! status: " + response.status);
            }
        } catch (error) {
            indicator.className = "status-indicator status-offline";
            statusText.textContent = "Backend is unreachable";
            statusText.className = "fw-semibold small text-danger";
        }
    }

    if (btnRefresh) {
        btnRefresh.addEventListener("click", checkBackendHealth);
    }

    /**
     * Checks current session authentication state for the homepage navbar.
     */
    async function checkAuthState() {
        const unauthNav = document.getElementById("unauthNav");
        const authUserMenu = document.getElementById("authUserMenu");
        const currentUserName = document.getElementById("currentUserName");
        const adminLink = document.getElementById("navAdminLink");
        const btnLogout = document.getElementById("btnLogout");

        function showLoggedOutUI() {
            if (unauthNav) {
                unauthNav.classList.remove("d-none");
                unauthNav.classList.add("d-flex");
            }
            if (authUserMenu) {
                authUserMenu.classList.add("d-none");
                authUserMenu.classList.remove("d-flex");
            }
            if (adminLink) adminLink.classList.add("d-none");
        }

        function showLoggedInUI(user) {
            if (unauthNav) {
                unauthNav.classList.add("d-none");
                unauthNav.classList.remove("d-flex");
            }
            if (authUserMenu) {
                authUserMenu.classList.remove("d-none");
                authUserMenu.classList.add("d-flex");
            }
            if (currentUserName) {
                currentUserName.textContent = user.name || user.email;
            }

            if (user.role === "ADMIN") {
                if (adminLink) adminLink.classList.remove("d-none");
                // Hide personal links from navbar for ADMIN
                if (authUserMenu) {
                    authUserMenu.querySelectorAll("a.app-nav-link:not(#navAdminLink)").forEach(el => el.classList.add("d-none"));
                }
                document.querySelectorAll("a[href='dashboard.html']").forEach(el => {
                    el.href = "admin.html";
                    if (el.textContent.includes("Get Started")) {
                        el.innerHTML = '<i class="bi bi-shield-lock me-1"></i> Admin Console';
                    }
                });
            } else {
                if (adminLink) adminLink.classList.add("d-none");
                if (authUserMenu) {
                    authUserMenu.querySelectorAll("a.app-nav-link:not(#navAdminLink)").forEach(el => el.classList.remove("d-none"));
                }
            }
        }

        try {
            const res = await fetch("/api/auth/me");
            if (res.ok) {
                const user = await res.json();
                showLoggedInUI(user);
            } else {
                showLoggedOutUI();
            }
        } catch (e) {
            showLoggedOutUI();
        }

        if (btnLogout) {
            btnLogout.addEventListener("click", async () => {
                try {
                    await fetch("/api/auth/logout", { method: "POST" });
                } catch (e) {}
                showLoggedOutUI();
                window.location.reload();
            });
        }
    }

    // Initial checks on page load
    checkBackendHealth();
    checkAuthState();
});
