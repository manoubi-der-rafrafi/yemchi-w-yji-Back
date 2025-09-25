package com.transport.transport.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class UtilisateurService {

  private static final Logger log = LoggerFactory.getLogger(UtilisateurService.class);

  /** Délai d’inactivité avant de considérer l’utilisateur hors-ligne. */
  private static final Duration TIMEOUT = Duration.ofSeconds(60);

  private final UtilisateurRepository repo;
  private final PasswordEncoder passwordEncoder;

  public UtilisateurService(UtilisateurRepository repo, PasswordEncoder passwordEncoder) {
    this.repo = repo;
    this.passwordEncoder = passwordEncoder;
  }

  // ------------------- CRUD & recherche -------------------

  public List<Utilisateur> findAll() {
    return repo.findAll();
  }

  public Optional<Utilisateur> findById(String id) {
    return repo.findById(id);
  }

  public void deleteById(String id) {
    repo.deleteById(id);
  }

  public Optional<Utilisateur> chercherParNumero(String numero) {
    return repo.findByTelephone(numero);
  }

  public Optional<Utilisateur> chercherParEmail(String email) {
    return repo.findByEmailIgnoreCase(email);
  }

  /** Crée / met à jour un utilisateur en appliquant les valeurs par défaut et le hash du mot de passe. */
  public Utilisateur saveUtilisateur(Utilisateur utilisateur) {
    if (utilisateur.getStatut() == null) {
      utilisateur.setStatut(Utilisateur.Statut.actif);
    }
    if (utilisateur.getRole() == null) {
      utilisateur.setRole(Utilisateur.Role.client);
    }
    // Hash le mot de passe si fourni en clair
    if (utilisateur.getMotDePasse() != null && utilisateur.getMotDePasse().length() < 20) {
      // Heuristique simple : si c’est court, on suppose que ce n’est pas déjà un hash
      utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
    }
    return repo.save(utilisateur);
  }

  /** Alias si un code existant appelle encore "save". */
  public Utilisateur save(Utilisateur utilisateur) {
    return saveUtilisateur(utilisateur);
  }

  // ------------------- Presence / Online -------------------

  /**
   * Marque un utilisateur comme en ligne (heartbeat).
   * Accepte un ID **ou** un email (pratique en dev). En prod avec JWT, passe l’ID (sub).
   */
  public void heartbeat(String principal) {
    if (principal == null || principal.isBlank()) return;

    Optional<Utilisateur> opt = repo.findById(principal)
        .or(() -> repo.findByEmailIgnoreCase(principal));

    if (opt.isEmpty()) return;

    Utilisateur u = opt.get();
    u.setLastSeen(LocalDateTime.now());
    u.setOnline(true);
    if (u.getStatut() != Utilisateur.Statut.banni) {
      u.setStatut(Utilisateur.Statut.actif); // online ⇒ actif
    }
    repo.save(u);
  }

  /**
   * Calcule "à la volée" si l’utilisateur est en ligne via lastSeen et synchronise BDD si besoin.
   */
  public boolean isOnline(String userId) {
    return repo.findById(userId)
        .map(u -> {
          boolean shouldBeOnline = u.getLastSeen() != null &&
              Duration.between(u.getLastSeen(), LocalDateTime.now()).compareTo(TIMEOUT) <= 0;

          // Synchronise les champs si l’état a changé
          boolean needSave = false;
          if (u.isOnline() != shouldBeOnline) {
            u.setOnline(shouldBeOnline);
            needSave = true;
          }
          if (u.getStatut() != Utilisateur.Statut.banni) {
            Utilisateur.Statut target = shouldBeOnline ? Utilisateur.Statut.actif : Utilisateur.Statut.inactif;
            if (u.getStatut() != target) {
              u.setStatut(target);
              needSave = true;
            }
          }
          if (needSave) repo.save(u);

          return shouldBeOnline;
        })
        .orElse(false);
  }

  /**
   * Tâche planifiée : passe en offline + inactif les comptes online dont lastSeen a expiré.
   * (toutes les 30s)
   */
  @Scheduled(fixedDelay = 30_000)
  public void expireInactives() {
    LocalDateTime cutoff = LocalDateTime.now().minus(TIMEOUT);
    List<Utilisateur> list = repo.findAllByOnlineIsTrueAndLastSeenBefore(cutoff);
    if (!list.isEmpty()) {
      list.forEach(u -> {
        u.setOnline(false);
        if (u.getStatut() != Utilisateur.Statut.banni) {
          u.setStatut(Utilisateur.Statut.inactif);
        }
      });
      repo.saveAll(list);
      log.debug("expireInactives: {} utilisateur(s) mis hors-ligne", list.size());
    }
  }
}
