package com.transport.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.model.Ami;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AmiService;
import com.transport.transport.service.AuthorizationService;

@RestController
@RequestMapping("/api/amis")
public class AmiController {

    private final AmiService amiService;
    private final AuthorizationService authorizationService;

    public AmiController(AmiService amiService, AuthorizationService authorizationService) {
        this.amiService = amiService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/inviter")
    public ResponseEntity<?> inviter(@RequestBody Map<String, String> payload, Authentication authentication) {
        String demandeurId = payload.get("utilisateur_demandeur");
        String recepteurId  = payload.get("utilisateur_recepteur");
        if (demandeurId == null || recepteurId == null) {
            return ResponseEntity.badRequest().body("IDs manquants");
        }
        authorizationService.requireSelfOrAdmin(demandeurId, authentication);
        try {
            Ami ami = new Ami();
            ami.setDemandeurId(demandeurId);
            ami.setRecepteurId(recepteurId);
            return ResponseEntity.ok(amiService.envoyerInvitation(ami));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    // GET /api/amis/{userId} → raw accepted relations (List<Ami>)
    @GetMapping("/{userId}")
    public List<Ami> listerRelationsAcceptees(@PathVariable String userId, Authentication authentication) {
        authorizationService.requireSelfOrAdmin(userId, authentication);
        return amiService.listerRelationsAcceptees(userId);
    }

    // GET /api/amis/{userId}/liste → friends as Utilisateur objects
    @GetMapping("/{userId}/liste")
    public List<Utilisateur> listerAmisUtilisateur(@PathVariable String userId, Authentication authentication) {
        authorizationService.requireSelfOrAdmin(userId, authentication);
        return amiService.listerAmisUtilisateur(userId);
    }

    // GET /api/amis/status?u1=...&u2=...
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam String u1,
                                                      @RequestParam String u2,
                                                      Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        if (!authorizationService.isAdmin(current)
            && !current.getId().equals(u1)
            && !current.getId().equals(u2)) {
            return ResponseEntity.status(403).body(Map.of("error", "Acces refuse"));
        }
        return ResponseEntity.ok(amiService.getRelationStatus(u1, u2));
    }

    // GET /api/amis/invitations/recues/demandeurs?userId=...
    @GetMapping("/invitations/recues/demandeurs")
    public ResponseEntity<List<Utilisateur>> getReceivedSenders(@RequestParam String userId, Authentication authentication) {
        authorizationService.requireSelfOrAdmin(userId, authentication);
        return ResponseEntity.ok(amiService.getSendersOfReceivedPendingInvitations(userId));
    }

    // PUT /api/amis/accepter?demandeurId=7&recepteurId=5
    @PutMapping("/accepter")
    public ResponseEntity<Ami> accepter(@RequestParam String demandeurId,
                                        @RequestParam String recepteurId,
                                        Authentication authentication) {
        authorizationService.requireSelfOrAdmin(recepteurId, authentication);
        return ResponseEntity.ok(amiService.accepterInvitationByUsers(demandeurId, recepteurId));
    }

    // PUT /api/amis/refuser?demandeurId=7&recepteurId=5
    @PutMapping("/refuser")
    public ResponseEntity<Ami> refuser(@RequestParam String demandeurId,
                                       @RequestParam String recepteurId,
                                       Authentication authentication) {
        authorizationService.requireSelfOrAdmin(recepteurId, authentication);
        return ResponseEntity.ok(amiService.refuserInvitationByUsers(demandeurId, recepteurId));
    }
    @GetMapping("/search/mes-amis/numero")
    public ResponseEntity<List<Utilisateur>> searchMyFriendsByNumero(
            @RequestParam("me") String meId,
            @RequestParam("q") String numero,
            Authentication authentication
    ) {
        authorizationService.requireSelfOrAdmin(meId, authentication);
        List<Utilisateur> res = amiService.searchMyFriendsByNumero(meId, numero);
        return ResponseEntity.ok(res); // renvoie [] si vide (200)
    }
    @GetMapping("/search/mes-amis/email")
    public ResponseEntity<List<Utilisateur>> searchMyFriendsByEmail(
            @RequestParam("me") String meId,
            @RequestParam("q")  String emailQuery,
            @RequestParam(value = "exact", defaultValue = "false") boolean exact,
            Authentication authentication
    ) {
        authorizationService.requireSelfOrAdmin(meId, authentication);
        return ResponseEntity.ok(amiService.searchMyFriendsByEmail(meId, emailQuery, exact));
    }
    @GetMapping("/search/mes-amis/nom")
  public ResponseEntity<List<Utilisateur>> searchMesAmisByNom(
      @RequestParam("me") String me,
      @RequestParam("q") String q,
      Authentication authentication
  ) {
    authorizationService.requireSelfOrAdmin(me, authentication);
    return ResponseEntity.ok(amiService.searchMesAmisByNom(me, q));
  }

  // /api/amis/search/mes-amis/prenom?me=ID&q=partiePrenom
  @GetMapping("/search/mes-amis/prenom")
  public ResponseEntity<List<Utilisateur>> searchMesAmisByPrenom(
      @RequestParam("me") String me,
      @RequestParam("q") String q,
      Authentication authentication
  ) {
    authorizationService.requireSelfOrAdmin(me, authentication);
    return ResponseEntity.ok(amiService.searchMesAmisByPrenom(me, q));
  }

  // /api/amis/search/mes-amis/nom-prenom?me=ID&q=terme   (NOM OU PRENOM)
  @GetMapping("/search/mes-amis/nom-prenom")
  public ResponseEntity<List<Utilisateur>> searchMesAmisByNomOrPrenom(
      @RequestParam("me") String me,
      @RequestParam("q") String q,
      Authentication authentication
  ) {
    authorizationService.requireSelfOrAdmin(me, authentication);
    return ResponseEntity.ok(amiService.searchMesAmisByNomOrPrenom(me, q));
  }

  // (Optionnel) /api/amis/search/mes-amis/nom-et-prenom?me=ID&nom=...&prenom=...
  @GetMapping("/search/mes-amis/nom-et-prenom")
  public ResponseEntity<List<Utilisateur>> searchMesAmisByNomAndPrenom(
      @RequestParam("me") String me,
      @RequestParam("nom") String nom,
      @RequestParam("prenom") String prenom,
      Authentication authentication
  ) {
    authorizationService.requireSelfOrAdmin(me, authentication);
    return ResponseEntity.ok(amiService.searchMesAmisByNomAndPrenom(me, nom, prenom));
  }

}
