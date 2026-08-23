package com.transport.transport.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.dto.LivreurDebtDetailsResponse;
import com.transport.transport.service.LivreurDebtService;

@RestController
@RequestMapping("/api/internal/finance-livreurs")
public class InternalFinanceLivreurController {
    private final LivreurDebtService livreurDebtService;
    private final String internalSecret;

    public InternalFinanceLivreurController(
            LivreurDebtService livreurDebtService,
            @Value("${app.internal.provisioning-secret}") String internalSecret) {
        this.livreurDebtService = livreurDebtService;
        this.internalSecret = internalSecret;
    }

    @GetMapping("/{livreurId}/details")
    public ResponseEntity<LivreurDebtDetailsResponse> details(
            @PathVariable String livreurId,
            @RequestHeader(value = "X-Internal-Secret", required = false) String providedSecret) {
        if (internalSecret == null || internalSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "API interne indisponible");
        }
        if (providedSecret == null || !MessageDigest.isEqual(
                internalSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Secret interne invalide");
        }
        return ResponseEntity.ok(livreurDebtService.details(livreurId));
    }
}
