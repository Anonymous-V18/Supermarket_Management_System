package com.anonymous.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public abstract class AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false, nullable = false)
    String createdBy;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    Instant createdDate;

    @LastModifiedBy
    @Column(name = "modified_by", insertable = false)
    String modifiedBy;

    @LastModifiedDate
    @Column(name = "modified_date", insertable = false)
    Instant modifiedDate;

}
