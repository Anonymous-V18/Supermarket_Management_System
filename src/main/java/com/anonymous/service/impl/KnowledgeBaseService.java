package com.anonymous.service.impl;

import com.anonymous.dto.request.KbDocumentUpdateRequest;
import com.anonymous.dto.response.KbDocumentResponse;
import com.anonymous.entity.KbChunk;
import com.anonymous.entity.KbDocument;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IKbChunkRepository;
import com.anonymous.repository.IKbDocumentRepository;
import com.anonymous.repository.IRoleRepository;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Warehouse;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IWarehouseRepository;
import com.anonymous.service.IKnowledgeBaseService;
import com.anonymous.service.chatbot.DocumentChunker;
import com.anonymous.service.chatbot.DocumentTextExtractor;
import com.anonymous.service.chatbot.KnowledgeIndexer;
import com.anonymous.service.chatbot.KnowledgeVersion;
import com.anonymous.service.chatbot.VectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

// Admin side of the chatbot knowledge base. MySQL keeps the documents, their allowed roles and the passage
// texts; Qdrant only gets vectors + ids. Writes go to MySQL first (one transaction), then to Qdrant;
// a Qdrant failure leaves the document FAILED (retried at start-up / from the UI), never half-saved.
@Slf4j
@Service
public class KnowledgeBaseService implements IKnowledgeBaseService {

    // Seed: the functional spec, one knowledge-base document per chapter, readable by the roles that do that work
    private static final String SEED_RESOURCE = "kb/seed/dac-ta-chuc-nang.html";
    private static final Set<String> ALL_STAFF = Set.of("ADMIN", "SALESMAN", "STOREKEEPER", "ACCOUNTING_STAFF", "EMPLOYEE");
    private static final Set<String> SALES = Set.of("ADMIN", "SALESMAN", "ACCOUNTING_STAFF");
    private static final Set<String> STOCK = Set.of("ADMIN", "STOREKEEPER", "ACCOUNTING_STAFF");
    private static final Map<String, Set<String>> SEED_ROLES = Map.ofEntries(
            Map.entry("Luồng quản lý nhân sự", Set.of("ADMIN")),
            Map.entry("Luồng quản lý nhà cung cấp", STOCK),
            Map.entry("Luồng nhập kho", STOCK),
            Map.entry("Luồng xuất kho", STOCK),
            Map.entry("Luồng chuyển kho", STOCK),
            Map.entry("Luồng kiểm kê kho", STOCK),
            Map.entry("Luồng bán hàng tại quầy", SALES),
            Map.entry("Luồng trả hàng", SALES),
            Map.entry("Luồng xem báo cáo", Set.of("ADMIN", "ACCOUNTING_STAFF", "STOREKEEPER")),
            Map.entry("Luồng tra cứu nhật ký hệ thống", Set.of("ADMIN")),
            Map.entry("Luồng quản lý checklist ca", Set.of("ADMIN")));

    private final IKbDocumentRepository documentRepository;
    private final IKbChunkRepository chunkRepository;
    private final IRoleRepository roleRepository;
    private final DocumentTextExtractor extractor;
    private final DocumentChunker chunker;
    private final KnowledgeIndexer indexer;
    private final VectorStore vectorStore;
    private final KnowledgeVersion knowledgeVersion;
    private final TransactionTemplate transaction;
    private final IEmployeeRepository employeeRepository;
    private final IWarehouseRepository warehouseRepository;

