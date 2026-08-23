package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

class N8nAnalysisServiceTest {

    @Test
    void rejectsImageHostsOutsideTheAllowList() {
        N8nAnalysisService service = serviceWithoutWebhooks();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.analyzeImage("https://attacker.example/image.jpg"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void failsClosedWhenAWebhookHasNoHeaderSecret() {
        assertThrows(IllegalStateException.class, () -> new N8nAnalysisService(
                new ObjectMapper(),
                "https://example.n8n.cloud/webhook/image",
                "",
                "",
                5_000,
                "res.cloudinary.com",
                "localhost,127.0.0.1,workflow"));
    }

    @Test
    void reportsUnavailableWhenTheWebhookIsNotConfigured() {
        N8nAnalysisService service = serviceWithoutWebhooks();

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> service.analyzeImage("https://res.cloudinary.com/demo/image.jpg"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
    }

    private N8nAnalysisService serviceWithoutWebhooks() {
        return new N8nAnalysisService(
                new ObjectMapper(), "", "", "", 5_000, "res.cloudinary.com", "localhost,127.0.0.1,workflow");
    }
}
