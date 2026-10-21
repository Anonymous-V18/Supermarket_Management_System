package com.anonymous.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftChecklistRequest {

    LocalDate workDate;

    String shiftCode;

    List<String> checkedItemIds;

    Boolean isCompleted;

    Double openingCash;

    Double closingCash;

    String notes;

}
