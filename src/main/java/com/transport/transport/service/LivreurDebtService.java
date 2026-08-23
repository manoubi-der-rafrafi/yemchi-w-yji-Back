package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.transport.transport.dto.LivreurDebtDetailsResponse;
import com.transport.transport.dto.LivreurDebtDetailsResponse.AffectationPaiement;
import com.transport.transport.dto.LivreurDebtDetailsResponse.CommandeDette;
import com.transport.transport.dto.LivreurDebtDetailsResponse.LigneDette;
import com.transport.transport.dto.LivreurDebtDetailsResponse.PaiementDetail;
import com.transport.transport.model.Commande;
import com.transport.transport.model.Facture;
import com.transport.transport.model.EtatFinancierLivreur;
import com.transport.transport.model.Produit;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.EtatFinancierLivreurRepository;
import com.transport.transport.repository.FactureRepository;
import com.transport.transport.repository.ProduitRepository;

@Service
public class LivreurDebtService {
    private static final BigDecimal ZERO = new BigDecimal("0.000");

    private final CommandeRepository commandeRepository;
    private final ProduitRepository produitRepository;
    private final FactureRepository factureRepository;
    private final EtatFinancierLivreurRepository etatFinancierRepository;

    public LivreurDebtService(
            CommandeRepository commandeRepository,
            ProduitRepository produitRepository,
            FactureRepository factureRepository,
            EtatFinancierLivreurRepository etatFinancierRepository) {
        this.commandeRepository = commandeRepository;
        this.produitRepository = produitRepository;
        this.factureRepository = factureRepository;
        this.etatFinancierRepository = etatFinancierRepository;
    }

