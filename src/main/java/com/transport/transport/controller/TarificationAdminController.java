package com.transport.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.model.MajorationTarif;
import com.transport.transport.model.TarificationVehicule;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.MajorationTarifAdminService;
import com.transport.transport.service.TarificationVehiculeAdminService;

@RestController
@RequestMapping("/api/admin/tarifications")
public class TarificationAdminController {
    private final TarificationVehiculeAdminService tarifService;
    private final MajorationTarifAdminService majorationService;
    private final AuthorizationService authorizationService;

    public TarificationAdminController(
            TarificationVehiculeAdminService tarifService,
            MajorationTarifAdminService majorationService,
            AuthorizationService authorizationService) {
        this.tarifService = tarifService;
        this.majorationService = majorationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/vehicules")
    public List<TarificationVehicule> listTarifs(Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return tarifService.list();
    }

    @PostMapping("/vehicules")
    public ResponseEntity<TarificationVehicule> activateTarif(
            @RequestBody TarificationVehicule tarif,
            Authentication authentication) {
        Utilisateur admin = authorizationService.currentUser(authentication);
        authorizationService.requireAdmin(authentication);
        return ResponseEntity.ok(tarifService.activate(tarif, admin.getId()));
    }

    @GetMapping("/majorations")
    public List<MajorationTarif> listMajorations(Authentication authentication) {
        authorizationService.requireAdmin(authentication);
        return majorationService.list();
    }

    @PostMapping("/majorations")
    public ResponseEntity<MajorationTarif> createMajoration(
            @RequestBody MajorationTarif majoration,
            Authentication authentication) {
        Utilisateur admin = authorizationService.currentUser(authentication);
        authorizationService.requireAdmin(authentication);
        return ResponseEntity.ok(majorationService.create(majoration, admin.getId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
