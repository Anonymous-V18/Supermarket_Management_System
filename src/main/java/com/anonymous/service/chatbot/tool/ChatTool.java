package com.anonymous.service.chatbot.tool;

import java.util.List;

// A live-data question the chatbot answers by calling an existing service.
// Implementations put @PreAuthorize(permission()) on execute(), so a tool is exactly as restricted as the
// screen / API it reads from; permission() must be the same Permissions constant (checked by a test).
public interface ChatTool {

    // Stable id, e.g. "revenue"; also stored as the message intent
    String name();

    // One line, Vietnamese; shown to the planner and in the capability list
    String description();

    // A constant of com.anonymous.constant.Permissions
    String permission();

    // Example questions: suggestions in the UI and phrases the rule router matches against
    List<String> examples();

    ToolAnswer execute(ToolArgs args);

}
