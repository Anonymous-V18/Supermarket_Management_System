package com.anonymous.service.impl;

import com.anonymous.dto.request.ChecklistItemInsertRequest;
import com.anonymous.dto.request.ChecklistItemRequest;
import com.anonymous.dto.response.ChecklistItemResponse;
import com.anonymous.entity.ChecklistItem;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IChecklistItemRepository;
import com.anonymous.service.IChecklistItemService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Admin list of shift tasks. Items are deactivated, never deleted: past checklists keep pointing at them.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChecklistItemService implements IChecklistItemService {

    IChecklistItemRepository checklistItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getAll() {
        return checklistItemRepository.findAllByOrderByGroupCodeDescSortOrderAsc().stream()
                .map(ChecklistItemService::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChecklistItemResponse insert(ChecklistItemInsertRequest request) {
        if (checklistItemRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.CHECKLIST_ITEM_CODE_EXISTED);
        }
        ChecklistItem item = ChecklistItem.builder().code(request.getCode()).build();
        fill(item, request);
        return toResponse(checklistItemRepository.save(item));
    }

    @Override
    @Transactional
    public ChecklistItemResponse update(String checklistItemId, ChecklistItemRequest request) {
        ChecklistItem item = checklistItemRepository.findById(checklistItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKLIST_ITEM_NOT_EXIST));
        fill(item, request);
        return toResponse(checklistItemRepository.save(item));
    }

    private static void fill(ChecklistItem item, ChecklistItemRequest request) {
        item.setLabel(request.getLabel().trim());
        item.setGroupCode(request.getGroupCode());
        item.setSortOrder(request.getSortOrder());
        item.setIsActive(request.getIsActive());
    }

    private static ChecklistItemResponse toResponse(ChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .code(item.getCode())
                .label(item.getLabel())
                .groupCode(item.getGroupCode())
                .sortOrder(item.getSortOrder())
                .isActive(item.getIsActive())
                .build();
    }

}
