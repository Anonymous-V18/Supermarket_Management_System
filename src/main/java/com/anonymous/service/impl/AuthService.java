package com.anonymous.service.impl;

import com.anonymous.client.IdentityClient;
import com.anonymous.converter.ICustomerMapper;
import com.anonymous.converter.IEmployeeMapper;
import com.anonymous.dto.identity.IdentityAuthResult;
import com.anonymous.dto.identity.IdentityIntrospectResult;
import com.anonymous.dto.identity.IdentityRoleResult;
import com.anonymous.dto.identity.IdentityUserResult;
import com.anonymous.dto.request.LoginRequest;
import com.anonymous.dto.request.LogoutRequest;
import com.anonymous.dto.request.RefreshTokenRequest;
import com.anonymous.dto.response.AuthResponse;
import com.anonymous.dto.response.CustomerResponse;
import com.anonymous.dto.response.EmployeeResponse;
import com.anonymous.dto.response.RefreshTokenResponse;
import com.anonymous.dto.response.RoleResponse;
import com.anonymous.entity.Customer;
import com.anonymous.entity.Employee;
import com.anonymous.entity.Position;
import com.anonymous.entity.User;
import com.anonymous.exception.AppException;
import com.anonymous.exception.ErrorCode;
import com.anonymous.repository.ICustomerRepository;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IPositionRepository;
import com.anonymous.repository.IUserRepository;
import com.anonymous.service.IAuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService implements IAuthService {

    IUserRepository userRepository;
    IEmployeeRepository employeeRepository;
    IPositionRepository positionRepository;
    ICustomerRepository customerRepository;
    IEmployeeMapper employeeMapper;
    ICustomerMapper customerMapper;
    IdentityClient identityClient;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Processing login request for user: {}", request.getUsername());

        // Authenticate with centralized Identity Service
        IdentityAuthResult identityAuth = identityClient.login(request.getUsername(), request.getPassword());
        if (identityAuth == null || identityAuth.getAccessToken() == null) {
            throw new AppException(ErrorCode.LOGIN_FAILED);
        }

        // Sync local user entity with IAM identity
        User localUser = syncUserWithIdentity(identityAuth.getUser());

        EmployeeResponse employeeResponse = null;
        if (localUser.getEmployee() != null) {
            localUser.getEmployee().setUser(localUser);
            employeeResponse = employeeMapper.toDTO(localUser.getEmployee());
        }

        CustomerResponse customerResponse = null;
        if (localUser.getCustomer() != null) {
            localUser.getCustomer().setUser(localUser);
            customerResponse = customerMapper.toDTO(localUser.getCustomer());
        }

        return new AuthResponse(identityAuth.getAccessToken(), employeeResponse, customerResponse);
    }

    private User syncUserWithIdentity(IdentityUserResult identityUser) {
        String username = identityUser.getUsername();
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User newUser = User.builder()
                    .username(username)
                    .isActive(true)
                    .build();
            return userRepository.save(newUser);
        });

        // Set roles in-memory for DTO mapping / downstream responses
        Set<RoleResponse> roleResponses = new HashSet<>();
        if (identityUser.getRoles() != null) {
            for (IdentityRoleResult idRole : identityUser.getRoles()) {
                roleResponses.add(RoleResponse.builder()
                        .id(idRole.getId())
                        .name(idRole.getName())
                        .code(idRole.getCode())
                        .build());
            }
        }
        user.setRoles(roleResponses);

        // Check if user is staff or admin and needs an Employee record
        boolean isStaffOrAdmin = isStaffOrAdminUser(identityUser, user);
        if (isStaffOrAdmin && user.getEmployee() == null) {
            Optional<Employee> existingEmp = employeeRepository.findByUser(user);
            if (existingEmp.isPresent()) {
                user.setEmployee(existingEmp.get());
            } else if ("admin1234".equalsIgnoreCase(username) || isUserAdmin(identityUser)) {
                // Link with default system admin employee if available
                Optional<Employee> adminEmp = employeeRepository.findByCode("EMP_ADMIN");
                if (adminEmp.isPresent()) {
                    Employee emp = adminEmp.get();
                    emp.setUser(user);
                    employeeRepository.save(emp);
                    user.setEmployee(emp);
                }
            } else {
                // Check if an unlinked employee was created earlier with matching email
                Optional<Employee> empByEmail = identityUser.getEmail() != null && !identityUser.getEmail().isBlank()
                        ? employeeRepository.findByEmail(identityUser.getEmail())
                        : Optional.empty();

                if (empByEmail.isPresent() && empByEmail.get().getUser() == null) {
                    Employee emp = empByEmail.get();
                    emp.setUser(user);
                    employeeRepository.save(emp);
                    user.setEmployee(emp);
                } else {
                    // Create employee profile for staff member with default matching Position
                    String fullName = identityUser.getFullName() != null && !identityUser.getFullName().isBlank()
                            ? identityUser.getFullName() : username;
                    Position defaultPosition = resolveDefaultPosition(identityUser);

                    Employee newEmp = Employee.builder()
                            .code("EMP_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")))
                            .name(fullName)
                            .email(identityUser.getEmail() != null ? identityUser.getEmail() : username + "@supermarket.local")
                            .phoneNumber(identityUser.getPhoneNumber() != null ? identityUser.getPhoneNumber() : "0900000000")
                            .gender("Other")
                            .position(defaultPosition)
                            .user(user)
                            .build();
                    newEmp = employeeRepository.save(newEmp);
                    user.setEmployee(newEmp);
                }
            }
        }

        // Check if customer
        boolean isCustomer = isCustomerUser(identityUser, user);
        if (isCustomer && user.getCustomer() == null) {
            Optional<Customer> existingCust = customerRepository.findByUser_Username(username);
            if (existingCust.isPresent()) {
                user.setCustomer(existingCust.get());
            } else {
                String fullName = identityUser.getFullName() != null && !identityUser.getFullName().isBlank()
                        ? identityUser.getFullName() : username;
                Customer newCust = Customer.builder()
                        .name(fullName)
                        .email(identityUser.getEmail() != null ? identityUser.getEmail() : username + "@customer.local")
                        .phoneNumber(identityUser.getPhoneNumber() != null ? identityUser.getPhoneNumber() : "0900000000")
                        .accumulatedPoints(0.0)
                        .user(user)
                        .build();
                newCust = customerRepository.save(newCust);
                user.setCustomer(newCust);
            }
        }

        return user;
    }

    private boolean isStaffOrAdminUser(IdentityUserResult identityUser, User localUser) {
        if (identityUser.getRoles() != null) {
            for (IdentityRoleResult r : identityUser.getRoles()) {
                String code = r.getCode();
                if ("SUPER_ADMIN".equalsIgnoreCase(code) || "ADMIN".equalsIgnoreCase(code)
                        || "STOREKEEPER".equalsIgnoreCase(code) || "SALESMAN".equalsIgnoreCase(code)) {
                    return true;
                }
            }
        }
        if (localUser.getRoles() != null) {
            return localUser.getRoles().stream().anyMatch(r ->
                    "ADMIN".equalsIgnoreCase(r.getCode())
                            || "STOREKEEPER".equalsIgnoreCase(r.getCode())
                            || "SALESMAN".equalsIgnoreCase(r.getCode()));
        }
        return false;
    }

    private boolean isUserAdmin(IdentityUserResult identityUser) {
        if (identityUser.getRoles() != null) {
            for (IdentityRoleResult r : identityUser.getRoles()) {
                if ("SUPER_ADMIN".equalsIgnoreCase(r.getCode()) || "ADMIN".equalsIgnoreCase(r.getCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isCustomerUser(IdentityUserResult identityUser, User localUser) {
        if (identityUser.getRoles() != null) {
            for (IdentityRoleResult r : identityUser.getRoles()) {
                if ("CUSTOMER".equalsIgnoreCase(r.getCode())) {
                    return true;
                }
            }
        }
        if (localUser.getRoles() != null) {
            return localUser.getRoles().stream().anyMatch(r -> "CUSTOMER".equalsIgnoreCase(r.getCode()));
        }
        return false;
    }

    private Position resolveDefaultPosition(IdentityUserResult identityUser) {
        if (identityUser.getRoles() != null) {
            for (IdentityRoleResult r : identityUser.getRoles()) {
                String code = r.getCode();
                if ("STOREKEEPER".equalsIgnoreCase(code)) {
                    return positionRepository.findById("pos-storekeeper-00000000000000001")
                            .orElseGet(() -> positionRepository.findAll().stream()
                                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains("kho"))
                                    .findFirst().orElse(null));
                } else if ("SALESMAN".equalsIgnoreCase(code)) {
                    return positionRepository.findById("pos-salesman-00000000000000000001")
                            .orElseGet(() -> positionRepository.findAll().stream()
                                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains("thu ngân"))
                                    .findFirst().orElse(null));
                } else if ("ADMIN".equalsIgnoreCase(code) || "SUPER_ADMIN".equalsIgnoreCase(code)) {
                    return positionRepository.findById("pos-manager-000000000000000000001")
                            .orElseGet(() -> positionRepository.findAll().stream()
                                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains("quản lý"))
                                    .findFirst().orElse(null));
                }
            }
        }
        return positionRepository.findAll().stream().findFirst().orElse(null);
    }

    @Override
    public boolean introspectToken(String token) {
        IdentityIntrospectResult result = identityClient.introspect(token);
        return result != null && result.isValid();
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        identityClient.logout(logoutRequest.getToken());
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        IdentityAuthResult result = identityClient.refreshToken(refreshTokenRequest.getToken());
        if (result != null && result.getAccessToken() != null) {
            return RefreshTokenResponse.builder()
                    .token(result.getAccessToken())
                    .build();
        }
        throw new AppException(ErrorCode.TOKEN_INVALID);
    }

    @Override
    public Map<String, Object> getClaimsToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> currentClaims = new HashMap<>();
        if (!(authentication instanceof AnonymousAuthenticationToken) && authentication != null) {
            if (authentication instanceof JwtAuthenticationToken jwtToken) {
                currentClaims.putAll(jwtToken.getToken().getClaims());
            } else {
                currentClaims.put("username", authentication.getName());
            }
        }
        return currentClaims;
    }

    @Override
    public Employee getCurrentEmployee() {
        Map<String, Object> claims = getClaimsToken();
        String username = null;
        if (claims.containsKey("username") && claims.get("username") != null) {
            username = claims.get("username").toString();
        }
        if (username == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                username = auth.getName();
            }
        }

        // 1. Find by username
        if (username != null && !username.isBlank()) {
            Optional<Employee> empByUsername = employeeRepository.findByUser_Username(username);
            if (empByUsername.isPresent()) {
                return empByUsername.get();
            }
        }

        // 2. Find by sub (employee ID or local user ID)
        if (claims.containsKey("sub") && claims.get("sub") != null) {
            String sub = claims.get("sub").toString();
            Optional<Employee> empById = employeeRepository.findById(sub);
            if (empById.isPresent()) {
                return empById.get();
            }
            Optional<Employee> empByUserId = employeeRepository.findByUser_Id(sub);
            if (empByUserId.isPresent()) {
                return empByUserId.get();
            }
        }

        // 3. Fallback to EMP_ADMIN if user is administrator
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = "admin1234".equalsIgnoreCase(username) || (auth != null && auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ROLE_SUPER_ADMIN")));

        if (isAdmin) {
            Optional<Employee> adminEmp = employeeRepository.findByCode("EMP_ADMIN");
            if (adminEmp.isPresent()) {
                return adminEmp.get();
            }
        }

        throw new AppException(ErrorCode.EMPLOYEE_NOT_EXIST);
    }

    @Override
    public Customer getCurrentCustomer() {
        Map<String, Object> claims = getClaimsToken();
        String username = null;
        if (claims.containsKey("username") && claims.get("username") != null) {
            username = claims.get("username").toString();
        }
        if (username == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                username = auth.getName();
            }
        }

        if (username != null && !username.isBlank()) {
            Optional<Customer> cust = customerRepository.findByUser_Username(username);
            if (cust.isPresent()) {
                return cust.get();
            }
        }

        if (claims.containsKey("sub") && claims.get("sub") != null) {
            String sub = claims.get("sub").toString();
            Optional<Customer> custById = customerRepository.findById(sub);
            if (custById.isPresent()) {
                return custById.get();
            }
            Optional<Customer> custByUserId = customerRepository.findByUser_Id(sub);
            if (custByUserId.isPresent()) {
                return custByUserId.get();
            }
        }

        throw new AppException(ErrorCode.CUSTOMER_NOT_EXIST);
    }

}
