package com.transport.transport.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document(collection = "produit")
public class Produit {

    @Id
    private String id;

    private String nom;
    private String type;

    private BigDecimal largeur;
    private BigDecimal profondeur;
    private BigDecimal hauteur;
    private BigDecimal poids;

    private Integer quantite;
    private Boolean affecter = false;
    private Integer quantiteAffecter;

    private String facade;
    private String description;

    private String image1;
    private String image2;
    private String image3;

    private BigDecimal prix;

    /** Référence à la commande (ID Mongo) au lieu de @ManyToOne */
    private String commandeId; // Commande.id

    // --- Getters/Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getLargeur() { return largeur; }
    public void setLargeur(BigDecimal largeur) { this.largeur = largeur; }

    public BigDecimal getProfondeur() { return profondeur; }
    public void setProfondeur(BigDecimal profondeur) { this.profondeur = profondeur; }

    public BigDecimal getHauteur() { return hauteur; }
    public void setHauteur(BigDecimal hauteur) { this.hauteur = hauteur; }

    public BigDecimal getPoids() { return poids; }
    public void setPoids(BigDecimal poids) { this.poids = poids; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public Boolean getAffecter() { return affecter; }
    public void setAffecter(Boolean affecter) { this.affecter = affecter; }

    public Integer getQuantiteAffecter() { return quantiteAffecter; }
    public void setQuantiteAffecter(Integer quantiteAffecter) { this.quantiteAffecter = quantiteAffecter; }

    public String getFacade() { return facade; }
    public void setFacade(String facade) { this.facade = facade; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage1() { return image1; }
    public void setImage1(String image1) { this.image1 = image1; }

    public String getImage2() { return image2; }
    public void setImage2(String image2) { this.image2 = image2; }

    public String getImage3() { return image3; }
    public void setImage3(String image3) { this.image3 = image3; }

    public BigDecimal getPrix() { return prix; }
    public void setPrix(BigDecimal prix) { this.prix = prix; }

    public String getCommandeId() { return commandeId; }
    public void setCommandeId(String commandeId) { this.commandeId = commandeId; }
}
