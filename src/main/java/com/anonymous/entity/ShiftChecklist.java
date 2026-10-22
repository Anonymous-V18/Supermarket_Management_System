package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "shift_checklist",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_shift_date", columnNames = {"user_id", "work_date", "shift_code"}))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftChecklist extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "work_date", nullable = false)
    LocalDate workDate;

    @Column(name = "shift_code", nullable = false, length = 50)
    String shiftCode;

    @OneToMany(mappedBy = "shiftChecklist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    Set<ShiftChecklistItem> checkedItems = new HashSet<>();

    @Column(name = "is_completed")
    Boolean isCompleted;

    @Column(name = "notes", columnDefinition = "TEXT")
    String notes;

}
