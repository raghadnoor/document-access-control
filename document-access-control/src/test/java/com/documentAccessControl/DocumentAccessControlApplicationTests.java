package com.documentAccessControl;

import com.documentAccessControl.dto.*;
import com.documentAccessControl.entity.Permission;
import com.documentAccessControl.repository.DocumentRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DocumentAccessControlApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private DocumentRepository documentRepository;

	private String getBaseUrl() {
		return "http://localhost:" + port + "/documents";
	}

	private HttpHeaders createHeaders(String user) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-User", user);
		headers.setContentType(MediaType.APPLICATION_JSON);
		return headers;
	}

	@BeforeEach
	void setUp() {
		documentRepository.deleteAll();
	}

	@Test
	@Order(1)
	void testCreateDocumentAsAdminShouldSucceed() {
		CreateDocumentRequest request = new CreateDocumentRequest();
		request.setName("Test Document");
		request.setContent("Document content here...");
		request.setFileType("csv");

		HttpEntity<CreateDocumentRequest> entity = new HttpEntity<>(request, createHeaders("admin"));
		ResponseEntity<DocumentDto> response = restTemplate.postForEntity(getBaseUrl(), entity, DocumentDto.class);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("Test Document", response.getBody().getName());
	}

	@Test
	@Order(2)
	void testCreateDocumentAsNonAdminShouldFail() {
		CreateDocumentRequest request = new CreateDocumentRequest();
		request.setName("Test Document");
		request.setContent("Content");
		request.setFileType("txt");

		HttpEntity<CreateDocumentRequest> entity = new HttpEntity<>(request, createHeaders("user1"));
		ResponseEntity<String> response = restTemplate.postForEntity(getBaseUrl(), entity, String.class);

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@Test
	@Order(3)
	void testGetDocumentsWithReadPermissionShouldReturnDocuments() {
		CreateDocumentRequest createRequest = new CreateDocumentRequest();
		createRequest.setName("Shared Document");
		createRequest.setContent("Shared content");
		createRequest.setFileType("pdf");
		createRequest.setAccessibleUsers(Arrays.asList(
				new UserPermissionDto("user1", Permission.READ)
		));

		HttpEntity<CreateDocumentRequest> createEntity = new HttpEntity<>(createRequest, createHeaders("admin"));
		restTemplate.postForEntity(getBaseUrl(), createEntity, DocumentDto.class);

		HttpEntity<?> getEntity = new HttpEntity<>(createHeaders("user1"));
		ResponseEntity<DocumentDto[]> response = restTemplate.exchange(
				getBaseUrl(), HttpMethod.GET, getEntity, DocumentDto[].class);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(1, response.getBody().length);
	}

	@Test
	@Order(4)
	void testGetDocumentByIdWithoutPermissionShouldFail() {
		CreateDocumentRequest createRequest = new CreateDocumentRequest();
		createRequest.setName("Private Document");
		createRequest.setContent("Private content");
		createRequest.setFileType("doc");

		HttpEntity<CreateDocumentRequest> createEntity = new HttpEntity<>(createRequest, createHeaders("admin"));
		ResponseEntity<DocumentDto> createResponse = restTemplate.postForEntity(getBaseUrl(), createEntity, DocumentDto.class);
		Long documentId = createResponse.getBody().getId();

		HttpEntity<?> getEntity = new HttpEntity<>(createHeaders("user2"));
		ResponseEntity<String> response = restTemplate.exchange(
				getBaseUrl() + "/" + documentId, HttpMethod.GET, getEntity, String.class);

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
	}

	@Test
	@Order(5)
	void testGrantPermissionAsAdminShouldSucceed() {
		CreateDocumentRequest createRequest = new CreateDocumentRequest();
		createRequest.setName("Grant Test Document");
		createRequest.setContent("Content for grant test");
		createRequest.setFileType("txt");

		HttpEntity<CreateDocumentRequest> createEntity = new HttpEntity<>(createRequest, createHeaders("admin"));
		ResponseEntity<DocumentDto> createResponse = restTemplate.postForEntity(getBaseUrl(), createEntity, DocumentDto.class);
		Long documentId = createResponse.getBody().getId();

		GrantPermissionRequest grantRequest = new GrantPermissionRequest("user3", Permission.READ);
		HttpEntity<GrantPermissionRequest> grantEntity = new HttpEntity<>(grantRequest, createHeaders("admin"));
		ResponseEntity<DocumentDto> grantResponse = restTemplate.postForEntity(
				getBaseUrl() + "/" + documentId + "/grant", grantEntity, DocumentDto.class);

		assertEquals(HttpStatus.OK, grantResponse.getStatusCode());
		assertTrue(grantResponse.getBody().getAccessibleUsers().stream()
				.anyMatch(u -> u.getUsername().equals("user3") && u.getPermission() == Permission.READ));
	}

	@Test
	@Order(6)
	void testDeleteDocumentWithDeletePermissionShouldSucceed() {
		CreateDocumentRequest createRequest = new CreateDocumentRequest();
		createRequest.setName("Document to Delete");
		createRequest.setContent("Will be deleted");
		createRequest.setFileType("tmp");
		createRequest.setAccessibleUsers(Arrays.asList(
				new UserPermissionDto("user4", Permission.DELETE)
		));

		HttpEntity<CreateDocumentRequest> createEntity = new HttpEntity<>(createRequest, createHeaders("admin"));
		ResponseEntity<DocumentDto> createResponse = restTemplate.postForEntity(getBaseUrl(), createEntity, DocumentDto.class);
		Long documentId = createResponse.getBody().getId();

		HttpEntity<?> deleteEntity = new HttpEntity<>(createHeaders("user4"));
		ResponseEntity<Void> deleteResponse = restTemplate.exchange(
				getBaseUrl() + "/" + documentId, HttpMethod.DELETE, deleteEntity, Void.class);

		assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
	}

	@Test
	@Order(7)
	void testAccessCheckShouldReturnAccessibleIds() {
		CreateDocumentRequest doc1 = new CreateDocumentRequest();
		doc1.setName("Doc 1");
		doc1.setContent("Content 1");
		doc1.setFileType("txt");
		doc1.setAccessibleUsers(Arrays.asList(new UserPermissionDto("user5", Permission.READ)));

		CreateDocumentRequest doc2 = new CreateDocumentRequest();
		doc2.setName("Doc 2");
		doc2.setContent("Content 2");
		doc2.setFileType("txt");

		HttpEntity<CreateDocumentRequest> entity1 = new HttpEntity<>(doc1, createHeaders("admin"));
		HttpEntity<CreateDocumentRequest> entity2 = new HttpEntity<>(doc2, createHeaders("admin"));

		ResponseEntity<DocumentDto> response1 = restTemplate.postForEntity(getBaseUrl(), entity1, DocumentDto.class);
		ResponseEntity<DocumentDto> response2 = restTemplate.postForEntity(getBaseUrl(), entity2, DocumentDto.class);

		Long id1 = response1.getBody().getId();
		Long id2 = response2.getBody().getId();

		AccessCheckRequest checkRequest = new AccessCheckRequest();
		checkRequest.setPermission(Permission.READ);
		checkRequest.setDocumentIds(Arrays.asList(id1, id2));

		HttpEntity<AccessCheckRequest> checkEntity = new HttpEntity<>(checkRequest, createHeaders("user5"));
		ResponseEntity<AccessCheckResponse> checkResponse = restTemplate.postForEntity(
				getBaseUrl() + "/access-check", checkEntity, AccessCheckResponse.class);

		assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
		assertEquals(1, checkResponse.getBody().getAccessibleIds().size());
		assertTrue(checkResponse.getBody().getAccessibleIds().contains(id1));
	}

	@Test
	@Order(8)
	void testAccessCheckAsAdminShouldReturnAllIds() {
		CreateDocumentRequest doc1 = new CreateDocumentRequest();
		doc1.setName("Admin Check Doc 1");
		doc1.setContent("Content 1");
		doc1.setFileType("txt");

		CreateDocumentRequest doc2 = new CreateDocumentRequest();
		doc2.setName("Admin Check Doc 2");
		doc2.setContent("Content 2");
		doc2.setFileType("txt");

		HttpEntity<CreateDocumentRequest> entity1 = new HttpEntity<>(doc1, createHeaders("admin"));
		HttpEntity<CreateDocumentRequest> entity2 = new HttpEntity<>(doc2, createHeaders("admin"));

		ResponseEntity<DocumentDto> response1 = restTemplate.postForEntity(getBaseUrl(), entity1, DocumentDto.class);
		ResponseEntity<DocumentDto> response2 = restTemplate.postForEntity(getBaseUrl(), entity2, DocumentDto.class);

		Long id1 = response1.getBody().getId();
		Long id2 = response2.getBody().getId();

		AccessCheckRequest checkRequest = new AccessCheckRequest();
		checkRequest.setPermission(Permission.READ);
		checkRequest.setDocumentIds(Arrays.asList(id1, id2));

		HttpEntity<AccessCheckRequest> checkEntity = new HttpEntity<>(checkRequest, createHeaders("admin"));
		ResponseEntity<AccessCheckResponse> checkResponse = restTemplate.postForEntity(
				getBaseUrl() + "/access-check", checkEntity, AccessCheckResponse.class);

		assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
		assertEquals(2, checkResponse.getBody().getAccessibleIds().size());
	}
}

