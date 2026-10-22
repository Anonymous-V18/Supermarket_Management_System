package com.anonymous.repository;

import com.anonymous.entity.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IInvoiceRepository extends ISoftDeleteRepository<Invoice> {

    interface InvoiceStatusStats {
        String getStatusCode();

        Long getInvoiceCount();

        BigDecimal getRevenue();

        Long getProductCount();
    }

    // Serializes returns against the same invoice (and cancelling it) so returned quantities can't be exceeded
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id AND i.isDeleted = false")
    Optional<Invoice> findForUpdate(@Param("id") String id);

    // employeeId = null summarizes every employee
    @Query("SELECT i.statusInvoice.code AS statusCode, COUNT(i) AS invoiceCount, " +
            "COALESCE(SUM(i.totalPrice), 0) AS revenue, COALESCE(SUM(i.totalProduct), 0) AS productCount " +
            "FROM Invoice i " +
            "WHERE i.isDeleted = false AND i.invoiceCreateDate >= :from AND i.invoiceCreateDate < :to " +
            "AND (:employeeId IS NULL OR i.employee.id = :employeeId) " +
            "GROUP BY i.statusInvoice.code")
    List<InvoiceStatusStats> summarizeByStatus(@Param("from") Instant from,
                                               @Param("to") Instant to,
                                               @Param("employeeId") String employeeId);

}
