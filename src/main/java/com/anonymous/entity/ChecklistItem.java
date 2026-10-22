package com.anonymous.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.FieldDefaults;

// Master list of shift checklist tasks; add or deactivate rows instead of changing code
@Entity
@Table(name = "checklist_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_checklist_item_code", columnNames = "code"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChecklistItem extends AbstractEntity {

    @Column(name = "code", nullable = false, length = 50)
    String code;

    @Column(name = "label", nullable = false)
    String label;

    // START: đầu ca, END: cuối ca
    @Column(name = "group_code", nullable = false, length = 20)
    String groupCode;

    @Column(name = "sort_order", nullable = false)
    Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    Boolean isActive;

}
