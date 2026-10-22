package com.anonymous.service.impl;

import com.anonymous.constant.DocumentStatus;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Product;
import com.anonymous.entity.StatusInvoice;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IProductRepository;
import com.anonymous.repository.IStatusInvoiceRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

// Rules shared by invoices, stock-ins and stock-outs (status workflow, ownership, line checks)
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentSupport {

    IEmployeeRepository employeeRepository;
    IStatusInvoiceRepository statusInvoiceRepository;
    IProductRepository productRepository;

    // JWT subject is the employee id (see JwtUtil)
    public Employee currentEmployee() {
        String employeeId = currentAuthentication().getToken().getSubject();
        return employeeRepository.findByIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
    }

    public boolean isAdmin() {
        return currentAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    // Only the employee who created a document (or an admin) may change it
    public void checkOwnerOrAdmin(Employee owner) {
        if (isAdmin()) {
            return;
        }
        if (owner == null || !owner.getId().equals(currentAuthentication().getToken().getSubject())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    public StatusInvoice statusForCreate(String statusId) {
        StatusInvoice status = statusId == null || statusId.isBlank()
                ? statusInvoiceRepository.findByCode(DocumentStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST))
                : statusInvoiceRepository.findById(statusId)
                .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));
        if (DocumentStatus.CANCELLED.equals(status.getCode())) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        return status;
    }

    public StatusInvoice statusForUpdate(StatusInvoice current, String statusId) {
        if (statusId == null || statusId.isBlank() || statusId.equals(current.getId())) {
            return current;
        }
        StatusInvoice next = statusInvoiceRepository.findById(statusId)
                .orElseThrow(() -> new AppException(ErrorCode.STATUS_NOT_EXIST));
        if (!DocumentStatus.canChange(current.getCode(), next.getCode())) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION);
        }
        return next;
    }

    // Completed documents are history: they can be cancelled, not removed
    public void checkDeletable(StatusInvoice status) {
        if (DocumentStatus.COMPLETED.equals(status.getCode())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_COMPLETED);
        }
    }

    public Product activeProduct(String productId) {
        return productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_EXIST));
    }

    // One line per product per document (also enforced by a unique constraint)
    public <L> void checkNoDuplicateProducts(Collection<L> lines, Function<L, String> productIdOf) {
        Set<String> seen = new HashSet<>();
        for (L line : lines) {
            if (!seen.add(productIdOf.apply(line))) {
                throw new AppException(ErrorCode.DUPLICATE_PRODUCT_LINE);
            }
        }
    }

    // Quantities a document moves, or nothing when its status doesn't count for stock
    public <L> Map<Product, Integer> stockQuantities(Collection<L> lines, String statusCode,
                                                     Function<L, Product> productOf, ToIntFunction<L> quantityOf) {
        Map<Product, Integer> quantities = new LinkedHashMap<>();
        if (DocumentStatus.affectsStock(statusCode)) {
            lines.forEach(line -> quantities.merge(productOf.apply(line), quantityOf.applyAsInt(line), Integer::sum));
        }
        return quantities;
    }

    private JwtAuthenticationToken currentAuthentication() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication) {
            return authentication;
        }
        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

}
