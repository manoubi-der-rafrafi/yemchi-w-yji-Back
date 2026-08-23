package com.transport.transport.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "etat_financier_livreur")
public class EtatFinancierLivreur {
    @Id
    private String id;
    private String livreurId;
    private BigDecimal produitsB2cAPayer;
    private BigDecimal coursesAPayer;
    private BigDecimal detteBrute;
    private BigDecimal creditEnLigneDisponible;
    private BigDecimal detteNette;
    private BigDecimal creditLivreur;
    private BigDecimal paiementsEnAttente;
    private LocalDateTime calculeLe;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLivreurId() { return livreurId; }
    public void setLivreurId(String livreurId) { this.livreurId = livreurId; }
    public BigDecimal getProduitsB2cAPayer() { return produitsB2cAPayer; }
    public void setProduitsB2cAPayer(BigDecimal value) { this.produitsB2cAPayer = value; }
    public BigDecimal getCoursesAPayer() { return coursesAPayer; }
    public void setCoursesAPayer(BigDecimal value) { this.coursesAPayer = value; }
    public BigDecimal getDetteBrute() { return detteBrute; }
    public void setDetteBrute(BigDecimal value) { this.detteBrute = value; }
    public BigDecimal getCreditEnLigneDisponible() { return creditEnLigneDisponible; }
    public void setCreditEnLigneDisponible(BigDecimal value) { this.creditEnLigneDisponible = value; }
    public BigDecimal getDetteNette() { return detteNette; }
    public void setDetteNette(BigDecimal value) { this.detteNette = value; }
    public BigDecimal getCreditLivreur() { return creditLivreur; }
    public void setCreditLivreur(BigDecimal value) { this.creditLivreur = value; }
    public BigDecimal getPaiementsEnAttente() { return paiementsEnAttente; }
    public void setPaiementsEnAttente(BigDecimal value) { this.paiementsEnAttente = value; }
    public LocalDateTime getCalculeLe() { return calculeLe; }
    public void setCalculeLe(LocalDateTime value) { this.calculeLe = value; }
}
