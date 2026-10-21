package com.anonymous.service.impl;

import com.anonymous.dto.request.ShiftChecklistRequest;
import com.anonymous.dto.response.ShiftChecklistResponse;
import com.anonymous.entity.ShiftChecklist;
import com.anonymous.repository.IShiftChecklistRepository;
import com.anonymous.service.IShiftChecklistService;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShiftChecklistService implements IShiftChecklistService {

    IShiftChecklistRepository shiftChecklistRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int DEFAULT_TOTAL_CHECKLIST_ITEMS = 6;

    record ShiftInfo(LocalDate date, String code, String name) {}

    @Override
    @Transactional(readOnly = true)
    public ShiftChecklistResponse getChecklist(LocalDate date, String shiftCode, String requestedUsername) {
        String username = resolveUsername(requestedUsername);
        ShiftInfo shiftInfo = resolveShift(date, shiftCode);

        return shiftChecklistRepository
                .findByUsernameAndWorkDateAndShiftCode(username, shiftInfo.date(), shiftInfo.code())
                .map(entity -> toResponse(entity, shiftInfo.name()))
                .orElseGet(() -> ShiftChecklistResponse.builder()
                        .username(username)
                        .workDate(shiftInfo.date())
                        .shiftCode(shiftInfo.code())
                        .shiftName(shiftInfo.name())
                        .checkedItemIds(Collections.emptyList())
                        .isCompleted(false)
                        .completedPercent(0)
                        .build());
    }

    @Override
    @Transactional
    public ShiftChecklistResponse saveOrUpdateChecklist(ShiftChecklistRequest request, String requestedUsername) {
        String username = resolveUsername(requestedUsername);
        ShiftInfo shiftInfo = resolveShift(request.getWorkDate(), request.getShiftCode());

        List<String> items = request.getCheckedItemIds() != null ? request.getCheckedItemIds() : new ArrayList<>();
        String serializedItems = serializeCheckedItems(items);

        int percent = calculatePercent(items);
        boolean isCompleted = request.getIsCompleted() != null
                ? request.getIsCompleted()
                : (percent == 100);

        ShiftChecklist entity = shiftChecklistRepository
                .findByUsernameAndWorkDateAndShiftCode(username, shiftInfo.date(), shiftInfo.code())
                .orElseGet(() -> ShiftChecklist.builder()
                        .username(username)
                        .workDate(shiftInfo.date())
                        .shiftCode(shiftInfo.code())
                        .build());

        entity.setCheckedItems(serializedItems);
        entity.setIsCompleted(isCompleted);

        if (request.getOpeningCash() != null) {
            entity.setOpeningCash(request.getOpeningCash());
        }
        if (request.getClosingCash() != null) {
            entity.setClosingCash(request.getClosingCash());
        }
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        ShiftChecklist saved = shiftChecklistRepository.save(entity);
        log.info("Saved shift checklist for user [{}] on date [{}] shift [{}] (progress: {}%)",
                username, shiftInfo.date(), shiftInfo.code(), percent);

        return toResponse(saved, shiftInfo.name());
    }

    private ShiftChecklistResponse toResponse(ShiftChecklist entity, String shiftName) {
        List<String> items = deserializeCheckedItems(entity.getCheckedItems());
        int percent = calculatePercent(items);

        return ShiftChecklistResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .workDate(entity.getWorkDate())
                .shiftCode(entity.getShiftCode())
                .shiftName(shiftName != null ? shiftName : getShiftNameByCode(entity.getShiftCode()))
                .checkedItemIds(items)
                .isCompleted(Boolean.TRUE.equals(entity.getIsCompleted()))
                .completedPercent(percent)
                .openingCash(entity.getOpeningCash())
                .closingCash(entity.getClosingCash())
                .notes(entity.getNotes())
                .build();
    }

    private int calculatePercent(List<String> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        return Math.min(100, Math.round((items.size() * 100.0f) / DEFAULT_TOTAL_CHECKLIST_ITEMS));
    }

    private ShiftInfo resolveShift(LocalDate date, String shiftCode) {
        ZonedDateTime now = ZonedDateTime.now(VIETNAM_ZONE);
        int hour = now.getHour();

        String resolvedCode = shiftCode;
        if (resolvedCode == null || resolvedCode.isBlank()) {
            if (hour >= 6 && hour < 14) {
                resolvedCode = "MORNING";
            } else if (hour >= 14 && hour < 22) {
                resolvedCode = "AFTERNOON";
            } else {
                resolvedCode = "NIGHT";
            }
        }
        resolvedCode = resolvedCode.trim().toUpperCase();

        LocalDate resolvedDate = date;
        if (resolvedDate == null) {
            resolvedDate = now.toLocalDate();
            // Nếu là ca đêm và đang ở khung giờ 00:00 - 05:59, ca này thuộc về ngày hôm trước
            if ("NIGHT".equals(resolvedCode) && hour < 6) {
                resolvedDate = resolvedDate.minusDays(1);
            }
        }

        String shiftName = getShiftNameByCode(resolvedCode);
        return new ShiftInfo(resolvedDate, resolvedCode, shiftName);
    }

    private String getShiftNameByCode(String code) {
        if (code == null) return "";
        return switch (code.toUpperCase()) {
            case "MORNING" -> "Ca sáng (06:00 - 14:00)";
            case "AFTERNOON" -> "Ca chiều (14:00 - 22:00)";
            case "NIGHT" -> "Ca đêm (22:00 - 06:00)";
            default -> code;
        };
    }

    private String resolveUsername(String requestedUsername) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken) && authentication.isAuthenticated()) {
            String authName = authentication.getName();
            if (authName != null && !authName.isBlank()) {
                return authName;
            }
        }
        if (requestedUsername != null && !requestedUsername.isBlank()) {
            return requestedUsername.trim();
        }
        return "pos_cashier";
    }

    private String serializeCheckedItems(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(items);
        } catch (Exception e) {
            log.error("Failed to serialize checked items: {}", e.getMessage());
            return "[]";
        }
    }

    private List<String> deserializeCheckedItems(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize checked items: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
