package com.documentAccessControl.repository;

import com.documentAccessControl.entity.Document;
import com.documentAccessControl.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN d.permissions p " +
            "WHERE d.createdBy = :username OR (p.username = :username AND p.permission = :permission)")
    List<Document> findDocumentsWithPermission(@Param("username") String username, @Param("permission") Permission permission);


    @Query("SELECT d.id FROM Document d LEFT JOIN d.permissions p " +
            "WHERE d.id IN :documentIds AND (d.createdBy = :username OR (p.username = :username AND p.permission = :permission))")
    List<Long> findAccessibleDocumentIds(@Param("username") String username,
                                         @Param("permission") Permission permission,
                                         @Param("documentIds") List<Long> documentIds);

    @Query("SELECT d.id FROM Document d WHERE d.id IN :documentIds")
    List<Long> findExistingDocumentIds(@Param("documentIds") List<Long> documentIds);

}
