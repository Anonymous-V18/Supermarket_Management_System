package com.anonymous.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerInsertRequest extends CustomerRequest {

    UserInsertRequest userInsertRequest;
    AddressInsertRequest newAddress;

}
