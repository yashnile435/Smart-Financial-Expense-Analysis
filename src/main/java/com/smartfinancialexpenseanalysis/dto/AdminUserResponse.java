package com.smartfinancialexpenseanalysis.dto;

import com.smartfinancialexpenseanalysis.entity.Role;

/**
 * Administrative user view response.
 * Strictly excludes passwords, password hashes, and security tokens.
 */
public class AdminUserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;

    public AdminUserResponse() {
    }

    public AdminUserResponse(Long id, String name, String email, Role role, boolean enabled) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
