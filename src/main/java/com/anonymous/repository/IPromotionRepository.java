package com.anonymous.repository;

import com.anonymous.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPromotionRepository extends JpaRepository<Promotion, String> {
}
