package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeResponse {

    String id;
    String code;
    String name;
    String gender;
    Instant dob;
    String phoneNumber;
    String email;
    PositionResponse position;
    AddressResponse address;
    UserResponse user;

}
