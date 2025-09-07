package com.transport.transport.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "utilisateur")
public class Utilisateur {

    @Id
    private String id;

    private String nom;
    private String prenom;
    private LocalDate dateNaissance;

    private String email;
    private String motDePasse;
    private String telephone;

    private Role role = Role.client;

    private String adresse;

    /** chemin/URL vers l'image de profil */
    private String image;

    private Statut statut = Statut.actif;

    @CreatedDate
    private LocalDateTime dateCreation;

    public enum Role { client, transporteur, admin }
    public enum Statut { actif, inactif, banni }

    public Utilisateur() {}

    // --- Getters/Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
