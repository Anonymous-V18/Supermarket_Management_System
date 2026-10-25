package com.anonymous.service.chatbot.tool;

import com.anonymous.entity.Employee;
import com.anonymous.entity.Position;
import com.anonymous.entity.User;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserToolTest {

    private IEmployeeRepository employeeRepository;
    private IUserRepository userRepository;
    private CurrentUserTool tool;

    @BeforeEach
    void setUp() {
        employeeRepository = mock(IEmployeeRepository.class);
        userRepository = mock(IUserRepository.class);
        tool = new CurrentUserTool(employeeRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executeWithEmployeeUser() {
        Jwt jwt = new Jwt("tokenValue", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "HS512"),
                Map.of("sub", "emp-001", "username", "nhanvien1"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_SALESMAN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = User.builder().username("nhanvien1").build();
        Employee emp = Employee.builder()
                .code("NV00001")
                .name("Nguyễn Văn A")
                .phoneNumber("0901234567")
                .email("nva@supermarket.vn")
                .position(Position.builder().name("Thu ngân").build())
                .user(user)
                .build();

        when(employeeRepository.findById("emp-001")).thenReturn(Optional.of(emp));

        ToolAnswer answer = tool.execute(ToolArgs.empty());
        assertThat(answer.markdown()).contains("Nguyễn Văn A");
        assertThat(answer.markdown()).contains("NV00001");
        assertThat(answer.markdown()).contains("nhanvien1");
        assertThat(answer.markdown()).contains("Thu ngân");
        assertThat(answer.markdown()).contains("SALESMAN");
        assertThat(answer.link()).isEqualTo("/home/account");
    }

    @Test
    void executeWithAdminUserOnly() {
        Jwt jwt = new Jwt("tokenValue", Instant.now(), Instant.now().plusSeconds(3600),
                Map.of("alg", "HS512"),
                Map.of("sub", "usr-00001", "username", "admin"));
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = User.builder().username("admin").isActive(true).build();
        when(employeeRepository.findById("usr-00001")).thenReturn(Optional.empty());
        when(userRepository.findById("usr-00001")).thenReturn(Optional.of(user));

        ToolAnswer answer = tool.execute(ToolArgs.empty());
        assertThat(answer.markdown()).contains("admin");
        assertThat(answer.markdown()).contains("Đang hoạt động");
        assertThat(answer.markdown()).contains("ADMIN");
        assertThat(answer.link()).isEqualTo("/home/account");
    }

}
