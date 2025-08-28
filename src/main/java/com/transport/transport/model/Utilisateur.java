package com.transport.transport.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String nom;

    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    private String email;

    @Column(name = "mot_de_passe")
    private String motDePasse;

    private String telephone;

    @Enumerated(EnumType.STRING)
    private Role role = Role.client;

    private String adresse;

    /** chemin/URL ou nom de fichier de l'image de profil */
    @Column(name = "image", length = 255)
    private String image;   // <-- NOUVEL ATTRIBUT

    @Enumerated(EnumType.STRING)
    private Statut statut = Statut.actif;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public enum Role {
        client, transporteur, admin
    }

    public enum Statut {
        actif, inactif, banni
    }

    // Constructeur par défaut
    public Utilisateur() {}

    // Getters / Setters
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }
    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public LocalDate getDateNaissance() {
        return dateNaissance;
    }
    public void setDateNaissance(LocalDate dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }
    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getTelephone() {
        return telephone;
    }
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Role getRole() {
        return role;
    }
    public void setRole(Role role) {
        this.role = role;
    }

    public String getAdresse() {
        return adresse;
    }
    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    // --- Getters/Setters pour image ---
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }

    public Statut getStatut() {
        return statut;
    }
    public void setStatut(Statut statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
}
