package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.KbDocumentUpdateRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.KbDocumentResponse;
import com.anonymous.service.IKnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

// Chatbot knowledge base: documents and who may read them. Admin only.
@RestController
@RequestMapping("/knowledge-documents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class KnowledgeBaseController {

    IKnowledgeBaseService knowledgeBaseService;

    @GetMapping("/show-all")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<List<KbDocumentResponse>> getAll() {
        return ApiResponse.<List<KbDocumentResponse>>builder()
                .result(knowledgeBaseService.getAll())
                .build();
    }

    @PostMapping(value = "/insert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<KbDocumentResponse> insert(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("roleCodes") Set<String> roleCodes
    ) {
        return ApiResponse.<KbDocumentResponse>builder()
                .result(knowledgeBaseService.upload(file, title, roleCodes))
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<KbDocumentResponse> update(@PathVariable("id") String documentId,
                                                  @RequestBody @Valid KbDocumentUpdateRequest request) {
        return ApiResponse.<KbDocumentResponse>builder()
                .result(knowledgeBaseService.update(documentId, request))
                .build();
    }

    @PutMapping(value = "/update/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<KbDocumentResponse> replaceContent(@PathVariable("id") String documentId,
                                                          @RequestPart("file") MultipartFile file) {
        return ApiResponse.<KbDocumentResponse>builder()
                .result(knowledgeBaseService.replaceContent(documentId, file))
                .build();
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<Void> delete(@PathVariable("id") String documentId) {
        knowledgeBaseService.delete(documentId);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/reindex/{id}")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<KbDocumentResponse> reindex(@PathVariable("id") String documentId) {
        return ApiResponse.<KbDocumentResponse>builder()
                .result(knowledgeBaseService.reindex(documentId))
                .build();
    }

    @PostMapping("/reindex-all")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<Void> reindexAll() {
        knowledgeBaseService.reindexAll();
        return ApiResponse.<Void>builder().build();
    }

}
