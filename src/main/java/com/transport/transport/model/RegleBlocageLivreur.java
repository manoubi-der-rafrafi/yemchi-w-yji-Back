package com.transport.transport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "regle_blocage_livreur")
public class RegleBlocageLivreur {
    @Id
    private String id;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal montantBlocage;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal pourcentageReglement;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDateTime createdAt;
    private String createdByAdminId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getMontantBlocage() { return montantBlocage; }
    public void setMontantBlocage(BigDecimal montantBlocage) { this.montantBlocage = montantBlocage; }
    public BigDecimal getPourcentageReglement() { return pourcentageReglement; }
    public void setPourcentageReglement(BigDecimal pourcentageReglement) { this.pourcentageReglement = pourcentageReglement; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }
}
