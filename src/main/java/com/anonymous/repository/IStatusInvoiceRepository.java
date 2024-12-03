package com.anonymous.repository;

import com.anonymous.entity.StatusInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStatusInvoiceRepository extends JpaRepository<StatusInvoice, String> {
}
