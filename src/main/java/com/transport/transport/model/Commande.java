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

    private Statut statut;
    private BigDecimal prix;
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
    // --- Enums ---
    public enum Statut {
        annulee,
        en_attente,
        en_cours,
        livree,
        confirmer,
        envoyee
    }

    /**
 * Moment / lieu du paiement.
 */
public enum ModePaiement {
    @JsonProperty("en_ligne") EN_LIGNE,
    @JsonProperty("depart")   DEPART,
    @JsonProperty("arrivee")  ARRIVEE
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

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

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
}