    public KnowledgeBaseService(IKbDocumentRepository documentRepository, IKbChunkRepository chunkRepository,
                                IRoleRepository roleRepository, DocumentTextExtractor extractor, DocumentChunker chunker,
                                KnowledgeIndexer indexer, VectorStore vectorStore, KnowledgeVersion knowledgeVersion,
                                PlatformTransactionManager transactionManager,
                                IEmployeeRepository employeeRepository,
                                IWarehouseRepository warehouseRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.roleRepository = roleRepository;
        this.extractor = extractor;
        this.chunker = chunker;
        this.indexer = indexer;
        this.vectorStore = vectorStore;
        this.knowledgeVersion = knowledgeVersion;
        this.transaction = new TransactionTemplate(transactionManager);
        this.employeeRepository = employeeRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Override
    public List<KbDocumentResponse> getAll() {
        return documentRepository.findAllByIsDeletedFalseOrderByCreatedDateDesc().stream().map(KnowledgeBaseService::toDTO).toList();
    }

    @Override
    public KbDocumentResponse upload(MultipartFile file, String title, Set<String> roleCodes) {
        checkRoles(roleCodes);
        DocumentTextExtractor.Extracted extracted = extractor.extract(file.getOriginalFilename(), bytesOf(file));
        String documentTitle = title != null && !title.isBlank() ? title.strip() : baseName(file.getOriginalFilename());
        return toDTO(create(documentTitle, extracted, file.getOriginalFilename(), roleCodes));
    }

    @Override
    public KbDocumentResponse replaceContent(String documentId, MultipartFile file) {
        KbDocument document = activeDocument(documentId);
        DocumentTextExtractor.Extracted extracted = extractor.extract(file.getOriginalFilename(), bytesOf(file));
        Map<String, Deque<String>> oldIdsByHash = new HashMap<>();
        for (KbChunk chunk : chunkRepository.findAllByDocumentIdOrderByChunkIndexAsc(documentId)) {
            oldIdsByHash.computeIfAbsent(chunk.getContentHash(), hash -> new ArrayDeque<>()).add(chunk.getId());
        }
        List<KbChunk> chunks = buildChunks(documentId, document.getTitle(), extracted.text());
        // An unchanged passage keeps its id, hence its vector in Qdrant: only new passages are embedded
        Set<String> reused = new HashSet<>();
        for (KbChunk chunk : chunks) {
            Deque<String> candidates = oldIdsByHash.get(chunk.getContentHash());
            if (candidates != null && !candidates.isEmpty()) {
                chunk.setId(candidates.poll());
                reused.add(chunk.getId());
            }
        }
        List<String> removed = oldIdsByHash.values().stream().flatMap(Collection::stream).toList();
        transaction.executeWithoutResult(status -> {
            chunkRepository.deleteAllByDocumentId(documentId);
            chunkRepository.flush();
            chunkRepository.saveAll(chunks);
            document.setSourceType(extracted.sourceType());
            document.setSourceName(file.getOriginalFilename());
            document.setContentHash(sha256(extracted.text()));
            document.setChunkCount(chunks.size());
            document.setIndexStatus(KbDocument.STATUS_PENDING);
            documentRepository.save(document);
        });
        try {
            vectorStore.deleteChunks(removed);
            indexer.index(document, chunks.stream().filter(chunk -> !reused.contains(chunk.getId())).toList());
        } catch (RuntimeException e) {
            // Partial state in Qdrant: rebuild the whole document on the next retry
            indexer.reindex(document);
        }
        knowledgeVersion.bump();
        return toDTO(document);
    }

    @Override
    public KbDocumentResponse update(String documentId, KbDocumentUpdateRequest request) {
        checkRoles(request.getRoleCodes());
        KbDocument document = activeDocument(documentId);
        boolean rolesChanged = !document.getRoleCodes().equals(request.getRoleCodes());
        document.setTitle(request.getTitle().strip());
        document.setRoleCodes(new HashSet<>(request.getRoleCodes()));
        documentRepository.save(document);
        if (rolesChanged) {
            try {
                // Payload-only update in Qdrant, no re-embedding
                vectorStore.updateRoles(documentId, document.getRoleCodes());
            } catch (RuntimeException e) {
                // MySQL already holds the new roles and the retriever re-checks them, so access is still right
                log.warn("Could not update roles of {} in Qdrant: {}", documentId, e.getMessage());
                document.setIndexStatus(KbDocument.STATUS_FAILED);
                document.setIndexError(abbreviate(e.getMessage()));
                documentRepository.save(document);
            }
        }
        knowledgeVersion.bump();
        return toDTO(document);
    }

    @Override
    public void delete(String documentId) {
        KbDocument document = activeDocument(documentId);
        try {
            vectorStore.deleteDocument(documentId);
        } catch (RuntimeException e) {
            // Leftover points are harmless: the retriever drops passages of deleted documents
            log.warn("Could not delete vectors of {}: {}", documentId, e.getMessage());
        }
        transaction.executeWithoutResult(status -> {
            chunkRepository.deleteAllByDocumentId(documentId);
            document.markDeleted();
            documentRepository.save(document);
        });
        knowledgeVersion.bump();
    }

    @Override
    public KbDocumentResponse reindex(String documentId) {
        KbDocument document = activeDocument(documentId);
        indexer.reindex(document);
        knowledgeVersion.bump();
        return toDTO(document);
    }

    @Override
    public void reindexAll() {
        documentRepository.findAllByIsDeletedFalse().forEach(indexer::reindex);
        knowledgeVersion.bump();
    }

    // Start-up: make sure the Qdrant collection exists, seed an empty knowledge base, retry failed indexing.
    // Runs in the background so a slow or unreachable Qdrant never delays the application.
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        Thread.ofVirtual().name("kb-bootstrap").start(() -> {
            boolean collectionCreated = false;
            try {
                collectionCreated = vectorStore.ensureCollection();
            } catch (RuntimeException e) {
                log.warn("Qdrant not reachable at start-up, chatbot uses MySQL full-text search: {}", e.getMessage());
            }
            try {
                if (!documentRepository.existsByIsDeletedFalse()) {
                    seed();
                } else if (collectionCreated || outdatedPoints() > 0) {
                    reindexAll();
                } else {
                    documentRepository.findAllByIndexStatusInAndIsDeletedFalse(
                            List.of(KbDocument.STATUS_PENDING, KbDocument.STATUS_FAILED)).forEach(indexer::reindex);
                }
                syncVirtualDocuments();
                knowledgeVersion.bump();
            } catch (RuntimeException e) {
                log.error("Knowledge base bootstrap failed", e);
            }
        });
    }

