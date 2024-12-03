package com.anonymous.service;

import com.anonymous.dto.response.StatusInvoiceResponse;

import java.util.List;

public interface IStatusInvoiceService {

    List<StatusInvoiceResponse> getAll();

}
