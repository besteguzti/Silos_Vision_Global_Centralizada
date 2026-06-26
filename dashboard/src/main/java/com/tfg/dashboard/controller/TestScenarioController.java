package com.tfg.dashboard.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.dashboard.dto.TestScenarioEvaluationResponse;
import com.tfg.dashboard.dto.TestScenarioRequest;
import com.tfg.dashboard.service.TestScenarioEvaluationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/test-scenarios")
public class TestScenarioController {

    private final TestScenarioEvaluationService evaluationService;

    public TestScenarioController(TestScenarioEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate")
    @ResponseStatus(HttpStatus.OK)
    public TestScenarioEvaluationResponse evaluate(
            @Valid @RequestBody TestScenarioRequest request
    ) {
        try {
            return evaluationService.evaluate(request);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}
