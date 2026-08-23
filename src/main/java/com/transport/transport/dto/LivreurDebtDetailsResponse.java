package com.transport.transport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LivreurDebtDetailsResponse(
        String livreurId,
        BigDecimal produitsB2cAPayer,
        BigDecimal coursesAPayer,
        BigDecimal detteBrute,
        BigDecimal creditEnLigneDisponible,
        BigDecimal detteNette,
        BigDecimal creditLivreur,
        BigDecimal paiementsEnAttente,
        List<CommandeDette> commandes,
        List<PaiementDetail> paiements) {

    public record CommandeDette(
            String commandeId,
            String externalOrderId,
            String sourceCommande,
            LocalDateTime dateLivraison,
            BigDecimal montantInitial,
            BigDecimal montantPaye,
            BigDecimal resteAPayer,
            String statutFinancier,
            List<LigneDette> lignes) {}

    public record LigneDette(
            String ligneId,
            String type,
            String produitId,
            String nom,
            Integer quantite,
            BigDecimal prixUnitaire,
            BigDecimal montantInitial,
            BigDecimal montantPaye,
            BigDecimal resteAPayer,
            String statutFinancier) {}

    public record PaiementDetail(
            String factureId,
            BigDecimal montant,
            String statut,
            String date,
            BigDecimal montantAffecte,
            BigDecimal montantNonAffecte,
            List<AffectationPaiement> affectations) {}

    public record AffectationPaiement(
            String commandeId,
            String ligneId,
            String type,
            String libelle,
            BigDecimal montant) {}
}
