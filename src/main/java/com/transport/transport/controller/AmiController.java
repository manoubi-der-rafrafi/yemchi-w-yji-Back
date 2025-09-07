package com.transport.transport.controller;

import com.transport.transport.model.Ami;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AmiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/amis")
public class AmiController {

    private final AmiService amiService;

    public AmiController(AmiService amiService) {
        this.amiService = amiService;
    }

    @PostMapping("/inviter")
    public ResponseEntity<?> inviter(@RequestBody Map<String, String> payload) {
        String demandeurId = payload.get("utilisateur_demandeur");
        String recepteurId  = payload.get("utilisateur_recepteur");
        if (demandeurId == null || recepteurId == null) {
            return ResponseEntity.badRequest().body("IDs manquants");
        }
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
    public List<Ami> listerRelationsAcceptees(@PathVariable String userId) {
        return amiService.listerRelationsAcceptees(userId);
    }

    // GET /api/amis/{userId}/liste → friends as Utilisateur objects
    @GetMapping("/{userId}/liste")
    public List<Utilisateur> listerAmisUtilisateur(@PathVariable String userId) {
        return amiService.listerAmisUtilisateur(userId);
    }

    // GET /api/amis/status?u1=...&u2=...
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam String u1,
                                                      @RequestParam String u2) {
        return ResponseEntity.ok(amiService.getRelationStatus(u1, u2));
    }

    // GET /api/amis/invitations/recues/demandeurs?userId=...
    @GetMapping("/invitations/recues/demandeurs")
    public ResponseEntity<List<Utilisateur>> getReceivedSenders(@RequestParam String userId) {
        return ResponseEntity.ok(amiService.getSendersOfReceivedPendingInvitations(userId));
    }

    // PUT /api/amis/accepter?demandeurId=7&recepteurId=5
    @PutMapping("/accepter")
    public ResponseEntity<Ami> accepter(@RequestParam String demandeurId,
                                        @RequestParam String recepteurId) {
        return ResponseEntity.ok(amiService.accepterInvitationByUsers(demandeurId, recepteurId));
    }

    // PUT /api/amis/refuser?demandeurId=7&recepteurId=5
    @PutMapping("/refuser")
    public ResponseEntity<Ami> refuser(@RequestParam String demandeurId,
                                       @RequestParam String recepteurId) {
        return ResponseEntity.ok(amiService.refuserInvitationByUsers(demandeurId, recepteurId));
    }
}
