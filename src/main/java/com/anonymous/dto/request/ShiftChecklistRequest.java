package com.anonymous.dto.request;

import jakarta.validation.constraints.Size;
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

    @Size(max = 2000, message = "Ghi chú tối đa 2000 ký tự")
    String notes;

}
