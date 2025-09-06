package com.transport.transport.service;

import com.transport.transport.model.Ami;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.model.StatutAmi;
import com.transport.transport.repository.AmiRepository;
import com.transport.transport.repository.UtilisateurRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AmiService {

    private final AmiRepository amiRepository;
    private final UtilisateurRepository utilisateurRepository;
    @Autowired
    public AmiService(AmiRepository amiRepository , UtilisateurRepository utilisateurRepository) {
        this.amiRepository = amiRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public Ami envoyerInvitation(Ami ami) {
        return amiRepository.save(ami);
    }

    public List<Ami> listerAmis(Utilisateur user) {
        return amiRepository.findByDemandeurOrRecepteurAndStatut(user, user, StatutAmi.ACCEPTE);
    }

    public Ami accepterInvitation(Ami ami) {
        ami.setStatut(StatutAmi.ACCEPTE);
        return amiRepository.save(ami);
    }

    public Ami refuserInvitation(Ami ami) {
        ami.setStatut(StatutAmi.REFUSE);
        return amiRepository.save(ami);
    }
    public List<Utilisateur> listerAmisUtilisateur(Long userId) {
        return amiRepository.findAcceptedFriendsOf(userId);
    }
    public Ami creerDemandeAmi(Integer demandeurId, Integer recepteurId) {
        Utilisateur demandeur = utilisateurRepository.findById(demandeurId)
                .orElseThrow(() -> new RuntimeException("Demandeur introuvable"));
        Utilisateur recepteur = utilisateurRepository.findById(recepteurId)
                .orElseThrow(() -> new RuntimeException("Recepteur introuvable"));

        // Vérifie si une demande en attente existe déjà
        if (amiRepository.existsByDemandeurIdAndRecepteurIdAndStatut(demandeurId, recepteurId, StatutAmi.EN_ATTENTE)) {
            throw new RuntimeException("Invitation déjà envoyée");
        }

        Ami ami = new Ami();
        ami.setDemandeur(demandeur);
        ami.setRecepteur(recepteur);
        ami.setStatut(StatutAmi.EN_ATTENTE);

        return amiRepository.save(ami);
    }
    public Map<String, Object> getRelationStatus(Integer u1, Integer u2) {
        var list = amiRepository.findAnyBetween(u1, u2);
        if (list == null || list.isEmpty()) {
            return Map.of("status", "NONE"); // aucune relation trouvée
        }

        Ami a = list.get(0); // on prend la première (ou la plus récente si tu as un champ createdAt)

        if (a.getStatut() == StatutAmi.EN_ATTENTE) {
            boolean sent = a.getDemandeur().getId().equals(u1);
            return Map.of(
                    "status", sent ? "PENDING_SENT" : "PENDING_RECEIVED",
                    "invitationId", a.getId()
            );
        }
        if (a.getStatut() == StatutAmi.ACCEPTE) {
            return Map.of("status", "ACCEPTED", "invitationId", a.getId());
        }
        if (a.getStatut() == StatutAmi.REFUSE) {
            return Map.of("status", "REFUSED", "invitationId", a.getId());
        }
        return Map.of("status", "UNKNOWN");
    }
    public List<Utilisateur> getSendersOfReceivedPendingInvitations(Integer userId) {
        return amiRepository.findPendingInvitationSenders(userId, StatutAmi.EN_ATTENTE);
    }
    @Transactional
    public Ami accepterInvitationByUsers(Integer demandeurId, Integer recepteurId) {
        Ami ami = amiRepository
                .findByDemandeurIdAndRecepteurIdAndStatut(demandeurId, recepteurId, StatutAmi.EN_ATTENTE)
                .orElseThrow(() -> new RuntimeException("Aucune invitation EN_ATTENTE pour ces utilisateurs"));

        ami.setStatut(StatutAmi.ACCEPTE);
        return amiRepository.save(ami);
    }

    @Transactional
    public Ami refuserInvitationByUsers(Integer demandeurId, Integer recepteurId) {
        Ami ami = amiRepository
                .findByDemandeurIdAndRecepteurIdAndStatut(demandeurId, recepteurId, StatutAmi.EN_ATTENTE)
                .orElseThrow(() -> new RuntimeException("Aucune invitation EN_ATTENTE pour ces utilisateurs"));

        ami.setStatut(StatutAmi.REFUSE);
        return amiRepository.save(ami);
    }

}
