package com.documentAccessControl.dto;

import com.documentAccessControl.entity.Permission;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class AccessCheckRequest {
    @NotNull(message = "Permission is required")
    private Permission permission;

    @NotNull(message = "Document IDs are required")
    private List<Long> documentIds;

    public AccessCheckRequest() {
    }

    public AccessCheckRequest(Permission permission, List<Long> documentIds) {
        this.permission = permission;
        this.documentIds = documentIds;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    public List<Long> getDocumentIds() {
        return documentIds;
    }

    public void setDocumentIds(List<Long> documentIds) {
        this.documentIds = documentIds;
    }
}
