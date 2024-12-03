package com.anonymous.repository;

import com.anonymous.entity.InvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IInvoiceDetailRepository extends JpaRepository<InvoiceDetail, String> {
}
