package com.anonymous.service.impl;

import com.anonymous.converter.IEmployeeMapper;
import com.anonymous.dto.request.EmployeeInsertRequest;
import com.anonymous.dto.request.EmployeeUpdateRequest;
import com.anonymous.dto.request.UserInsertRequest;
import com.anonymous.dto.response.EmployeeResponse;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.entity.Address;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Position;
import com.anonymous.entity.Role;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IPositionRepository;
import com.anonymous.repository.impl.EmployeeRepositoryImpl;
import com.anonymous.repository.specification.SearchSpecs;
import com.anonymous.service.IAddressService;
import com.anonymous.service.IAuthService;
import com.anonymous.service.ICodeGeneratorService;
import com.anonymous.service.IEmployeeService;
import com.anonymous.service.IUserService;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    ICodeGeneratorService codeGeneratorService;
    EmployeeRepositoryImpl employeeRepositoryImpl;
    @NonFinal
    @Value("${com.anonymous.employee-default-password}")
    String EMPLOYEE_DEFAULT_PASSWORD;

    @Override
    @Transactional
    public EmployeeResponse insert(EmployeeInsertRequest employeeInsertRequest) {
        Employee employee = employeeMapper.toEntity(employeeInsertRequest);

        Position position = positionRepository.findByIdAndIsDeletedFalse(employeeInsertRequest.getPositionId())
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

        employee.setCode(codeGeneratorService.next(ICodeGeneratorService.SEQUENCE_EMPLOYEE));

        employee = employeeRepository.save(employee);
        return employeeMapper.toDTO(employee);
    }

    @Override
    public EmployeeResponse update(EmployeeUpdateRequest employeeUpdateRequest) {
        String currentEmployeeId = authService.getClaimsToken().get("sub").toString();
        Employee employee = employeeRepository.findByIdAndIsDeletedFalse(currentEmployeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        employee = employeeMapper.toEntity(employee, employeeUpdateRequest);

        Address address = addressService.update(employeeUpdateRequest.getCurrentAddress());
        employee.setAddress(address);

        employee = employeeRepository.save(employee);
        return employeeMapper.toDTO(employee);
    }

    @Override
    public EmployeeResponse update(String oldEmployeeId, EmployeeUpdateRequest employeeUpdateRequest) {
        Employee employee = employeeRepository.findByIdAndIsDeletedFalse(oldEmployeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        employee = employeeMapper.toEntity(employee, employeeUpdateRequest);

        boolean isUpdatePosition = !employeeUpdateRequest.getPositionId().equals(employee.getPosition().getId());
        if (isUpdatePosition) {
            Position position = positionRepository.findByIdAndIsDeletedFalse(employeeUpdateRequest.getPositionId())
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
        return employeeRepository.findAllByIsDeletedFalse().stream()
                .filter(employee -> {
                    User user = employee.getUser();
                    if (user == null) {
                        return false;
                    }
                    boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                            .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getCode()) || "ADMIN".equalsIgnoreCase(role.getName()));
                    boolean isCurrentUser = user.getUsername() != null && user.getUsername().equals(currentUsername);
                    return !isAdmin && !isCurrentUser;
                })
                .map(employeeMapper::toDTO)
                .toList();
    }

    @Override
    public EmployeeResponse getById(String employeeId) {
        Employee employee = employeeRepository.findByIdAndIsDeletedFalse(employeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        return employeeMapper.toDTO(employee);
    }

    @Override
    public EmployeeResponse getMyInfo() {
        String currentEmployeeId = authService.getClaimsToken().get("sub").toString();
        Employee employee = employeeRepository.findByIdAndIsDeletedFalse(currentEmployeeId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_NOT_EXIST));
        return employeeMapper.toDTO(employee);
    }

    @Override
    public List<EmployeeResponse> getBySearchCriteria(int page, int limit, String sortBy, String... search) {
        List<Employee> employees = employeeRepositoryImpl.searchCriteria(page, limit, sortBy, search);
        return employees.stream().map(employeeMapper::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(String keyword, Pageable pageable) {
        String currentUsername = authService.getClaimsToken().get("username").toString();
        Specification<Employee> manageable = (root, query, cb) -> {
            Join<Employee, User> user = root.join("user");
            Subquery<String> admins = query.subquery(String.class);
            Root<User> adminUser = admins.from(User.class);
            Join<User, Role> role = adminUser.join("roles");
            admins.select(adminUser.get("id")).where(cb.equal(role.get("code"), "ADMIN"));
            return cb.and(cb.notEqual(user.get("username"), currentUsername), cb.not(user.get("id").in(admins)));
        };
        return PageResponse.of(employeeRepository.findAll(SearchSpecs.allOf(
                SearchSpecs.notDeleted(),
                manageable,
                SearchSpecs.keyword(keyword, "code", "name", "phoneNumber", "email")
        ), pageable), employeeMapper::toDTO);
    }

}
