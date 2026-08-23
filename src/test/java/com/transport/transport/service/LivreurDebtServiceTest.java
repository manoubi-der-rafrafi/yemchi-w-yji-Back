package com.transport.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.transport.dto.LivreurDebtDetailsResponse;
import com.transport.transport.model.Commande;
import com.transport.transport.model.Facture;
import com.transport.transport.model.Produit;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.FactureRepository;
import com.transport.transport.repository.ProduitRepository;
import com.transport.transport.repository.EtatFinancierLivreurRepository;

@ExtendWith(MockitoExtension.class)
class LivreurDebtServiceTest {
    @Mock private CommandeRepository commandeRepository;
    @Mock private ProduitRepository produitRepository;
    @Mock private FactureRepository factureRepository;
    @Mock private EtatFinancierLivreurRepository etatFinancierRepository;

    private LivreurDebtService service;

    @BeforeEach
    void setUp() {
        service = new LivreurDebtService(
                commandeRepository, produitRepository, factureRepository, etatFinancierRepository);
    }

    @Test
    void affecteLesVersementsEnFifoProduitsPuisCourse() {
        Commande commande1 = commande("c1", LocalDateTime.of(2026, 6, 1, 12, 0), "110", "40");
        Commande commande2 = commande("c2", LocalDateTime.of(2026, 8, 1, 12, 0), "10", "22");
        when(commandeRepository.findByTransporteurIdAndStatut("livreur-1", Commande.Statut.livree))
                .thenReturn(List.of(commande2, commande1));
        when(produitRepository.findByCommandeIdIn(List.of("c1", "c2"))).thenReturn(List.of(
                produit("p1", "c1", "Produit 1", 1, "50"),
                produit("p2", "c1", "Produit 2", 1, "60"),
                produit("p3", "c2", "Produit 1", 1, "10")));
        when(factureRepository.findByIdLivreur("livreur-1")).thenReturn(List.of(
                versement("f1", "2026-08-02T10:00:00", "50"),
                versement("f2", "2026-08-03T10:00:00", "120")));

        LivreurDebtDetailsResponse result = service.details("livreur-1");

        assertThat(result.detteBrute()).isEqualByComparingTo("12.000");
        assertThat(result.produitsB2cAPayer()).isEqualByComparingTo("0.000");
        assertThat(result.coursesAPayer()).isEqualByComparingTo("12.000");
        assertThat(result.detteNette()).isEqualByComparingTo("12.000");
        assertThat(result.commandes()).extracting(LivreurDebtDetailsResponse.CommandeDette::commandeId)
                .containsExactly("c1", "c2");
        assertThat(result.commandes().get(0).statutFinancier()).isEqualTo("SOLDEE");
        assertThat(result.commandes().get(0).resteAPayer()).isEqualByComparingTo("0.000");
        assertThat(result.commandes().get(1).statutFinancier()).isEqualTo("PARTIELLE");
        assertThat(result.commandes().get(1).resteAPayer()).isEqualByComparingTo("12.000");
        assertThat(result.paiements().get(0).montantAffecte()).isEqualByComparingTo("50.000");
        assertThat(result.paiements().get(1).montantAffecte()).isEqualByComparingTo("120.000");
    }

    private Commande commande(String id, LocalDateTime date, String totalProduits, String partSociete) {
        Commande commande = new Commande();
        commande.setId(id);
        commande.setExternalOrderId("EXT-" + id);
        commande.setTransporteurId("livreur-1");
        commande.setStatut(Commande.Statut.livree);
        commande.setDateFin(date);
        commande.setSourceCommande(Commande.SourceCommande.B2C);
        commande.setEncaisseurInitial(Commande.EncaisseurInitial.LIVREUR);
        commande.setModePaiement(Commande.ModePaiement.ARRIVEE);
        commande.setPrixProduitsPartenaire(new BigDecimal(totalProduits));
        commande.setPrixSociete(new BigDecimal(partSociete));
        return commande;
    }

    private Produit produit(String id, String commandeId, String nom, int quantite, String prix) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setCommandeId(commandeId);
        produit.setNom(nom);
        produit.setQuantite(quantite);
        produit.setPrix(new BigDecimal(prix));
        produit.setPrixTotalLigne(new BigDecimal(prix).multiply(BigDecimal.valueOf(quantite)));
        return produit;
    }

    private Facture versement(String id, String date, String montant) {
        Facture facture = new Facture();
        facture.setId(id);
        facture.setIdLivreur("livreur-1");
        facture.setDateTimle(date);
        facture.setMontant(new BigDecimal(montant));
        facture.setType(Facture.FactureType.LIVREUR_VERSE_ENTREPRISE);
        facture.setConfirmer(Facture.ConfirmationStatut.ACCEPTER);
        return facture;
    }
}
