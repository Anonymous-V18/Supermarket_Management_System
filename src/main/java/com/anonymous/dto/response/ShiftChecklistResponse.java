package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShiftChecklistResponse {

    String id;

    String username;

    LocalDate workDate;

    String shiftCode;

    String shiftName;

    List<String> checkedItemIds;

    Boolean isCompleted;

    Integer completedPercent;

    Double openingCash;

    Double closingCash;

    String notes;

}
