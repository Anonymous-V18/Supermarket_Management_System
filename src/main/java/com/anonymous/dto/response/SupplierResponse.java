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
public class SupplierResponse {

    String id;
    String name;
    String email;
    String phoneNumber;
    String moreInfo;
    Instant contactDate;
    AddressResponse address;

}
