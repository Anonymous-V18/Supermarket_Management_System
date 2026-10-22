package com.anonymous.repository;

import com.anonymous.entity.Unit;
import org.springframework.stereotype.Repository;

@Repository
public interface IUnitRepository extends ISoftDeleteRepository<Unit> {
}
