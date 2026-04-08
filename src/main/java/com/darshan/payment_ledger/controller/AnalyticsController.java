package com.darshan.payment_ledger.controller;

import com.darshan.payment_ledger.dto.AnalyticsResponse;
import com.darshan.payment_ledger.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // GET /api/v1/analytics
    // Server-side aggregation — no more fetching all rows to the browser
    @GetMapping
    public ResponseEntity<AnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(analyticsService.getAnalytics());
    }
}
