package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for updating user role (USER / ADMIN).
 */
public class UpdateUserRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;

    public UpdateUserRoleRequest() {
    }

    public UpdateUserRoleRequest(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
