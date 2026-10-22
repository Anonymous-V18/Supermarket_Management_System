package com.anonymous.repository;

import com.anonymous.entity.ChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IChecklistItemRepository extends JpaRepository<ChecklistItem, String> {

    List<ChecklistItem> findByIsActiveTrueOrderBySortOrderAsc();

    List<ChecklistItem> findAllByOrderByGroupCodeDescSortOrderAsc();

    boolean existsByCode(String code);

}
