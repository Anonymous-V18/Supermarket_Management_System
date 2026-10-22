package com.anonymous.repository;

import com.anonymous.entity.SalesReturn;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISalesReturnRepository extends ISoftDeleteRepository<SalesReturn> {

    interface ReturnedQuantity {
        String getProductId();

        Long getQuantity();
    }

    // Quantities already returned from an invoice by returns that still count (not deleted, not cancelled)
    @Query("SELECT d.product.id AS productId, SUM(d.quantity) AS quantity " +
            "FROM SalesReturnDetail d JOIN d.salesReturn r " +
            "WHERE r.invoice.id = :invoiceId AND r.isDeleted = false AND r.statusInvoice.code <> 'CANCELLED' " +
            "AND (:excludeId IS NULL OR r.id <> :excludeId) " +
            "GROUP BY d.product.id")
    List<ReturnedQuantity> sumReturnedByProduct(@Param("invoiceId") String invoiceId,
                                                @Param("excludeId") String excludeReturnId);

    @Query("SELECT COUNT(r) > 0 FROM SalesReturn r " +
            "WHERE r.invoice.id = :invoiceId AND r.isDeleted = false AND r.statusInvoice.code <> 'CANCELLED'")
    boolean existsActiveByInvoiceId(@Param("invoiceId") String invoiceId);

}
