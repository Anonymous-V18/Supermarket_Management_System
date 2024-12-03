package com.anonymous.service.impl;

import com.anonymous.converter.IEmployeeMapper;
import com.anonymous.dto.request.EmployeeInsertRequest;
import com.anonymous.dto.request.EmployeeUpdateRequest;
import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.EmployeeResponse;
import com.anonymous.entity.Address;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Position;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IPositionRepository;
import com.anonymous.service.IAddressService;
import com.anonymous.service.IAuthService;
import com.anonymous.service.IEmployeeService;
import com.anonymous.service.IUserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmployeeService implements IEmployeeService {

    IEmployeeRepository employeeRepository;
    IEmployeeMapper employeeMapper;
    IPositionRepository positionRepository;
    IAuthService authService;
    IAddressService addressService;
    IUserService userService;
    @NonFinal
    @Value("${com.anonymous.employee-default-password}")
    String EMPLOYEE_DEFAULT_PASSWORD;
    @NonFinal
    @Value("${com.anonymous.username-admin}")
    String ADMIN_USERNAME;

    @Override
    @Transactional
    public EmployeeResponse insert(EmployeeInsertRequest employeeInsertRequest) {
        Employee employee = employeeMapper.toEntity(employeeInsertRequest);

        Position position = positionRepository.findById(employeeInsertRequest.getPositionId())
                .orElseThrow(() -> new AppException(ErrorCode.POSITION_NOT_EXIST));
        employee.setPosition(position);

        UserInsertRequest userInsertRequest = new UserInsertRequest();
        userInsertRequest.setUsername(employeeInsertRequest.getEmail());
        userInsertRequest.setPassword(EMPLOYEE_DEFAULT_PASSWORD);
        userInsertRequest.setRoleIds(employeeInsertRequest.getRoleIds());
        User user = userService.insert(userInsertRequest);
        employee.setUser(user);

        Address address = addressService.insert(employeeInsertRequest.getNewAddress());
        employee.setAddress(address);

        String code = generateEmployeeCode();
        employee.setCode(code);

        employee = employeeRepository.save(employee);
        return employeeMapper.toDTO(employee);
    }

    @Override
    public EmployeeResponse update(EmployeeUpdateRequest employeeUpdateRequest) {
        String currentEmployeeId = authService.getClaimsToken().get("sub").toString();
        Employee employee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        employee = employeeMapper.toEntity(employee, employeeUpdateRequest);

        Address address = addressService.update(employeeUpdateRequest.getCurrentAddress());
        employee.setAddress(address);

        employee = employeeRepository.save(employee);
        return employeeMapper.toDTO(employee);
    }

    @Override
    public EmployeeResponse update(String oldEmployeeId, EmployeeUpdateRequest employeeUpdateRequest) {
        Employee employee = employeeRepository.findById(oldEmployeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        employee = employeeMapper.toEntity(employee, employeeUpdateRequest);

        boolean isUpdatePosition = employeeUpdateRequest.getPositionId().equals(employee.getPosition().getId());
        if (isUpdatePosition) {
            Position position = positionRepository.findById(employeeUpdateRequest.getPositionId())
                    .orElseThrow(() -> new AppException(ErrorCode.POSITION_NOT_EXIST));
            employee.setPosition(position);
        }

        userService.changeRole(employee.getUser(), employeeUpdateRequest.getRoleIds());

        Address address = addressService.update(employeeUpdateRequest.getCurrentAddress());
        employee.setAddress(address);


        employee = employeeRepository.save(employee);
        return employeeMapper.toDTO(employee);
    }

    @Override
    public List<EmployeeResponse> getAll() {
        String currentUsername = authService.getClaimsToken().get("username").toString();
        return employeeRepository.findAll().stream()
                .filter(employee ->
                        !employee.getUser().getUsername().equals(ADMIN_USERNAME) &&
                                !employee.getUser().getUsername().equals(currentUsername)
                )
                .map(employeeMapper::toDTO)
                .toList();
    }

    private String generateEmployeeCode() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return localDateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    @Override
    public EmployeeResponse getById(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        return employeeMapper.toDTO(employee);
    }

    @Override
    public EmployeeResponse getMyInfo() {
        String currentEmployeeId = authService.getClaimsToken().get("sub").toString();
        Employee employee = employeeRepository.findById(currentEmployeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        return employeeMapper.toDTO(employee);
    }
}
