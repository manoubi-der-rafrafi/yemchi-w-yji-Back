package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.transport.transport.dto.TransporteurInfo;
import com.transport.transport.dto.partner.PartnerCommandeResponse;
import com.transport.transport.dto.partner.PartnerCreateCommandeRequest;
import com.transport.transport.dto.partner.PartnerTrackingResponse;
import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.security.PartnerPrincipal;

@Service
public class PartnerCommandeService {

    private final CommandeRepository commandeRepository;
    private final ProduitService produitService;
    private final UtilisateurRepository utilisateurRepository;
    private final VehicleAnalysisService vehicleAnalysisService;

    @Autowired
    public PartnerCommandeService(
            CommandeRepository commandeRepository,
            ProduitService produitService,
            UtilisateurRepository utilisateurRepository,
            VehicleAnalysisService vehicleAnalysisService) {
        this.commandeRepository = commandeRepository;
        this.produitService = produitService;
        this.utilisateurRepository = utilisateurRepository;
        this.vehicleAnalysisService = vehicleAnalysisService;
    }

    public PartnerCommandeResponse createConfirmedCommande(
            PartnerPrincipal principal,
            PartnerCreateCommandeRequest request) {
        validateCreateRequest(request);

        commandeRepository.findByPartenaireIdAndExternalOrderId(principal.getPartnerId(), request.externalOrderId())
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "externalOrderId deja utilise");
                });

        Commande commande = new Commande();
        commande.setPartenaireId(principal.getPartnerId());
        commande.setExternalBusinessId(principal.getExternalBusinessId());
        commande.setExternalOrderId(request.externalOrderId().trim());
        commande.setNomDepart(request.depart().nom());
        commande.setNomArrivee(request.arrivee().nom());
        commande.setTelDepart(request.depart().telephone());
        commande.setTelArrivee(request.arrivee().telephone());
        commande.setLocalisationDepart(request.depart().adresse());
        commande.setDestination(request.arrivee().adresse());
        commande.setLatitudeDepart(request.depart().latitude());
        commande.setLongitudeDepart(request.depart().longitude());
        commande.setLatitudeDestination(request.arrivee().latitude());
        commande.setLongitudeDestination(request.arrivee().longitude());
        commande.setInstructions(request.instructions());
        commande.setPrix(request.prix());
        commande.setModePaiement(request.modePaiement() != null ? request.modePaiement() : Commande.ModePaiement.EN_LIGNE);
        commande.setStatut(Commande.Statut.confirmer);
        commande.setDateConfirmer(LocalDateTime.now());
        commande.setDateDemande(LocalDateTime.now());
        commande.setVehicule(vehicleAnalysisService.resolveVehicleForPartnerProducts(request.produits()));

        Commande savedCommande = commandeRepository.save(commande);

        List<Produit> produits = new ArrayList<>();
        for (PartnerCreateCommandeRequest.ProductItem item : request.produits()) {
            Produit produit = new Produit();
            produit.setCommandeId(savedCommande.getId());
            produit.setNom(item.nom());
            produit.setType(item.type());
            produit.setQuantite(item.quantite() != null ? item.quantite() : 1);
            produit.setPoids(item.poids());
            produit.setLargeur(item.largeur());
            produit.setProfondeur(item.profondeur());
            produit.setHauteur(item.hauteur());
            produit.setDescription(item.description());
            produit.setImage1(item.image1());
            produit.setImage2(item.image2());
            produit.setImage3(item.image3());
            produits.add(produit);
        }

        return new PartnerCommandeResponse(savedCommande, produitService.createProduits(produits));
    }

    public TransporteurInfo getTransporteurByExternalOrderId(PartnerPrincipal principal, String externalOrderId) {
        Commande commande = getCommandeByExternalOrderId(principal, externalOrderId);
        if (commande.getTransporteurId() == null || commande.getTransporteurId().isBlank()) {
            return null;
        }
        Utilisateur transporteur = utilisateurRepository.findById(commande.getTransporteurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transporteur introuvable"));
        return toTransporteurInfo(transporteur);
    }

    public PartnerTrackingResponse getTrackingByExternalOrderId(PartnerPrincipal principal, String externalOrderId) {
        Commande commande = getCommandeByExternalOrderId(principal, externalOrderId);
        TransporteurInfo transporteur = null;
        Long etaMinutes = null;

        if (commande.getTransporteurId() != null && !commande.getTransporteurId().isBlank()) {
            Utilisateur transporteurEntity = utilisateurRepository.findById(commande.getTransporteurId()).orElse(null);
            if (transporteurEntity != null) {
                transporteur = toTransporteurInfo(transporteurEntity);
                etaMinutes = estimateArrivalMinutes(
                        transporteurEntity.getLatitude(),
                        transporteurEntity.getLongitude(),
                        commande.getLatitudeDestination(),
                        commande.getLongitudeDestination());
            }
        }

        return new PartnerTrackingResponse(
                commande.getExternalOrderId(),
                commande.getId(),
                commande.getStatut() != null ? commande.getStatut().name() : null,
                transporteur,
                commande.getLatitudeDestination(),
                commande.getLongitudeDestination(),
                etaMinutes);
    }

    private Commande getCommandeByExternalOrderId(PartnerPrincipal principal, String externalOrderId) {
        return commandeRepository.findByPartenaireIdAndExternalOrderId(principal.getPartnerId(), externalOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable"));
    }

    private TransporteurInfo toTransporteurInfo(Utilisateur transporteur) {
        return new TransporteurInfo(
                transporteur.getId(),
                transporteur.getNom(),
                transporteur.getPrenom(),
                transporteur.getTelephone(),
                transporteur.getImage(),
                transporteur.getLatitude(),
                transporteur.getLongitude());
    }

    private void validateCreateRequest(PartnerCreateCommandeRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload commande obligatoire");
        }
        if (request.externalOrderId() == null || request.externalOrderId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "externalOrderId obligatoire");
        }
        if (request.depart() == null || request.arrivee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Depart et arrivee obligatoires");
        }
        if (request.depart().adresse() == null || request.depart().adresse().isBlank()
                || request.arrivee().adresse() == null || request.arrivee().adresse().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adresses depart/arrivee obligatoires");
        }
        if (request.produits() == null || request.produits().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Au moins un produit est obligatoire");
        }
    }

    private TypeVehicule estimateVehicule(List<PartnerCreateCommandeRequest.ProductItem> produits) {
        BigDecimal totalPoids = BigDecimal.ZERO;
        BigDecimal totalVolume = BigDecimal.ZERO;

        for (PartnerCreateCommandeRequest.ProductItem item : produits) {
            int quantite = item.quantite() != null && item.quantite() > 0 ? item.quantite() : 1;
            if (item.poids() != null) {
                totalPoids = totalPoids.add(item.poids().multiply(BigDecimal.valueOf(quantite)));
            }
            if (item.largeur() != null && item.profondeur() != null && item.hauteur() != null) {
                BigDecimal volume = item.largeur()
                        .multiply(item.profondeur())
                        .multiply(item.hauteur())
                        .multiply(BigDecimal.valueOf(quantite))
                        .divide(BigDecimal.valueOf(1_000_000), 3, RoundingMode.HALF_UP);
                totalVolume = totalVolume.add(volume);
            }
        }

        if (lte(totalPoids, "15") && lte(totalVolume, "0.125")) {
            return TypeVehicule.DEUX_ROUES_MOTORISES;
        }
        if (lte(totalPoids, "80") && lte(totalVolume, "1.000")) {
            return TypeVehicule.VEHICULE_PARTICULIER;
        }
        if (lte(totalPoids, "300") && lte(totalVolume, "3.000")) {
            return TypeVehicule.VEHICULE_UTILITAIRE_LEGER;
        }
        if (lte(totalPoids, "800") && lte(totalVolume, "8.000")) {
            return TypeVehicule.FOURGON_MINIBUS;
        }
        return TypeVehicule.GROS_UTILITAIRE;
    }

    private boolean lte(BigDecimal value, String limit) {
        return value.compareTo(new BigDecimal(limit)) <= 0;
    }

    private Long estimateArrivalMinutes(
            double driverLatitude,
            double driverLongitude,
            Double destinationLatitude,
            Double destinationLongitude) {
        if (destinationLatitude == null || destinationLongitude == null) {
            return null;
        }
        double distanceKm = haversineKm(driverLatitude, driverLongitude, destinationLatitude, destinationLongitude);
        double averageSpeedKmPerHour = 30.0;
        return Math.max(1L, Math.round((distanceKm / averageSpeedKmPerHour) * 60.0));
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }
}
