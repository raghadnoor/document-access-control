package com.documentAccessControl.controller;

import com.documentAccessControl.dto.*;
import com.documentAccessControl.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private static final String X_USER_HEADER = "X-User";

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    public ResponseEntity<DocumentDto> createDocument(@RequestHeader(X_USER_HEADER) String username, @Valid @RequestBody CreateDocumentRequest request){
        validateUser(username);
        DocumentDto documentDto = documentService.createDocument(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentDto);
    }

    @GetMapping
    public ResponseEntity<List<DocumentDto>> getListOfAccessibleDocuments(@RequestHeader(X_USER_HEADER) String username){
        validateUser(username);
        List<DocumentDto> documentDtos = documentService.getListOfAccessibleDocuments(username);
        return ResponseEntity.ok(documentDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getDocumentById(@RequestHeader(X_USER_HEADER) String username, @PathVariable Long id){
        validateUser(username);
        DocumentDto documentDto = documentService.getDocumentById(username, id);
        return ResponseEntity.ok(documentDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocuemnt(@RequestHeader(X_USER_HEADER) String username, @PathVariable Long id){
        validateUser(username);
        documentService.deleteDocument(username, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/grant")
    public ResponseEntity<DocumentDto> grantPermission(@RequestHeader(X_USER_HEADER) String username, @PathVariable Long id,
                                                        @Valid @RequestBody GrantPermissionRequest request){
        validateUser(username);
        DocumentDto documentDto = documentService.grantPermission(username, id, request);
        return ResponseEntity.ok(documentDto);
    }

    @PostMapping("/access-check")
    public ResponseEntity<AccessCheckResponse> checkAccess(@RequestHeader(X_USER_HEADER) String username, @Valid @RequestBody AccessCheckRequest request){
        validateUser(username);
        AccessCheckResponse response = documentService.checkAccess(username, request);
        return ResponseEntity.ok(response);
    }

    private void validateUser(String username){
        if(username == null || username.trim().isEmpty()){
            throw new IllegalArgumentException("X-User header is required");
        }
    }

}
