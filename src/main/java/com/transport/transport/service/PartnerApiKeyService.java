package com.transport.transport.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.PartnerApiKey;
import com.transport.transport.model.Partenaire;
import com.transport.transport.repository.PartnerApiKeyRepository;
import com.transport.transport.repository.PartenaireRepository;
import com.transport.transport.security.PartnerPrincipal;

@Service
public class PartnerApiKeyService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789abcdefghijkmnopqrstuvwxyz";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PartnerApiKeyRepository partnerApiKeyRepository;
    private final PartenaireRepository partenaireRepository;
    private final PasswordEncoder passwordEncoder;

    public PartnerApiKeyService(
            PartnerApiKeyRepository partnerApiKeyRepository,
            PartenaireRepository partenaireRepository,
            PasswordEncoder passwordEncoder) {
        this.partnerApiKeyRepository = partnerApiKeyRepository;
        this.partenaireRepository = partenaireRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CreatedPartnerApiKey createKey(Partenaire partenaire, List<String> requestedScopes) {
        Set<String> scopes = normalizeScopes(requestedScopes);
        String prefix = "pk_live_" + randomString(10);
        String secret = randomString(32);
        String apiKey = prefix + "." + secret;

        PartnerApiKey saved = new PartnerApiKey();
        saved.setPartnerId(partenaire.getId());
        saved.setKeyPrefix(prefix);
        saved.setSecretHash(passwordEncoder.encode(secret));
        saved.setScopes(scopes);
        saved.setStatut(PartnerApiKey.Statut.ACTIF);
        partnerApiKeyRepository.save(saved);

        return new CreatedPartnerApiKey(saved, apiKey);
    }

    public PartnerPrincipal authenticate(String apiKey) {
        int separator = apiKey.indexOf('.');
        if (separator <= 0 || separator >= apiKey.length() - 1) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key invalide");
        }

        String prefix = apiKey.substring(0, separator);
        String secret = apiKey.substring(separator + 1);

        PartnerApiKey key = partnerApiKeyRepository.findByKeyPrefix(prefix)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key invalide"));

        if (key.getStatut() != PartnerApiKey.Statut.ACTIF) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key inactive");
        }
        if (key.getExpiresAt() != null && key.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key expiree");
        }
        if (!passwordEncoder.matches(secret, key.getSecretHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key invalide");
        }

        Partenaire partenaire = partenaireRepository.findById(key.getPartnerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Partenaire introuvable"));
        if (partenaire.getStatut() != Partenaire.Statut.ACTIF) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Partenaire inactif");
        }

        key.setLastUsedAt(LocalDateTime.now());
        partnerApiKeyRepository.save(key);

        return new PartnerPrincipal(
                partenaire.getId(),
                partenaire.getExternalBusinessId(),
                partenaire.getBusinessName(),
                key.getScopes(),
                key.getKeyPrefix());
    }

    public void requireScope(PartnerPrincipal principal, String scope) {
        if (principal == null || !principal.getScopes().contains(scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Scope partenaire manquant: " + scope);
        }
    }

    private Set<String> normalizeScopes(List<String> requestedScopes) {
        LinkedHashSet<String> scopes = new LinkedHashSet<>();
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            scopes.add("delivery:create");
            scopes.add("driver:read");
            scopes.add("tracking:read");
            return scopes;
        }
        for (String scope : requestedScopes) {
            if (scope != null && !scope.isBlank()) {
                scopes.add(scope.trim());
            }
        }
        if (scopes.isEmpty()) {
            scopes.add("delivery:create");
        }
        return scopes;
    }

    private String randomString(int size) {
        StringBuilder builder = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    public record CreatedPartnerApiKey(PartnerApiKey savedKey, String plainApiKey) {}
}
