package com.sentinela.controller;

import com.sentinela.dto.RiskResponse;
import com.sentinela.dto.TransactionRequest;
import com.sentinela.service.RiskAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/risk")
public class RiskController {

    private final RiskAnalysisService service;

    public RiskController(RiskAnalysisService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public RiskResponse analyze(@Valid @RequestBody TransactionRequest request) {
        return service.analyze(request);
    }
}
