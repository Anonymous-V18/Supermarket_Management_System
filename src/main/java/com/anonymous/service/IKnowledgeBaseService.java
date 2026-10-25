package com.anonymous.service;

import com.anonymous.dto.request.KbDocumentUpdateRequest;
import com.anonymous.dto.response.KbDocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

public interface IKnowledgeBaseService {

    List<KbDocumentResponse> getAll();

    KbDocumentResponse upload(MultipartFile file, String title, Set<String> roleCodes);

    // New file for an existing document; unchanged passages keep their vectors (no re-embedding)
    KbDocumentResponse replaceContent(String documentId, MultipartFile file);

    KbDocumentResponse update(String documentId, KbDocumentUpdateRequest request);

    void delete(String documentId);

    KbDocumentResponse reindex(String documentId);

    // Rebuilds every document's vectors (if Qdrant lost its data or needs re-sync)
    void reindexAll();

    // Synchronizes relational database records (employees, warehouses) into virtual RAG documents
    void syncVirtualDocuments();

}
