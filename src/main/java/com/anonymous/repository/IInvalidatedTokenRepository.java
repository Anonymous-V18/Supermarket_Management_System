package com.anonymous.repository;

import com.anonymous.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IInvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {
}
