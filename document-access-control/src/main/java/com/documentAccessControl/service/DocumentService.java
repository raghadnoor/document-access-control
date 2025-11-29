package com.documentAccessControl.service;

import com.documentAccessControl.dto.*;
import com.documentAccessControl.entity.Document;
import com.documentAccessControl.entity.DocumentPermission;
import com.documentAccessControl.entity.Permission;
import com.documentAccessControl.exception.AccessDeniedException;
import com.documentAccessControl.exception.DocumentNotFoundException;
import com.documentAccessControl.repository.DocumentRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentService {
    private static final String ADMIN_USER = "admin";

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public DocumentDto createDocument(String username, CreateDocumentRequest request){
        if(!ADMIN_USER.equalsIgnoreCase(username)){
            throw new AccessDeniedException("Only Admin user can create documents");
        }

        Document document = new Document(request.getName(), request.getContent(), request.getFileType(), username);

        if(request.getAccessibleUsers() != null){
            for (UserPermissionDto userPermissionDto : request.getAccessibleUsers()){
                DocumentPermission documentPermission = new DocumentPermission(userPermissionDto.getUsername(), userPermissionDto.getPermission());
                document.addPermission(documentPermission);
            }
        }

        Document savedDocument = documentRepository.save(document);
        return documentBeanToDto(savedDocument);
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> getListOfAccessibleDocuments(String username){
        List<Document> documents;

        System.out.println("Username --> "+username);

        if(ADMIN_USER.equalsIgnoreCase(username)){
            documents = documentRepository.findAll();
        } else {
            documents = documentRepository.findDocumentsWithPermission(username, Permission.READ);
        }

        return documents.stream()
                .map(this::documentBeanToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentDto getDocumentById(String username, Long id){
        System.out.println("username -> "+username + " **** with document id -> "+id);
        Document document = documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if(!hasPermission(username, document, Permission.READ)){
            throw new AccessDeniedException("You don't have READ permission for this document");
        }

        return documentBeanToDto(document);
    }

    public void deleteDocument(String username, Long id){
        System.out.println("username -> "+username + " **** with document id -> "+id);
        Document document = documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if(!hasPermission(username, document, Permission.DELETE)){
            throw new AccessDeniedException("You don't have DELETE permission for this document");
        }

        documentRepository.delete(document);
    }

    public DocumentDto grantPermission(String username, Long id, GrantPermissionRequest request) {
        Document document = documentRepository.findById(id).orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        if (!canGrantPermission(username, document)) {
            throw new AccessDeniedException("You don't have a permission to grant access to this document");
        }

        boolean permissionExists = document.getPermissions().stream()
                .anyMatch(p -> p.getUsername().equals(request.getUsername())
                        && p.getPermission() == request.getPermission());

        if (!permissionExists) {
            DocumentPermission newPermission = new DocumentPermission(
                    request.getUsername(),
                    request.getPermission()
            );
            document.addPermission(newPermission);
            documentRepository.save(document);
        }

        return documentBeanToDto(document);

    }

    @Transactional(readOnly = true)
    public AccessCheckResponse checkAccess(String username, AccessCheckRequest request){
        List<Long> accessibleIds;

        if (ADMIN_USER.equals(username)) {
            accessibleIds = documentRepository.findExistingDocumentIds(request.getDocumentIds());
        } else {
            accessibleIds = documentRepository.findAccessibleDocumentIds(username, request.getPermission(), request.getDocumentIds());
        }
        return new AccessCheckResponse(accessibleIds);

    }

    private boolean hasPermission(String username, Document document, Permission permission){
        if(ADMIN_USER.equalsIgnoreCase(username)){
            return true;
        }

        if(document.getCreatedBy().equals(username)){
            return true;
        }

        return document.getPermissions().stream()
                .anyMatch(p -> p.getUsername().equals(username)
                 && p.getPermission() == permission);
    }

    boolean canGrantPermission (String username, Document document){
        if(ADMIN_USER.equalsIgnoreCase(username)){
            return true;
        }

        if(document.getCreatedBy().equals(username)){
            return true;
        }

        return document.getPermissions().stream()
                .anyMatch(p -> p.getUsername().equals(username) && p.getPermission().equals(Permission.WRITE));



    }

    private DocumentDto documentBeanToDto(Document document){
        DocumentDto documentDto = new DocumentDto();
        List<UserPermissionDto> userPermissionDtoList = document.getPermissions().stream()
                .map(p -> new UserPermissionDto(p.getUsername(), p.getPermission()))
                .collect(Collectors.toList());

        documentDto.setId(document.getId());
        documentDto.setName(document.getName());
        documentDto.setContent(document.getContent());
        documentDto.setFileType(document.getFileType());
        documentDto.setAccessibleUsers(userPermissionDtoList);

        return documentDto;

    }
}
