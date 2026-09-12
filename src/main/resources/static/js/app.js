// Frontend script to test backend connectivity via /api/health
document.addEventListener("DOMContentLoaded", () => {
    const indicator = document.getElementById("statusIndicator");
    const statusText = document.getElementById("statusText");
    const healthResponseContainer = document.getElementById("healthResponseContainer");
    const healthResponse = document.getElementById("healthResponse");
    const btnRefresh = document.getElementById("btnRefreshHealth");

    async function checkBackendHealth() {
        indicator.className = "status-indicator status-checking";
        statusText.textContent = "Checking backend connection...";
        statusText.className = "fw-bold fs-5 text-secondary";

        try {
            const response = await fetch("/api/health");
            if (response.ok) {
                const data = await response.json();
                indicator.className = "status-indicator status-online";
                statusText.textContent = "Backend is ONLINE (" + data.status + ")";
                statusText.className = "fw-bold fs-5 text-success";
                healthResponse.textContent = JSON.stringify(data, null, 2);
                healthResponseContainer.classList.remove("d-none");
            } else {
                throw new Error("HTTP error! status: " + response.status);
            }
        } catch (error) {
            indicator.className = "status-indicator status-offline";
            statusText.textContent = "Backend is unreachable";
            statusText.className = "fw-bold fs-5 text-danger";
            healthResponse.textContent = "Could not connect to /api/health:\n" + error.message;
            healthResponseContainer.classList.remove("d-none");
        }
    }

    if (btnRefresh) {
        btnRefresh.addEventListener("click", checkBackendHealth);
    }

    // Initial check on load
    checkBackendHealth();
});
