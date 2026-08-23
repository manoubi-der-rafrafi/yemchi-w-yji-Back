package com.transport.transport.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.model.Commande;
import com.transport.transport.model.StatutAmi;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.AmiRepository;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class AuthorizationService {

  private final UtilisateurRepository utilisateurRepository;
  private final AmiRepository amiRepository;
  private final CommandeRepository commandeRepository;

  public AuthorizationService(
      UtilisateurRepository utilisateurRepository,
      AmiRepository amiRepository,
      CommandeRepository commandeRepository) {
    this.utilisateurRepository = utilisateurRepository;
    this.amiRepository = amiRepository;
    this.commandeRepository = commandeRepository;
  }

  public Utilisateur currentUser(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
    }

    String principal = authentication.getName();
    Utilisateur user = utilisateurRepository.findByEmailIgnoreCase(principal)
        .or(() -> utilisateurRepository.findById(principal))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur authentifie introuvable"));
    if (user.getStatut() == Utilisateur.Statut.banni) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte banni");
    }
    return user;
  }

  public boolean isAdmin(Utilisateur user) {
    return user != null && user.getRole() == Utilisateur.Role.admin;
  }

  public boolean isTransporteur(Utilisateur user) {
    return user != null && user.getRole() == Utilisateur.Role.transporteur;
  }

  public void requireSelfOrAdmin(String targetUserId, Authentication authentication) {
    Utilisateur current = currentUser(authentication);
    if (!isAdmin(current) && !current.getId().equals(targetUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
    }
  }

  public void requireAdmin(Authentication authentication) {
    Utilisateur current = currentUser(authentication);
    if (!isAdmin(current)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces admin requis");
    }
  }

  public void requireCommandeAccess(Commande commande, Authentication authentication) {
    Utilisateur current = currentUser(authentication);
    if (canAccessCommande(current, commande)) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
  }

  /** Autorise uniquement le client proprietaire de la commande ou un admin. */
  public void requireCommandeOwnerOrAdmin(Commande commande, Authentication authentication) {
    Utilisateur current = currentUser(authentication);
    if (isAdmin(current) || current.getId().equals(commande.getClientId())) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces reserve au client proprietaire");
  }

  /** Autorise uniquement un transporteur assigne a la commande ou un admin. */
  public void requireAssignedTransporteurOrAdmin(Commande commande, Authentication authentication) {
    Utilisateur current = currentUser(authentication);
    boolean assignedTransporteur = isTransporteur(current)
        && (current.getId().equals(commande.getTransporteurId())
            || current.getId().equals(commande.getTransporteurSecoursId()));
    if (isAdmin(current) || assignedTransporteur) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces reserve au transporteur assigne");
  }

  public void requireCommandeProductsReadAccess(Commande commande, Authentication authentication) {
    Utilisateur current = currentUser(authentication);
    if (canAccessCommande(current, commande)) {
      return;
    }
    boolean commandeDisponiblePourTransporteur = isTransporteur(current)
        && commande.getTransporteurId() == null
        && commande.getTransporteurSecoursId() == null
        && commande.getStatut() == Commande.Statut.confirmer;
    if (commandeDisponiblePourTransporteur) {
      return;
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces refuse");
  }

  public boolean canAccessCommande(Utilisateur current, Commande commande) {
    if (current == null || commande == null) {
      return false;
    }
    if (isAdmin(current)) {
      return true;
    }
    String currentId = current.getId();
    return currentId.equals(commande.getClientId())
        || currentId.equals(commande.getTransporteurId())
        || currentId.equals(commande.getTransporteurSecoursId())
        || currentId.equals(commande.getIdAmie());
  }

  public void requireTransporteurOrAdmin(Authentication authentication) {
    Utilisateur current = currentUser(authentication);
    if (!isAdmin(current) && !isTransporteur(current)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acces transporteur requis");
    }
  }

  public boolean canAccessUserData(Utilisateur current, String targetUserId) {
    if (current == null || targetUserId == null || targetUserId.isBlank()) {
      return false;
    }
    if (isAdmin(current) || current.getId().equals(targetUserId)) {
      return true;
    }
    boolean acceptedFriend = amiRepository.findAnyBetween(current.getId(), targetUserId)
        .stream()
        .anyMatch(a -> a.getStatut() == StatutAmi.ACCEPTE);
    if (acceptedFriend) {
      return true;
    }
    return commandeRepository.findAll()
        .stream()
        .anyMatch(c -> canAccessCommande(current, c) && participates(targetUserId, c));
  }

  public Set<String> filterAllowedUserIds(Authentication authentication, List<String> requestedIds) {
    Utilisateur current = currentUser(authentication);
    if (requestedIds == null || requestedIds.isEmpty()) {
      return Set.of();
    }
    if (isAdmin(current)) {
      return requestedIds.stream().filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet());
    }
    return requestedIds.stream()
        .filter(id -> canAccessUserData(current, id))
        .collect(Collectors.toSet());
  }

  private boolean participates(String userId, Commande commande) {
    return userId.equals(commande.getClientId())
        || userId.equals(commande.getTransporteurId())
        || userId.equals(commande.getTransporteurSecoursId())
        || userId.equals(commande.getIdAmie());
  }
}
