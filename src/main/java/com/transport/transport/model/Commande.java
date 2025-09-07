package com.transport.transport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "commande")
public class Commande {

    @Id
    private String id;

    private String localisationDepart;
    private String destination;

    /** Période réelle de la course/livraison (si applicable) */
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    /** Date de création/demande de commande */
    @CreatedDate
    private LocalDateTime dateDemande;

    private Statut statut;

    private BigDecimal prix;

    private ModePaiement modePaiement;

    private String instructions;

    /** Références par ID plutôt que @ManyToOne */
    private String clientId;        // Utilisateur.id
    private String transporteurId;  // Utilisateur.id

    @LastModifiedDate
    private LocalDateTime majLe;

    // --- Enums ---
    public enum Statut { en_attente, en_cours, livree, annulee, confirmer }
    public enum ModePaiement { cash, en_ligne, carte }

    // --- Getters/Setters ---
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

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getTransporteurId() { return transporteurId; }
    public void setTransporteurId(String transporteurId) { this.transporteurId = transporteurId; }

    public LocalDateTime getMajLe() { return majLe; }
    public void setMajLe(LocalDateTime majLe) { this.majLe = majLe; }
}
