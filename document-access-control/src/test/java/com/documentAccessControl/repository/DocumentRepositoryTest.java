package com.documentAccessControl.repository;

import com.documentAccessControl.entity.Document;
import com.documentAccessControl.entity.DocumentPermission;
import com.documentAccessControl.entity.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class DocumentRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DocumentRepository documentRepository;

    private Document testDocument;

    @BeforeEach
    void setUp(){
        testDocument = new Document("Test Document",
                "Document content here...",
                "csv",
                "admin");
    }

    @Test
    void testSaveDocument() {
        Document saved = documentRepository.save(testDocument);
        assertNotNull(saved.getId());
        assertEquals("Test Document", saved.getName());

        Optional<Document> retrieved = documentRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Test Document", retrieved.get().getName());
    }

    @Test
    void testFindDocumentById() {
        Document saved = documentRepository.save(testDocument);
        entityManager.flush();

        Optional<Document> found = documentRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Document", found.get().getName());
    }

    @Test
    void testUpdateDocument() {
        Document saved = documentRepository.save(testDocument);
        entityManager.flush();

        saved.setName("Updated Document");
        Document updated = documentRepository.save(saved);

        Optional<Document> retrieved = documentRepository.findById(updated.getId());
        assertTrue(retrieved.isPresent());
        assertEquals("Updated Document", retrieved.get().getName());
    }

    @Test
    void testDeleteDocument() {
        Document saved = documentRepository.save(testDocument);
        Long id = saved.getId();
        entityManager.flush();

        documentRepository.delete(saved);
        entityManager.flush();

        Optional<Document> retrieved = documentRepository.findById(id);
        assertFalse(retrieved.isPresent());
    }
    @Test
    void testFindAll() {
        Document doc1 = new Document("Doc1", "Content1", "pdf", "admin");
        Document doc2 = new Document("Doc2", "Content2", "txt", "admin");

        documentRepository.save(doc1);
        documentRepository.save(doc2);
        entityManager.flush();

        List<Document> all = documentRepository.findAll();
        assertTrue(all.size() >= 2);
    }

    @Test
    void testDocumentWithPermissions() {
        DocumentPermission permission = new DocumentPermission("user1", Permission.READ);
        testDocument.addPermission(permission);

        Document saved = documentRepository.save(testDocument);
        entityManager.flush();

        Optional<Document> retrieved = documentRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertTrue(retrieved.get().getPermissions().size() > 0);
    }

    @Test
    void testFindDocumentsWithPermission() {
        Document doc = new Document("Shared Doc", "Content", "pdf", "user1");
        DocumentPermission permission = new DocumentPermission("user2", Permission.READ);
        doc.addPermission(permission);

        documentRepository.save(doc);
        entityManager.flush();

        List<Document> found = documentRepository.findDocumentsWithPermission("user2", Permission.READ);
        assertTrue(found.size() > 0);
    }

    @Test
    void testFindAccessibleDocumentIds() {
        Document doc = new Document("Test", "Content", "pdf", "user1");
        DocumentPermission permission = new DocumentPermission("user2", Permission.READ);
        doc.addPermission(permission);

        Document saved = documentRepository.save(doc);
        entityManager.flush();

        List<Long> ids = documentRepository.findAccessibleDocumentIds(
                "user2", Permission.READ, List.of(saved.getId())
        );

        assertTrue(ids.contains(saved.getId()));
    }


}
