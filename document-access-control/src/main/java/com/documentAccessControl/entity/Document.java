package com.documentAccessControl.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_type", columnDefinition = "VARCHAR(255)")
    private String fileType;

    @Column(name = "created_by", nullable = false, columnDefinition = "VARCHAR(255)")
    private String createdBy;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DocumentPermission> permissions = new ArrayList<>();

    public Document() {
    }

    public Document(String name, String content, String fileType, String createdBy) {
        this.name = name;
        this.content = content;
        this.fileType = fileType;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate(){
        if(createdAt == null){
            createdAt = LocalDateTime.now();
        }
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<DocumentPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<DocumentPermission> permissions) {
        this.permissions = permissions;
    }

    public void addPermission(DocumentPermission permission){
        permissions.add(permission);
        permission.setDocument(this);
    }

    public void removePermission(DocumentPermission permission) {
        permissions.remove(permission);
        permission.setDocument(null);
    }
}
