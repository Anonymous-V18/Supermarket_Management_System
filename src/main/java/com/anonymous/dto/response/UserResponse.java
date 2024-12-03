package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    String id;
    String createdBy;
    Instant createdDate;
    String modifiedBy;
    Instant modifiedDate;
    String username;
    Boolean isActive;
    Set<RoleResponse> roles = new HashSet<>();

}
