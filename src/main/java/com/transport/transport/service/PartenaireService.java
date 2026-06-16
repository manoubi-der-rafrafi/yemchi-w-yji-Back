package com.transport.transport.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.Partenaire;
import com.transport.transport.repository.PartenaireRepository;

@Service
public class PartenaireService {

    private final PartenaireRepository partenaireRepository;
    private final PartnerApiKeyService partnerApiKeyService;

    public PartenaireService(
            PartenaireRepository partenaireRepository,
            PartnerApiKeyService partnerApiKeyService) {
        this.partenaireRepository = partenaireRepository;
        this.partnerApiKeyService = partnerApiKeyService;
    }

    public PartnerProvisioningResult createPartner(
            String externalBusinessId,
            String externalOwnerUserId,
            String businessName,
            List<String> scopes) {
        if (externalBusinessId == null || externalBusinessId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalBusinessId obligatoire");
        }
        if (businessName == null || businessName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessName obligatoire");
        }
        partenaireRepository.findByExternalBusinessId(externalBusinessId)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Partenaire deja existant");
                });

        Partenaire partenaire = new Partenaire();
        partenaire.setExternalBusinessId(externalBusinessId.trim());
        partenaire.setExternalOwnerUserId(externalOwnerUserId);
        partenaire.setBusinessName(businessName.trim());
        partenaire.setStatut(Partenaire.Statut.ACTIF);
        partenaire = partenaireRepository.save(partenaire);

        var createdKey = partnerApiKeyService.createKey(partenaire, scopes);
        return new PartnerProvisioningResult(partenaire, createdKey.savedKey().getKeyPrefix(), createdKey.plainApiKey());
    }

    public Partenaire getRequiredById(String partnerId) {
        return partenaireRepository.findById(partnerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Partenaire introuvable"));
    }

    public Partenaire getRequiredByExternalBusinessId(String externalBusinessId) {
        return partenaireRepository.findByExternalBusinessId(externalBusinessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partenaire introuvable"));
    }

    public record PartnerProvisioningResult(
            Partenaire partenaire,
            String keyPrefix,
            String plainApiKey) {}
}
