package com.anonymous.configuration;

import com.anonymous.entity.Employee;
import com.anonymous.entity.Role;
import com.anonymous.entity.User;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IRoleRepository;
import com.anonymous.repository.IUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Order(1)
public class DatabaseInitializer implements ApplicationRunner {

    IUserRepository userRepository;
    IRoleRepository roleRepository;
    IEmployeeRepository employeeRepository;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${com.anonymous.username-admin:admin1234}")
    String adminUsername;

    @NonFinal
    @Value("${com.anonymous.admin-password:Admin@123456}")
    String adminPassword;

    private static final Map<String, String> SYSTEM_ROLES = Map.of(
            "ADMIN", "Administrator",
            "EMPLOYEE", "Employee",
            "STOREKEEPER", "Storekeeper",
            "SALESMAN", "Salesman",
            "ACCOUNTING_STAFF", "Accounting Staff",
            "CUSTOMER", "Customer"
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Starting database initialization for roles and default admin account...");

        // 1. Khởi tạo danh sách các vai trò (Roles) nếu chưa tồn tại
        SYSTEM_ROLES.forEach((code, name) -> {
            roleRepository.findByCode(code).orElseGet(() -> {
                Role newRole = Role.builder()
                        .code(code)
                        .name(name)
                        .build();
                log.info("Creating default role: [{}] - {}", code, name);
                return roleRepository.save(newRole);
            });
        });

        // 2. Khởi tạo tài khoản Quản trị viên (ADMIN) mặc định nếu chưa tồn tại
        User admin = userRepository.findByUsername(adminUsername).orElseGet(() -> {
            Role adminRole = roleRepository.findByCode("ADMIN")
                    .orElseThrow(() -> new IllegalStateException("ADMIN role not found after initialization!"));

            User newAdmin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .isActive(true)
                    .roles(Set.of(adminRole))
                    .build();

            newAdmin = userRepository.save(newAdmin);
            log.info(">>> Initialized default ADMIN account successfully with username: [{}]", adminUsername);
            return newAdmin;
        });

        // 3. Đảm bảo Admin có hồ sơ Employee liên kết (tránh NullPointerException khi đăng nhập/thao tác)
        if (admin.getEmployee() == null && employeeRepository.findByUser(admin).isEmpty()) {
            Employee adminEmployee = Employee.builder()
                    .code("EMP_ADMIN")
                    .name("System Administrator")
                    .email("admin@supermarket.com")
                    .phoneNumber("0999999999")
                    .gender("Nam")
                    .user(admin)
                    .build();

            employeeRepository.save(adminEmployee);
            admin.setEmployee(adminEmployee);
            log.info(">>> Initialized default Employee profile for ADMIN: [{}]", adminEmployee.getCode());
        } else {
            log.info("Admin account [{}] and Employee profile already exist. Skipping creation.", adminUsername);
        }
    }
}
