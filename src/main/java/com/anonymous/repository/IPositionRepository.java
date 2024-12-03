package com.anonymous.repository;

import com.anonymous.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPositionRepository extends JpaRepository<Position, String> {
}
