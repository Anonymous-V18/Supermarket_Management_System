package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

// One checked task of one shift checklist; created_date is when it was ticked
@Entity
@Table(name = "shift_checklist_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_shift_checklist_item",
                columnNames = {"shift_checklist_id", "checklist_item_id"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftChecklistItem extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_checklist_id", nullable = false)
    ShiftChecklist shiftChecklist;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checklist_item_id", nullable = false)
    ChecklistItem checklistItem;

}
