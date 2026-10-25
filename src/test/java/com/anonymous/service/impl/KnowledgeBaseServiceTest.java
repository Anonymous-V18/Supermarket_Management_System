package com.anonymous.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeBaseServiceTest {

    @Test
    void specIsSplitIntoOneDocumentPerChapter() {
        Map<String, String> chapters = KnowledgeBaseService.splitChapters(
                "# Đặc tả\nMục lục\n## Luồng bán hàng\n- Quét mã vạch\n## Luồng trả hàng\n- Nhập lý do\n## Trống\n");
        assertThat(chapters).containsOnlyKeys("Luồng bán hàng", "Luồng trả hàng");
        assertThat(chapters.get("Luồng trả hàng")).isEqualTo("- Nhập lý do\n");
    }

}
