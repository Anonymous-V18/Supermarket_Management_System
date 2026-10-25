package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

// A knowledge-base passage an answer was built from; the text is read live from kb_chunk, not copied
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCitationResponse {

    // [n] in the answer text
    int index;

    String chunkId;

    String documentTitle;

    String headingPath;

    String snippet;

}
