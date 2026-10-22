package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.request.ChecklistItemInsertRequest;
import com.anonymous.dto.request.ChecklistItemRequest;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.ChecklistItemResponse;
import com.anonymous.service.IChecklistItemService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Shift checklist tasks (admin); the dashboard only shows active ones
@RestController
@RequestMapping("/checklist-items")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChecklistItemController {

    IChecklistItemService checklistItemService;

    @GetMapping("/show-all-no-param")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<List<ChecklistItemResponse>> showAll() {
        return ApiResponse.<List<ChecklistItemResponse>>builder()
                .result(checklistItemService.getAll())
                .build();
    }

    @PostMapping("/insert")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<ChecklistItemResponse> insert(@RequestBody @Valid ChecklistItemInsertRequest request) {
        return ApiResponse.<ChecklistItemResponse>builder()
                .result(checklistItemService.insert(request))
                .message("Thêm thành công !")
                .build();
    }

    @PutMapping("/update/{id}")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<ChecklistItemResponse> update(@PathVariable(value = "id") String id,
                                                     @RequestBody @Valid ChecklistItemRequest request) {
        return ApiResponse.<ChecklistItemResponse>builder()
                .result(checklistItemService.update(id, request))
                .message("Cập nhật thành công !")
                .build();
    }

}
