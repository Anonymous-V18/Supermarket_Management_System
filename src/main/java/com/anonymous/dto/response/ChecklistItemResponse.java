package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChecklistItemResponse {

    String id;

    String code;

    String label;

    String groupCode;

    Integer sortOrder;

    // Shift checklist only
    Boolean checked;

    // Checklist admin only
    Boolean isActive;

}
