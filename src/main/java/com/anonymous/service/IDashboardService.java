package com.anonymous.service;

import com.anonymous.dto.response.DashboardSummaryResponse;

import java.time.LocalDate;

public interface IDashboardService {

    DashboardSummaryResponse getSummary(LocalDate date, String shiftCode);

}
