package com.anonymous.service;

import com.anonymous.dto.request.ChecklistItemInsertRequest;
import com.anonymous.dto.request.ChecklistItemRequest;
import com.anonymous.dto.response.ChecklistItemResponse;

import java.util.List;

public interface IChecklistItemService {

    List<ChecklistItemResponse> getAll();

    ChecklistItemResponse insert(ChecklistItemInsertRequest request);

    ChecklistItemResponse update(String id, ChecklistItemRequest request);

}
