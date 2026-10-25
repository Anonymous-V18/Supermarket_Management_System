package com.anonymous.repository;

import com.anonymous.entity.KbDocument;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IKbDocumentRepository extends ISoftDeleteRepository<KbDocument> {

    List<KbDocument> findAllByIsDeletedFalseOrderByCreatedDateDesc();

    List<KbDocument> findAllByIndexStatusInAndIsDeletedFalse(Collection<String> indexStatuses);

    java.util.Optional<KbDocument> findByTitleAndIsDeletedFalse(String title);

    boolean existsByIsDeletedFalse();

}
