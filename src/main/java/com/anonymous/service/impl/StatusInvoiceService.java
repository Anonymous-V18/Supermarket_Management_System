package com.anonymous.service.impl;

import com.anonymous.converter.IStatusInvoiceMapper;
import com.anonymous.dto.response.StatusInvoiceResponse;
import com.anonymous.repository.IStatusInvoiceRepository;
import com.anonymous.service.IStatusInvoiceService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StatusInvoiceService implements IStatusInvoiceService {

    IStatusInvoiceRepository statusInvoiceRepository;
    IStatusInvoiceMapper statusInvoiceMapper;

    @Override
    public List<StatusInvoiceResponse> getAll() {
        return statusInvoiceRepository.findAll().stream().map(statusInvoiceMapper::toDTO).toList();
    }
}
