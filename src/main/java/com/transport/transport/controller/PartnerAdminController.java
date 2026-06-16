package com.transport.transport.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.dto.partner.PartnerCreateRequest;
import com.transport.transport.dto.partner.PartnerProvisionResponse;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.PartenaireService;

@RestController
@RequestMapping("/api/admin/partners")
public class PartnerAdminController {

    private final PartenaireService partenaireService;
    private final AuthorizationService authorizationService;

    public PartnerAdminController(
            PartenaireService partenaireService,
            AuthorizationService authorizationService) {
        this.partenaireService = partenaireService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<PartnerProvisionResponse> createPartner(
            @RequestBody PartnerCreateRequest request,
            Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        var result = partenaireService.createPartner(
                request.externalBusinessId(),
                request.externalOwnerUserId(),
                request.businessName(),
                request.scopes());
        return ResponseEntity.ok(new PartnerProvisionResponse(
                result.partenaire().getId(),
                result.partenaire().getExternalBusinessId(),
                result.partenaire().getBusinessName(),
                result.keyPrefix(),
                result.plainApiKey()));
    }
}
