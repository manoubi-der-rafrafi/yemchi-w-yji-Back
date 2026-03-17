package com.transport.transport.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.dto.CommandeProduitsPanneResponse;
import com.transport.transport.dto.TransporteurPanneCommandesResponse;
import com.transport.transport.dto.TransporteurPanneInfo;
import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;
import com.transport.transport.dto.UserPosition;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.ProduitRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class UtilisateurService {

  private static final Logger log = LoggerFactory.getLogger(UtilisateurService.class);

  /** Délai d’inactivité avant de considérer l’utilisateur hors-ligne. */
  private static final Duration TIMEOUT = Duration.ofSeconds(60);

  private final UtilisateurRepository repo;
  private final CommandeRepository commandeRepository;
  private final ProduitRepository produitRepository;
  private final PasswordEncoder passwordEncoder;
  
  
  public UtilisateurService(
      UtilisateurRepository repo,
      CommandeRepository commandeRepository,
      ProduitRepository produitRepository,
      PasswordEncoder passwordEncoder) {
    this.repo = repo;
    this.commandeRepository = commandeRepository;
    this.produitRepository = produitRepository;
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

  public boolean isEmailVerified(String email) {
    if (email == null || email.isBlank()) return false;
    return repo.findByEmailIgnoreCase(email)
        .map(u -> Boolean.TRUE.equals(u.getIsEmailVerified()))
        .orElse(false);
  }

  public Utilisateur createUserWithEmail(String email) {
    Utilisateur u = new Utilisateur();
    u.setEmail(email);
    u.setIsEmailVerified(false);
    return saveUtilisateur(u);
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

  public Utilisateur updateLocalisation(String userId, Double latitude, Double longitude) {
        // Double-check des bornes (déjà validées par @Valid coté controller)
        if (latitude == null || longitude == null ||
            latitude < -90.0 || latitude > 90.0 ||
            longitude < -180.0 || longitude > 180.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude/Longitude invalides");
        }

        Utilisateur user = repo.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

        user.setLatitude(latitude);
        user.setLongitude(longitude);
        repo.save(user);

        // Par sécurité, ne retourne jamais le mot de passe
        user.setMotDePasse(null);
        return user;
    }

    public Utilisateur updateZone(String userId, Utilisateur.Zone zone) {
    Utilisateur u = repo.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
    u.setZone(zone);
    repo.save(u);
    u.setMotDePasse(null); // ne jamais renvoyer le mdp
    return u;
  }

  public Utilisateur updateSousZone(String userId, Utilisateur.SousZone sousZone) {
    Utilisateur u = repo.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
    u.setSousZone(sousZone);
    repo.save(u);
    u.setMotDePasse(null);
    return u;
  }

  public Utilisateur updateZonesDepartAriver(
      String userId,
      java.util.Map<String, java.util.List<String>> zoneDepart,
      java.util.Map<String, java.util.List<String>> zoneAriver
  ) {
    Utilisateur u = repo.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));
    u.setZoneDepart(zoneDepart);
    u.setZoneAriver(zoneAriver);
    repo.save(u);
    u.setMotDePasse(null);
    return u;
  }

  public Utilisateur marquerEnPanne(String userId) {
    return updateEtatIncident(userId, Utilisateur.EtatIncident.PANNE);
  }

  public Utilisateur marquerEnAccident(String userId) {
    return updateEtatIncident(userId, Utilisateur.EtatIncident.ACCIDENT);
  }

  public Utilisateur declarerAccidentAvecProduits(
      String userId,
      Map<String, Integer> produitsAffectes,
      List<String> produitsNonAffectes) {
    Utilisateur transporteur = repo.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

    if (transporteur.getRole() != Utilisateur.Role.transporteur) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Seul un utilisateur avec le role transporteur peut avoir un etat d'incident");
    }

    Map<String, Integer> affectes = (produitsAffectes == null) ? Map.of() : new HashMap<>(produitsAffectes);
    List<String> nonAffectes = (produitsNonAffectes == null) ? List.of() : new ArrayList<>(produitsNonAffectes);

    Set<String> doublons = new HashSet<>(affectes.keySet());
    doublons.retainAll(nonAffectes);
    if (!doublons.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Un produit ne peut pas etre a la fois affecte et non affecte");
    }

    Set<String> produitIds = new HashSet<>(affectes.keySet());
    produitIds.addAll(nonAffectes);
    if (produitIds.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Au moins un produit affecte ou non affecte est requis");
    }

    List<Produit> produits = produitRepository.findAllById(produitIds);
    if (produits.size() != produitIds.size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Un ou plusieurs produits sont introuvables");
    }

    Map<String, Produit> produitsById = produits.stream()
        .collect(Collectors.toMap(Produit::getId, p -> p));

    Set<String> commandeIds = produits.stream()
        .map(Produit::getCommandeId)
        .collect(Collectors.toSet());

    if (commandeIds.contains(null)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Un ou plusieurs produits ne sont lies a aucune commande");
    }

    Map<String, Commande> commandesById = commandeRepository.findAllById(commandeIds)
        .stream()
        .collect(Collectors.toMap(Commande::getId, c -> c));

    if (commandesById.size() != commandeIds.size()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Une ou plusieurs commandes des produits sont introuvables");
    }

    for (Map.Entry<String, Integer> entry : affectes.entrySet()) {
      String produitId = entry.getKey();
      Integer quantiteAffecter = entry.getValue();
      Produit produit = produitsById.get(produitId);

      if (quantiteAffecter == null || quantiteAffecter < 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La quantite affectee est invalide pour le produit " + produitId);
      }
      if (produit.getQuantite() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La quantite du produit " + produitId + " est absente");
      }
      if (quantiteAffecter > produit.getQuantite()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "La quantite affectee depasse la quantite du produit " + produitId);
      }

      Commande commande = commandesById.get(produit.getCommandeId());
      if (!transporteur.getId().equals(commande.getTransporteurId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Le produit " + produitId + " n'appartient pas a une commande de ce transporteur");
      }
    }

    for (String produitId : nonAffectes) {
      Produit produit = produitsById.get(produitId);
      Commande commande = commandesById.get(produit.getCommandeId());
      if (!transporteur.getId().equals(commande.getTransporteurId())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Le produit " + produitId + " n'appartient pas a une commande de ce transporteur");
      }
    }

    transporteur.setEtatIncident(Utilisateur.EtatIncident.ACCIDENT);

    affectes.forEach((produitId, quantiteAffecter) -> {
      Produit produit = produitsById.get(produitId);
      produit.setAffecter(true);
      produit.setQuantiteAffecter(quantiteAffecter);
    });

    nonAffectes.forEach(produitId -> {
      Produit produit = produitsById.get(produitId);
      produit.setAffecter(false);
      produit.setQuantiteAffecter(0);
    });

    repo.save(transporteur);
    produitRepository.saveAll(produits);
    transporteur.setMotDePasse(null);
    return transporteur;
  }

  private Utilisateur updateEtatIncident(String userId, Utilisateur.EtatIncident etatIncident) {
    Utilisateur u = repo.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé"));

    if (u.getRole() != Utilisateur.Role.transporteur) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Seul un utilisateur avec le role transporteur peut avoir un etat d'incident");
    }

    u.setEtatIncident(etatIncident);
    repo.save(u);
    u.setMotDePasse(null);
    return u;
  }

  public List<UserPosition> getPositionsByUserIds(List<String> ids) {
    if (ids == null || ids.isEmpty()) return List.of();

    return repo.findAllById(ids)
        .stream()
        .map(u -> new UserPosition(u.getId(), u.getLatitude(), u.getLongitude()))
        .collect(Collectors.toList());
  }

  public List<TransporteurPanneCommandesResponse> getTransporteursEnPanneAvecCommandes() {
    List<Utilisateur> transporteursEnPanne = repo.findByRoleAndEtatIncident(
        Utilisateur.Role.transporteur,
        Utilisateur.EtatIncident.PANNE);

    return transporteursEnPanne.stream()
        .map(transporteur -> {
          List<CommandeProduitsPanneResponse> commandes = commandeRepository
              .findByTransporteurIdAndStatutOrderByDateDemandeDesc(
                  transporteur.getId(),
                  Commande.Statut.en_route)
              .stream()
              .map(commande -> {
                List<Produit> produits = produitRepository.findByCommandeId(commande.getId());
                return new CommandeProduitsPanneResponse(commande, produits);
              })
              .collect(Collectors.toList());

          TransporteurPanneInfo transporteurInfo = new TransporteurPanneInfo(
              transporteur.getId(),
              transporteur.getNom(),
              transporteur.getPrenom(),
              transporteur.getTelephone(),
              transporteur.getImage(),
              transporteur.getLatitude(),
              transporteur.getLongitude());

          return new TransporteurPanneCommandesResponse(transporteurInfo, commandes);
        })
        .collect(Collectors.toList());
  }

}
