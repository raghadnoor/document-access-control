package com.documentAccessControl.dto;

import java.util.List;

public class DocumentDto {
    private Long id;
    private String name;
    private String content;
    private String fileType;
    private List<UserPermissionDto> accessibleUsers;

    public DocumentDto() {
    }

    public DocumentDto(Long id) {
        this.id = id;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public List<UserPermissionDto> getAccessibleUsers() {
        return accessibleUsers;
    }

    public void setAccessibleUsers(List<UserPermissionDto> accessibleUsers) {
        this.accessibleUsers = accessibleUsers;
    }
}
