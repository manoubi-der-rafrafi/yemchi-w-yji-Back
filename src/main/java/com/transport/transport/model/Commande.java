package com.transport.transport.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commande")
public class Commande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "localisation_depart", length = 255)
    private String localisationDepart;

    @Column(length = 255)
    private String destination;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "date_demande")
    private LocalDateTime dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Statut statut;

    @Column(precision = 10, scale = 2)
    private BigDecimal prix;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    private ModePaiement modePaiement;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @ManyToOne
    @JoinColumn(name = "id_client")
    private Utilisateur client;

    @ManyToOne
    @JoinColumn(name = "id_transporteur")
    private Utilisateur transporteur;

    // Enumérations
    public enum Statut {
        en_attente, en_cours, livree, annulee , confirmer
    }

    public enum ModePaiement {
        cash, en_ligne, carte
    }

    // Getters et Setters

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getLocalisationDepart() {
        return localisationDepart;
    }
    public void setLocalisationDepart(String localisationDepart) {
        this.localisationDepart = localisationDepart;
    }

    public String getDestination() {
        return destination;
    }
    public void setDestination(String destination) {
        this.destination = destination;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }
    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }
    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public LocalDateTime getDateDemande() {
        return dateDemande;
    }
    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    public Statut getStatut() {
        return statut;
    }
    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public BigDecimal getPrix() {
        return prix;
    }
    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public ModePaiement getModePaiement() {
        return modePaiement;
    }
    public void setModePaiement(ModePaiement modePaiement) {
        this.modePaiement = modePaiement;
    }

    public String getInstructions() {
        return instructions;
    }
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Utilisateur getClient() {
        return client;
    }
    public void setClient(Utilisateur client) {
        this.client = client;
    }

    public Utilisateur getTransporteur() {
        return transporteur;
    }
    public void setTransporteur(Utilisateur transporteur) {
        this.transporteur = transporteur;
    }
}
