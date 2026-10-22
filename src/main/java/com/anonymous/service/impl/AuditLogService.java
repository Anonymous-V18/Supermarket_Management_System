package com.anonymous.service.impl;

import com.anonymous.converter.IAuditLogMapper;
import com.anonymous.dto.response.AuditLogResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.entity.AuditLog;
import com.anonymous.repository.IAuditLogRepository;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.IAuditLogService;
import com.anonymous.utils.ShiftUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditLogService implements IAuditLogService {

    IAuditLogRepository auditLogRepository;
    IAuditLogMapper auditLogMapper;

    // Own transaction: written after the request finished, whether it committed or rolled back
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String keyword, String action, LocalDate from, LocalDate to,
                                                 Pageable pageable) {
        return PageResponse.of(auditLogRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.keyword(keyword, "username", "resource", "path", "reference"),
                actionIs(action),
                createdBetween(from, to)
        ), pageable), auditLogMapper::toDTO);
    }

    private static Specification<AuditLog> actionIs(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    // Inclusive Vietnam dates
    private static Specification<AuditLog> createdBetween(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return null;
        }
        return (root, query, cb) -> cb.and(
                from == null ? cb.conjunction()
                        : cb.greaterThanOrEqualTo(root.get("createdDate"), from.atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant()),
                to == null ? cb.conjunction()
                        : cb.lessThan(root.get("createdDate"), to.plusDays(1).atStartOfDay(ShiftUtil.VIETNAM_ZONE).toInstant()));
    }

}
