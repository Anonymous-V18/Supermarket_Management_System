package com.anonymous.service.impl;

import com.anonymous.dto.request.ShiftChecklistRequest;
import com.anonymous.dto.response.ChecklistItemResponse;
import com.anonymous.dto.response.ShiftChecklistResponse;
import com.anonymous.entity.ChecklistItem;
import com.anonymous.entity.ShiftChecklist;
import com.anonymous.entity.ShiftChecklistItem;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IChecklistItemRepository;
import com.anonymous.repository.IShiftChecklistRepository;
import com.anonymous.repository.IUserRepository;
import com.anonymous.service.IShiftChecklistService;
import com.anonymous.utils.ShiftUtil;
import com.anonymous.utils.ShiftUtil.ShiftInfo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ShiftChecklistService implements IShiftChecklistService {

    IShiftChecklistRepository shiftChecklistRepository;
    IChecklistItemRepository checklistItemRepository;
    IUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ShiftChecklistResponse getChecklist(LocalDate date, String shiftCode) {
        User user = currentUser();
        ShiftInfo shiftInfo = ShiftUtil.resolve(date, shiftCode);
        List<ChecklistItem> activeItems = checklistItemRepository.findByIsActiveTrueOrderBySortOrderAsc();

        return shiftChecklistRepository
                .findByUserAndWorkDateAndShiftCode(user, shiftInfo.date(), shiftInfo.code())
                .map(entity -> toResponse(entity, shiftInfo.name(), activeItems))
                .orElseGet(() -> ShiftChecklistResponse.builder()
                        .username(user.getUsername())
                        .workDate(shiftInfo.date())
                        .shiftCode(shiftInfo.code())
                        .shiftName(shiftInfo.name())
                        .checkedItemIds(Collections.emptyList())
                        .items(toItemResponses(activeItems, Collections.emptySet()))
                        .isCompleted(false)
                        .completedPercent(0)
                        .build());
    }

    @Override
    @Transactional
    public ShiftChecklistResponse saveOrUpdateChecklist(ShiftChecklistRequest request) {
        User user = currentUser();
        ShiftInfo shiftInfo = ShiftUtil.resolve(request.getWorkDate(), request.getShiftCode());
        List<ChecklistItem> activeItems = checklistItemRepository.findByIsActiveTrueOrderBySortOrderAsc();

        Set<String> requestedCodes = request.getCheckedItemIds() != null
                ? new HashSet<>(request.getCheckedItemIds())
                : Collections.emptySet();
        // Unknown or deactivated codes (e.g. from an outdated client) are ignored
        List<ChecklistItem> itemsToCheck = activeItems.stream()
                .filter(item -> requestedCodes.contains(item.getCode()))
                .toList();

        ShiftChecklist entity = shiftChecklistRepository
                .findByUserAndWorkDateAndShiftCode(user, shiftInfo.date(), shiftInfo.code())
                .orElseGet(() -> ShiftChecklist.builder()
                        .user(user)
                        .workDate(shiftInfo.date())
                        .shiftCode(shiftInfo.code())
                        .build());

        syncCheckedItems(entity, itemsToCheck);

        int percent = calculatePercent(itemsToCheck.size(), activeItems.size());
        entity.setIsCompleted(request.getIsCompleted() != null ? request.getIsCompleted() : percent == 100);
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }

        ShiftChecklist saved = shiftChecklistRepository.save(entity);
        log.info("Saved shift checklist for user [{}] on date [{}] shift [{}] (progress: {}%)",
                user.getUsername(), shiftInfo.date(), shiftInfo.code(), percent);

        return toResponse(saved, shiftInfo.name(), activeItems);
    }

    // Keep rows that are still checked so their created_date (time ticked) is preserved
    private void syncCheckedItems(ShiftChecklist entity, List<ChecklistItem> itemsToCheck) {
        Set<String> codesToCheck = itemsToCheck.stream().map(ChecklistItem::getCode).collect(Collectors.toSet());
        entity.getCheckedItems().removeIf(row -> !codesToCheck.contains(row.getChecklistItem().getCode()));

        Set<String> alreadyChecked = checkedCodesOf(entity);
        itemsToCheck.stream()
                .filter(item -> !alreadyChecked.contains(item.getCode()))
                .forEach(item -> entity.getCheckedItems().add(ShiftChecklistItem.builder()
                        .shiftChecklist(entity)
                        .checklistItem(item)
                        .build()));
    }

    private ShiftChecklistResponse toResponse(ShiftChecklist entity, String shiftName, List<ChecklistItem> activeItems) {
        Set<String> checkedCodes = checkedCodesOf(entity);
        List<String> activeCheckedCodes = activeItems.stream()
                .map(ChecklistItem::getCode)
                .filter(checkedCodes::contains)
                .toList();

        return ShiftChecklistResponse.builder()
                .id(entity.getId())
                .username(entity.getUser().getUsername())
                .workDate(entity.getWorkDate())
                .shiftCode(entity.getShiftCode())
                .shiftName(shiftName != null ? shiftName : ShiftUtil.nameOf(entity.getShiftCode()))
                .checkedItemIds(activeCheckedCodes)
                .items(toItemResponses(activeItems, checkedCodes))
                .isCompleted(Boolean.TRUE.equals(entity.getIsCompleted()))
                .completedPercent(calculatePercent(activeCheckedCodes.size(), activeItems.size()))
                .notes(entity.getNotes())
                .build();
    }

    private List<ChecklistItemResponse> toItemResponses(List<ChecklistItem> activeItems, Set<String> checkedCodes) {
        return activeItems.stream()
                .map(item -> ChecklistItemResponse.builder()
                        .code(item.getCode())
                        .label(item.getLabel())
                        .groupCode(item.getGroupCode())
                        .sortOrder(item.getSortOrder())
                        .checked(checkedCodes.contains(item.getCode()))
                        .build())
                .toList();
    }

    private Set<String> checkedCodesOf(ShiftChecklist entity) {
        return entity.getCheckedItems().stream()
                .map(row -> row.getChecklistItem().getCode())
                .collect(Collectors.toSet());
    }

    private int calculatePercent(int checkedCount, int totalCount) {
        if (totalCount == 0) {
            return 0;
        }
        return Math.min(100, Math.round((checkedCount * 100.0f) / totalCount));
    }

    // The checklist always belongs to the logged-in account (JWT "username" claim; the subject is the employee id)
    private User currentUser() {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String username = authentication.getToken().getClaimAsString("username");
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIST));
    }

}
