package com.transport.transport.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "produit")
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100)
    private String nom;

    @Column(length = 100)
    private String type;

    @Column(precision = 5, scale = 2)
    private BigDecimal largeur;

    @Column(precision = 5, scale = 2)
    private BigDecimal profondeur;

    @Column(precision = 5, scale = 2)
    private BigDecimal hauteur;

    @Column(precision = 6, scale = 2)
    private BigDecimal poids;

    private Integer quantite;

    @Column(length = 100)
    private String facade;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String image1;

    @Column(length = 255)
    private String image2;

    @Column(length = 255)
    private String image3;

    @Column(precision = 10, scale = 2)
    private BigDecimal prix;

    // Relation ManyToOne avec Commande (clé étrangère id_commande)
    @ManyToOne
    @JoinColumn(name = "id_commande")
    private Commande commande;

    // Getters et Setters

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

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getLargeur() {
        return largeur;
    }
    public void setLargeur(BigDecimal largeur) {
        this.largeur = largeur;
    }

    public BigDecimal getProfondeur() {
        return profondeur;
    }
    public void setProfondeur(BigDecimal profondeur) {
        this.profondeur = profondeur;
    }

    public BigDecimal getHauteur() {
        return hauteur;
    }
    public void setHauteur(BigDecimal hauteur) {
        this.hauteur = hauteur;
    }

    public BigDecimal getPoids() {
        return poids;
    }
    public void setPoids(BigDecimal poids) {
        this.poids = poids;
    }

    public Integer getQuantite() {
        return quantite;
    }
    public void setQuantite(Integer quantite) {
        this.quantite = quantite;
    }

    public String getFacade() {
        return facade;
    }
    public void setFacade(String facade) {
        this.facade = facade;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getImage1() {
        return image1;
    }
    public void setImage1(String image1) {
        this.image1 = image1;
    }

    public String getImage2() {
        return image2;
    }
    public void setImage2(String image2) {
        this.image2 = image2;
    }

    public String getImage3() {
        return image3;
    }
    public void setImage3(String image3) {
        this.image3 = image3;
    }

    public Commande getCommande() {
        return commande;
    }
    public void setCommande(Commande commande) {
        this.commande = commande;
    }
    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

}
