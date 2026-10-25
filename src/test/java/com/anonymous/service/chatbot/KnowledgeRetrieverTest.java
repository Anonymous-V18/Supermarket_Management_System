package com.anonymous.service.chatbot;

import com.anonymous.configuration.ChatbotProperties;
import com.anonymous.entity.KbChunk;
import com.anonymous.entity.KbDocument;
import com.anonymous.repository.IKbChunkRepository;
import com.anonymous.repository.IKbDocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeRetrieverTest {

    private static final Set<String> ROLES = Set.of("ADMIN");

    // Embedding service stand-in: enabled, the vector itself does not matter (the store stub ignores it)
    private static final class FakeEmbedder extends TextEmbedder {

        FakeEmbedder(ChatbotProperties properties) {
            super(properties);
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public float[] embedQuery(String question) {
            return new float[DIMENSIONS];
        }

    }

    private static KbChunk chunk(String id, String heading, String content) {
        return KbChunk.builder().id(id).documentId("doc").chunkIndex(0).headingPath(heading).content(content).build();
    }

    private static KnowledgeRetriever retriever(VectorStore.SearchResult result, List<KbChunk> chunks) {
        ChatbotProperties properties = new ChatbotProperties();
        VectorStore store = mock(VectorStore.class);
        when(store.search(any(), any(), any(), anyInt())).thenReturn(result);
        IKbChunkRepository chunkRepository = mock(IKbChunkRepository.class);
        when(chunkRepository.findAllById(anyCollection())).thenReturn(chunks);
        KbDocument document = KbDocument.builder().title("Luồng quản lý nhân sự").roleCodes(Set.of("ADMIN")).build();
        document.setId("doc");
        IKbDocumentRepository documentRepository = mock(IKbDocumentRepository.class);
        when(documentRepository.findAllByIdInAndIsDeletedFalse(anyCollection())).thenReturn(List.of(document));
        return new KnowledgeRetriever(properties, store, new SparseVectorEncoder(), new FakeEmbedder(properties),
                chunkRepository, documentRepository);
    }

    @Test
    void fusionPutsChunksFoundByBothListsFirst() {
        List<String> fused = KnowledgeRetriever.fuse(
                List.of(new VectorStore.Hit("b", 0.9), new VectorStore.Hit("c", 0.8)),
                List.of(new VectorStore.Hit("a", 12), new VectorStore.Hit("b", 10)));
        assertThat(fused).containsExactly("b", "a", "c");
    }

    @Test
    void withoutDenseHitsTheKeywordOrderIsKept() {
        assertThat(KnowledgeRetriever.fuse(List.of(),
                List.of(new VectorStore.Hit("a", 3), new VectorStore.Hit("b", 2)))).containsExactly("a", "b");
    }

    @Test
    void aPassageCloseInMeaningIsFoundWithoutSharedWords() {
        KbChunk password = chunk("p", "Luồng quản lý nhân sự > Thêm nhân viên",
                "Hệ thống tự tạo tài khoản: tên đăng nhập = email, mật khẩu = mật khẩu mặc định cấu hình trên server.");
        KnowledgeRetriever retriever = retriever(
                new VectorStore.SearchResult(List.of(new VectorStore.Hit("p", 0.9)), List.of()), List.of(password));

        KnowledgeRetriever.Retrieval retrieval = retriever.retrieve("Lần đầu vào hệ thống thì gõ gì?", ROLES);

        assertThat(retrieval.mode()).isEqualTo("hybrid");
        assertThat(retrieval.passages()).extracting(passage -> passage.chunk().getId()).containsExactly("p");
        assertThat(AnswerComposer.extract(retrieval.topic(), retrieval.passages())).contains("mật khẩu mặc định");
    }

    @Test
    void aWeakDenseMatchAloneIsNotAnAnswer() {
        KbChunk unrelated = chunk("u", "Luồng quản lý nhân sự > Thêm nhân viên", "Hệ thống cấp mã nhân viên dạng NV00101.");
        KnowledgeRetriever retriever = retriever(
                new VectorStore.SearchResult(List.of(new VectorStore.Hit("u", 0.5)), List.of()), List.of(unrelated));

        assertThat(retriever.retrieve("Thủ đô của Pháp là gì?", ROLES).passages()).isEmpty();
    }

}
