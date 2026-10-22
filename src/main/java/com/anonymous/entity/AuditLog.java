package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

// One row per write request or sign-in; append-only, request bodies are never stored
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog extends AbstractEntity {

    // CREATE, UPDATE, DELETE, LOGIN, LOGOUT
    @Column(name = "action", nullable = false, length = 20)
    String action;

    // First path segment, e.g. "invoices"
    @Column(name = "resource", nullable = false, length = 50)
    String resource;

    @Column(name = "http_method", nullable = false, length = 10)
    String httpMethod;

    @Column(name = "path", nullable = false)
    String path;

    // Document code / id the request touched, when known
    @Column(name = "reference")
    String reference;

    @Column(name = "username")
    String username;

    @Column(name = "ip_address", length = 45)
    String ipAddress;

    @Column(name = "success", nullable = false)
    Boolean success;

    @Column(name = "status_code", nullable = false)
    Integer statusCode;

    @Column(name = "error_code")
    Integer errorCode;

}
