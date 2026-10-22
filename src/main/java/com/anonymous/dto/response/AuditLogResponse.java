package com.anonymous.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {

    String id;

    Instant createdDate;

    String action;

    String resource;

    String httpMethod;

    String path;

    String reference;

    String username;

    String ipAddress;

    Boolean success;

    Integer statusCode;

    Integer errorCode;

}
