package com.documentAccessControl.integration;

import com.documentAccessControl.dto.*;
import com.documentAccessControl.entity.Permission;
import com.documentAccessControl.repository.DocumentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DocumentAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    private CreateDocumentRequest createDocumentRequest;

    @BeforeEach
    void setUp(){
        documentRepository.deleteAll();

        createDocumentRequest = new CreateDocumentRequest();
        createDocumentRequest.setName("Test Document");
        createDocumentRequest.setContent("Document content here...");
        createDocumentRequest.setFileType("csv");
    }

    @Test
    void testCreateAndRetrieveDocument() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Document")))
                .andReturn();

        DocumentDto createdDoc = objectMapper.readValue(createResult.getResponse().getContentAsString(), DocumentDto.class);

        mockMvc.perform(get("/documents/" + createdDoc.getId())
                        .header("X-User", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(createdDoc.getId().intValue())))
                .andExpect(jsonPath("$.name", is("Test Document")));
    }

    @Test
    void testCreateDocumentWithPermissions() throws Exception {
        UserPermissionDto userPerm = new UserPermissionDto("user1", Permission.READ);
        createDocumentRequest.setAccessibleUsers(Arrays.asList(userPerm));

        MvcResult result = mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentDto created = objectMapper.readValue(result.getResponse().getContentAsString(), DocumentDto.class);

        assertNotNull(created.getId());
        assertEquals(1, created.getAccessibleUsers().size());
    }

    @Test
    void testGrantPermissionFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentDto created = objectMapper.readValue(createResult.getResponse().getContentAsString(), DocumentDto.class);

        GrantPermissionRequest grantRequest = new GrantPermissionRequest();
        grantRequest.setUsername("user2");
        grantRequest.setPermission(Permission.WRITE);

        mockMvc.perform(post("/documents/" + created.getId() + "/grant")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessibleUsers", hasSize(1)));
    }

    @Test
    void testListAccessibleDocuments() throws Exception {
        mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/documents")
                        .header("X-User", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testDeleteDocument() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentDto created = objectMapper.readValue(createResult.getResponse().getContentAsString(), DocumentDto.class);

        mockMvc.perform(delete("/documents/" + created.getId())
                        .header("X-User", "admin"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/documents/" + created.getId())
                        .header("X-User", "admin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testBatchAccessCheck() throws Exception {
        MvcResult result1 = mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentDto doc1 = objectMapper.readValue(result1.getResponse().getContentAsString(), DocumentDto.class);

        MvcResult result2 = mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentDto doc2 = objectMapper.readValue(result2.getResponse().getContentAsString(), DocumentDto.class);

        AccessCheckRequest checkRequest = new AccessCheckRequest();
        checkRequest.setDocumentIds(Arrays.asList(doc1.getId(), doc2.getId()));
        checkRequest.setPermission(Permission.READ);

        mockMvc.perform(post("/documents/access-check")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessibleIds", hasSize(2)));
    }

    @Test
    void testAccessControlFlow() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/documents")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDocumentRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentDto created = objectMapper.readValue(createResult.getResponse().getContentAsString(), DocumentDto.class);

        mockMvc.perform(get("/documents/" + created.getId())
                        .header("X-User", "user1"))
                .andExpect(status().isForbidden());

        GrantPermissionRequest grantRequest = new GrantPermissionRequest();
        grantRequest.setUsername("user1");
        grantRequest.setPermission(Permission.READ);

        mockMvc.perform(post("/documents/" + created.getId() + "/grant")
                        .header("X-User", "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(grantRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/documents/" + created.getId())
                        .header("X-User", "user1"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/documents/" + created.getId())
                        .header("X-User", "user1"))
                .andExpect(status().isForbidden());
    }

}
