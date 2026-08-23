package com.transport.transport.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.transport.transport.dto.StatutFinancierLivreurResponse;
import com.transport.transport.model.Commande;
import com.transport.transport.model.EtatBlocageLivreur;
import com.transport.transport.model.Facture;
import com.transport.transport.model.RegleBlocageLivreur;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.EtatBlocageLivreurRepository;
import com.transport.transport.repository.FactureRepository;
import com.transport.transport.repository.RegleBlocageLivreurRepository;

class FinanceLivreurServiceTest {
    private CommandeRepository commandeRepository;
    private FactureRepository factureRepository;
    private RegleBlocageLivreurRepository regleRepository;
    private EtatBlocageLivreurRepository etatRepository;
    private FinanceLivreurService service;

    @BeforeEach
    void setUp() {
        commandeRepository = org.mockito.Mockito.mock(CommandeRepository.class);
        factureRepository = org.mockito.Mockito.mock(FactureRepository.class);
        regleRepository = org.mockito.Mockito.mock(RegleBlocageLivreurRepository.class);
        etatRepository = org.mockito.Mockito.mock(EtatBlocageLivreurRepository.class);
        service = new FinanceLivreurService(
                commandeRepository, factureRepository, regleRepository, etatRepository);
        when(factureRepository.findByIdLivreurAndTypeAndConfirmer(any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void bloqueSurDetteNetteEtNonSurPrixBrut() {
        Commande b2cCash = commande(
                Commande.ModePaiement.ARRIVEE,
                Commande.SourceCommande.B2C,
                "60.000",
                "40.000",
                "500.000");
        Commande c2cCash = commande(
                Commande.ModePaiement.ARRIVEE,
                Commande.SourceCommande.C2C,
                "180.000",
                "20.000",
                null);
        when(commandeRepository.findByTransporteurIdAndStatut("livreur-1", Commande.Statut.livree))
                .thenReturn(List.of(b2cCash, c2cCash));
        when(regleRepository.findFirstByDateFinIsNullOrderByDateDebutDesc())
                .thenReturn(Optional.of(regle("500.000", "75.000")));
        when(etatRepository.findByLivreurId("livreur-1")).thenReturn(Optional.empty());
        when(etatRepository.save(any(EtatBlocageLivreur.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatutFinancierLivreurResponse result = service.calculerEtSynchroniser("livreur-1");

        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("560.000"), result.detteNette());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("420.000"), result.paiementMinimum());
        assertTrue(result.bloque());
    }

    @Test
    void neCompteQueLesPaiementsAcceptesEtDebloqueApresReglementMinimum() {
        when(commandeRepository.findByTransporteurIdAndStatut("livreur-1", Commande.Statut.livree))
                .thenReturn(List.of(commande(
                        Commande.ModePaiement.ARRIVEE,
                        Commande.SourceCommande.B2C,
                        "60.000",
                        "40.000",
                        "500.000")));
        when(regleRepository.findFirstByDateFinIsNullOrderByDateDebutDesc())
                .thenReturn(Optional.of(regle("500.000", "75.000")));

        EtatBlocageLivreur etat = new EtatBlocageLivreur();
        etat.setLivreurId("livreur-1");
        etat.setBloque(true);
        etat.setDetteAuBlocage(new BigDecimal("540.000"));
        etat.setPaiementMinimum(new BigDecimal("405.000"));
        etat.setCibleDette(new BigDecimal("135.000"));
        etat.setPaiementsAcceptesAuBlocage(BigDecimal.ZERO);
        etat.setBloqueLe(LocalDateTime.now().minusDays(1));
        when(etatRepository.findByLivreurId("livreur-1")).thenReturn(Optional.of(etat));
        when(etatRepository.save(any(EtatBlocageLivreur.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Facture paiement = new Facture();
        paiement.setMontant(new BigDecimal("405.000"));
        when(factureRepository.findByIdLivreurAndTypeAndConfirmer(
                "livreur-1",
                Facture.FactureType.LIVREUR_VERSE_ENTREPRISE,
                Facture.ConfirmationStatut.ACCEPTER))
                .thenReturn(List.of(paiement));

        StatutFinancierLivreurResponse result = service.calculerEtSynchroniser("livreur-1");

        assertFalse(result.bloque());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("135.000"), result.detteNette());
    }

    private Commande commande(
            Commande.ModePaiement mode,
            Commande.SourceCommande source,
            String prixLivreur,
            String prixSociete,
            String produits) {
        Commande commande = new Commande();
        commande.setModePaiement(mode);
        commande.setSourceCommande(source);
        commande.setPrixLivreur(new BigDecimal(prixLivreur));
        commande.setPrixSociete(new BigDecimal(prixSociete));
        if (produits != null) commande.setPrixProduitsPartenaire(new BigDecimal(produits));
        return commande;
    }

    private RegleBlocageLivreur regle(String montant, String pourcentage) {
        RegleBlocageLivreur regle = new RegleBlocageLivreur();
        regle.setId("regle-1");
        regle.setMontantBlocage(new BigDecimal(montant));
        regle.setPourcentageReglement(new BigDecimal(pourcentage));
        regle.setDateDebut(LocalDateTime.now().minusDays(1));
        return regle;
    }
}
