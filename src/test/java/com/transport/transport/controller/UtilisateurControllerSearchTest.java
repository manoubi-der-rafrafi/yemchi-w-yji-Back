package com.transport.transport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudinary.Cloudinary;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.transport.transport.dto.UtilisateurSearchResponse;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.service.AuthTokenService;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.MailService;
import com.transport.transport.service.UtilisateurService;

class UtilisateurControllerSearchTest {

  private UtilisateurService utilisateurService;
  private AuthorizationService authorizationService;
  private UtilisateurController controller;
  private Authentication authentication;

  @BeforeEach
  void setUp() {
    utilisateurService = mock(UtilisateurService.class);
    authorizationService = mock(AuthorizationService.class);
    authentication = mock(Authentication.class);

    controller = new UtilisateurController(
        utilisateurService,
        mock(Cloudinary.class),
        mock(AuthenticationManager.class),
        mock(JwtEncoder.class),
        mock(JwtDecoder.class),
        mock(PasswordEncoder.class),
        mock(GoogleIdTokenVerifier.class),
        mock(MailService.class),
        authorizationService,
        mock(AuthTokenService.class),
        mock(AuthSessionController.class),
        mock(com.transport.transport.service.SignupVerificationService.class));

    when(authorizationService.currentUser(authentication)).thenReturn(user("requester"));
  }

  @Test
  void rechercheParEmailExigeUtilisateurConnecteEtRetourneProfilPublic() throws Exception {
    Utilisateur cible = user("target");
    cible.setMotDePasse("{bcrypt}secret");
    cible.setAdresse("adresse privee");
    cible.setImageCarteIdentiteFace("identite-privee.jpg");
    cible.setLatitude(36.8);
    cible.setLongitude(10.2);
    when(utilisateurService.chercherParEmail("target@example.com")).thenReturn(Optional.of(cible));

    var response = controller.chercherParEmail("target@example.com", authentication);

    verify(authorizationService).currentUser(authentication);
    assertEquals(200, response.getStatusCode().value());
    assertPublicResponse(response.getBody());
  }

  @Test
  void rechercheParNumeroExigeUtilisateurConnecteEtRetourneProfilPublic() throws Exception {
    Utilisateur cible = user("target");
    when(utilisateurService.chercherParNumero("12345678")).thenReturn(Optional.of(cible));

    var response = controller.chercherParNumero("12345678", authentication);

    verify(authorizationService).currentUser(authentication);
    assertEquals(200, response.getStatusCode().value());
    assertPublicResponse(response.getBody());
  }

  @Test
  void commandesDesTransporteursEnPanneSontRefuseesAuxClients() {
    doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces transporteur requis"))
        .when(authorizationService).requireTransporteurOrAdmin(authentication);

    assertThrows(
        ResponseStatusException.class,
        () -> controller.getTransporteursEnPanneAvecCommandes(authentication));

    verify(authorizationService).requireTransporteurOrAdmin(authentication);
    verifyNoInteractions(utilisateurService);
  }

  @Test
  void commandesDesTransporteursEnPanneRestentAccessiblesAuxTransporteurs() {
    when(authentication.getName()).thenReturn("transporteur@example.com");
    when(utilisateurService.getTransporteursEnPanneAvecCommandes("transporteur@example.com"))
        .thenReturn(List.of());

    var response = controller.getTransporteursEnPanneAvecCommandes(authentication);

    verify(authorizationService).requireTransporteurOrAdmin(authentication);
    verify(utilisateurService).getTransporteursEnPanneAvecCommandes("transporteur@example.com");
    assertEquals(200, response.getStatusCode().value());
    assertEquals(List.of(), response.getBody());
  }

  @Test
  void profilLieRetourneUniquementLeProfilPublic() throws Exception {
    Utilisateur requester = user("requester");
    Utilisateur cible = user("target");
    cible.setMotDePasse("{bcrypt}secret");
    cible.setAdresse("adresse privee");
    cible.setImageCarteIdentiteFace("identite-privee.jpg");
    cible.setLatitude(36.8);
    cible.setLongitude(10.2);
    UtilisateurRepository repository = mock(UtilisateurRepository.class);
    ReflectionTestUtils.setField(controller, "utilisateurRepository", repository);
    when(authorizationService.currentUser(authentication)).thenReturn(requester);
    when(authorizationService.canAccessUserData(requester, "target")).thenReturn(true);
    when(authorizationService.isAdmin(requester)).thenReturn(false);
    when(repository.findById("target")).thenReturn(Optional.of(cible));

    var response = controller.getUtilisateurById("target", authentication);

    assertEquals(200, response.getStatusCode().value());
    assertPublicResponse((UtilisateurSearchResponse) response.getBody());
  }

  private void assertPublicResponse(UtilisateurSearchResponse response) throws Exception {
    assertNotNull(response);
    assertEquals("target", response.id());
    assertEquals("Target", response.nom());
    assertEquals("target@example.com", response.email());
    assertEquals("12345678", response.telephone());
    assertEquals(Utilisateur.Role.client, response.role());

    String json = new ObjectMapper().writeValueAsString(response);
    assertFalse(json.contains("motDePasse"));
    assertFalse(json.contains("adresse"));
    assertFalse(json.contains("imageCarteIdentite"));
    assertFalse(json.contains("latitude"));
    assertFalse(json.contains("longitude"));
  }

  private Utilisateur user(String id) {
    Utilisateur user = new Utilisateur();
    user.setId(id);
    user.setNom("Target");
    user.setPrenom("Client");
    user.setEmail(id + "@example.com");
    user.setTelephone("12345678");
    user.setImage("profile.jpg");
    user.setRole(Utilisateur.Role.client);
    return user;
  }
}
