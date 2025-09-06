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
    public ResponseEntity<?> inviter(@RequestBody Map<String, Integer> payload) {
        Integer demandeurId = payload.get("utilisateur_demandeur");
        Integer recepteurId = payload.get("utilisateur_recepteur");

        if (demandeurId == null || recepteurId == null) {
            return ResponseEntity.badRequest().body("IDs manquants");
        }

        try {
            Ami ami = amiService.creerDemandeAmi(demandeurId, recepteurId);
            return ResponseEntity.ok(ami);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{userId}")
    public List<Ami> listerAmis(@PathVariable Utilisateur userId) {
        return amiService.listerAmis(userId);
    }




    @GetMapping("/{userId}/liste")
    public List<Utilisateur> listerAmis(@PathVariable Long userId) {
        return amiService.listerAmisUtilisateur(userId);
    }
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam Integer u1,
                                                      @RequestParam Integer u2) {
        return ResponseEntity.ok(amiService.getRelationStatus(u1, u2));
    }
    @GetMapping("/invitations/recues/demandeurs")
    public ResponseEntity<List<Utilisateur>> getReceivedSenders(@RequestParam Integer userId) {
        return ResponseEntity.ok(amiService.getSendersOfReceivedPendingInvitations(userId));
    }
    // PUT /api/amis/accepter?demandeurId=7&recepteurId=5
    @PutMapping("/accepter")
    public ResponseEntity<Ami> accepter(@RequestParam Integer demandeurId,
                                        @RequestParam Integer recepteurId) {
        return ResponseEntity.ok(amiService.accepterInvitationByUsers(demandeurId, recepteurId));
    }

    // PUT /api/amis/refuser?demandeurId=7&recepteurId=5
    @PutMapping("/refuser")
    public ResponseEntity<Ami> refuser(@RequestParam Integer demandeurId,
                                       @RequestParam Integer recepteurId) {
        return ResponseEntity.ok(amiService.refuserInvitationByUsers(demandeurId, recepteurId));
    }
}
