package com.anonymous.configuration.audit;

import com.anonymous.entity.AuditLog;
import com.anonymous.service.IAuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Set;

// Writes one audit_log row for every write request (POST/PUT/PATCH/DELETE) and every sign-in / sign-out,
// successful or not. Bodies are never stored: only who, what, which document and the outcome.
// Requests rejected before reaching a controller (no or invalid token) are not recorded here.
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditInterceptor implements HandlerInterceptor {

    static final String ATTR_USERNAME = AuditInterceptor.class.getName() + ".username";
    static final String ATTR_REQUEST_REFERENCE = AuditInterceptor.class.getName() + ".requestReference";
    static final String ATTR_RESULT_REFERENCE = AuditInterceptor.class.getName() + ".resultReference";
    static final String ATTR_ERROR_CODE = AuditInterceptor.class.getName() + ".errorCode";

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    // Token housekeeping, not user actions
    private static final Set<String> IGNORED_PATHS = Set.of("/auth/refresh-token", "/auth/introspect-token");

    private final IAuditLogService auditLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        String path = request.getServletPath();
        String action = actionOf(request.getMethod(), path);
        if (action == null) {
            return;
        }
        try {
            Object errorCode = request.getAttribute(ATTR_ERROR_CODE);
            auditLogService.record(AuditLog.builder()
                    .action(action)
                    .resource(limit(resourceOf(path), 50))
                    .httpMethod(request.getMethod())
                    .path(limit(path, 255))
                    .reference(limit(referenceOf(request), 255))
                    .username(limit(usernameOf(request), 255))
                    .ipAddress(limit(request.getRemoteAddr(), 45))
                    .success(ex == null && response.getStatus() < 400)
                    .statusCode(response.getStatus())
                    .errorCode(errorCode instanceof Integer code ? code : null)
                    .build());
        } catch (RuntimeException e) {
            // Auditing must never break the request that was already answered
            log.error("Could not write audit log for {} {}", request.getMethod(), path, e);
        }
    }

    static String actionOf(String method, String path) {
        if (IGNORED_PATHS.contains(path)) {
            return null;
        }
        if ("/auth/login".equals(path)) {
            return "LOGIN";
        }
        if ("/auth/logout".equals(path)) {
            return "LOGOUT";
        }
        if (!WRITE_METHODS.contains(method)) {
            return null;
        }
        return switch (method) {
            case "POST" -> "CREATE";
            case "DELETE" -> "DELETE";
            default -> "UPDATE";
        };
    }

    static String resourceOf(String path) {
        String[] parts = path.split("/");
        return parts.length > 1 && !parts[1].isBlank() ? parts[1] : "/";
    }

    // Code of the saved document, else the {id} in the path, else the ids sent for deletion
    private static String referenceOf(HttpServletRequest request) {
        Object fromResult = request.getAttribute(ATTR_RESULT_REFERENCE);
        if (fromResult != null) {
            return fromResult.toString();
        }
        if (request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> variables
                && variables.get("id") != null) {
            return variables.get("id").toString();
        }
        Object fromBody = request.getAttribute(ATTR_REQUEST_REFERENCE);
        return fromBody != null ? fromBody.toString() : null;
    }

    // Sign-in attempts carry the typed username; everything else uses the token's username claim
    private static String usernameOf(HttpServletRequest request) {
        Object typed = request.getAttribute(ATTR_USERNAME);
        if (typed != null) {
            return typed.toString();
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
            String username = jwt.getToken().getClaimAsString("username");
            return username != null ? username : jwt.getName();
        }
        return null;
    }

    private static String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

}
