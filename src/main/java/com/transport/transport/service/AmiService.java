package com.transport.transport.service;

import com.transport.transport.model.Ami;
import com.transport.transport.model.StatutAmi;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.AmiRepository;
import com.transport.transport.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AmiService {

    private final AmiRepository amiRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AmiService(AmiRepository amiRepository, UtilisateurRepository utilisateurRepository) {
        this.amiRepository = amiRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /* ----------------------- Invitations CRUD ----------------------- */

    public Ami envoyerInvitation(Ami ami) {
        // Optionally validate existence of both users
        utilisateurRepository.findById(ami.getDemandeurId())
                .orElseThrow(() -> new RuntimeException("Demandeur introuvable"));
        utilisateurRepository.findById(ami.getRecepteurId())
                .orElseThrow(() -> new RuntimeException("Recepteur introuvable"));

        // Prevent duplicate pending invitation
        if (amiRepository.existsByDemandeurIdAndRecepteurIdAndStatut(
                ami.getDemandeurId(), ami.getRecepteurId(), StatutAmi.EN_ATTENTE)) {
            throw new RuntimeException("Invitation déjà envoyée");
        }
        ami.setStatut(StatutAmi.EN_ATTENTE);
        return amiRepository.save(ami);
    }

    public Ami accepterInvitationByUsers(String demandeurId, String recepteurId) {
        Ami ami = amiRepository
                .findByDemandeurIdAndRecepteurIdAndStatut(demandeurId, recepteurId, StatutAmi.EN_ATTENTE)
                .orElseThrow(() -> new RuntimeException("Aucune invitation EN_ATTENTE pour ces utilisateurs"));
        ami.setStatut(StatutAmi.ACCEPTE);
        return amiRepository.save(ami);
    }

    public Ami refuserInvitationByUsers(String demandeurId, String recepteurId) {
        Ami ami = amiRepository
                .findByDemandeurIdAndRecepteurIdAndStatut(demandeurId, recepteurId, StatutAmi.EN_ATTENTE)
                .orElseThrow(() -> new RuntimeException("Aucune invitation EN_ATTENTE pour ces utilisateurs"));
        ami.setStatut(StatutAmi.REFUSE);
        return amiRepository.save(ami);
    }

    /* ----------------------- Queries on relations ----------------------- */

    // If you want raw Ami relations (accepted) for a user:
    public List<Ami> listerRelationsAcceptees(String userId) {
        return amiRepository.findAcceptedRelationsOf(userId);
    }

    // If you want the user's friends as Utilisateur objects (accepted only):
    public List<Utilisateur> listerAmisUtilisateur(String userId) {
        List<Ami> relations = amiRepository.findAcceptedRelationsOf(userId);

        // IDs des amis (type String maintenant)
        Set<String> friendIds = relations.stream()
                .map(a -> otherSideId(a, userId))   // doit retourner String
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return friendIds.isEmpty()
                ? List.of()
                : utilisateurRepository.findAllById(friendIds);
    }


    // Senders of pending invitations RECEIVED by this user:
    public List<Utilisateur> getSendersOfReceivedPendingInvitations(String userId) {
        var pending = amiRepository.findPendingInvitationsForUser(userId, StatutAmi.EN_ATTENTE);

        Set<String> senderIds = pending.stream()
                .map(Ami::getDemandeurId)   // renvoie déjà un String
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return senderIds.isEmpty()
                ? List.of()
                : utilisateurRepository.findAllById(senderIds);
    }


    // Relation status between u1 and u2:
    public Map<String, Object> getRelationStatus(String  u1, String  u2) {
        var list = amiRepository.findAnyBetween(u1, u2);
        if (list == null || list.isEmpty()) {
            return Map.of("status", "NONE");
        }
        Ami a = list.get(0); // or sort by date if needed
        return switch (a.getStatut()) {
            case EN_ATTENTE -> {
                boolean sent = Objects.equals(a.getDemandeurId(), u1);
                yield Map.of("status", sent ? "PENDING_SENT" : "PENDING_RECEIVED",
                        "invitationId", a.getId());
            }
            case ACCEPTE -> Map.of("status", "ACCEPTED", "invitationId", a.getId());
            case REFUSE  -> Map.of("status", "REFUSED", "invitationId", a.getId());
        };
    }

    /* ----------------------- Helpers ----------------------- */

    private String  otherSideId(Ami a, String  me) {
        if (Objects.equals(a.getDemandeurId(), me)) return a.getRecepteurId();
        if (Objects.equals(a.getRecepteurId(), me)) return a.getDemandeurId();
        return null;
    }
}
