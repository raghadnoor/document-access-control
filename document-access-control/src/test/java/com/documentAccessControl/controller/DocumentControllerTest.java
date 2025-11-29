package com.documentAccessControl.controller;

import com.documentAccessControl.dto.*;
import com.documentAccessControl.entity.Permission;
import com.documentAccessControl.exception.AccessDeniedException;
import com.documentAccessControl.exception.DocumentNotFoundException;
import com.documentAccessControl.service.DocumentService;
import com.documentAccessControl.service.DocumentServiceTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
public class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @Autowired
    private ObjectMapper objectMapper;

    private DocumentDto documentDtoTest;

    private CreateDocumentRequest createDocumentRequestTest;

    @BeforeEach
    void setUp(){
        documentDtoTest = new DocumentDto(1L, "Test Document", "Document content here...", "csv", Arrays.asList());

        createDocumentRequestTest = new CreateDocumentRequest();
        createDocumentRequestTest.setName("Test Document");
        createDocumentRequestTest.setContent("Document content here...");
        createDocumentRequestTest.setFileType("csv");

    }

    @Test
    void testCreateDocumentSuccess() throws Exception {
        when(documentService.createDocument(eq("admin"), any(CreateDocumentRequest.class)))
                .thenReturn(documentDtoTest);

        mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequestTest)))
                .andExpect(status().isCreated());

        verify(documentService, times(1)).createDocument(eq("admin"), any(CreateDocumentRequest.class));
    }

    @Test
    void testCreateDocumentMissingXUserHeader() throws Exception {
        mockMvc.perform(post("/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequestTest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testCreateDocumentInvalidRequest() throws Exception {
        CreateDocumentRequest invalidRequest = new CreateDocumentRequest();
        invalidRequest.setContent("No name");

        mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testCreateDocumentAccessDenied() throws Exception {
        when(documentService.createDocument(eq("user1"), any(CreateDocumentRequest.class)))
                .thenThrow(new AccessDeniedException("Only admin can create documents"));

        mockMvc.perform(post("/documents")
                        .header("X-User", "user1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequestTest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test
    void testGetListOfAccessibleDocumentsSuccess() throws Exception{
        List<DocumentDto> documents = Arrays.asList(documentDtoTest);
        when(documentService.getListOfAccessibleDocuments("admin")).thenReturn(documents);

        mockMvc.perform(get("/documents")
                       .header("X-User", "admin"))
                .andExpect(status().isOk());

        verify(documentService, times(1)).getListOfAccessibleDocuments("admin");
    }

    @Test
    void testGetListOfAccessibleDocumentsMissingHeader() throws Exception{
        mockMvc.perform(get("/documents"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void testGetDocumentByIdSuccess() throws Exception{
        when(documentService.getDocumentById("admin", 1L)).thenReturn(documentDtoTest);

        mockMvc.perform(get("/documents/1")
                .header("X-User","admin"))
                .andExpect(status().isOk());

        verify(documentService, times(1)).getDocumentById("admin",1L);
    }

    @Test
    void testGetDocumentByIdNotFound() throws Exception {
        when(documentService.getDocumentById("admin", 1L))
                .thenThrow(new DocumentNotFoundException("Document not found"));

        mockMvc.perform(get("/documents/1")
                        .header("X-User", "admin"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("Not Found")));
    }

    @Test
    void testGetDocumentByIdAccessDenied() throws Exception {
        when(documentService.getDocumentById("user1", 1L))
                .thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/documents/1")
                        .header("X-User", "user1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteDocumentSuccess() throws Exception {
        doNothing().when(documentService).deleteDocument("admin", 1L);

        mockMvc.perform(delete("/documents/1")
                        .header("X-User", "admin"))
                .andExpect(status().isNoContent());

        verify(documentService, times(1)).deleteDocument("admin", 1L);
    }

    @Test
    void testDeleteDocument_NotFound() throws Exception {
        doThrow(new DocumentNotFoundException("Document not found"))
                .when(documentService).deleteDocument("admin", 1L);

        mockMvc.perform(delete("/documents/1")
                        .header("X-User", "admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteDocument_AccessDenied() throws Exception {
        doThrow(new AccessDeniedException("Access denied"))
                .when(documentService).deleteDocument("user1", 1L);

        mockMvc.perform(delete("/documents/1")
                        .header("X-User", "user1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGrantPermission_Success() throws Exception {
        GrantPermissionRequest grantRequest = new GrantPermissionRequest();
        grantRequest.setUsername("user1");
        grantRequest.setPermission(Permission.READ);

        when(documentService.grantPermission(eq("admin"), eq(1L), any(GrantPermissionRequest.class)))
                .thenReturn(documentDtoTest);

        mockMvc.perform(post("/documents/1/grant")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantRequest)))
                .andExpect(status().isOk());

        verify(documentService, times(1)).grantPermission(eq("admin"), eq(1L), any(GrantPermissionRequest.class));
    }

    @Test
    void testCheckAccess_Success() throws Exception {
        AccessCheckRequest checkRequest = new AccessCheckRequest();
        checkRequest.setDocumentIds(Arrays.asList(1L, 2L));
        checkRequest.setPermission(Permission.READ);

        AccessCheckResponse response = new AccessCheckResponse(Arrays.asList(1L));
        when(documentService.checkAccess(eq("admin"), any(AccessCheckRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/documents/access-check")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkRequest)))
                .andExpect(status().isOk());

        verify(documentService, times(1)).checkAccess(eq("admin"), any(AccessCheckRequest.class));
    }

    @Test
    void testEmptyXUserHeader() throws Exception {
        mockMvc.perform(get("/documents")
                        .header("X-User", "   "))
                .andExpect(status().is4xxClientError());
    }
}
