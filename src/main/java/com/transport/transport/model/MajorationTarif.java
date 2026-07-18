package com.transport.transport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "majoration_tarif")
public class MajorationTarif {
    @Id
    private String id;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal pourcentageAjout;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String descriptionCause;
    @CreatedDate
    private LocalDateTime createdAt;
    private String createdByAdminId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getPourcentageAjout() { return pourcentageAjout; }
    public void setPourcentageAjout(BigDecimal pourcentageAjout) { this.pourcentageAjout = pourcentageAjout; }
    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
    public String getDescriptionCause() { return descriptionCause; }
    public void setDescriptionCause(String descriptionCause) { this.descriptionCause = descriptionCause; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedByAdminId() { return createdByAdminId; }
    public void setCreatedByAdminId(String createdByAdminId) { this.createdByAdminId = createdByAdminId; }
}
