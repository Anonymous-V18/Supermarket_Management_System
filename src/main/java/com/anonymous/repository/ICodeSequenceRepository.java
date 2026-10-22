package com.anonymous.repository;

import com.anonymous.entity.CodeSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICodeSequenceRepository extends JpaRepository<CodeSequence, String> {

    // SELECT ... FOR UPDATE: concurrent inserts wait for each other instead of taking the same number
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CodeSequence s WHERE s.name = :name")
    Optional<CodeSequence> findByNameForUpdate(@Param("name") String name);

}
