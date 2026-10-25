package com.anonymous.service.chatbot.tool;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ToolRegistry {

    private final Map<String, ChatTool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<ChatTool> tools) {
        tools.forEach(tool -> this.tools.put(tool.name(), tool));
    }

    public Optional<ChatTool> find(String name) {
        return Optional.ofNullable(name).map(tools::get);
    }

    public List<ChatTool> all() {
        return List.copyOf(tools.values());
    }

    // Tools the roles may call: only these are suggested, matched by the router or offered to the planner
    public List<ChatTool> allowedFor(Collection<String> roleCodes) {
        return tools.values().stream().filter(tool -> ToolAccess.allows(tool.permission(), roleCodes)).toList();
    }

}
