package com.transport.transport.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonAlias;

@Document(collection = "facture")
public class Facture {

    @Id
    private String id;

    private BigDecimal montant;
    private String dateTimle;
    private String image;

    @JsonAlias({ "id_livreur", "idLivreur" })
    @Field("id_livreur")
    private String idLivreur;

    private FactureType type;

    public enum FactureType {
        ENTREPRISE_VERSE_LIVREUR,
        LIVREUR_VERSE_ENTREPRISE
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getDateTimle() { return dateTimle; }
    public void setDateTimle(String dateTimle) { this.dateTimle = dateTimle; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getIdLivreur() { return idLivreur; }
    public void setIdLivreur(String idLivreur) { this.idLivreur = idLivreur; }

    public FactureType getType() { return type; }
    public void setType(FactureType type) { this.type = type; }
}
