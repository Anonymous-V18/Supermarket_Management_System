package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessageResponse {

    String id;

    // USER or ASSISTANT
    String role;

    String content;

    String intent;

    List<ChatCitationResponse> citations;

    // Screen that shows the same data (tool answers)
    String link;

    String linkLabel;

    Integer latencyMs;

    Integer feedback;

    Instant createdDate;

}
