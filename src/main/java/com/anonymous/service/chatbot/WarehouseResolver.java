package com.anonymous.service.chatbot;

import com.anonymous.dto.response.WarehouseToSelectionResponse;
import com.anonymous.service.IWarehouseService;
import com.anonymous.utils.TtlCache;
import com.anonymous.utils.VietnameseTextUtil;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Finds a warehouse named in a question ("kho Quận 1", "kho trung tam"), accent-insensitively
@Component
public class WarehouseResolver {

    public record Match(String id, String name, String phrase) {
    }

    private final IWarehouseService warehouseService;
    private final TtlCache<String, List<WarehouseToSelectionResponse>> cache = new TtlCache<>(1, Duration.ofMinutes(5));

    public WarehouseResolver(IWarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    public List<WarehouseToSelectionResponse> all() {
        return cache.computeIfAbsent("all", key -> warehouseService.getAllToSelection());
    }

    public Optional<Match> find(String accentFree) {
        return all().stream()
                .map(warehouse -> matchOf(warehouse, accentFree))
                .flatMap(Optional::stream)
                .max(Comparator.comparingInt(match -> match.phrase().length()));
    }

    // By the planner's free-text warehouse name
    public Optional<Match> byName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String wanted = accentFree(name);
        return find(wanted).or(() -> find("kho " + wanted));
    }

    private static Optional<Match> matchOf(WarehouseToSelectionResponse warehouse, String text) {
        String full = accentFree(warehouse.getName());
        String bare = full.startsWith("kho ") ? full.substring(4) : full;
        if (text.contains(full)) {
            return Optional.of(new Match(warehouse.getId(), warehouse.getName(), full));
        }
        // Only accept the bare name when it is said as "kho <name>", so "Quận 1" in an address does not match
        if (!bare.equals(full) && bare.length() >= 2 && text.contains("kho " + bare)) {
            return Optional.of(new Match(warehouse.getId(), warehouse.getName(), "kho " + bare));
        }
        return Optional.empty();
    }

    static String accentFree(String text) {
        return VietnameseTextUtil.stripDiacritics(VietnameseTextUtil.normalize(text));
    }

}
