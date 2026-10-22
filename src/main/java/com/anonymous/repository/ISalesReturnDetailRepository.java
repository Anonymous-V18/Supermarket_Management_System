package com.anonymous.repository;

import com.anonymous.entity.SalesReturnDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ISalesReturnDetailRepository extends JpaRepository<SalesReturnDetail, String> {
}
