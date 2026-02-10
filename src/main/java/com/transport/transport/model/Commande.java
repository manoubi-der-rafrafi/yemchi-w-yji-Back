package com.transport.transport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Document(collection = "commande")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) // accepte localisation_depart, mode_paiement, date_debut, etc.
public class Commande {

    @Id
    private String id;

    private String localisationDepart;
    private String destination;

    /** Période réelle de la course/livraison (si applicable) */
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    /** Date de création/demande de commande (nécessite @EnableMongoAuditing) */
    @CreatedDate
    private LocalDateTime dateDemande;
    /** Date et heure de confirmation de la commande */
    private LocalDateTime dateConfirmer;

    private Statut statut;
    private BigDecimal prix;
    private BigDecimal poids;
    private BigDecimal volume;
    private TypeVehicule vehicule;
    private ModePaiement modePaiement;
    private String instructions;

    /** Téléphone au départ : accepte "tel_depart" OU "telDepart" */
    @JsonAlias({ "tel_depart", "telDepart" })
    private String telDepart;

    @JsonAlias({ "tel_arrivee", "telArrivee" })
    private String telArrivee;
    /** Références par ID plutôt que @ManyToOne */
    // clientId : le front envoie souvent "clientId" (camel), mais avec @JsonNaming il attend "client_id"
    // => on accepte les deux grâce à JsonAlias
    @JsonAlias({ "client_id", "clientId" })
    private String clientId;

    // Le front t’envoie "id_transporteur" (non standard); SnakeCase produirait "transporteur_id"
    // => accepte "id_transporteur" et "transporteur_id"
    @JsonAlias({ "id_transporteur", "transporteur_id" })
    private String transporteurId;

    @LastModifiedDate
    private LocalDateTime majLe;
    @JsonAlias({ "id_amie", "idAmie" })
    private String idAmie;

    private Double latitudeDepart;
    private Double longitudeDepart;
    private Double latitudeDestination;
    private Double longitudeDestination;
    private Double distanceKm;

    @JsonAlias({ "sous_zone_depart", "sousZoneDepart" })
    @JsonProperty("sous_zone_depart")
    private SousZone sousZoneDepart;

    @JsonAlias({ "sous_zone_arrivee", "sousZoneArrivee" })
    @JsonProperty("sous_zone_arrivee")
    private SousZone sousZoneArrivee;

    @com.fasterxml.jackson.annotation.JsonAlias("zonePrincipaleDepart")
    @com.fasterxml.jackson.annotation.JsonProperty("zone_principale_depart")
    private Zone zonePrincipaleDepart;

    @com.fasterxml.jackson.annotation.JsonAlias("zonePrincipaleArrivee")
    @com.fasterxml.jackson.annotation.JsonProperty("zone_principale_arrivee")
    private Zone zonePrincipaleArrivee;

    private boolean qrCodeDepartScanne;        // indique si le scan a été fait
    private LocalDateTime dateScanDepart;      // horodatage du scan

    private boolean qrCodeReceptionScanne;
    private LocalDateTime dateScanReception;




    // --- Enums ---
    public enum Statut {
        ANNULEE,
        annulee,
        en_attende,
        en_attente,
        en_cours,
        livree,
        confirmer,
        envoyee,
        accepter,
        en_route,
        en_appelle,
        appelle_client_1,
        appelle_client_2,
        non_repondre_client_1,
        non_repondre_client_2
    }

    /**
 * Moment / lieu du paiement.
 */
public enum ModePaiement {
    @JsonProperty("en_ligne") EN_LIGNE,
    @JsonProperty("depart")   DEPART,
    @JsonProperty("arrivee")  ARRIVEE
}

// --- Enum pour les grandes zones (régions principales) ---
public enum Zone {
    GRAND_TUNIS,
    COTIER_NORD,
    NORD_EST,
    NORD_OUEST,
    CENTRE_EST,
    CENTRE,
    CENTRE_OUEST,
    SAHEL,
    SFAX,
    SUD_EST,
    SUD_OUEST,
    INTERIEUR
}

// --- Enum pour les sous-zones (zones détaillées pour scooters) ---
public enum SousZone {
    // Grand Tunis
    TUNIS,
    TUNIS_CENTRE,
    ARIANA,
    ARIANA_NORD,
    BEN_AROUS,
    BEN_AROUS_SUD,
    MANOUBA,
    MANOUBA_OUEST,

    // Côtier Nord
    BIZERTE,
    BIZERTE_METRO,
    NABEUL,
    NABEUL_HAMMAMET,
    KELIBIA_MENZEL_TEMIME,

    // Nord Ouest
    BEJA,
    JENDOUBA,
    KEF,
    SILIANA,

    // Centre Est
    SOUSSE,
    MONASTIR,
    MAHDIA,

