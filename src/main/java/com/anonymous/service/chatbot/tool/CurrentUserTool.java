package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;
import com.anonymous.entity.Employee;
import com.anonymous.entity.User;
import com.anonymous.repository.IEmployeeRepository;
import com.anonymous.repository.IUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.anonymous.service.chatbot.tool.ChatFormat.table;

// Information of the currently authenticated user / employee session
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CurrentUserTool implements ChatTool {

    IEmployeeRepository employeeRepository;
    IUserRepository userRepository;

    @Override
    public String name() {
        return "current_user";
    }

    @Override
    public String description() {
        return "Thông tin tài khoản, hồ sơ của người dùng hiện đang đăng nhập (tôi là ai, ai đang đăng nhập, thông tin của tôi, quyền của tôi)";
    }

    @Override
    public String permission() {
        return Permissions.AUTHENTICATED;
    }

    @Override
    public List<String> examples() {
        return List.of("Hiện tại ai đang đăng nhập vậy", "Tôi là ai", "Thông tin tài khoản của tôi");
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(Permissions.AUTHENTICATED)
    public ToolAnswer execute(ToolArgs args) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String subject = null;
        String usernameClaim = null;
        Set<String> authorities = new TreeSet<>();

        if (auth instanceof JwtAuthenticationToken jwt) {
            subject = jwt.getToken().getSubject();
            usernameClaim = (String) jwt.getToken().getClaims().get("username");
            auth.getAuthorities().forEach(a -> {
                String name = a.getAuthority();
                if (name.startsWith("ROLE_")) {
                    authorities.add(name.substring(5));
                } else {
                    authorities.add(name);
                }
            });
        } else if (auth != null) {
            subject = auth.getName();
            auth.getAuthorities().forEach(a -> authorities.add(a.getAuthority()));
        }

        Employee employee = null;
        User user = null;

        if (subject != null) {
            Optional<Employee> empOpt = employeeRepository.findById(subject);
            if (empOpt.isPresent()) {
                employee = empOpt.get();
                user = employee.getUser();
            } else {
                Optional<User> uOpt = userRepository.findById(subject);
                if (uOpt.isPresent()) {
                    user = uOpt.get();
                    employee = user.getEmployee();
                } else if (usernameClaim != null) {
                    uOpt = userRepository.findByUsername(usernameClaim);
                    if (uOpt.isPresent()) {
                        user = uOpt.get();
                        employee = user.getEmployee();
                    }
                }
            }
        }

        String rolesStr = authorities.isEmpty() ? "—" : String.join(", ", authorities);

        StringBuilder md = new StringBuilder();
        md.append("**Thông tin tài khoản đang đăng nhập:**\n\n");

        List<String> headers = List.of("Thông tin", "Chi tiết");
        List<List<String>> rows = new ArrayList<>();

        if (employee != null) {
            rows.add(List.of("Họ và tên", employee.getName() != null ? employee.getName() : "—"));
            rows.add(List.of("Mã nhân viên", employee.getCode() != null ? employee.getCode() : "—"));
            String uname = user != null && user.getUsername() != null ? user.getUsername() : (usernameClaim != null ? usernameClaim : "—");
            rows.add(List.of("Tên đăng nhập", uname));
            rows.add(List.of("Chức vụ", employee.getPosition() != null ? employee.getPosition().getName() : "—"));
            rows.add(List.of("Số điện thoại", employee.getPhoneNumber() != null ? employee.getPhoneNumber() : "—"));
            rows.add(List.of("Email", employee.getEmail() != null ? employee.getEmail() : "—"));
            rows.add(List.of("Quyền hạn / Vai trò", rolesStr));
        } else if (user != null) {
            rows.add(List.of("Tên đăng nhập", user.getUsername() != null ? user.getUsername() : (usernameClaim != null ? usernameClaim : "—")));
            rows.add(List.of("Trạng thái", Boolean.TRUE.equals(user.getIsActive()) ? "Đang hoạt động" : "—"));
            rows.add(List.of("Quyền hạn / Vai trò", rolesStr));
        } else {
            rows.add(List.of("Tài khoản", usernameClaim != null ? usernameClaim : (subject != null ? subject : "—")));
            rows.add(List.of("Quyền hạn / Vai trò", rolesStr));
        }

        md.append(table(headers, rows));
        return ToolAnswer.of(md.toString(), "/home/account", "Đến trang Thông tin tài khoản");
    }

}
