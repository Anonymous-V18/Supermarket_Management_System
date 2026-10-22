package com.anonymous.controller;

import com.anonymous.constant.Permissions;
import com.anonymous.dto.response.ApiResponse;
import com.anonymous.dto.response.AuditLogResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.service.IAuditLogService;
import com.anonymous.utils.PageUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogController {

    IAuditLogService auditLogService;

    @GetMapping("/search")
    @PreAuthorize(Permissions.ADMIN)
    public ApiResponse<PageResponse<AuditLogResponse>> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AuditLogResponse>>builder()
                .result(auditLogService.search(keyword, action, from, to, PageUtil.newestFirst(page, size)))
                .build();
    }

}
