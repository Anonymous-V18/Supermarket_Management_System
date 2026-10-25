package com.anonymous.service.chatbot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentProcessingTest {

    private static final String HTML = """
            <html><body><nav>Mục lục</nav>
            <h1>Đặc tả</h1>
            <section><div class="sec-head"><span class="sec-no">09</span><h2>Luồng chuyển kho</h2></div>
            <dl><dt>Người thực hiện</dt><dd>Thủ kho, Admin</dd></dl>
            <div class="flow"><span class="state">Lập phiếu</span><span>→</span><span class="state">Đang vận chuyển</span><span>kho chuyển bị trừ</span></div>
            <ol><li>Vào <b>Chuyển kho</b> → Tạo phiếu.</li></ol>
            <table><thead><tr><th>Trạng thái</th><th>Tồn kho</th></tr></thead>
            <tbody><tr><td>Đã nhận hàng</td><td>kho nhận được cộng</td></tr></tbody></table>
            </section></body></html>
            """;

    @Test
    void htmlBecomesHeadingsListsAndMarkdownTables() {
        String text = DocumentTextExtractor.htmlText(HTML);
        assertThat(text).doesNotContain("Mục lục").doesNotContain("09");
        assertThat(text).contains("# Đặc tả", "## Luồng chuyển kho", "- Người thực hiện: Thủ kho, Admin",
                "- Vào Chuyển kho → Tạo phiếu.");
        // Side-by-side boxes keep a space between them
        assertThat(text).contains("Lập phiếu → Đang vận chuyển kho chuyển bị trừ");
        assertThat(text).contains("| Trạng thái | Tồn kho |\n| --- | --- |\n| Đã nhận hàng | kho nhận được cộng |");
    }

    @Test
    void chunksStayInsideTheirSection() {
        List<DocumentChunker.Chunk> chunks = new DocumentChunker().chunk("Luồng chuyển kho",
                "Mở đầu\n## Quy tắc\n- Kho nhận phải khác kho chuyển\n### Lỗi\n- Số lượng tồn kho không đủ");
        assertThat(chunks).extracting(DocumentChunker.Chunk::headingPath)
                .containsExactly("Luồng chuyển kho", "Luồng chuyển kho > Quy tắc", "Luồng chuyển kho > Quy tắc > Lỗi");
        assertThat(chunks.get(2).content()).isEqualTo("- Số lượng tồn kho không đủ");
    }

    @Test
    void longSectionsAreSplitWithoutLosingText() {
        String line = "Câu thử nghiệm số liệu tồn kho. ".repeat(120).strip();
        List<DocumentChunker.Chunk> chunks = new DocumentChunker().chunk("Tài liệu", line);
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allMatch(chunk -> chunk.content().length() <= DocumentChunker.MAX_CHARS);
        assertThat(String.join(" ", chunks.stream().map(DocumentChunker.Chunk::content).toList()).replace(" ", ""))
                .isEqualTo(line.replace(" ", ""));
    }

    @Test
    void sparseQueryAndDocumentShareTermIds() {
        SparseVectorEncoder encoder = new SparseVectorEncoder();
        SparseVectorEncoder.SparseVector document = encoder.encodeDocument("Hóa đơn đã hoàn thành chỉ được hủy");
        SparseVectorEncoder.SparseVector query = encoder.encodeQuery("huy hoa don");
        assertThat(java.util.Arrays.stream(query.indices()).filter(index ->
                java.util.Arrays.stream(document.indices()).anyMatch(other -> other == index)).count()).isGreaterThan(0);
        for (float value : document.values()) {
            assertThat(value).isPositive();
        }
    }

    @Test
    void topicIgnoresQuestionFramingAndWeighsRareWords() {
        TopicMatcher topic = new TopicMatcher("Quy trình kiểm kê kho gồm những bước nào?")
                .withCorpus(List.of("kiểm kê kho", "nhập kho", "xuất kho", "chuyển kho"));
        assertThat(topic.share("Luồng kiểm kê kho")).isEqualTo(1.0);
        // "kho" is in every candidate, so it alone says little
        assertThat(topic.share("Luồng nhập kho")).isLessThan(0.1);
    }

}
