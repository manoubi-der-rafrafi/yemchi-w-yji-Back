package com.transport.transport.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.transport.transport.model.Ami;
import com.transport.transport.model.StatutAmi;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.AmiRepository;
import com.transport.transport.repository.UtilisateurRepository;

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
    
    public List<Utilisateur> searchMyFriendsByNumero(String meId, String numero) {
        if (numero == null) numero = "";
        numero = numero.replaceAll("\\D", "");        // garde que les chiffres
        if (numero.length() < 2) return List.of();    // on évite les recherches trop larges

        // 1) récupérer toutes les relations ACCEPTÉES du user
        List<Ami> relations = amiRepository.findAcceptedRelationsOf(meId);

        // 2) extraire uniquement l'ID de l'ami (l'autre côté de la relation)
        List<String> friendIds = relations.stream()
                .map(a -> {
                    if (meId.equals(a.getDemandeurId())) return a.getRecepteurId();
                    if (meId.equals(a.getRecepteurId())) return a.getDemandeurId();
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (friendIds.isEmpty()) return List.of();

        // 3) construire la regex (commence par). Pour "exact", mets '^' + numero + '$'
        String regex = "^" + numero;

        // 4) chercher les Utilisateur dont _id ∈ friendIds ET telephone ~ regex
        return utilisateurRepository.findByIdInAndTelephoneRegex(friendIds, regex);
    }
    /**
     * Recherche par e-mail parmi les amis ACCEPTÉS de meId.
     * @param meId   id Mongo de l'utilisateur courant
     * @param q      terme de recherche (email partiel ou complet)
     * @param exact  si true => match exact (ignore case), sinon "contient" (ignore case)
     */
    public List<Utilisateur> searchMyFriendsByEmail(String meId, String q, boolean exact) {
        if (q == null || q.isBlank()) return List.of();

        // 1) récupérer toutes les relations ACCEPTÉES du user
        var relations = amiRepository.findAcceptedRelationsOf(meId);

        // 2) extraire l'ID de l'ami (autre côté)
        var friendIds = relations.stream()
                .map(a -> {
                    if (meId.equals(a.getDemandeurId())) return a.getRecepteurId();
                    if (meId.equals(a.getRecepteurId())) return a.getDemandeurId();
                    return null;
                })
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (friendIds.isEmpty()) return List.of();

        // 3) requête utilisateurs parmi ces IDs
        if (exact) {
            return utilisateurRepository
                    .findFirstByIdInAndEmailIgnoreCase(friendIds, q)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            return utilisateurRepository.findByIdInAndEmailContainingIgnoreCase(friendIds, q);
        }
    }
    private List<String> getMesAmisIds(String me) {
    List<Ami> liens = amiRepository.findAcceptedRelationsOf(me);
    Set<String> ids = new LinkedHashSet<>();
    for (Ami a : liens) {
        if (Objects.equals(a.getDemandeurId(), me)) {
            ids.add(a.getRecepteurId());
        } else if (Objects.equals(a.getRecepteurId(), me)) {
            ids.add(a.getDemandeurId());
        }
    }
    return new ArrayList<>(ids);
}


  // Recherche sur NOM (dans mes amis)
  public List<Utilisateur> searchMesAmisByNom(String me, String q) {
    List<String> ids = getMesAmisIds(me);
    if (ids.isEmpty()) return Collections.emptyList();
    return utilisateurRepository.findByIdInAndNomContainingIgnoreCase(ids, q);
  }

  // Recherche sur PRENOM (dans mes amis)
  public List<Utilisateur> searchMesAmisByPrenom(String me, String q) {
    List<String> ids = getMesAmisIds(me);
    if (ids.isEmpty()) return Collections.emptyList();
    return utilisateurRepository.findByIdInAndPrenomContainingIgnoreCase(ids, q);
  }

  // Recherche sur NOM OU PRENOM (un seul champ "q")
  public List<Utilisateur> searchMesAmisByNomOrPrenom(String me, String q) {
    List<String> ids = getMesAmisIds(me);
    if (ids.isEmpty()) return Collections.emptyList();
    return utilisateurRepository
        .findByIdInAndNomContainingIgnoreCaseOrIdInAndPrenomContainingIgnoreCase(ids, q, ids, q);
    // (ou la version regex Mongo) utilisateurRepository.searchByIdsAndNomOrPrenomRegex(ids, q);
  }

  // Recherche sur NOM ET PRENOM (deux champs)
  public List<Utilisateur> searchMesAmisByNomAndPrenom(String me, String nom, String prenom) {
    List<String> ids = getMesAmisIds(me);
    if (ids.isEmpty()) return Collections.emptyList();
    return utilisateurRepository
        .findByIdInAndNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(ids, nom, prenom);
    // (ou la version regex Mongo) utilisateurRepository.searchByIdsAndNomAndPrenomRegex(ids, nom, prenom);
  }
}
