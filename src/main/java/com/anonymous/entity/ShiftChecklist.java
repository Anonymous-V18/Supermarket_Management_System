package com.anonymous.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Entity
@Table(name = "shift_checklist",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_shift_date", columnNames = {"username", "work_date", "shift_code"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftChecklist extends AbstractEntity {

    @Column(name = "username", nullable = false)
    String username;

    @Column(name = "work_date", nullable = false)
    LocalDate workDate;

    @Column(name = "shift_code", nullable = false, length = 50)
    String shiftCode;

    @Column(name = "checked_items", columnDefinition = "TEXT")
    String checkedItems;

    @Column(name = "is_completed")
    Boolean isCompleted;

    @Column(name = "opening_cash")
    Double openingCash;

    @Column(name = "closing_cash")
    Double closingCash;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

}
