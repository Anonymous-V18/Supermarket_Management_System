package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCapabilitiesResponse {

    List<String> roleCodes;

    // Data questions this user may ask; the rest is refused
    List<ChatToolResponse> tools;

    List<String> suggestions;

    boolean canManageKnowledge;

    // false: answers are rule-based / verbatim only
    boolean llmEnabled;

}
