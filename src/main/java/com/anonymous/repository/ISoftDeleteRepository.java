package com.anonymous.repository;

import com.anonymous.entity.SoftDeletableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

// findById / findAll still return deleted rows (needed to show old invoices); use these for anything "current"
@NoRepositoryBean
public interface ISoftDeleteRepository<T extends SoftDeletableEntity> extends JpaRepository<T, String>, JpaSpecificationExecutor<T> {

    Optional<T> findByIdAndIsDeletedFalse(String id);

    List<T> findAllByIsDeletedFalse();

    List<T> findAllByIdInAndIsDeletedFalse(Collection<String> ids);

}
