package com.anonymous.service;

import com.anonymous.dto.request.SalesReturnInsertRequest;
import com.anonymous.dto.request.SalesReturnRequest;
import com.anonymous.dto.response.PageResponse;
import com.anonymous.dto.response.ReturnableLineResponse;
import com.anonymous.dto.response.SalesReturnResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ISalesReturnService {

    SalesReturnResponse insert(SalesReturnInsertRequest request);

    SalesReturnResponse update(String id, SalesReturnRequest request);

    void delete(String[] ids);

    PageResponse<SalesReturnResponse> search(String keyword, String statusCode, Pageable pageable);

    SalesReturnResponse getById(String id);

    List<ReturnableLineResponse> getReturnableLines(String invoiceId);

}
