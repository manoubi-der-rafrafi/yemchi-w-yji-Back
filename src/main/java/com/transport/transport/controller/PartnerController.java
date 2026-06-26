package com.transport.transport.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.dto.partner.PartnerCommandeResponse;
import com.transport.transport.dto.partner.PartnerCreateCommandeRequest;
import com.transport.transport.dto.partner.PartnerTrackingResponse;
import com.transport.transport.security.PartnerPrincipal;
import com.transport.transport.service.PartnerApiKeyService;
import com.transport.transport.service.PartnerCommandeService;
import com.transport.transport.service.PartnerOrderNotificationService;

@RestController
@RequestMapping("/api/partner/commandes")
public class PartnerController {

    private final PartnerCommandeService partnerCommandeService;
    private final PartnerApiKeyService partnerApiKeyService;
    private final PartnerOrderNotificationService partnerOrderNotificationService;

    public PartnerController(
            PartnerCommandeService partnerCommandeService,
            PartnerApiKeyService partnerApiKeyService,
            PartnerOrderNotificationService partnerOrderNotificationService) {
        this.partnerCommandeService = partnerCommandeService;
        this.partnerApiKeyService = partnerApiKeyService;
        this.partnerOrderNotificationService = partnerOrderNotificationService;
    }

    @PostMapping
    public ResponseEntity<PartnerCommandeResponse> createConfirmedCommande(
            @RequestBody PartnerCreateCommandeRequest request,
            Authentication authentication) {
        PartnerPrincipal principal = requirePartner(authentication);
        partnerApiKeyService.requireScope(principal, "delivery:create");
        try {
            PartnerCommandeResponse response = partnerCommandeService.createConfirmedCommande(principal, request);
            partnerOrderNotificationService.notifySuccess(principal, request, response);
            return ResponseEntity.ok(response);
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            partnerOrderNotificationService.notifyFailure(principal, request, exception);
            throw exception;
        } catch (RuntimeException exception) {
            partnerOrderNotificationService.notifyFailure(principal, request, exception);
            throw exception;
        }
    }

    @GetMapping("/external/{externalOrderId}/transporteur")
    public ResponseEntity<?> getTransporteurByExternalOrderId(
            @PathVariable String externalOrderId,
            Authentication authentication) {
        PartnerPrincipal principal = requirePartner(authentication);
        partnerApiKeyService.requireScope(principal, "driver:read");
        return ResponseEntity.ok(partnerCommandeService.getTransporteurByExternalOrderId(principal, externalOrderId));
    }

    @GetMapping("/external/{externalOrderId}/tracking")
    public ResponseEntity<PartnerTrackingResponse> getTrackingByExternalOrderId(
            @PathVariable String externalOrderId,
            Authentication authentication) {
        PartnerPrincipal principal = requirePartner(authentication);
        partnerApiKeyService.requireScope(principal, "tracking:read");
        return ResponseEntity.ok(partnerCommandeService.getTrackingByExternalOrderId(principal, externalOrderId));
    }

    private PartnerPrincipal requirePartner(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof PartnerPrincipal principal)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Authentification partenaire requise");
        }
        return principal;
    }
}
