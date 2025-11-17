package com.transport.transport.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "demande")
public class Demande {

    @Id
    private String id;

    private String nom;
    private String prenom;
    private String numero;
    private String imageCarteIdentiteFace;
    private String imageCarteIdentiteArriere;
    private String imagePermis;
    private String imageCarteGrise;
    private String imageAssurance;

    @CreatedDate
    private LocalDateTime dateDemande;

    private LocalDateTime dateReponse;
    private boolean reponse;

    public Demande() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getImageCarteIdentiteFace() {
        return imageCarteIdentiteFace;
    }

    public void setImageCarteIdentiteFace(String imageCarteIdentiteFace) {
        this.imageCarteIdentiteFace = imageCarteIdentiteFace;
    }

    public String getImageCarteIdentiteArriere() {
        return imageCarteIdentiteArriere;
    }

    public void setImageCarteIdentiteArriere(String imageCarteIdentiteArriere) {
        this.imageCarteIdentiteArriere = imageCarteIdentiteArriere;
    }

    public String getImagePermis() {
        return imagePermis;
    }

    public void setImagePermis(String imagePermis) {
        this.imagePermis = imagePermis;
    }

    public String getImageCarteGrise() {
        return imageCarteGrise;
    }

    public void setImageCarteGrise(String imageCarteGrise) {
        this.imageCarteGrise = imageCarteGrise;
    }

    public String getImageAssurance() {
        return imageAssurance;
    }

    public void setImageAssurance(String imageAssurance) {
        this.imageAssurance = imageAssurance;
    }

    public LocalDateTime getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(LocalDateTime dateDemande) {
        this.dateDemande = dateDemande;
    }

    public LocalDateTime getDateReponse() {
        return dateReponse;
    }

    public void setDateReponse(LocalDateTime dateReponse) {
        this.dateReponse = dateReponse;
    }

    public boolean isReponse() {
        return reponse;
    }

    public void setReponse(boolean reponse) {
        this.reponse = reponse;
    }
}
