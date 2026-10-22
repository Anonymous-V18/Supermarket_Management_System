package com.anonymous.service;

import com.anonymous.dto.response.AuditLogResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.entity.AuditLog;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface IAuditLogService {

    void record(AuditLog auditLog);

    PageResponse<AuditLogResponse> search(String keyword, String action, LocalDate from, LocalDate to, Pageable pageable);

}
