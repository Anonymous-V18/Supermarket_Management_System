package com.anonymous.service.impl;

import com.anonymous.dto.response.DashboardSummaryResponse;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IInvoiceRepository;
import com.anonymous.repository.IInvoiceRepository.InvoiceStatusStats;
import com.anonymous.repository.IPromotionRepository;
import com.anonymous.service.IDashboardService;
import com.anonymous.utils.ShiftUtil;
import com.anonymous.utils.ShiftUtil.ShiftInfo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardService implements IDashboardService {

    IInvoiceRepository invoiceRepository;
    IPromotionRepository promotionRepository;

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_CANCELLED = "CANCELLED";

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDate date, String shiftCode) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        // JWT subject is the employee id (see JwtUtil); admin sees the whole store
        String employeeId = isAdmin ? null : authentication.getToken().getSubject();

        ShiftInfo shiftInfo = ShiftUtil.resolve(date, shiftCode);
        Instant shiftStart = shiftInfo.startInstant();
        Instant shiftEnd = shiftInfo.endInstant();

        Map<String, InvoiceStatusStats> statsByStatus = invoiceRepository
                .summarizeByStatus(shiftStart, shiftEnd, employeeId).stream()
                .collect(Collectors.toMap(InvoiceStatusStats::getStatusCode, Function.identity()));
        // Every invoice that wasn't cancelled counts as sold (pending ones are still being paid)
        List<InvoiceStatusStats> soldStats = statsByStatus.values().stream()
                .filter(stats -> !STATUS_CANCELLED.equals(stats.getStatusCode()))
                .toList();

        return DashboardSummaryResponse.builder()
                .workDate(shiftInfo.date())
                .shiftCode(shiftInfo.code())
                .shiftName(shiftInfo.name())
                .shiftStart(shiftStart)
                .shiftEnd(shiftEnd)
                .scope(isAdmin ? "STORE" : "EMPLOYEE")
                .invoiceCount(soldStats.stream().mapToLong(InvoiceStatusStats::getInvoiceCount).sum())
                .revenue(soldStats.stream().map(InvoiceStatusStats::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add))
                .productsSold(soldStats.stream().mapToLong(InvoiceStatusStats::getProductCount).sum())
                .completedInvoiceCount(countOf(statsByStatus, STATUS_COMPLETED))
                .pendingInvoiceCount(countOf(statsByStatus, STATUS_PENDING))
                .cancelledInvoiceCount(countOf(statsByStatus, STATUS_CANCELLED))
                .activePromotions(getActivePromotions())
                .build();
    }

    private long countOf(Map<String, InvoiceStatusStats> statsByStatus, String statusCode) {
        InvoiceStatusStats stats = statsByStatus.get(statusCode);
        return stats != null ? stats.getInvoiceCount() : 0L;
    }

    private List<DashboardSummaryResponse.ActivePromotion> getActivePromotions() {
        Instant now = Instant.now();
        return promotionRepository
                .findByIsDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByEndDateAsc(now, now).stream()
                .map(promotion -> DashboardSummaryResponse.ActivePromotion.builder()
                        .id(promotion.getId())
                        .name(promotion.getName())
                        .voucherCode(promotion.getVoucherCode())
                        .endDate(promotion.getEndDate())
                        .build())
                .toList();
    }

}
