package com.documentAccessControl.dto;

import com.documentAccessControl.entity.Permission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class GrantPermissionRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotNull(message = "Permission is required")
    private Permission permission;

    public GrantPermissionRequest() {
    }

    public GrantPermissionRequest(String username, Permission permission) {
        this.username = username;
        this.permission = permission;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}
