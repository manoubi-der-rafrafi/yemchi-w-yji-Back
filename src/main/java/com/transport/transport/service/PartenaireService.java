package com.transport.transport.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.Partenaire;
import com.transport.transport.model.Commande;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.PartenaireRepository;

@Service
public class PartenaireService {

    private final PartenaireRepository partenaireRepository;
    private final CommandeRepository commandeRepository;
    private final PartnerApiKeyService partnerApiKeyService;

    public PartenaireService(
            PartenaireRepository partenaireRepository,
            CommandeRepository commandeRepository,
            PartnerApiKeyService partnerApiKeyService) {
        this.partenaireRepository = partenaireRepository;
        this.commandeRepository = commandeRepository;
        this.partnerApiKeyService = partnerApiKeyService;
    }

    public PartnerProvisioningResult createPartner(
            String externalBusinessId,
            String externalOwnerUserId,
            String businessName,
            String logoUrl,
            String phone,
            List<String> phoneNumbers,
            String email,
            String address,
            String facebookUrl,
            String instagramUrl,
            String tiktokUrl,
            Double latitude,
            Double longitude,
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
        appliquerProfil(partenaire, businessName, logoUrl, phone, phoneNumbers, email, address,
                facebookUrl, instagramUrl, tiktokUrl, latitude, longitude);
        partenaire.setStatut(Partenaire.Statut.ACTIF);
        partenaire = partenaireRepository.save(partenaire);

        var createdKey = partnerApiKeyService.createKey(partenaire, scopes);
        return new PartnerProvisioningResult(partenaire, createdKey.savedKey().getKeyPrefix(), createdKey.plainApiKey());
    }

    public Partenaire updatePartnerProfile(
            String externalBusinessId,
            String businessName,
            String logoUrl,
            String phone,
            List<String> phoneNumbers,
            String email,
            String address,
            String facebookUrl,
            String instagramUrl,
            String tiktokUrl,
            Double latitude,
            Double longitude) {
        if (externalBusinessId == null || externalBusinessId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalBusinessId obligatoire");
        }
        validateProfile(businessName, latitude, longitude);

        Partenaire partenaire = getRequiredByExternalBusinessId(externalBusinessId.trim());
        appliquerProfil(partenaire, businessName, logoUrl, phone, phoneNumbers, email, address,
                facebookUrl, instagramUrl, tiktokUrl, latitude, longitude);
        partenaire = partenaireRepository.save(partenaire);

        List<Commande> commandesActives = commandeRepository
                .findByExternalBusinessIdAndSourceCommandeAndStatutNotIn(
                        partenaire.getExternalBusinessId(),
                        Commande.SourceCommande.B2C,
                        List.of(Commande.Statut.livree, Commande.Statut.ANNULEE, Commande.Statut.annulee));
        for (Commande commande : commandesActives) {
            commande.setPartenaireNom(partenaire.getBusinessName());
            commande.setPartenaireLogoUrl(partenaire.getLogoUrl());
            commande.setNomDepart(partenaire.getBusinessName());
            commande.setTelDepart(partenaire.getPhone());
            commande.setLocalisationDepart(partenaire.getAddress());
            commande.setLatitudeDepart(partenaire.getLatitude());
            commande.setLongitudeDepart(partenaire.getLongitude());
        }
        if (!commandesActives.isEmpty()) {
            commandeRepository.saveAll(commandesActives);
        }
        return partenaire;
    }

    private void appliquerProfil(
            Partenaire partenaire,
            String businessName,
            String logoUrl,
            String phone,
            List<String> phoneNumbers,
            String email,
            String address,
            String facebookUrl,
            String instagramUrl,
            String tiktokUrl,
            Double latitude,
            Double longitude) {
        validateProfile(businessName, latitude, longitude);
        partenaire.setBusinessName(businessName.trim());
        partenaire.setLogoUrl(normalizeOptional(logoUrl));
        partenaire.setPhone(normalizeOptional(phone));
        partenaire.setPhoneNumbers(phoneNumbers == null
                ? List.of()
                : phoneNumbers.stream().map(this::normalizeOptional).filter(value -> value != null).distinct().toList());
        partenaire.setEmail(normalizeOptional(email));
        partenaire.setAddress(normalizeOptional(address));
        partenaire.setFacebookUrl(normalizeOptional(facebookUrl));
        partenaire.setInstagramUrl(normalizeOptional(instagramUrl));
        partenaire.setTiktokUrl(normalizeOptional(tiktokUrl));
        partenaire.setLatitude(latitude);
        partenaire.setLongitude(longitude);
    }

    private void validateProfile(String businessName, Double latitude, Double longitude) {
        if (businessName == null || businessName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "businessName obligatoire");
        }
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude et longitude obligatoires ensemble");
        }
        if (latitude != null && (latitude < -90 || latitude > 90)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude invalide");
        }
        if (longitude != null && (longitude < -180 || longitude > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Longitude invalide");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
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
