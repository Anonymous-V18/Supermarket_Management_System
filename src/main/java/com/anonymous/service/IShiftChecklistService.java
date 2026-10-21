package com.anonymous.service;

import com.anonymous.dto.request.ShiftChecklistRequest;
import com.anonymous.dto.response.ShiftChecklistResponse;

import java.time.LocalDate;

public interface IShiftChecklistService {

    ShiftChecklistResponse getChecklist(LocalDate date, String shiftCode, String requestedUsername);

    ShiftChecklistResponse saveOrUpdateChecklist(ShiftChecklistRequest request, String requestedUsername);

}
