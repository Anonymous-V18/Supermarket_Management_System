package com.anonymous.repository;

import com.anonymous.entity.Promotion;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IPromotionRepository extends ISoftDeleteRepository<Promotion> {

    List<Promotion> findByIsDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByEndDateAsc(Instant startBefore, Instant endAfter);

}