    // Centre / Centre Ouest
    ZAGHOUAN,
    // Sfax
    SFAX,
    KAIROUAN,
    KASSERINE,
    SIDI_BOUZID,

    // Sud Est
    GABES,
    MEDENINE,
    TATAOUINE,
    DJERBA_ZARZIS,

    // Intérieur
    // Sud Ouest
    GAFSA,
    TOZEUR,
    KEBILI
}


    // --- Getters / Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLocalisationDepart() { return localisationDepart; }
    public void setLocalisationDepart(String localisationDepart) { this.localisationDepart = localisationDepart; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }
    public LocalDateTime getDateConfirmer() { return dateConfirmer; }
    public void setDateConfirmer(LocalDateTime dateConfirmer) { this.dateConfirmer = dateConfirmer; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public BigDecimal getPoids() { return poids; }
    public void setPoids(BigDecimal poids) { this.poids = poids; }

    public BigDecimal getVolume() { return volume; }
    public void setVolume(BigDecimal volume) { this.volume = volume; }

    public TypeVehicule getVehicule() { return vehicule; }
    public void setVehicule(TypeVehicule vehicule) { this.vehicule = vehicule; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public String getTelDepart() { return telDepart; }
    public void setTelDepart(String telDepart) { this.telDepart = telDepart; }

    public String getTelArrivee() { return telArrivee; }
    public void setTelArrivee(String telArrivee) { this.telArrivee = telArrivee; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getTransporteurId() { return transporteurId; }
    public void setTransporteurId(String transporteurId) { this.transporteurId = transporteurId; }

    public LocalDateTime getMajLe() { return majLe; }
    public void setMajLe(LocalDateTime majLe) { this.majLe = majLe; }
    public String getIdAmie() {
        return idAmie;
    }
    public void setIdAmie(String idAmie) {
        this.idAmie = idAmie;
    }

    public Double getLatitudeDepart() { return latitudeDepart; }
    public void setLatitudeDepart(Double latitudeDepart) { this.latitudeDepart = latitudeDepart; }

    public Double getLongitudeDepart() { return longitudeDepart; }
    public void setLongitudeDepart(Double longitudeDepart) { this.longitudeDepart = longitudeDepart; }

    public Double getLatitudeDestination() { return latitudeDestination; }
    public void setLatitudeDestination(Double latitudeDestination) { this.latitudeDestination = latitudeDestination; }

    public Double getLongitudeDestination() { return longitudeDestination; }
    public void setLongitudeDestination(Double longitudeDestination) { this.longitudeDestination = longitudeDestination; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
    // --- Sous-zone ---
    public SousZone getSousZoneDepart() {
        return sousZoneDepart;
    }

    public void setSousZoneDepart(SousZone sousZoneDepart) {
        this.sousZoneDepart = sousZoneDepart;
    }

    public SousZone getSousZoneArrivee() {
        return sousZoneArrivee;
    }

    public void setSousZoneArrivee(SousZone sousZoneArrivee) {
        this.sousZoneArrivee = sousZoneArrivee;
    }

    // --- Zone principale ---
    public Zone getZonePrincipaleDepart() {
        return zonePrincipaleDepart;
    }

    public void setZonePrincipaleDepart(Zone zonePrincipaleDepart) {
        this.zonePrincipaleDepart = zonePrincipaleDepart;
    }

    public Zone getZonePrincipaleArrivee() {
        return zonePrincipaleArrivee;
    }

    public void setZonePrincipaleArrivee(Zone zonePrincipaleArrivee) {
        this.zonePrincipaleArrivee = zonePrincipaleArrivee;
    }
    public boolean isQrCodeDepartScanne() {
        return qrCodeDepartScanne;
    }

    public void setQrCodeDepartScanne(boolean qrCodeDepartScanne) {
        this.qrCodeDepartScanne = qrCodeDepartScanne;
    }

    public LocalDateTime getDateScanDepart() {
        return dateScanDepart;
    }

    public void setDateScanDepart(LocalDateTime dateScanDepart) {
        this.dateScanDepart = dateScanDepart;
    }

    public boolean isQrCodeReceptionScanne() {
        return qrCodeReceptionScanne;
    }

    public void setQrCodeReceptionScanne(boolean qrCodeReceptionScanne) {
        this.qrCodeReceptionScanne = qrCodeReceptionScanne;
    }

    public LocalDateTime getDateScanReception() {
        return dateScanReception;
    }

    public void setDateScanReception(LocalDateTime dateScanReception) {
        this.dateScanReception = dateScanReception;
    }

    public void marquerDepartScanne() {
        this.qrCodeDepartScanne = true;
        this.dateScanDepart = LocalDateTime.now();
    }

    public void marquerReceptionScanne() {
        this.qrCodeReceptionScanne = true;
        this.dateScanReception = LocalDateTime.now();
    }
}
