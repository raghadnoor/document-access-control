package com.documentAccessControl.dto;

import java.util.List;

public class AccessCheckResponse {
    private List<Long> accessibleIds;

    public AccessCheckResponse() {
    }

    public AccessCheckResponse(List<Long> accessibleIds) {
        this.accessibleIds = accessibleIds;
    }

    public List<Long> getAccessibleIds() {
        return accessibleIds;
    }

    public void setAccessibleIds(List<Long> accessibleIds) {
        this.accessibleIds = accessibleIds;
    }
}
