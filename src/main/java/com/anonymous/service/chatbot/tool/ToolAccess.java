package com.anonymous.service.chatbot.tool;

import com.anonymous.constant.Permissions;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Reads the role list out of a Permissions expression, so the chatbot decides which tools to offer from the
// very same constant that @PreAuthorize enforces (no second list to keep in sync)
public final class ToolAccess {

    private static final Pattern ROLE = Pattern.compile("'([A-Z_]+)'");

    private ToolAccess() {
    }

    // null = any signed-in user
    public static Set<String> rolesOf(String permission) {
        if (Permissions.AUTHENTICATED.equals(permission)) {
            return null;
        }
        Set<String> roles = new LinkedHashSet<>();
        Matcher matcher = ROLE.matcher(permission);
        while (matcher.find()) {
            roles.add(matcher.group(1));
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Unsupported permission expression: " + permission);
        }
        return roles;
    }

    public static boolean allows(String permission, Collection<String> roleCodes) {
        Set<String> allowed = rolesOf(permission);
        return allowed == null || roleCodes.stream().anyMatch(allowed::contains);
    }

}
