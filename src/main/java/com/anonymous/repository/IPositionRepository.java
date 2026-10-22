package com.anonymous.repository;

import com.anonymous.entity.Position;
import org.springframework.stereotype.Repository;

@Repository
public interface IPositionRepository extends ISoftDeleteRepository<Position> {
}
