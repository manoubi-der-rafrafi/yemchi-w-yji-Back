package com.transport.transport.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.dto.StatutFinancierLivreurResponse;
import com.transport.transport.dto.LivreurDebtDetailsResponse;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.FinanceLivreurService;
import com.transport.transport.service.LivreurDebtService;

@RestController
@RequestMapping("/api/finance-livreurs")
public class FinanceLivreurController {
    private final FinanceLivreurService financeLivreurService;
    private final AuthorizationService authorizationService;
    private final LivreurDebtService livreurDebtService;

    public FinanceLivreurController(
            FinanceLivreurService financeLivreurService,
            LivreurDebtService livreurDebtService,
            AuthorizationService authorizationService) {
        this.financeLivreurService = financeLivreurService;
        this.livreurDebtService = livreurDebtService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/me/details")
    public ResponseEntity<LivreurDebtDetailsResponse> mesDetails(Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        authorizationService.requireSelfOrAdmin(current.getId(), authentication);
        return ResponseEntity.ok(livreurDebtService.details(current.getId()));
    }

    @GetMapping("/me")
    public ResponseEntity<StatutFinancierLivreurResponse> monStatut(Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        authorizationService.requireSelfOrAdmin(current.getId(), authentication);
        return ResponseEntity.ok(financeLivreurService.calculerEtSynchroniser(current.getId()));
    }

    @GetMapping("/{livreurId}")
    public ResponseEntity<StatutFinancierLivreurResponse> statut(
            @PathVariable String livreurId,
            Authentication authentication) {
        authorizationService.requireSelfOrAdmin(livreurId, authentication);
        return ResponseEntity.ok(financeLivreurService.calculerEtSynchroniser(livreurId));
    }

    @GetMapping("/{livreurId}/details")
    public ResponseEntity<LivreurDebtDetailsResponse> details(
            @PathVariable String livreurId,
            Authentication authentication) {
        authorizationService.requireSelfOrAdmin(livreurId, authentication);
        return ResponseEntity.ok(livreurDebtService.details(livreurId));
    }
}
