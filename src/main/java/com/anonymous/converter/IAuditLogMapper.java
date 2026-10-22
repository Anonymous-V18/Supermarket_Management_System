package com.anonymous.converter;

import com.anonymous.dto.response.AuditLogResponse;
import com.anonymous.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper
public interface IAuditLogMapper {

    AuditLogResponse toDTO(AuditLog entity);

}
