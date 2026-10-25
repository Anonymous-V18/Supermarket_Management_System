package com.anonymous.service.chatbot;

import com.anonymous.utils.VietnameseTextUtil;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keyword side of the hybrid search. Documents get BM25 term-frequency weights; Qdrant multiplies them by
// the inverse document frequency itself (sparse vector "modifier: idf"), which together gives BM25 ranking.
@Component
public class SparseVectorEncoder {

    public record SparseVector(int[] indices, float[] values) {

        public boolean isEmpty() {
            return indices.length == 0;
        }

    }

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    // Typical chunk length in terms (every syllable and syllable pair counts twice: accented and accent-free).
    // Measured on the seed spec, which RetrievalGoldSetTest prints; chunks are cut to a similar size, so a
    // constant is close enough
    private static final double AVERAGE_LENGTH = 490;
    // Weight of the accent-free twin of a term in a question typed with accents: it still finds passages
    // written without accents, but does not count the same word twice
    private static final double ACCENT_FREE_TWIN_WEIGHT = 0.3;
    private static final double SYNONYM_EXPANSION_WEIGHT = 0.7;

    public SparseVector encodeDocument(String text) {
        List<String> terms = VietnameseTextUtil.terms(text);
        Map<Integer, Integer> counts = new HashMap<>();
        for (String term : terms) {
            counts.merge(indexOf(term), 1, Integer::sum);
        }
        double lengthNorm = 1 - B + B * terms.size() / AVERAGE_LENGTH;
        Map<Integer, Double> weights = new HashMap<>();
        counts.forEach((index, tf) -> weights.put(index, tf * (K1 + 1) / (tf + K1 * lengthNorm)));
        return toVector(weights);
    }

    public SparseVector encodeQuery(String text) {
        boolean accented = VietnameseTextUtil.hasDiacritics(text);
        Map<Integer, Double> weights = new HashMap<>();
        for (String term : VietnameseTextUtil.terms(text)) {
            double weight = accented && VietnameseTextUtil.isAccentFreeTwin(term) ? ACCENT_FREE_TWIN_WEIGHT : 1.0;
            weights.merge(indexOf(term), weight, Math::max);
        }
        for (String syn : DomainThesaurus.synonymsForQuery(text)) {
            for (String term : VietnameseTextUtil.terms(syn)) {
                double weight = (accented && VietnameseTextUtil.isAccentFreeTwin(term) ? ACCENT_FREE_TWIN_WEIGHT : 1.0)
                        * SYNONYM_EXPANSION_WEIGHT;
                weights.merge(indexOf(term), weight, Math::max);
            }
        }
        return toVector(weights);
    }

    // String.hashCode is fixed by the language spec, so ids stay stable across restarts and machines
    static int indexOf(String term) {
        return term.hashCode() & 0x7fffffff;
    }

    private static SparseVector toVector(Map<Integer, Double> weights) {
        int[] indices = new int[weights.size()];
        float[] values = new float[weights.size()];
        int i = 0;
        for (Map.Entry<Integer, Double> entry : weights.entrySet()) {
            indices[i] = entry.getKey();
            values[i] = entry.getValue().floatValue();
            i++;
        }
        return new SparseVector(indices, values);
    }

}
