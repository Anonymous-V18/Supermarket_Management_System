package com.anonymous.service.impl;

import com.anonymous.dto.request.AddressInsertRequest;
import com.anonymous.dto.request.AddressUpdateRequest;
import com.anonymous.entity.Address;
import com.anonymous.entity.City;
import com.anonymous.entity.District;
import com.anonymous.entity.Ward;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IAddressRepository;
import com.anonymous.repository.ICityRepository;
import com.anonymous.service.IAddressService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressService implements IAddressService {

    IAddressRepository addressRepository;
    ICityRepository cityRepository;

    @Override
    @Transactional
    public Address insert(AddressInsertRequest addressInsertRequest) {
        City city = cityRepository.findById(addressInsertRequest.getCityId())
                .orElseThrow(() -> new AppException(ErrorCode.CITY_NOT_EXIST));

        District district = city.getDistricts().stream()
                .filter(d -> d.getId().equals(addressInsertRequest.getDistrictId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.DISTRICT_NOT_EXIST));

        Ward ward = district.getWards().stream()
                .filter(w -> w.getId().equals(addressInsertRequest.getWardId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_EXIST));

        Address address = Address.builder()
                .district(district)
                .ward(ward)
                .street(addressInsertRequest.getStreet())
                .build();

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address update(AddressUpdateRequest addressUpdateRequest) {
        String oldAddressId = addressUpdateRequest.getId();
        // Records seeded without an address have nothing to update, so create one instead
        if (oldAddressId == null || oldAddressId.isBlank()) {
            AddressInsertRequest addressInsertRequest = new AddressInsertRequest();
            addressInsertRequest.setCityId(addressUpdateRequest.getCityId());
            addressInsertRequest.setDistrictId(addressUpdateRequest.getDistrictId());
            addressInsertRequest.setWardId(addressUpdateRequest.getWardId());
            addressInsertRequest.setStreet(addressUpdateRequest.getStreet());
            return insert(addressInsertRequest);
        }

        Address address = addressRepository.findById(oldAddressId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_EXIST));

        // Resolve from the requested city, not the stored one, so moving to another city works
        City city = cityRepository.findById(addressUpdateRequest.getCityId())
                .orElseThrow(() -> new AppException(ErrorCode.CITY_NOT_EXIST));

        District district = city.getDistricts().stream()
                .filter(d -> d.getId().equals(addressUpdateRequest.getDistrictId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.DISTRICT_NOT_EXIST));

        Ward ward = district.getWards().stream()
                .filter(w -> w.getId().equals(addressUpdateRequest.getWardId()))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.WARD_NOT_EXIST));

        address.setDistrict(district);
        address.setWard(ward);
        address.setStreet(addressUpdateRequest.getStreet());
        return addressRepository.save(address);
    }
}
