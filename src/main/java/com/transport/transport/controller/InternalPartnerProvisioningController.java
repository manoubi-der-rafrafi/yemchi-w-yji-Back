package com.transport.transport.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.dto.partner.PartnerCreateRequest;
import com.transport.transport.dto.partner.PartnerProvisionResponse;
import com.transport.transport.dto.partner.PartnerProfileRequest;
import com.transport.transport.model.Partenaire;
import com.transport.transport.service.PartenaireService;

@RestController
@RequestMapping("/api/internal/partners")
public class InternalPartnerProvisioningController {

    private final PartenaireService partenaireService;
    private final String internalProvisioningSecret;

    public InternalPartnerProvisioningController(
            PartenaireService partenaireService,
            @Value("${app.internal.provisioning-secret}") String internalProvisioningSecret) {
        this.partenaireService = partenaireService;
        this.internalProvisioningSecret = internalProvisioningSecret;
    }

    @PostMapping("/provision")
    public ResponseEntity<PartnerProvisionResponse> provisionPartner(
            @RequestHeader(value = "X-Internal-Secret", required = false) String providedSecret,
            @RequestBody PartnerCreateRequest request) {
        requireProvisioningSecret(providedSecret);

        var result = partenaireService.createPartner(
                request.externalBusinessId(),
                request.externalOwnerUserId(),
                request.businessName(),
                request.logoUrl(),
                request.phone(),
                request.phoneNumbers(),
                request.email(),
                request.address(),
                request.facebookUrl(),
                request.instagramUrl(),
                request.tiktokUrl(),
                request.latitude(),
                request.longitude(),
                request.scopes());

        return ResponseEntity.ok(new PartnerProvisionResponse(
                result.partenaire().getId(),
                result.partenaire().getExternalBusinessId(),
                result.partenaire().getBusinessName(),
                result.keyPrefix(),
                result.plainApiKey()));
    }

    @PatchMapping("/{externalBusinessId}")
    public ResponseEntity<Partenaire> updatePartnerProfile(
            @RequestHeader(value = "X-Internal-Secret", required = false) String providedSecret,
            @PathVariable String externalBusinessId,
            @RequestBody PartnerProfileRequest request) {
        requireProvisioningSecret(providedSecret);
        return ResponseEntity.ok(partenaireService.updatePartnerProfile(
                externalBusinessId,
                request.businessName(),
                request.logoUrl(),
                request.phone(),
                request.phoneNumbers(),
                request.email(),
                request.address(),
                request.facebookUrl(),
                request.instagramUrl(),
                request.tiktokUrl(),
                request.latitude(),
                request.longitude()));
    }

    private void requireProvisioningSecret(String providedSecret) {
        if (internalProvisioningSecret == null || internalProvisioningSecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Provisioning interne indisponible");
        }

        if (providedSecret == null || !MessageDigest.isEqual(
                internalProvisioningSecret.getBytes(StandardCharsets.UTF_8),
                providedSecret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Secret interne invalide");
        }
    }
}
