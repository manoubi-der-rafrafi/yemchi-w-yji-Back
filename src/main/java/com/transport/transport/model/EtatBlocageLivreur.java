package com.transport.transport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Document(collection = "etat_blocage_livreur")
public class EtatBlocageLivreur {
    @Id
    private String id;
    @Indexed(unique = true)
    private String livreurId;
    private boolean bloque;
    private String regleBlocageId;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal detteAuBlocage;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal paiementMinimum;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal cibleDette;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal paiementsAcceptesAuBlocage;
    private LocalDateTime bloqueLe;
    private LocalDateTime debloqueLe;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLivreurId() { return livreurId; }
    public void setLivreurId(String livreurId) { this.livreurId = livreurId; }
    public boolean isBloque() { return bloque; }
    public void setBloque(boolean bloque) { this.bloque = bloque; }
    public String getRegleBlocageId() { return regleBlocageId; }
    public void setRegleBlocageId(String regleBlocageId) { this.regleBlocageId = regleBlocageId; }
    public BigDecimal getDetteAuBlocage() { return detteAuBlocage; }
    public void setDetteAuBlocage(BigDecimal detteAuBlocage) { this.detteAuBlocage = detteAuBlocage; }
    public BigDecimal getPaiementMinimum() { return paiementMinimum; }
    public void setPaiementMinimum(BigDecimal paiementMinimum) { this.paiementMinimum = paiementMinimum; }
    public BigDecimal getCibleDette() { return cibleDette; }
    public void setCibleDette(BigDecimal cibleDette) { this.cibleDette = cibleDette; }
    public BigDecimal getPaiementsAcceptesAuBlocage() { return paiementsAcceptesAuBlocage; }
    public void setPaiementsAcceptesAuBlocage(BigDecimal value) { this.paiementsAcceptesAuBlocage = value; }
    public LocalDateTime getBloqueLe() { return bloqueLe; }
    public void setBloqueLe(LocalDateTime bloqueLe) { this.bloqueLe = bloqueLe; }
    public LocalDateTime getDebloqueLe() { return debloqueLe; }
    public void setDebloqueLe(LocalDateTime debloqueLe) { this.debloqueLe = debloqueLe; }
}
