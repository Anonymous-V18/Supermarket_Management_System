package com.anonymous.service;

import com.anonymous.dto.request.EmployeeInsertRequest;
import com.anonymous.dto.request.EmployeeUpdateRequest;
import com.anonymous.dto.response.EmployeeResponse;

import java.util.List;

public interface IEmployeeService {

    EmployeeResponse insert(EmployeeInsertRequest employeeInsertRequest);

    EmployeeResponse update(EmployeeUpdateRequest employeeUpdateRequest);

    EmployeeResponse update(String oldEmployeeId, EmployeeUpdateRequest employeeUpdateRequest);

    List<EmployeeResponse> getAll();

    EmployeeResponse getById(String employeeId);

    EmployeeResponse getMyInfo();
}
