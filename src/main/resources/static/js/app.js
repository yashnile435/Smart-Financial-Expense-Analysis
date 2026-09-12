/**
 * Project development stages configuration.
 * Easily update the status ('completed' | 'active' | 'upcoming') as development proceeds.
 */
const PROJECT_STAGES = [
    {
        number: 1,
        title: "Stage 1: Project Setup (Java + Spring Boot + Maven + MySQL)",
        status: "completed"
    },
    {
        number: 2,
        title: "Stage 2: Database & Entities",
        status: "completed"
    },
    {
        number: 3,
        title: "Stage 3: Authentication (Registration, Login, Roles)",
        status: "completed"
    },
    {
        number: 4,
        title: "Stage 4: Expense Management (CRUD, Search, Filter)",
        status: "completed"
    },
    {
        number: 5,
        title: "Stage 5: Budget Management (Limits, Utilization, Tracking)",
        status: "active"
    },
    {
        number: "6-10",
        title: "Stages 6-10: Analytics, Dashboard, UI & Testing",
        status: "upcoming"
    }
];

/**
 * Dynamically renders the project stages list based on the configuration array.
 */
function renderProjectStages() {
    const listElement = document.getElementById("projectStagesList");
    if (!listElement) return;

    listElement.innerHTML = PROJECT_STAGES.map(stage => {
        let itemClass = "list-group-item d-flex justify-content-between align-items-center stage-item";
        let iconHtml = "";
        let badgeHtml = "";

        if (stage.status === "completed") {
            itemClass += " stage-completed bg-white";
            iconHtml = '<i class="bi bi-check-circle-fill text-success me-2"></i>';
            badgeHtml = '<span class="badge bg-success">Completed</span>';
        } else if (stage.status === "active") {
            itemClass += " stage-active bg-primary-subtle text-primary-emphasis fw-semibold";
            iconHtml = '<i class="bi bi-record-circle-fill text-primary me-2"></i>';
            badgeHtml = '<span class="badge bg-primary">Active</span>';
        } else {
            itemClass += " stage-upcoming text-muted bg-light-subtle";
            iconHtml = '<i class="bi bi-clock me-2"></i>';
            badgeHtml = '<span class="badge bg-secondary">Upcoming</span>';
        }

        return `<li class="${itemClass}">
            <span>${iconHtml}${stage.title}</span>
            ${badgeHtml}
        </li>`;
    }).join("");
}

// Frontend script to initialize stages and test backend connectivity via /api/health
document.addEventListener("DOMContentLoaded", () => {
    // Render stages from configuration
    renderProjectStages();

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

