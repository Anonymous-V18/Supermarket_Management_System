package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KbDocumentResponse {

    String id;

    String title;

    String sourceType;

    String sourceName;

    Integer chunkCount;

    // PENDING, INDEXED, FAILED
    String indexStatus;

    String indexError;

    Set<String> roleCodes;

    String createdBy;

    Instant createdDate;

    Instant modifiedDate;

}
