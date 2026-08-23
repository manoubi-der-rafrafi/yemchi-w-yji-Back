package com.transport.transport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudinary.Cloudinary;
import com.transport.transport.model.Facture;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.FactureService;

class FactureControllerTest {
  private AuthorizationService authorization;
  private FactureService factureService;
  private FactureController controller;

  @BeforeEach
  void setUp() {
    authorization = mock(AuthorizationService.class);
    factureService = mock(FactureService.class);
    controller = new FactureController(mock(Cloudinary.class), authorization);
    ReflectionTestUtils.setField(controller, "factureService", factureService);
    when(factureService.createFacture(any())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void transporteurCannotForgeBeneficiaryTypeConfirmationOrMongoId() {
    Authentication authentication = mock(Authentication.class);
    Utilisateur transporteur = new Utilisateur();
    transporteur.setId("livreur-connecte");
    when(authorization.currentUser(authentication)).thenReturn(transporteur);
    when(authorization.isAdmin(transporteur)).thenReturn(false);

    Facture facture = new Facture();
    facture.setId("facture-existante");
    facture.setIdLivreur("autre-livreur");
    facture.setMontant(new BigDecimal("75.50"));
    facture.setType(Facture.FactureType.ENTREPRISE_VERSE_LIVREUR);
    facture.setConfirmer(Facture.ConfirmationStatut.ACCEPTER);

    Facture saved = controller.createFacture(facture, authentication);

    assertNull(saved.getId());
    assertEquals("livreur-connecte", saved.getIdLivreur());
    assertEquals(Facture.FactureType.LIVREUR_VERSE_ENTREPRISE, saved.getType());
    assertEquals(Facture.ConfirmationStatut.NON_TRAITER, saved.getConfirmer());
  }
}