    // Points written by an older encoding or embedding model (0 when Qdrant cannot tell)
    private long outdatedPoints() {
        try {
            long outdated = vectorStore.countOutdated(indexer.indexVersion());
            if (outdated > 0) {
                log.info("{} knowledge-base points were indexed differently, re-indexing everything", outdated);
            }
            return outdated;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private void seed() {
        ClassPathResource resource = new ClassPathResource(SEED_RESOURCE);
        if (!resource.exists()) {
            return;
        }
        String text;
        try {
            text = extractor.extract("seed.html", resource.getContentAsByteArray()).text();
        } catch (IOException e) {
            log.warn("Could not read knowledge-base seed: {}", e.getMessage());
            return;
        }
        int count = 0;
        for (Map.Entry<String, String> chapter : splitChapters(text).entrySet()) {
            Set<String> roles = SEED_ROLES.entrySet().stream()
                    .filter(entry -> chapter.getKey().startsWith(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(ALL_STAFF);
            create(chapter.getKey(), new DocumentTextExtractor.Extracted("HTML", chapter.getValue()),
                    "Đặc tả chức năng Supermarket Management.html", roles);
            count++;
        }
        log.info("Seeded the chatbot knowledge base with {} documents from the functional spec", count);
    }

    // "## Chapter" headings start a new document; text before the first chapter (title, contents) is dropped
    static Map<String, String> splitChapters(String text) {
        Map<String, String> chapters = new LinkedHashMap<>();
        String current = null;
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\n")) {
            if (line.startsWith("## ")) {
                if (current != null) {
                    chapters.put(current, body.toString());
                }
                current = line.substring(3).strip();
                body.setLength(0);
            } else if (current != null) {
                body.append(line).append('\n');
            }
        }
        if (current != null) {
            chapters.put(current, body.toString());
        }
        chapters.values().removeIf(String::isBlank);
        return chapters;
    }

    private KbDocument create(String title, DocumentTextExtractor.Extracted extracted, String sourceName, Set<String> roleCodes) {
        KbDocument document = KbDocument.builder()
                .title(title)
                .sourceType(extracted.sourceType())
                .sourceName(sourceName)
                .contentHash(sha256(extracted.text()))
                .chunkCount(0)
                .indexStatus(KbDocument.STATUS_PENDING)
                .roleCodes(new HashSet<>(roleCodes))
                .build();
        List<KbChunk> chunks = transaction.execute(status -> {
            KbDocument saved = documentRepository.save(document);
            List<KbChunk> built = buildChunks(saved.getId(), title, extracted.text());
            saved.setChunkCount(built.size());
            documentRepository.save(saved);
            return chunkRepository.saveAll(built);
        });
        indexer.index(document, chunks);
        knowledgeVersion.bump();
        return document;
    }

    private List<KbChunk> buildChunks(String documentId, String title, String text) {
        List<DocumentChunker.Chunk> pieces = chunker.chunk(title, text);
        List<KbChunk> chunks = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            DocumentChunker.Chunk piece = pieces.get(i);
            chunks.add(KbChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .documentId(documentId)
                    .chunkIndex(i)
                    .headingPath(piece.headingPath())
                    .content(piece.content())
                    .contentHash(sha256(piece.headingPath() + "\n" + piece.content()))
                    .build());
        }
        return chunks;
    }

    private KbDocument activeDocument(String documentId) {
        return documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.KB_DOCUMENT_NOT_EXIST));
    }

