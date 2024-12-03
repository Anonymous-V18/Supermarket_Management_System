package com.anonymous.converter;

import com.anonymous.dto.request.EmployeeInsertRequest;
import com.anonymous.dto.request.EmployeeUpdateRequest;
import com.anonymous.dto.response.EmployeeResponse;
import com.anonymous.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface IEmployeeMapper {

    EmployeeResponse toDTO(Employee employee);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "stockOuts", ignore = true)
    @Mapping(target = "stockIns", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "address", ignore = true)
    Employee toEntity(EmployeeInsertRequest employeeInsertRequest);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "stockOuts", ignore = true)
    @Mapping(target = "stockIns", ignore = true)
    @Mapping(target = "position", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "address", ignore = true)
    Employee toEntity(@MappingTarget Employee employee, EmployeeUpdateRequest employeeUpdateRequest);

}
