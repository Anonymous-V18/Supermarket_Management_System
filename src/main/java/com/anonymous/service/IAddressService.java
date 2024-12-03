package com.anonymous.service;

import com.anonymous.dto.request.AddressInsertRequest;
import com.anonymous.dto.request.AddressUpdateRequest;
import com.anonymous.entity.Address;

public interface IAddressService {

    Address insert(AddressInsertRequest addressInsertRequest);

    Address update(AddressUpdateRequest addressUpdateRequest);

}