    public LivreurDebtDetailsResponse details(String livreurId) {
        List<Commande> livrees = new ArrayList<>(commandeRepository.findByTransporteurIdAndStatut(
                livreurId, Commande.Statut.livree));
        livrees.sort(Comparator
                .comparing(this::dateReference, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Commande::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        List<String> commandeIds = livrees.stream().map(Commande::getId).filter(id -> id != null).toList();
        Map<String, List<Produit>> produitsParCommande = new HashMap<>();
        if (!commandeIds.isEmpty()) {
            for (Produit produit : produitRepository.findByCommandeIdIn(commandeIds)) {
                produitsParCommande.computeIfAbsent(produit.getCommandeId(), ignored -> new ArrayList<>()).add(produit);
            }
        }

        List<MutableCommandeDette> dettes = new ArrayList<>();
        BigDecimal creditEnLigne = ZERO;
        for (Commande commande : livrees) {
            if (commande.getModePaiement() == Commande.ModePaiement.EN_LIGNE) {
                creditEnLigne = creditEnLigne.add(partLivreur(commande));
                continue;
            }
            if (!encaisseParLivreur(commande)) continue;
            dettes.add(construireDette(commande, produitsParCommande.getOrDefault(commande.getId(), List.of())));
        }

        List<Facture> factures = new ArrayList<>(factureRepository.findByIdLivreur(livreurId));
        factures.sort(Comparator
                .comparing(Facture::getDateTimle, Comparator.nullsLast(String::compareTo))
                .thenComparing(Facture::getId, Comparator.nullsLast(String::compareTo)));

        List<MutableLigneDette> lignes = dettes.stream().flatMap(dette -> dette.lignes.stream()).toList();
        List<PaiementDetail> paiements = new ArrayList<>();
        BigDecimal paiementsEnAttente = ZERO;
        BigDecimal versementsEntreprise = ZERO;
        BigDecimal excedentLivreur = ZERO;

        for (Facture facture : factures) {
            BigDecimal montant = nonNegatif(facture.getMontant());
            if (facture.getConfirmer() == Facture.ConfirmationStatut.NON_TRAITER
                    && facture.getType() == Facture.FactureType.LIVREUR_VERSE_ENTREPRISE) {
                paiementsEnAttente = paiementsEnAttente.add(montant);
            }
            if (facture.getConfirmer() == Facture.ConfirmationStatut.ACCEPTER
                    && facture.getType() == Facture.FactureType.ENTREPRISE_VERSE_LIVREUR) {
                versementsEntreprise = versementsEntreprise.add(montant);
            }

            List<AffectationPaiement> affectations = new ArrayList<>();
            BigDecimal restant = montant;
            if (facture.getConfirmer() == Facture.ConfirmationStatut.ACCEPTER
                    && facture.getType() == Facture.FactureType.LIVREUR_VERSE_ENTREPRISE) {
                for (MutableLigneDette ligne : lignes) {
                    if (restant.signum() <= 0) break;
                    BigDecimal disponible = ligne.reste();
                    if (disponible.signum() <= 0) continue;
                    BigDecimal affecte = restant.min(disponible);
                    ligne.montantPaye = ligne.montantPaye.add(affecte);
                    restant = restant.subtract(affecte);
                    affectations.add(new AffectationPaiement(
                            ligne.commandeId, ligne.ligneId, ligne.type, ligne.nom, scale(affecte)));
                }
                excedentLivreur = excedentLivreur.add(restant);
            }
            BigDecimal montantAffecte = montant.subtract(restant);
            paiements.add(new PaiementDetail(
                    facture.getId(), scale(montant),
                    facture.getConfirmer() != null ? facture.getConfirmer().name() : null,
                    facture.getDateTimle(), scale(montantAffecte), scale(restant), affectations));
        }

        BigDecimal produitsRestants = ZERO;
        BigDecimal coursesRestantes = ZERO;
        List<CommandeDette> commandes = new ArrayList<>();
        for (MutableCommandeDette dette : dettes) {
            BigDecimal initial = dette.lignes.stream().map(ligne -> ligne.montantInitial)
                    .reduce(ZERO, BigDecimal::add);
            BigDecimal paye = dette.lignes.stream().map(ligne -> ligne.montantPaye)
                    .reduce(ZERO, BigDecimal::add);
            BigDecimal reste = initial.subtract(paye).max(ZERO);
            for (MutableLigneDette ligne : dette.lignes) {
                if ("PRODUIT_B2C".equals(ligne.type)) produitsRestants = produitsRestants.add(ligne.reste());
                if ("PART_SOCIETE_COURSE".equals(ligne.type)) coursesRestantes = coursesRestantes.add(ligne.reste());
            }
            commandes.add(new CommandeDette(
                    dette.commande.getId(), dette.commande.getExternalOrderId(),
                    dette.commande.getSourceCommande() != null ? dette.commande.getSourceCommande().name() : "C2C",
                    dateReference(dette.commande), scale(initial), scale(paye), scale(reste), statut(initial, paye),
                    dette.lignes.stream().map(this::toResponse).toList()));
        }

        BigDecimal detteBrute = produitsRestants.add(coursesRestantes);
        BigDecimal creditAvantVersementEntreprise = creditEnLigne.add(excedentLivreur);
        BigDecimal creditDisponible = creditAvantVersementEntreprise.subtract(versementsEntreprise).max(ZERO);
        BigDecimal solde = creditAvantVersementEntreprise.subtract(detteBrute).subtract(versementsEntreprise);
        LivreurDebtDetailsResponse response = new LivreurDebtDetailsResponse(
                livreurId, scale(produitsRestants), scale(coursesRestantes), scale(detteBrute),
                scale(creditDisponible), scale(solde.signum() < 0 ? solde.abs() : ZERO),
                scale(solde.signum() > 0 ? solde : ZERO), scale(paiementsEnAttente), commandes, paiements);
        synchroniserInstantane(response);
        return response;
    }

    private void synchroniserInstantane(LivreurDebtDetailsResponse response) {
        EtatFinancierLivreur etat = new EtatFinancierLivreur();
        etat.setId(response.livreurId());
        etat.setLivreurId(response.livreurId());
        etat.setProduitsB2cAPayer(response.produitsB2cAPayer());
        etat.setCoursesAPayer(response.coursesAPayer());
        etat.setDetteBrute(response.detteBrute());
        etat.setCreditEnLigneDisponible(response.creditEnLigneDisponible());
        etat.setDetteNette(response.detteNette());
        etat.setCreditLivreur(response.creditLivreur());
        etat.setPaiementsEnAttente(response.paiementsEnAttente());
        etat.setCalculeLe(LocalDateTime.now());
        etatFinancierRepository.save(etat);
    }

    private MutableCommandeDette construireDette(Commande commande, List<Produit> produits) {
        MutableCommandeDette dette = new MutableCommandeDette(commande);
        if (estB2c(commande)) {
            BigDecimal totalLignes = ZERO;
            List<Produit> produitsTries = new ArrayList<>(produits);
            produitsTries.sort(Comparator.comparing(Produit::getId, Comparator.nullsLast(String::compareTo)));
            for (Produit produit : produitsTries) {
                BigDecimal total = totalProduit(produit);
                if (total.signum() <= 0) continue;
                dette.lignes.add(new MutableLigneDette(
                        commande.getId(), "produit:" + produit.getId(), "PRODUIT_B2C", produit.getId(),
                        produit.getNom() != null ? produit.getNom() : "Produit B2C",
                        produit.getQuantite() != null ? produit.getQuantite() : 1,
                        nonNegatif(produit.getPrix()), total));
                totalLignes = totalLignes.add(total);
            }
            BigDecimal totalCommande = nonNegatif(commande.getPrixProduitsPartenaire());
            BigDecimal reprise = totalCommande.subtract(totalLignes);
            if (reprise.signum() > 0) {
                dette.lignes.add(new MutableLigneDette(
                        commande.getId(), "produits-reprise:" + commande.getId(), "PRODUIT_B2C", null,
                        "Produits B2C (reprise du total)", 1, reprise, reprise));
            }
        }
        BigDecimal partSociete = partSociete(commande);
        if (partSociete.signum() > 0) {
            dette.lignes.add(new MutableLigneDette(
                    commande.getId(), "course:" + commande.getId(), "PART_SOCIETE_COURSE", null,
                    "Part societe de la course", 1, partSociete, partSociete));
        }
        return dette;
    }

    private LigneDette toResponse(MutableLigneDette ligne) {
        BigDecimal reste = ligne.reste();
        return new LigneDette(
                ligne.ligneId, ligne.type, ligne.produitId, ligne.nom, ligne.quantite,
                scale(ligne.prixUnitaire), scale(ligne.montantInitial), scale(ligne.montantPaye),
                scale(reste), statut(ligne.montantInitial, ligne.montantPaye));
    }

    private boolean encaisseParLivreur(Commande commande) {
        if (commande.getEncaisseurInitial() != null) {
            return commande.getEncaisseurInitial() == Commande.EncaisseurInitial.LIVREUR;
        }
        return commande.getModePaiement() != Commande.ModePaiement.EN_LIGNE;
    }

    private boolean estB2c(Commande commande) {
        return commande.getSourceCommande() == Commande.SourceCommande.B2C
                || (commande.getPartenaireId() != null && !commande.getPartenaireId().isBlank());
    }

    private BigDecimal totalProduit(Produit produit) {
        if (produit.getPrixTotalLigne() != null) return nonNegatif(produit.getPrixTotalLigne());
        int quantite = produit.getQuantite() != null && produit.getQuantite() > 0 ? produit.getQuantite() : 1;
        return nonNegatif(produit.getPrix()).multiply(BigDecimal.valueOf(quantite));
    }

    private BigDecimal partLivreur(Commande commande) {
        if (commande.getPrixLivreur() != null) return nonNegatif(commande.getPrixLivreur());
        return nonNegatif(commande.getPrix()).divide(new BigDecimal("2"), 3, RoundingMode.HALF_UP);
    }

    private BigDecimal partSociete(Commande commande) {
        if (commande.getPrixSociete() != null) return nonNegatif(commande.getPrixSociete());
        return nonNegatif(commande.getPrix()).divide(new BigDecimal("2"), 3, RoundingMode.HALF_UP);
    }

    private LocalDateTime dateReference(Commande commande) {
        if (commande.getDateFin() != null) return commande.getDateFin();
        if (commande.getMajLe() != null) return commande.getMajLe();
        return commande.getDateDemande();
    }

    private String statut(BigDecimal initial, BigDecimal paye) {
        if (paye.signum() <= 0) return "NON_PAYEE";
        if (paye.compareTo(initial) >= 0) return "SOLDEE";
        return "PARTIELLE";
    }

    private BigDecimal nonNegatif(BigDecimal value) {
        return value == null || value.signum() < 0 ? ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return nonNegatif(value).setScale(3, RoundingMode.HALF_UP);
    }

    private static class MutableCommandeDette {
        private final Commande commande;
        private final List<MutableLigneDette> lignes = new ArrayList<>();
        private MutableCommandeDette(Commande commande) { this.commande = commande; }
    }

    private static class MutableLigneDette {
        private final String commandeId;
        private final String ligneId;
        private final String type;
        private final String produitId;
        private final String nom;
        private final Integer quantite;
        private final BigDecimal prixUnitaire;
        private final BigDecimal montantInitial;
        private BigDecimal montantPaye = ZERO;

        private MutableLigneDette(
                String commandeId, String ligneId, String type, String produitId, String nom,
                Integer quantite, BigDecimal prixUnitaire, BigDecimal montantInitial) {
            this.commandeId = commandeId;
            this.ligneId = ligneId;
            this.type = type;
            this.produitId = produitId;
            this.nom = nom;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
            this.montantInitial = montantInitial;
        }

        private BigDecimal reste() { return montantInitial.subtract(montantPaye).max(ZERO); }
    }
}
