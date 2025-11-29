package com.documentAccessControl.service;

import com.documentAccessControl.dto.*;
import com.documentAccessControl.entity.Document;
import com.documentAccessControl.entity.Permission;
import com.documentAccessControl.exception.AccessDeniedException;
import com.documentAccessControl.exception.DocumentNotFoundException;
import com.documentAccessControl.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {
    @Mock
    private DocumentRepository documentRepository;

    @InjectMocks
    private DocumentService documentService;

    private Document testDocument;

    private CreateDocumentRequest createRequest;

    @BeforeEach
    void setUp() {
        testDocument = new Document("Test Doc", "Content", "pdf", "admin");
        testDocument.setId(1L);
        testDocument.setCreatedAt(LocalDateTime.now());

        createRequest = new CreateDocumentRequest();
        createRequest.setName("Test Document");
        createRequest.setContent("Test Content");
        createRequest.setFileType("pdf");
    }

    @Test
    void testCreateDocumentSuccess() {
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        DocumentDto result = documentService.createDocument("admin", createRequest);

        assertNotNull(result);
        assertEquals("Test Doc", result.getName());
        assertEquals("Content", result.getContent());
        verify(documentRepository, times(1)).save(any(Document.class));
    }


    @Test
    void testCreateDocumentNonAdminUser() {
        assertThrows(AccessDeniedException.class, () ->
                documentService.createDocument("user1", createRequest)
        );
        verify(documentRepository, never()).save(any());
    }

    @Test
    void testCreateDocumentWithAccessibleUsers() {
        UserPermissionDto userPerm = new UserPermissionDto("user1", Permission.READ);
        createRequest.setAccessibleUsers(Arrays.asList(userPerm));

        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        DocumentDto result = documentService.createDocument("admin", createRequest);

        assertNotNull(result);
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void testGetListOfAccessibleDocumentsAdminUser() {
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findAll()).thenReturn(documents);

        List<DocumentDto> result = documentService.getListOfAccessibleDocuments("admin");

        assertEquals(1, result.size());
        verify(documentRepository, times(1)).findAll();
    }

    @Test
    void testGetListOfAccessibleDocumentsRegularUser() {
        List<Document> documents = Arrays.asList(testDocument);
        when(documentRepository.findDocumentsWithPermission("user1", Permission.READ))
                .thenReturn(documents);

        List<DocumentDto> result = documentService.getListOfAccessibleDocuments("user1");

        assertEquals(1, result.size());
        verify(documentRepository, times(1)).findDocumentsWithPermission("user1", Permission.READ);
    }

    @Test
    void testGetDocumentByIdSuccess() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        DocumentDto result = documentService.getDocumentById("admin", 1L);

        assertNotNull(result);
        assertEquals("Test Doc", result.getName());
        verify(documentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetDocumentByIdNotFound() {
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () ->
                documentService.getDocumentById("admin", 1L)
        );
    }

    @Test
    void testGetDocumentByIdAccessDenied() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        assertThrows(AccessDeniedException.class, () ->
                documentService.getDocumentById("user1", 1L)
        );
    }

    @Test
    void testDeleteDocumentSuccess() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        documentService.deleteDocument("admin", 1L);

        verify(documentRepository, times(1)).delete(testDocument);
    }

    @Test
    void testDeleteDocumentNotFound() {
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () ->
                documentService.deleteDocument("admin", 1L)
        );
    }

    @Test
    void testDeleteDocumentAccessDenied() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        assertThrows(AccessDeniedException.class, () ->
                documentService.deleteDocument("user1", 1L)
        );
    }

    @Test
    void testGrantPermissionSuccess() {
        GrantPermissionRequest request = new GrantPermissionRequest();
        request.setUsername("user1");
        request.setPermission(Permission.READ);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        DocumentDto result = documentService.grantPermission("admin", 1L, request);

        assertNotNull(result);
        verify(documentRepository, times(1)).save(any(Document.class));
    }

    @Test
    void testGrantPermissionDocumentNotFound() {
        GrantPermissionRequest request = new GrantPermissionRequest();
        request.setUsername("user1");
        request.setPermission(Permission.READ);

        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () ->
                documentService.grantPermission("admin", 1L, request)
        );
    }

    @Test
    void testGrantPermissionAccessDenied() {
        GrantPermissionRequest request = new GrantPermissionRequest();
        request.setUsername("user1");
        request.setPermission(Permission.READ);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        assertThrows(AccessDeniedException.class, () ->
                documentService.grantPermission("user2", 1L, request)
        );
    }

    @Test
    void testCheckAccessAdminUser() {
        AccessCheckRequest request = new AccessCheckRequest();
        request.setDocumentIds(Arrays.asList(1L, 2L));
        request.setPermission(Permission.READ);

        when(documentRepository.findExistingDocumentIds(Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(1L, 2L));

        AccessCheckResponse response = documentService.checkAccess("admin", request);

        assertEquals(2, response.getAccessibleIds().size());
        verify(documentRepository, times(1)).findExistingDocumentIds(Arrays.asList(1L, 2L));
    }

    @Test
    void testCheckAccessRegularUser() {
        AccessCheckRequest request = new AccessCheckRequest();
        request.setDocumentIds(Arrays.asList(1L, 2L));
        request.setPermission(Permission.READ);

        when(documentRepository.findAccessibleDocumentIds("user1", Permission.READ, Arrays.asList(1L, 2L)))
                .thenReturn(Arrays.asList(1L));

        AccessCheckResponse response = documentService.checkAccess("user1", request);

        assertEquals(1, response.getAccessibleIds().size());
        verify(documentRepository, times(1)).findAccessibleDocumentIds("user1", Permission.READ, Arrays.asList(1L, 2L));
    }
}
