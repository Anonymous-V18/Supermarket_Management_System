package com.anonymous.entity;

import com.anonymous.configuration.JpaAuditingConfig;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

// Rows are never physically deleted: invoices, stock documents and reports keep pointing at them.
// Lists and new transactions must use the repository's "...IsDeletedFalse" queries.
@Getter
@Setter
@MappedSuperclass
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class SoftDeletableEntity extends AbstractEntity {

    @Column(name = "is_deleted", nullable = false)
    Boolean isDeleted = false;

    @Column(name = "deleted_date")
    Instant deletedDate;

    @Column(name = "deleted_by")
    String deletedBy;

    public void markDeleted() {
        this.isDeleted = true;
        this.deletedDate = Instant.now();
        // Same identity as created_by / modified_by
        this.deletedBy = new JpaAuditingConfig.AuditorAwareImpl().getCurrentAuditor().orElse("SYSTEM");
    }

}
