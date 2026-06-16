package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.transport.transport.model.PartnerApiKey;
import com.transport.transport.model.Partenaire;
import com.transport.transport.repository.PartnerApiKeyRepository;
import com.transport.transport.repository.PartenaireRepository;

class PartnerApiKeyServiceTest {

    private PartnerApiKeyRepository partnerApiKeyRepository;
    private PartenaireRepository partenaireRepository;
    private PartnerApiKeyService service;

    @BeforeEach
    void setUp() {
        partnerApiKeyRepository = org.mockito.Mockito.mock(PartnerApiKeyRepository.class);
        partenaireRepository = org.mockito.Mockito.mock(PartenaireRepository.class);
        service = new PartnerApiKeyService(
                partnerApiKeyRepository,
                partenaireRepository,
                new BCryptPasswordEncoder());
    }

    @Test
    void createThenAuthenticateApiKey() {
        Partenaire partenaire = new Partenaire();
        partenaire.setId("partner-1");
        partenaire.setExternalBusinessId("12");
        partenaire.setBusinessName("Boutique Test");
        partenaire.setStatut(Partenaire.Statut.ACTIF);

        when(partnerApiKeyRepository.save(any(PartnerApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(partenaireRepository.findById("partner-1")).thenReturn(Optional.of(partenaire));

        var created = service.createKey(partenaire, List.of("delivery:create", "tracking:read"));
        assertNotNull(created.plainApiKey());
        assertTrue(created.plainApiKey().startsWith("pk_live_"));

        when(partnerApiKeyRepository.findByKeyPrefix(created.savedKey().getKeyPrefix()))
                .thenReturn(Optional.of(created.savedKey()));

        var principal = service.authenticate(created.plainApiKey());
        assertEquals("partner-1", principal.getPartnerId());
        assertEquals("12", principal.getExternalBusinessId());
        assertTrue(principal.getScopes().contains("delivery:create"));
    }
}