    private void checkRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new AppException(ErrorCode.KB_ROLES_REQUIRED);
        }
        if (roleRepository.findAllByCodeIn(roleCodes).size() != roleCodes.size()) {
            throw new AppException(ErrorCode.ROLE_NOT_EXIST);
        }
    }

    private static byte[] bytesOf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.KB_FILE_EMPTY);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new AppException(ErrorCode.KB_FILE_EMPTY);
        }
    }

    private static String baseName(String fileName) {
        if (fileName == null) {
            return "Tài liệu";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String abbreviate(String text) {
        return text == null || text.length() <= 500 ? text : text.substring(0, 500);
    }

    private static KbDocumentResponse toDTO(KbDocument document) {
        return KbDocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .sourceType(document.getSourceType())
                .sourceName(document.getSourceName())
                .chunkCount(document.getChunkCount())
                .indexStatus(document.getIndexStatus())
                .indexError(document.getIndexError())
                .roleCodes(new TreeSet<>(document.getRoleCodes()))
                .createdBy(document.getCreatedBy())
                .createdDate(document.getCreatedDate())
                .modifiedDate(document.getModifiedDate())
                .build();
    }


    @Override
    public void syncVirtualDocuments() {
        try {
            syncEmployeeVirtualDoc();
            syncWarehouseVirtualDoc();
        } catch (RuntimeException e) {
            log.warn("Syncing virtual entity documents failed: {}", e.getMessage());
        }
    }

    private void syncEmployeeVirtualDoc() {
        List<Employee> employees = employeeRepository.findAllByIsDeletedFalse();
        if (employees.isEmpty()) {
            return;
        }
        StringBuilder md = new StringBuilder();
        md.append("# Danh mục hồ sơ nhân sự siêu thị\n\n");
        for (Employee emp : employees) {
            md.append("## Nhân viên ").append(emp.getCode()).append(": ").append(emp.getName()).append("\n");
            md.append("- Mã nhân viên: ").append(emp.getCode()).append("\n");
            md.append("- Họ và tên: ").append(emp.getName()).append("\n");
            if (emp.getGender() != null) {
                md.append("- Giới tính: ").append(emp.getGender()).append("\n");
            }
            if (emp.getDob() != null) {
                md.append("- Ngày sinh: ").append(emp.getDob().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                        .append(" (sinh năm ").append(emp.getDob().getYear()).append(")\n");
            }
            if (emp.getPosition() != null && emp.getPosition().getName() != null) {
                md.append("- Chức vụ / Vị trí: ").append(emp.getPosition().getName()).append("\n");
            }
            if (emp.getPhoneNumber() != null) {
                md.append("- Số điện thoại: ").append(emp.getPhoneNumber()).append("\n");
            }
            if (emp.getEmail() != null) {
                md.append("- Email: ").append(emp.getEmail()).append("\n");
            }
            String empAddr = formatAddress(emp.getAddress());
            if (empAddr != null) {
                md.append("- Địa chỉ: ").append(empAddr).append("\n");
            }
            md.append("- Trạng thái: Đang làm việc\n\n");
        }

        saveOrUpdateVirtualDocument(
                "Danh mục Hồ sơ Nhân sự",
                md.toString(),
                "system-virtual-employees.md",
                ALL_STAFF
        );
    }

    private void syncWarehouseVirtualDoc() {
        List<Warehouse> warehouses = warehouseRepository.findAllByIsDeletedFalse();
        if (warehouses.isEmpty()) {
            return;
        }
        StringBuilder md = new StringBuilder();
        md.append("# Danh mục chi nhánh kho siêu thị\n\n");
        for (Warehouse wh : warehouses) {
            md.append("## Chi nhánh kho: ").append(wh.getName()).append("\n");
            md.append("- Tên kho: ").append(wh.getName()).append("\n");
            if (wh.getPhoneNumber() != null) {
                md.append("- Số điện thoại: ").append(wh.getPhoneNumber()).append("\n");
            }
            if (wh.getEmail() != null) {
                md.append("- Email: ").append(wh.getEmail()).append("\n");
            }
            String whAddr = formatAddress(wh.getAddress());
            if (whAddr != null) {
                md.append("- Địa chỉ: ").append(whAddr).append("\n");
            }
            if (wh.getMoreInfo() != null && !wh.getMoreInfo().isBlank()) {
                md.append("- Thông tin thêm: ").append(wh.getMoreInfo()).append("\n");
            }
            md.append("\n");
        }

        saveOrUpdateVirtualDocument(
                "Danh mục Chi nhánh Kho",
                md.toString(),
                "system-virtual-warehouses.md",
                ALL_STAFF
        );
    }

    private void saveOrUpdateVirtualDocument(String title, String content, String sourceName, Set<String> roles) {
        String hash = sha256(content);
        Optional<KbDocument> existing = documentRepository.findByTitleAndIsDeletedFalse(title);
        if (existing.isPresent()) {
            KbDocument doc = existing.get();
            if (hash.equals(doc.getContentHash())) {
                return;
            }
            List<KbChunk> chunks = buildChunks(doc.getId(), title, content);
            transaction.executeWithoutResult(status -> {
                chunkRepository.deleteAllByDocumentId(doc.getId());
                chunkRepository.flush();
                chunkRepository.saveAll(chunks);
                doc.setContentHash(hash);
                doc.setChunkCount(chunks.size());
                doc.setIndexStatus(KbDocument.STATUS_PENDING);
                documentRepository.save(doc);
            });
            try {
                vectorStore.deleteDocument(doc.getId());
            } catch (RuntimeException ignored) {
            }
            indexer.index(doc, chunks);
            knowledgeVersion.bump();
        } else {
            create(title, new DocumentTextExtractor.Extracted("MD", content), sourceName, roles);
        }
    }

    private static String formatAddress(com.anonymous.entity.Address addr) {
        if (addr == null) return null;
        List<String> parts = new ArrayList<>();
        if (addr.getStreet() != null && !addr.getStreet().isBlank()) parts.add(addr.getStreet());
        if (addr.getWard() != null && addr.getWard().getName() != null) parts.add(addr.getWard().getName());
        if (addr.getDistrict() != null && addr.getDistrict().getName() != null) {
            parts.add(addr.getDistrict().getName());
            if (addr.getDistrict().getCity() != null && addr.getDistrict().getCity().getName() != null) {
                parts.add(addr.getDistrict().getCity().getName());
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

}
