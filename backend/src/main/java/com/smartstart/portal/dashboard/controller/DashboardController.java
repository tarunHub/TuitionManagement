package com.smartstart.portal.dashboard.controller;

import com.smartstart.portal.dashboard.dto.DashboardDtos.DashboardResponse;
import com.smartstart.portal.dashboard.dto.DashboardDtos.MonthlySummaryResponse;
import com.smartstart.portal.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Tag(name = "Dashboard")
@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @Operation(summary = "Dashboard cards for a month")
    @GetMapping
    public DashboardResponse dashboard(@Parameter(example = "2026-08") @RequestParam YearMonth month) {
        return service.dashboard(month);
    }

    @Operation(summary = "Billed / collected / outstanding for each month of a year, plus totals")
    @GetMapping("/monthly-summary")
    public MonthlySummaryResponse monthlySummary(@RequestParam @Min(2000) @Max(2100) int year) {
        return service.monthlySummary(year);
    }
}
