package com.tfg.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import com.tfg.dashboard.dto.report.MonthlyReportContextDto;
import com.tfg.dashboard.service.MonthlyReportContextService;

import jakarta.validation.constraints.NotBlank;

//Endpoint auxiliar para consultar datos históricos del informe mensual.
@RestController
@RequestMapping("/api/reports")
@Validated
public class MonthlyReportController {

    private final MonthlyReportContextService monthlyReportContextService;

    public MonthlyReportController(MonthlyReportContextService monthlyReportContextService) {
        this.monthlyReportContextService = monthlyReportContextService;
    }

    @GetMapping("/monthly-context")
    public MonthlyReportContextDto monthlyContext(
            @RequestParam(defaultValue = "LAST_30_DAYS") @NotBlank String period,
            @RequestParam(defaultValue = "false") boolean includeGenerated) {

        return monthlyReportContextService.buildMonthlyContext(period, includeGenerated);
    }
}
