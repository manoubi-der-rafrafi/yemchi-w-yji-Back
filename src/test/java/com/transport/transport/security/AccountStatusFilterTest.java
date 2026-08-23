package com.transport.transport.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;

class AccountStatusFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void blocksBannedAccountEvenWithAValidJwt() throws Exception {
        UtilisateurRepository users = mock(UtilisateurRepository.class);
        Utilisateur banned = new Utilisateur();
        banned.setId("banned-1");
        banned.setStatut(Utilisateur.Statut.banni);
        when(users.findById("banned-1")).thenReturn(Optional.of(banned));
        authenticate("banned-1");

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AccountStatusFilter(users).doFilter(
                new MockHttpServletRequest(), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void letsActiveAccountContinue() throws Exception {
        UtilisateurRepository users = mock(UtilisateurRepository.class);
        Utilisateur active = new Utilisateur();
        active.setId("active-1");
        active.setStatut(Utilisateur.Statut.actif);
        when(users.findById("active-1")).thenReturn(Optional.of(active));
        authenticate("active-1");

        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AccountStatusFilter(users).doFilter(
                new MockHttpServletRequest(), response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    private static void authenticate(String userId) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", userId)
                .claim("uid", userId)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
