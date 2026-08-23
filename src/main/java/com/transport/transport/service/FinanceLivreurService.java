package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.transport.transport.dto.StatutFinancierLivreurResponse;
import com.transport.transport.model.Commande;
import com.transport.transport.model.EtatBlocageLivreur;
import com.transport.transport.model.Facture;
import com.transport.transport.model.RegleBlocageLivreur;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.EtatBlocageLivreurRepository;
import com.transport.transport.repository.FactureRepository;
import com.transport.transport.repository.RegleBlocageLivreurRepository;

@Service
public class FinanceLivreurService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CommandeRepository commandeRepository;
    private final FactureRepository factureRepository;
    private final RegleBlocageLivreurRepository regleRepository;
    private final EtatBlocageLivreurRepository etatRepository;

    public FinanceLivreurService(
            CommandeRepository commandeRepository,
            FactureRepository factureRepository,
            RegleBlocageLivreurRepository regleRepository,
            EtatBlocageLivreurRepository etatRepository) {
        this.commandeRepository = commandeRepository;
        this.factureRepository = factureRepository;
        this.regleRepository = regleRepository;
        this.etatRepository = etatRepository;
    }

    public StatutFinancierLivreurResponse calculerEtSynchroniser(String livreurId) {
        List<Commande> livrees = commandeRepository.findByTransporteurIdAndStatut(
                livreurId,
                Commande.Statut.livree);

        BigDecimal detteProduits = BigDecimal.ZERO;
        BigDecimal detteCourses = BigDecimal.ZERO;
        BigDecimal creditEnLigne = BigDecimal.ZERO;

        for (Commande commande : livrees) {
            if (commande.getModePaiement() == Commande.ModePaiement.EN_LIGNE) {
                creditEnLigne = creditEnLigne.add(partLivreur(commande));
                continue;
            }
            detteCourses = detteCourses.add(partSociete(commande));
            if (commande.getSourceCommande() == Commande.SourceCommande.B2C
                    || (commande.getPartenaireId() != null && !commande.getPartenaireId().isBlank())) {
                detteProduits = detteProduits.add(nonNegatif(commande.getPrixProduitsPartenaire()));
            }
        }

        BigDecimal paiementsLivreur = sommeFacturesAcceptees(
                livreurId,
                Facture.FactureType.LIVREUR_VERSE_ENTREPRISE);
        BigDecimal versementsEntreprise = sommeFacturesAcceptees(
                livreurId,
                Facture.FactureType.ENTREPRISE_VERSE_LIVREUR);
        BigDecimal solde = creditEnLigne
                .subtract(detteProduits)
                .subtract(detteCourses)
                .subtract(versementsEntreprise)
                .add(paiementsLivreur);
        BigDecimal detteNette = solde.signum() < 0 ? solde.abs() : BigDecimal.ZERO;

        RegleBlocageLivreur regle = regleRepository
                .findFirstByDateFinIsNullOrderByDateDebutDesc()
                .orElse(null);
        if (regle == null) {
            return reponseSansConfiguration(
                    detteNette, detteProduits, detteCourses, creditEnLigne,
                    paiementsLivreur, versementsEntreprise);
        }

        EtatBlocageLivreur etat = etatRepository.findByLivreurId(livreurId).orElse(null);
        if (etat != null && etat.isBloque()) {
            BigDecimal payeDepuisBlocage = paiementsLivreur
                    .subtract(nonNegatif(etat.getPaiementsAcceptesAuBlocage()))
                    .max(BigDecimal.ZERO);
            BigDecimal paiementMinimum = nonNegatif(etat.getPaiementMinimum());
            BigDecimal cibleDette = nonNegatif(etat.getCibleDette());
            boolean paiementSuffisant = payeDepuisBlocage.compareTo(paiementMinimum) >= 0;
            boolean detteSuffisammentReduite = detteNette.compareTo(cibleDette) <= 0;
            if (paiementSuffisant && detteSuffisammentReduite) {
                etat.setBloque(false);
                etat.setDebloqueLe(LocalDateTime.now());
                etatRepository.save(etat);
            }
        }

        if ((etat == null || !etat.isBloque())
                && detteNette.compareTo(regle.getMontantBlocage()) >= 0) {
            BigDecimal paiementMinimum = detteNette
                    .multiply(regle.getPourcentageReglement())
                    .divide(HUNDRED, 3, RoundingMode.HALF_UP);
            if (etat == null) {
                etat = new EtatBlocageLivreur();
                etat.setLivreurId(livreurId);
            }
            etat.setBloque(true);
            etat.setRegleBlocageId(regle.getId());
            etat.setDetteAuBlocage(detteNette);
            etat.setPaiementMinimum(paiementMinimum);
            etat.setCibleDette(detteNette.subtract(paiementMinimum).max(BigDecimal.ZERO));
            etat.setPaiementsAcceptesAuBlocage(paiementsLivreur);
            etat.setBloqueLe(LocalDateTime.now());
            etat.setDebloqueLe(null);
            etat = etatRepository.save(etat);
        }

        boolean bloque = etat != null && etat.isBloque();
        BigDecimal paiementRestant = BigDecimal.ZERO;
        BigDecimal paiementMinimum = BigDecimal.ZERO;
        BigDecimal cibleDette = BigDecimal.ZERO;
        LocalDateTime bloqueLe = null;
        if (bloque) {
            paiementMinimum = nonNegatif(etat.getPaiementMinimum());
            BigDecimal payeDepuisBlocage = paiementsLivreur
                    .subtract(nonNegatif(etat.getPaiementsAcceptesAuBlocage()))
                    .max(BigDecimal.ZERO);
            paiementRestant = paiementMinimum.subtract(payeDepuisBlocage).max(BigDecimal.ZERO);
            cibleDette = nonNegatif(etat.getCibleDette());
            bloqueLe = etat.getBloqueLe();
        }

        String message = bloque
                ? "Nouvelles commandes bloquees. Reglez le montant indique puis attendez la validation de l'administrateur."
                : "Compte autorise a accepter de nouvelles commandes.";
        return new StatutFinancierLivreurResponse(
                true, bloque, scale(detteNette), scale(detteProduits), scale(detteCourses),
                scale(creditEnLigne), scale(paiementsLivreur), scale(versementsEntreprise),
                scale(regle.getMontantBlocage()), scale(regle.getPourcentageReglement()),
                scale(paiementMinimum), scale(paiementRestant), scale(cibleDette), bloqueLe, message);
    }

    private StatutFinancierLivreurResponse reponseSansConfiguration(
            BigDecimal detteNette,
            BigDecimal detteProduits,
            BigDecimal detteCourses,
            BigDecimal creditEnLigne,
            BigDecimal paiementsLivreur,
            BigDecimal versementsEntreprise) {
        return new StatutFinancierLivreurResponse(
                false, false, scale(detteNette), scale(detteProduits), scale(detteCourses),
                scale(creditEnLigne), scale(paiementsLivreur), scale(versementsEntreprise),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, "Aucune regle de blocage active.");
    }

    private BigDecimal sommeFacturesAcceptees(String livreurId, Facture.FactureType type) {
        return factureRepository.findByIdLivreurAndTypeAndConfirmer(
                livreurId, type, Facture.ConfirmationStatut.ACCEPTER)
                .stream()
                .map(Facture::getMontant)
                .map(this::nonNegatif)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal partLivreur(Commande commande) {
        if (commande.getPrixLivreur() != null) return nonNegatif(commande.getPrixLivreur());
        return nonNegatif(commande.getPrix()).divide(new BigDecimal("2"), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal partSociete(Commande commande) {
        if (commande.getPrixSociete() != null) return nonNegatif(commande.getPrixSociete());
        return nonNegatif(commande.getPrix()).divide(new BigDecimal("2"), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegatif(BigDecimal value) {
        return value == null || value.signum() < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return nonNegatif(value).setScale(3, RoundingMode.HALF_UP);
    }
}
