package com.transport.transport.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.transport.transport.service.AiRequestRateLimiter;
import com.transport.transport.service.N8nAnalysisService;

@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {
    private final N8nAnalysisService analysis;
    private final AiRequestRateLimiter rateLimiter;
    private final int imageLimitPerMinute;
    private final int orderLimitPerMinute;

    public AiAnalysisController(
            N8nAnalysisService analysis,
            AiRequestRateLimiter rateLimiter,
            @Value("${app.n8n.image-limit-per-minute:10}") int imageLimitPerMinute,
            @Value("${app.n8n.order-limit-per-minute:20}") int orderLimitPerMinute) {
        this.analysis = analysis;
        this.rateLimiter = rateLimiter;
        this.imageLimitPerMinute = imageLimitPerMinute;
        this.orderLimitPerMinute = orderLimitPerMinute;
    }

    @PostMapping("/analyze-image")
    public JsonNode analyzeImage(@RequestBody ImageAnalysisRequest request, Authentication authentication) {
        rateLimiter.check("image", authentication.getName(), imageLimitPerMinute);
        return analysis.analyzeImage(request == null ? null : request.image());
    }

    @PostMapping("/analyze-order")
    public JsonNode analyzeOrder(@RequestBody OrderAnalysisRequest request, Authentication authentication) {
        rateLimiter.check("order", authentication.getName(), orderLimitPerMinute);
        return analysis.analyzeOrder(request == null ? null : request.prompt());
    }

    public record ImageAnalysisRequest(String image) {}
    public record OrderAnalysisRequest(String prompt) {}
}
