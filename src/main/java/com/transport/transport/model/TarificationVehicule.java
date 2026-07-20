package com.transport.transport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "tarification_vehicule")
public class TarificationVehicule {
    @Id
    private String id;
    private TypeVehicule typeVehicule;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal prixCommencement;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal prixCommencementLivreur;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal prixCommencementSociete;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal prixParKilometre;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal prixParKilometreLivreur;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal prixParKilometreSociete;

    @CreatedDate
    private LocalDateTime createdAt;
    private String createdByAdminId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public TypeVehicule getTypeVehicule() { return typeVehicule; }
    public void setTypeVehicule(TypeVehicule typeVehicule) { this.typeVehicule = typeVehicule; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public BigDecimal getPrixCommencement() { return prixCommencement; }
    public void setPrixCommencement(BigDecimal value) { this.prixCommencement = value; }
    public BigDecimal getPrixCommencementLivreur() { return prixCommencementLivreur; }
    public void setPrixCommencementLivreur(BigDecimal value) { this.prixCommencementLivreur = value; }
    public BigDecimal getPrixCommencementSociete() { return prixCommencementSociete; }
    public void setPrixCommencementSociete(BigDecimal value) { this.prixCommencementSociete = value; }
    public BigDecimal getPrixParKilometre() { return prixParKilometre; }
    public void setPrixParKilometre(BigDecimal value) { this.prixParKilometre = value; }
    public BigDecimal getPrixParKilometreLivreur() { return prixParKilometreLivreur; }
    public void setPrixParKilometreLivreur(BigDecimal value) { this.prixParKilometreLivreur = value; }
    public BigDecimal getPrixParKilometreSociete() { return prixParKilometreSociete; }
    public void setPrixParKilometreSociete(BigDecimal value) { this.prixParKilometreSociete = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }
}
