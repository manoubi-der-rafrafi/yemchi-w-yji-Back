package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.transport.transport.dto.TransporteurInfo;
import com.transport.transport.dto.CommandeTransporteurPrincipalResponse;
import com.transport.transport.dto.CommandeProduitsSecoursResponse;
import com.transport.transport.dto.TransporteurSecoursCommandesResponse;
import com.transport.transport.model.Commande;
import com.transport.transport.model.Commande.Statut;
import com.transport.transport.model.Produit;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.ProduitRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class CommandeService {

    private static final Logger logger = LoggerFactory.getLogger(CommandeService.class);

    @Autowired
    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final VehicleAnalysisService vehicleAnalysisService;

    public CommandeService(
            CommandeRepository commandeRepository,
            ProduitRepository produitRepository,
            UtilisateurRepository utilisateurRepository,
            VehicleAnalysisService vehicleAnalysisService) {
        this.commandeRepository = commandeRepository;
        this.produitRepository = produitRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.vehicleAnalysisService = vehicleAnalysisService;
    }

    // Historique simple d'un client
    public List<Commande> getHistoriqueClient(String  clientId) {
        utilisateurRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));
        return commandeRepository.findByClientIdOrderByDateDemandeDesc(clientId);
    }
    // Recuperer toutes les commandes
    public List<Commande> getAllCommandes() {
        return commandeRepository.findAll();
    }

    // Recuperer une commande par son id
    public Optional<Commande> getCommandeById(String  id) {
        return commandeRepository.findById(id);
    }

    // Ajouter une nouvelle commande
    public Commande createCommande(Commande commande) {
        enrichCommandeGeography(commande);
        return commandeRepository.save(commande);
    }

    // Mettre a jour une commande existante
    public Commande updateCommande(String id, Commande details) {
        logger.info("updateCommande id={} vehicule={}", id, details.getVehicule());
        return commandeRepository.findById(id).map(commande -> {
            // copie EXPLICITE de tous les champs que tu veux rendre modifiables
            if (details.getLocalisationDepart() != null) {
                commande.setLocalisationDepart(details.getLocalisationDepart());
            }
            if (details.getDestination() != null) {
                commande.setDestination(details.getDestination());
            }
            if (details.getDateDebut() != null) {
                commande.setDateDebut(details.getDateDebut());
            }
            if (details.getDateFin() != null) {
                commande.setDateFin(details.getDateFin());
            }
            if (details.getDateDemande() != null) {
                commande.setDateDemande(details.getDateDemande());
            }
            if (details.getStatut() != null) {
                if (details.getStatut() == Statut.confirmer
                        && commande.getStatut() != Statut.confirmer) {
                    commande.setDateConfirmer(LocalDateTime.now());
                    commande.setVehicule(resolveVehicleForCommande(commande));
                }
                commande.setStatut(details.getStatut());
            }
            if (details.getPrix() != null) {
                commande.setPrix(details.getPrix());
            }
            if (details.getPoids() != null) {
                commande.setPoids(details.getPoids());
            }
            if (details.getVolume() != null) {
                commande.setVolume(details.getVolume());
            }
            if (details.getModePaiement() != null) {
                commande.setModePaiement(details.getModePaiement());
            }
            if (details.getInstructions() != null) {
                commande.setInstructions(details.getInstructions());
            }

            if (details.getTelDepart() != null) {            // �o. ajout�
                commande.setTelDepart(details.getTelDepart());
            }
            if (details.getClientId() != null) {              // �o. s'assure que �a se copie
                commande.setClientId(details.getClientId());
            }
            if (details.getTransporteurId() != null) {
                commande.setTransporteurId(details.getTransporteurId());
            }
            if (details.getTransporteurSecoursId() != null) {
                commande.setTransporteurSecoursId(details.getTransporteurSecoursId());
            }
            if (details.getPartenaireId() != null) {
                commande.setPartenaireId(details.getPartenaireId());
            }
            if (details.getExternalBusinessId() != null) {
                commande.setExternalBusinessId(details.getExternalBusinessId());
            }
            if (details.getExternalOrderId() != null) {
                commande.setExternalOrderId(details.getExternalOrderId());
            }
            if (details.getIdAmie() != null) {
                commande.setIdAmie(details.getIdAmie());
            }
            if (details.getNomDepart() != null) {
                commande.setNomDepart(details.getNomDepart());
            }
            if (details.getNomArrivee() != null) {
                commande.setNomArrivee(details.getNomArrivee());
            }
            if (details.getTelArrivee() != null) {
                commande.setTelArrivee(details.getTelArrivee());
            }

            if (details.getLatitudeDepart() != null) {
                commande.setLatitudeDepart(details.getLatitudeDepart());
            }
            if (details.getLongitudeDepart() != null) {
                commande.setLongitudeDepart(details.getLongitudeDepart());
            }
            if (details.getLatitudeDestination() != null) {
                commande.setLatitudeDestination(details.getLatitudeDestination());
            }
            if (details.getLongitudeDestination() != null) {
                commande.setLongitudeDestination(details.getLongitudeDestination());
            }

            if (details.getDistanceKm() != null) {
                commande.setDistanceKm(details.getDistanceKm());
            }

            if (details.getSousZoneDepart() != null) {
                commande.setSousZoneDepart(details.getSousZoneDepart());
            }
            if (details.getSousZoneArrivee() != null) {
                commande.setSousZoneArrivee(details.getSousZoneArrivee());
            }

            if (details.getZonePrincipaleDepart() != null) {
                commande.setZonePrincipaleDepart(details.getZonePrincipaleDepart());
            }
            if (details.getZonePrincipaleArrivee() != null) {
                commande.setZonePrincipaleArrivee(details.getZonePrincipaleArrivee());
            }
            commande.setQrCodeDepartScanne(details.isQrCodeDepartScanne());
            if (details.getDateScanDepart() != null) {
                commande.setDateScanDepart(details.getDateScanDepart());
            }

            commande.setQrCodeReceptionScanne(details.isQrCodeReceptionScanne());
            if (details.getDateScanReception() != null) {
                commande.setDateScanReception(details.getDateScanReception());
            }
            if (details.getRelaisTransporteurEffectue() != null) {
                commande.setRelaisTransporteurEffectue(details.getRelaisTransporteurEffectue());
            }
            // --- Met a jour la date de modification automatique ---
            commande.setMajLe(LocalDateTime.now());
            enrichCommandeGeography(commande);
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }

    public Commande updateCommandePatch(String id, Commande patch) {

    logger.info("updateCommandePatch id={} vehicule={}", id, patch.getVehicule());
    return commandeRepository.findById(id).map(new Function<Commande, Commande>() {
        @Override
        public Commande apply(Commande existing) {
            // Copy only non-null fields from patch → existing
            BeanWrapper srcWrapper = new BeanWrapperImpl(patch);
            BeanWrapper targetWrapper = new BeanWrapperImpl(existing);
            
            for (var pd : srcWrapper.getPropertyDescriptors()) {
                String field = pd.getName();
                
                // Skip technical fields that must NOT be patched
                if (field.equals("id") ||
                        field.equals("createdAt") ||
                        field.equals("majLe")) {
                    continue;
                }
                
                Object newValue = srcWrapper.getPropertyValue(field);
                if (newValue != null) {
                    targetWrapper.setPropertyValue(field, newValue);
                }
            }
            
            // Always update modification date
            existing.setMajLe(LocalDateTime.now());
            enrichCommandeGeography(existing);
            
            return commandeRepository.save(existing);
        }
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

    private void enrichCommandeGeography(Commande commande) {
        if (commande == null) {
            return;
        }

        hydrateDestinationFromFriend(commande);
        hydrateZonesFromCoordinates(
                commande.getLatitudeDepart(),
                commande.getLongitudeDepart(),
                commande.getZonePrincipaleDepart(),
                commande.getSousZoneDepart(),
                commande::setZonePrincipaleDepart,
                commande::setSousZoneDepart);
        hydrateZonesFromCoordinates(
                commande.getLatitudeDestination(),
                commande.getLongitudeDestination(),
                commande.getZonePrincipaleArrivee(),
                commande.getSousZoneArrivee(),
                commande::setZonePrincipaleArrivee,
                commande::setSousZoneArrivee);
    }

    private void hydrateDestinationFromFriend(Commande commande) {
        String friendId = commande.getIdAmie();
        if (friendId == null || friendId.isBlank()) {
            return;
        }

        utilisateurRepository.findById(friendId).ifPresent(friend -> {
            if (isBlank(commande.getDestination()) && !isBlank(friend.getAdresse())) {
                commande.setDestination(friend.getAdresse());
            }
            if (isBlank(commande.getTelArrivee()) && !isBlank(friend.getTelephone())) {
                commande.setTelArrivee(friend.getTelephone());
            }
            if ((commande.getLatitudeDestination() == null || commande.getLongitudeDestination() == null)
                    && hasUsableCoordinates(friend.getLatitude(), friend.getLongitude())) {
                commande.setLatitudeDestination(friend.getLatitude());
                commande.setLongitudeDestination(friend.getLongitude());
            }
            if (commande.getSousZoneArrivee() == null && friend.getSousZone() != null) {
                commande.setSousZoneArrivee(convertSousZone(friend.getSousZone()));
            }
            if (commande.getZonePrincipaleArrivee() == null && friend.getZone() != null) {
                commande.setZonePrincipaleArrivee(convertZone(friend.getZone()));
            }
        });
    }

    private void hydrateZonesFromCoordinates(
            Double latitude,
            Double longitude,
            Commande.Zone currentZone,
            Commande.SousZone currentSousZone,
            java.util.function.Consumer<Commande.Zone> zoneSetter,
            java.util.function.Consumer<Commande.SousZone> sousZoneSetter) {
        if (currentZone != null && currentSousZone != null) {
            return;
        }
        if (!hasUsableCoordinates(latitude, longitude)) {
            return;
        }

        ZoneMatch match = resolveZone(latitude, longitude);
        if (match == null) {
            return;
        }
        if (currentZone == null) {
            zoneSetter.accept(match.zone());
        }
        if (currentSousZone == null) {
            sousZoneSetter.accept(match.sousZone());
        }
    }

    private boolean hasUsableCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return Math.abs(latitude) > 0.000001d || Math.abs(longitude) > 0.000001d;
    }

    private boolean hasUsableCoordinates(double latitude, double longitude) {
        return Math.abs(latitude) > 0.000001d || Math.abs(longitude) > 0.000001d;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Commande.Zone convertZone(Utilisateur.Zone zone) {
        return zone == null ? null : Commande.Zone.valueOf(zone.name());
    }

    private Commande.SousZone convertSousZone(Utilisateur.SousZone sousZone) {
        return sousZone == null ? null : Commande.SousZone.valueOf(sousZone.name());
    }

    private ZoneMatch resolveZone(double latitude, double longitude) {
        for (ZoneBounds bounds : ZONE_BOUNDS) {
            if (bounds.matches(latitude, longitude)) {
                return new ZoneMatch(bounds.zone(), bounds.sousZone());
            }
        }
        return null;
    }

    private record ZoneMatch(Commande.Zone zone, Commande.SousZone sousZone) {}

    private record ZoneBounds(
            Commande.Zone zone,
            Commande.SousZone sousZone,
            double latMin,
            double latMax,
            double lonMin,
            double lonMax) {
        boolean matches(double latitude, double longitude) {
            return latitude >= latMin && latitude <= latMax && longitude >= lonMin && longitude <= lonMax;
        }
    }

    private static final List<ZoneBounds> ZONE_BOUNDS = List.of(
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.TUNIS, 36.70, 36.95, 10.00, 10.45),
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.ARIANA, 36.75, 37.10, 10.00, 10.45),
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.BEN_AROUS, 36.55, 36.85, 10.05, 10.45),
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.MANOUBA, 36.60, 36.98, 9.85, 10.25),
            new ZoneBounds(Commande.Zone.NORD_EST, Commande.SousZone.BIZERTE, 37.00, 37.55, 9.30, 10.35),
            new ZoneBounds(Commande.Zone.NORD_EST, Commande.SousZone.NABEUL, 36.15, 36.95, 10.25, 11.20),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.BEJA, 36.50, 37.20, 8.60, 9.60),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.JENDOUBA, 36.50, 37.10, 8.30, 9.20),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.KEF, 35.90, 36.80, 8.20, 9.10),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.SILIANA, 35.70, 36.40, 8.70, 9.80),
            new ZoneBounds(Commande.Zone.CENTRE, Commande.SousZone.ZAGHOUAN, 36.10, 36.60, 9.90, 10.60),
            new ZoneBounds(Commande.Zone.CENTRE, Commande.SousZone.KAIROUAN, 35.20, 36.10, 9.50, 10.50),
            new ZoneBounds(Commande.Zone.CENTRE_OUEST, Commande.SousZone.KASSERINE, 34.80, 35.70, 8.20, 9.50),
            new ZoneBounds(Commande.Zone.CENTRE_OUEST, Commande.SousZone.SIDI_BOUZID, 34.60, 35.60, 8.80, 10.00),
            new ZoneBounds(Commande.Zone.SAHEL, Commande.SousZone.SOUSSE, 35.70, 36.20, 10.30, 10.90),
            new ZoneBounds(Commande.Zone.SAHEL, Commande.SousZone.MONASTIR, 35.50, 35.90, 10.60, 11.20),
            new ZoneBounds(Commande.Zone.SAHEL, Commande.SousZone.MAHDIA, 35.20, 35.70, 10.60, 11.30),
            new ZoneBounds(Commande.Zone.SFAX, Commande.SousZone.SFAX, 34.50, 35.10, 10.40, 11.20),
            new ZoneBounds(Commande.Zone.SUD_EST, Commande.SousZone.GABES, 33.70, 34.30, 9.80, 10.40),
            new ZoneBounds(Commande.Zone.SUD_EST, Commande.SousZone.MEDENINE, 33.00, 33.80, 10.00, 11.20),
            new ZoneBounds(Commande.Zone.SUD_EST, Commande.SousZone.TATAOUINE, 31.50, 33.40, 9.50, 11.20),
            new ZoneBounds(Commande.Zone.SUD_OUEST, Commande.SousZone.TOZEUR, 33.50, 34.20, 7.30, 8.40),
            new ZoneBounds(Commande.Zone.SUD_OUEST, Commande.SousZone.KEBILI, 33.10, 34.00, 8.20, 9.30),
            new ZoneBounds(Commande.Zone.SUD_OUEST, Commande.SousZone.GAFSA, 33.70, 34.80, 8.30, 9.30));


    // Supprimer une commande par son id
    public void deleteCommande(String  id) {
        commandeRepository.deleteById(id);
    }
    public Optional<Commande> getCommandeEnCoursByClientId(String  idClient) {
        List<Commande> commandes = commandeRepository.findByClientIdAndStatutOrderByDateDemandeDesc(
                idClient,
                Commande.Statut.en_cours);
        if (commandes.isEmpty()) {
            return Optional.empty();
        }
        if (commandes.size() == 1) {
            return Optional.of(commandes.get(0));
        }

        logger.warn(
                "Detected {} commandes en_cours for client {}. Rebuilding a single commande.",
                commandes.size(),
                idClient);
        return Optional.of(rebuildSingleCommandeEnCours(idClient, commandes));
    }

    private Commande rebuildSingleCommandeEnCours(String idClient, List<Commande> commandesEnCours) {
        List<Commande> sortedCommandes = new ArrayList<>(commandesEnCours);
        sortedCommandes.sort(Comparator.comparing(
                this::commandeSortDate,
                Comparator.nullsLast(Comparator.reverseOrder())));

        Commande commandeReference = sortedCommandes.get(0);
        Commande rebuiltCommande = new Commande();
        copyCommandeFields(commandeReference, rebuiltCommande);
        for (int i = 1; i < sortedCommandes.size(); i++) {
            mergeCommandeFields(sortedCommandes.get(i), rebuiltCommande);
        }

        rebuiltCommande.setId(null);
        rebuiltCommande.setClientId(idClient);
        rebuiltCommande.setStatut(Commande.Statut.en_cours);
        rebuiltCommande.setDateDemande(
                Objects.requireNonNullElse(commandeReference.getDateDemande(), LocalDateTime.now()));
        rebuiltCommande.setMajLe(LocalDateTime.now());
        enrichCommandeGeography(rebuiltCommande);

        Commande savedCommande = commandeRepository.save(rebuiltCommande);

        List<Produit> produits = produitRepository.findByCommandeIdIn(
                sortedCommandes.stream().map(Commande::getId).toList());
        if (!produits.isEmpty()) {
            for (Produit produit : produits) {
                produit.setCommandeId(savedCommande.getId());
            }
            produitRepository.saveAll(produits);
        }

        commandeRepository.deleteAll(sortedCommandes);
        return savedCommande;
    }

    private LocalDateTime commandeSortDate(Commande commande) {
        if (commande == null) {
            return null;
        }
        if (commande.getMajLe() != null) {
            return commande.getMajLe();
        }
        if (commande.getDateDemande() != null) {
            return commande.getDateDemande();
        }
        return commande.getDateConfirmer();
    }

    private void copyCommandeFields(Commande source, Commande target) {
        if (source == null || target == null) {
            return;
        }
        target.setLocalisationDepart(source.getLocalisationDepart());
        target.setDestination(source.getDestination());
        target.setDateDebut(source.getDateDebut());
        target.setDateFin(source.getDateFin());
        target.setDateDemande(source.getDateDemande());
        target.setDateConfirmer(source.getDateConfirmer());
        target.setStatut(source.getStatut());
        target.setPrix(source.getPrix());
        target.setPoids(source.getPoids());
        target.setVolume(source.getVolume());
        target.setVehicule(source.getVehicule());
        target.setModePaiement(source.getModePaiement());
        target.setInstructions(source.getInstructions());
        target.setTelDepart(source.getTelDepart());
        target.setTelArrivee(source.getTelArrivee());
        target.setClientId(source.getClientId());
        target.setTransporteurId(source.getTransporteurId());
        target.setTransporteurSecoursId(source.getTransporteurSecoursId());
        target.setPartenaireId(source.getPartenaireId());
        target.setExternalBusinessId(source.getExternalBusinessId());
        target.setExternalOrderId(source.getExternalOrderId());
        target.setNomDepart(source.getNomDepart());
        target.setNomArrivee(source.getNomArrivee());
        target.setMajLe(source.getMajLe());
        target.setIdAmie(source.getIdAmie());
        target.setLatitudeDepart(source.getLatitudeDepart());
        target.setLongitudeDepart(source.getLongitudeDepart());
        target.setLatitudeDestination(source.getLatitudeDestination());
        target.setLongitudeDestination(source.getLongitudeDestination());
        target.setDistanceKm(source.getDistanceKm());
        target.setSousZoneDepart(source.getSousZoneDepart());
        target.setSousZoneArrivee(source.getSousZoneArrivee());
        target.setZonePrincipaleDepart(source.getZonePrincipaleDepart());
        target.setZonePrincipaleArrivee(source.getZonePrincipaleArrivee());
        target.setQrCodeDepartScanne(source.isQrCodeDepartScanne());
        target.setDateScanDepart(source.getDateScanDepart());
        target.setQrCodeReceptionScanne(source.isQrCodeReceptionScanne());
        target.setDateScanReception(source.getDateScanReception());
        target.setRelaisTransporteurEffectue(source.getRelaisTransporteurEffectue());
    }

    private void mergeCommandeFields(Commande source, Commande target) {
        if (source == null || target == null) {
            return;
        }
        if (isBlank(target.getLocalisationDepart())) {
            target.setLocalisationDepart(source.getLocalisationDepart());
        }
        if (isBlank(target.getDestination())) {
            target.setDestination(source.getDestination());
        }
        if (target.getDateDebut() == null) {
            target.setDateDebut(source.getDateDebut());
        }
        if (target.getDateFin() == null) {
            target.setDateFin(source.getDateFin());
        }
        if (target.getDateDemande() == null) {
            target.setDateDemande(source.getDateDemande());
        }
        if (target.getDateConfirmer() == null) {
            target.setDateConfirmer(source.getDateConfirmer());
        }
        if (target.getPrix() == null) {
            target.setPrix(source.getPrix());
        }
        if (target.getPoids() == null) {
            target.setPoids(source.getPoids());
        }
        if (target.getVolume() == null) {
            target.setVolume(source.getVolume());
        }
        if (target.getVehicule() == null) {
            target.setVehicule(source.getVehicule());
        }
        if (target.getModePaiement() == null) {
            target.setModePaiement(source.getModePaiement());
        }
        if (isBlank(target.getInstructions())) {
            target.setInstructions(source.getInstructions());
        }
        if (isBlank(target.getTelDepart())) {
            target.setTelDepart(source.getTelDepart());
        }
        if (isBlank(target.getTelArrivee())) {
            target.setTelArrivee(source.getTelArrivee());
        }
        if (isBlank(target.getTransporteurId())) {
            target.setTransporteurId(source.getTransporteurId());
        }
        if (isBlank(target.getTransporteurSecoursId())) {
            target.setTransporteurSecoursId(source.getTransporteurSecoursId());
        }
        if (isBlank(target.getPartenaireId())) {
            target.setPartenaireId(source.getPartenaireId());
        }
        if (isBlank(target.getExternalBusinessId())) {
            target.setExternalBusinessId(source.getExternalBusinessId());
        }
        if (isBlank(target.getExternalOrderId())) {
            target.setExternalOrderId(source.getExternalOrderId());
        }
        if (isBlank(target.getNomDepart())) {
            target.setNomDepart(source.getNomDepart());
        }
        if (isBlank(target.getNomArrivee())) {
            target.setNomArrivee(source.getNomArrivee());
        }
        if (isBlank(target.getIdAmie())) {
            target.setIdAmie(source.getIdAmie());
        }
        if (target.getLatitudeDepart() == null) {
            target.setLatitudeDepart(source.getLatitudeDepart());
        }
        if (target.getLongitudeDepart() == null) {
            target.setLongitudeDepart(source.getLongitudeDepart());
        }
        if (target.getLatitudeDestination() == null) {
            target.setLatitudeDestination(source.getLatitudeDestination());
        }
        if (target.getLongitudeDestination() == null) {
            target.setLongitudeDestination(source.getLongitudeDestination());
        }
        if (target.getDistanceKm() == null) {
            target.setDistanceKm(source.getDistanceKm());
        }
        if (target.getSousZoneDepart() == null) {
            target.setSousZoneDepart(source.getSousZoneDepart());
        }
        if (target.getSousZoneArrivee() == null) {
            target.setSousZoneArrivee(source.getSousZoneArrivee());
        }
        if (target.getZonePrincipaleDepart() == null) {
            target.setZonePrincipaleDepart(source.getZonePrincipaleDepart());
        }
        if (target.getZonePrincipaleArrivee() == null) {
            target.setZonePrincipaleArrivee(source.getZonePrincipaleArrivee());
        }
        if (!target.isQrCodeDepartScanne() && source.isQrCodeDepartScanne()) {
            target.setQrCodeDepartScanne(true);
            target.setDateScanDepart(source.getDateScanDepart());
        }
        if (!target.isQrCodeReceptionScanne() && source.isQrCodeReceptionScanne()) {
            target.setQrCodeReceptionScanne(true);
            target.setDateScanReception(source.getDateScanReception());
        }
        if ((target.getRelaisTransporteurEffectue() == null || !target.getRelaisTransporteurEffectue())
                && Boolean.TRUE.equals(source.getRelaisTransporteurEffectue())) {
            target.setRelaisTransporteurEffectue(true);
        }
    }

    public List<com.transport.transport.controller.CommandeController.CommandeProduitsTransporteurResponse>
            getCommandesProduitsTransporteurByClientId(String idClient) {
        List<Commande> commandes = commandeRepository.findByClientIdAndStatutOrderByDateDemandeDesc(
                idClient, Commande.Statut.en_route);
        List<com.transport.transport.controller.CommandeController.CommandeProduitsTransporteurResponse> resultats =
                new java.util.ArrayList<>();
        for (Commande commande : commandes) {
            List<Produit> produits = produitRepository.findByCommandeId(commande.getId());

            TransporteurInfo transporteurInfo = null;
            String transporteurId = commande.getTransporteurId();
            if (transporteurId != null) {
                Utilisateur transporteur = utilisateurRepository.findById(transporteurId).orElse(null);
                if (transporteur != null) {
                    transporteurInfo = new TransporteurInfo(
                            transporteur.getId(),
                            transporteur.getNom(),
                            transporteur.getPrenom(),
                            transporteur.getTelephone(),
                            transporteur.getImage(),
                            transporteur.getLatitude(),
                            transporteur.getLongitude());
                }
            }

            resultats.add(new com.transport.transport.controller.CommandeController
                    .CommandeProduitsTransporteurResponse(commande, produits, transporteurInfo));
        }
        return resultats;
    }

    public List<com.transport.transport.controller.CommandeController.CommandeProduitsResponse>
            getCommandesEnRouteAvecProduitsByTransporteur(String idTransporteur) {
        List<Commande> commandes =
                commandeRepository.findByTransporteurIdAndStatutInAndQrCodeDepartScanneTrueOrderByDateDemandeDesc(
                        idTransporteur,
                        List.of(Commande.Statut.en_cours, Commande.Statut.en_route));
        List<com.transport.transport.controller.CommandeController.CommandeProduitsResponse> resultats =
                new java.util.ArrayList<>();

        for (Commande commande : commandes) {
            List<Produit> produits = produitRepository.findByCommandeId(commande.getId());
            resultats.add(new com.transport.transport.controller.CommandeController
                    .CommandeProduitsResponse(commande, produits));
        }

        return resultats;
    }

    public Commande confirmerCommande(String  id) {
        return commandeRepository.findById(id).map(commande -> {
            commande.setStatut(Commande.Statut.confirmer);
            commande.setDateConfirmer(LocalDateTime.now()); // date de confirmation
            commande.setVehicule(resolveVehicleForCommande(commande));
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }

    public Commande prepareCommandeVehicle(String id) {
        return commandeRepository.findById(id).map(commande -> {
            commande.setVehicule(resolveVehicleForCommande(commande));
            commande.setMajLe(LocalDateTime.now());
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }

    private TypeVehicule resolveVehicleForCommande(Commande commande) {
        List<Produit> produits = produitRepository.findByCommandeId(commande.getId());
        return vehicleAnalysisService.resolveVehicleForProduits(produits);
    }
    public List<Commande> getByIdAmie(String idAmie) {
        return commandeRepository.findByIdAmie(idAmie);
    }
    public int countCommandesByIdAmie(String idAmie) {
        return commandeRepository.countByIdAmie(idAmie);
    }
    public int countCommandesByIdAmieAndStatutEnvoyee(String idAmie) {
        return commandeRepository.countByIdAmieAndStatut(idAmie, Commande.Statut.envoyee);
    }   
    public List<Commande> getCommandesByZonePrincipale(Commande.Zone zone) {
    return commandeRepository.findByZonePrincipaleDepartAndZonePrincipaleArrivee(zone, zone);
}
    public List<Commande> getCommandesByZonePrincipaleConfirmees(Commande.Zone zone) {
    //return commandeRepository.findByStatutAndZonePrincipale(Statut.confirmer, zone);
    // Variante triee :
     return commandeRepository.findByStatutAndZonePrincipale(
         Statut.confirmer, zone, Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public List<Commande> getCommandesByZonePrincipaleAndVehicule(
        Commande.Zone zone,
        TypeVehicule vehicule) {
    if (zone == null) {
        throw new IllegalArgumentException("La zone est obligatoire");
    }
    if (vehicule == null) {
        throw new IllegalArgumentException("Le type de vehicule est obligatoire");
    }
    return commandeRepository.findByZonePrincipaleDepartAndZonePrincipaleArriveeAndVehicule(
            zone, zone, vehicule);
}
public Commande assignerTransporteur(String idCommande, String idTransporteur) {
    return commandeRepository.findById(idCommande).map(commande -> {
        if (commande.getTransporteurId() != null) {
            throw new IllegalStateException("Commande deja assignee a un transporteur");
        }
        commande.setTransporteurId(idTransporteur);
        commande.setMajLe(LocalDateTime.now());
        commande.setStatut(Statut.en_appelle);
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}
public Commande assignerTransporteurSecours(
        String idCommande,
        String idTransporteurSecours,
        String authenticatedEmail) {
    logger.info(
            "assignerTransporteurSecours start idCommande={} idTransporteurSecours={} authenticatedEmail={}",
            idCommande,
            idTransporteurSecours,
            authenticatedEmail);
    Utilisateur utilisateurConnecte = utilisateurRepository.findByEmailIgnoreCase(authenticatedEmail)
            .orElseThrow(() -> new SecurityException("Utilisateur authentifie introuvable"));

    logger.info(
            "assignerTransporteurSecours utilisateurConnecte id={} role={}",
            utilisateurConnecte.getId(),
            utilisateurConnecte.getRole());

    Utilisateur transporteurSecours = utilisateurRepository.findById(idTransporteurSecours)
            .orElseThrow(() -> new IllegalArgumentException("Transporteur de secours introuvable"));

    logger.info(
            "assignerTransporteurSecours transporteurSecours id={} role={}",
            transporteurSecours.getId(),
            transporteurSecours.getRole());

    if (transporteurSecours.getRole() != Utilisateur.Role.transporteur) {
        logger.warn(
                "assignerTransporteurSecours refused: cible non transporteur idTransporteurSecours={} role={}",
                transporteurSecours.getId(),
                transporteurSecours.getRole());
        throw new IllegalArgumentException("L'utilisateur cible n'est pas un transporteur");
    }

    return commandeRepository.findById(idCommande).map(commande -> {
        logger.info(
                "assignerTransporteurSecours commande id={} transporteurId={} transporteurSecoursIdActuel={}",
                commande.getId(),
                commande.getTransporteurId(),
                commande.getTransporteurSecoursId());
        boolean estAdmin = utilisateurConnecte.getRole() == Utilisateur.Role.admin;
        boolean estTransporteur = utilisateurConnecte.getRole() == Utilisateur.Role.transporteur;

        logger.info(
                "assignerTransporteurSecours authorization estAdmin={} estTransporteur={}",
                estAdmin,
                estTransporteur);

        if (!estAdmin && !estTransporteur) {
            logger.warn(
                    "assignerTransporteurSecours refused: principal id={} role={} cannot modify commande",
                    utilisateurConnecte.getId(),
                    utilisateurConnecte.getRole());
            throw new SecurityException("Acces refuse pour cette commande");
        }
        if (commande.getTransporteurSecoursId() != null && !commande.getTransporteurSecoursId().isBlank()) {
            logger.warn(
                    "assignerTransporteurSecours refused: secours deja affecte idCommande={} transporteurSecoursIdActuel={}",
                    commande.getId(),
                    commande.getTransporteurSecoursId());
            throw new IllegalArgumentException("Un transporteur de secours est deja affecte a cette commande");
        }
        if (idTransporteurSecours.equals(commande.getTransporteurId())) {
            logger.warn(
                    "assignerTransporteurSecours refused: self-assignment idCommande={} transporteurId={}",
                    commande.getId(),
                    commande.getTransporteurId());
            throw new IllegalArgumentException("Le transporteur principal ne peut pas etre son propre secours");
        }

        commande.setTransporteurSecoursId(idTransporteurSecours);
        commande.setRelaisTransporteurEffectue(false);
        commande.setMajLe(LocalDateTime.now());
        logger.info(
                "assignerTransporteurSecours success idCommande={} transporteurSecoursId={}",
                commande.getId(),
                idTransporteurSecours);
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}
public Commande marquerRelaisTransporteurEffectue(String idCommande) {
    return commandeRepository.findById(idCommande).map(commande -> {
        if (commande.getTransporteurSecoursId() == null || commande.getTransporteurSecoursId().isBlank()) {
            throw new IllegalStateException("Aucun transporteur de secours affecte a cette commande");
        }
        if (commande.getStatut() != Statut.en_route) {
            throw new IllegalStateException("Le relais ne peut etre marque que pour une commande en_route");
        }
        if (!commande.isQrCodeDepartScanne()) {
            throw new IllegalStateException(
                    "Le relais ne s'applique que si les produits ont deja ete recuperes au depart");
        }
        if (Boolean.TRUE.equals(commande.getRelaisTransporteurEffectue())) {
            return commande;
        }

        commande.setRelaisTransporteurEffectue(true);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}
public List<Commande> getCommandesByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdOrderByDateDemandeDesc(idTransporteur);
}
public List<Commande> getCommandesByTransporteurAndEtatIncident(
        String idTransporteur,
        Utilisateur.EtatIncident etatIncident) {
    Utilisateur transporteur = utilisateurRepository.findById(idTransporteur)
            .orElseThrow(() -> new IllegalArgumentException("Transporteur introuvable"));

    if (transporteur.getRole() != Utilisateur.Role.transporteur) {
        throw new IllegalArgumentException("L'utilisateur cible n'est pas un transporteur");
    }

    if (etatIncident == null) {
        throw new IllegalArgumentException("L'etat d'incident est obligatoire");
    }

    if (transporteur.getEtatIncident() != etatIncident) {
        return List.of();
    }

    return commandeRepository.findByTransporteurIdOrderByDateDemandeDesc(idTransporteur);
}
public List<CommandeTransporteurPrincipalResponse> getCommandesEnRouteByTransporteurSecours(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurSecoursIdAndStatutOrderByDateDemandeDesc(
            idTransporteur,
            Commande.Statut.en_route);

    return commandes.stream()
            .map(commande -> {
                List<Produit> produits = produitRepository.findByCommandeId(commande.getId());
                TransporteurInfo transporteurInfo = null;
                String transporteurPrincipalId = commande.getTransporteurId();

                if (transporteurPrincipalId != null && !transporteurPrincipalId.isBlank()) {
                    Utilisateur transporteurPrincipal = utilisateurRepository.findById(transporteurPrincipalId)
                            .orElse(null);
                    if (transporteurPrincipal != null) {
                        transporteurInfo = new TransporteurInfo(
                                transporteurPrincipal.getId(),
                                transporteurPrincipal.getNom(),
                                transporteurPrincipal.getPrenom(),
                                transporteurPrincipal.getTelephone(),
                                transporteurPrincipal.getImage(),
                                transporteurPrincipal.getLatitude(),
                                transporteurPrincipal.getLongitude());
                    }
                }

                return new CommandeTransporteurPrincipalResponse(commande, produits, transporteurInfo);
            })
            .collect(Collectors.toList());
}
public List<TransporteurSecoursCommandesResponse> getTransporteursSecoursAvecCommandes(String idTransporteur) {
    List<Commande> commandesSecours = commandeRepository.findByTransporteurIdOrderByDateDemandeDesc(idTransporteur)
            .stream()
            .filter(commande -> commande.getTransporteurSecoursId() != null
                    && !commande.getTransporteurSecoursId().isBlank())
            .collect(Collectors.toList());

    if (commandesSecours.isEmpty()) {
        return List.of();
    }

    Map<String, Utilisateur> transporteursSecoursById = utilisateurRepository.findAllById(
                    commandesSecours.stream()
                            .map(Commande::getTransporteurSecoursId)
                            .distinct()
                            .collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toMap(Utilisateur::getId, Function.identity()));

    return commandesSecours.stream()
            .collect(Collectors.groupingBy(
                    Commande::getTransporteurSecoursId,
                    LinkedHashMap::new,
                    Collectors.toList()))
            .entrySet()
            .stream()
            .map(entry -> {
                Utilisateur transporteurSecours = transporteursSecoursById.get(entry.getKey());
                if (transporteurSecours == null) {
                    return null;
                }

                TransporteurInfo transporteurInfo = new TransporteurInfo(
                        transporteurSecours.getId(),
                        transporteurSecours.getNom(),
                        transporteurSecours.getPrenom(),
                        transporteurSecours.getTelephone(),
                        transporteurSecours.getImage(),
                        transporteurSecours.getLatitude(),
                        transporteurSecours.getLongitude());

                List<CommandeProduitsSecoursResponse> commandes = entry.getValue().stream()
                        .map(commande -> new CommandeProduitsSecoursResponse(
                                commande,
                                produitRepository.findByCommandeId(commande.getId())))
                        .collect(Collectors.toList());

                return new TransporteurSecoursCommandesResponse(transporteurInfo, commandes);
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
}
public List<Commande> getCommandesNonLivreesByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdAndStatutNotInOrderByDateDemandeDesc(
            idTransporteur,
            List.of(Commande.Statut.livree, Commande.Statut.ANNULEE, Commande.Statut.annulee));
}
public List<Commande> getCommandesLivreesByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdAndStatutOrderByDateDemandeDesc(
            idTransporteur, Commande.Statut.livree);
}
public BigDecimal getSommePrixCommandesLivreesByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatut(
            idTransporteur, Commande.Statut.livree);
    BigDecimal total = BigDecimal.ZERO;
    for (Commande commande : commandes) {
        if (commande.getPrix() != null) {
            total = total.add(commande.getPrix());
        }
    }
    return total;
}
public BigDecimal getSommePrixCommandesLivreesEnLigneByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatutAndModePaiement(
            idTransporteur, Commande.Statut.livree, Commande.ModePaiement.EN_LIGNE);
    BigDecimal total = BigDecimal.ZERO;
    for (Commande commande : commandes) {
        if (commande.getPrix() != null) {
            total = total.add(commande.getPrix());
        }
    }
    return total;
}
public BigDecimal getSommePrixCommandesLivreesHorsLigneByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatutAndModePaiementNot(
            idTransporteur, Commande.Statut.livree, Commande.ModePaiement.EN_LIGNE);
    BigDecimal total = BigDecimal.ZERO;
    for (Commande commande : commandes) {
        if (commande.getPrix() != null) {
            total = total.add(commande.getPrix());
        }
    }
    return total;
}
public List<Commande> getCommandesEnLigneByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdAndModePaiementOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE);
}
public List<Commande> getCommandesHorsLigneByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdAndModePaiementNotOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE);
}
public List<Commande> getCommandesEnLigneByTransporteur(String idTransporteur, Commande.Statut statut) {
    if (statut == null) {
        return getCommandesEnLigneByTransporteur(idTransporteur);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementAndStatutOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE, statut);
}
public List<Commande> getCommandesHorsLigneByTransporteur(String idTransporteur, Commande.Statut statut) {
    if (statut == null) {
        return getCommandesHorsLigneByTransporteur(idTransporteur);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementNotAndStatutOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE, statut);
}
public List<Commande> getCommandesEnLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone) {
    return commandeRepository.findByTransporteurIdAndModePaiementAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public List<Commande> getCommandesHorsLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone) {
    return commandeRepository.findByTransporteurIdAndModePaiementNotAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public List<Commande> getCommandesEnLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone,
        Commande.Statut statut) {
    if (statut == null) {
        return getCommandesEnLigneByTransporteurAndSousZone(idTransporteur, sousZone);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementAndStatutAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            statut,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public List<Commande> getCommandesHorsLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone,
        Commande.Statut statut) {
    if (statut == null) {
        return getCommandesHorsLigneByTransporteurAndSousZone(idTransporteur, sousZone);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementNotAndStatutAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            statut,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public Map<String, Float> getPourcentageRevenuParSousZoneLivreeByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatut(
            idTransporteur, Commande.Statut.livree);
    Map<Commande.SousZone, BigDecimal> totalParSousZone = new EnumMap<>(Commande.SousZone.class);
    BigDecimal total = BigDecimal.ZERO;

    for (Commande commande : commandes) {
        if (commande.getPrix() == null) {
            continue;
        }
        Commande.SousZone sousZone = commande.getSousZoneArrivee();
        if (sousZone == null) {
            continue;
        }
        totalParSousZone.merge(sousZone, commande.getPrix(), BigDecimal::add);
        total = total.add(commande.getPrix());
    }

    Map<String, Float> resultats = new LinkedHashMap<>();
    if (total.compareTo(BigDecimal.ZERO) == 0) {
        return resultats;
    }
    for (Map.Entry<Commande.SousZone, BigDecimal> entry : totalParSousZone.entrySet()) {
        BigDecimal pourcentage = entry.getValue()
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
        resultats.put(entry.getKey().name(), pourcentage.floatValue());
    }
    return resultats;
}
public List<Commande> getCommandesBySousZones(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee) {
    if (sousZonesDepart == null || sousZonesArrivee == null || sousZonesDepart.isEmpty() || sousZonesArrivee.isEmpty()) {
        throw new IllegalArgumentException("Les listes de sous-zones doivent etre renseignees");
    }
    return commandeRepository.findBySousZoneDepartInAndSousZoneArriveeIn(sousZonesDepart, sousZonesArrivee);
}
    public List<Commande> getCommandesBySousZonesAndVehicule(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee,
        TypeVehicule vehicule) {
    if (sousZonesDepart == null || sousZonesArrivee == null || sousZonesDepart.isEmpty() || sousZonesArrivee.isEmpty()) {
        throw new IllegalArgumentException("Les listes de sous-zones doivent etre renseignees");
    }
    if (vehicule == null) {
        throw new IllegalArgumentException("Le type de vehicule est obligatoire");
    }
    return commandeRepository.findBySousZoneDepartInAndSousZoneArriveeInAndVehiculeAndStatut(
            sousZonesDepart, sousZonesArrivee, vehicule, Commande.Statut.confirmer);
}

public List<String> trouverTransporteursMinCommandes(String commandeId) {
    Commande commande = commandeRepository.findById(commandeId)
            .orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));

    if (commande.getDateConfirmer() == null
            || LocalDateTime.now().isBefore(commande.getDateConfirmer().plusMinutes(3))) {
        return null;
    }

    if (commande.getSousZoneDepart() == null || commande.getSousZoneArrivee() == null) {
        return null;
    }

    List<Utilisateur> transporteurs = utilisateurRepository.findAll()
            .stream()
            .filter(u -> u.getRole() == Utilisateur.Role.transporteur)
            .filter(u -> u.getStatut() == Utilisateur.Statut.actif)
            .filter(u -> u.getEtatIncident() == null
                    || u.getEtatIncident() == Utilisateur.EtatIncident.RIEN)
            .filter(u -> transporteurCouvreSousZones(
                    u, commande.getSousZoneDepart(), commande.getSousZoneArrivee()))
            .collect(Collectors.toList());

    if (transporteurs.isEmpty()) {
        return null;
    }

    List<Commande.Statut> statutsEnCours = Arrays.asList(
            Commande.Statut.en_route,
            Commande.Statut.appelle_client_1,
            Commande.Statut.appelle_client_2,
            Commande.Statut.non_repondre_client_1,
            Commande.Statut.non_repondre_client_2
    );
    List<Commande> toutesLesCommandes = commandeRepository.findAll();

    long min = Long.MAX_VALUE;
    Map<String, Long> counts = new LinkedHashMap<>();
    for (Utilisateur transporteur : transporteurs) {
        long count = toutesLesCommandes.stream()
                .filter(c -> {
                    boolean chargePrincipale =
                            transporteur.getId().equals(c.getTransporteurId())
                                    && statutsEnCours.contains(c.getStatut());
                    boolean chargeSecours =
                            transporteur.getId().equals(c.getTransporteurSecoursId())
                                    && c.getStatut() == Commande.Statut.en_route;
                    return chargePrincipale || chargeSecours;
                })
                .count();
        counts.put(transporteur.getId(), count);
        if (count < min) {
            min = count;
        }
    }

    final long minFinal = min;
    List<String> result = counts.entrySet().stream()
            .filter(e -> e.getValue() == minFinal)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

    return result.isEmpty() ? null : result;
}

private boolean transporteurCouvreSousZones(
        Utilisateur transporteur,
        Commande.SousZone sousZoneDepart,
        Commande.SousZone sousZoneArrivee) {
    return mapContainsSousZone(transporteur.getZoneDepart(), sousZoneDepart)
            && mapContainsSousZone(transporteur.getZoneAriver(), sousZoneArrivee);
}

private boolean mapContainsSousZone(
        Map<String, List<String>> zonesMap,
        Commande.SousZone sousZone) {
    if (zonesMap == null || zonesMap.isEmpty() || sousZone == null) {
        return false;
    }
    String target = sousZone.name();
    for (List<String> zones : zonesMap.values()) {
        if (zones == null || zones.isEmpty()) {
            continue;
        }
        for (String z : zones) {
            if (z != null && z.equalsIgnoreCase(target)) {
                return true;
            }
        }
    }
    return false;
}

public Commande marquerDepartScanne(String id) {
    return commandeRepository.findById(id).map(commande -> {
        commande.marquerDepartScanne();
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

public Commande demarrerAppelClient1(String id) {
    return commandeRepository.findById(id).map(commande -> {
        if (commande.getStatut() != Statut.en_appelle) {
            throw new IllegalStateException("Statut invalide pour demarrer l'appel client 1");
        }
        commande.setStatut(Statut.appelle_client_1);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

public Commande marquerAppelClient1(String id) {
    return commandeRepository.findById(id).map(commande -> {
        if (commande.getStatut() != Statut.appelle_client_1) {
            throw new IllegalStateException("Statut invalide pour marquer l'appel client 1");
        }
        commande.setStatut(Statut.appelle_client_2);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

public Commande marquerNonReponseClient1(String id) {
    return commandeRepository.findById(id).map(commande -> {
        if (commande.getStatut() != Statut.appelle_client_1) {
            throw new IllegalStateException("Statut invalide pour marquer la non reponse client 1");
        }
        commande.setStatut(Statut.non_repondre_client_1);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

public Commande marquerAppelClient2(String id) {
    return commandeRepository.findById(id).map(commande -> {
        if (commande.getStatut() != Statut.appelle_client_2) {
            throw new IllegalStateException("Statut invalide pour marquer l'appel client 2");
        }
        commande.setStatut(Statut.en_route);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

public Commande marquerNonReponseClient2(String id) {
    return commandeRepository.findById(id).map(commande -> {
        if (commande.getStatut() != Statut.appelle_client_2) {
            throw new IllegalStateException("Statut invalide pour marquer la non reponse client 2");
        }
        commande.setStatut(Statut.non_repondre_client_2);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

public Commande marquerReceptionScanne(String id) {
    return commandeRepository.findById(id).map(commande -> {
        commande.marquerReceptionScanne();
        commande.setStatut(Statut.livree);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}


}

