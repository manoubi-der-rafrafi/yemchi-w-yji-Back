package com.transport.transport.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.transport.transport.model.Facture;
import com.transport.transport.repository.FactureRepository;

@Service
public class FactureService {

    @Autowired
    private FactureRepository factureRepository;

    public Facture createFacture(Facture facture) {
        return factureRepository.save(facture);
    }

    public List<Facture> listByLivreurId(String livreurId) {
        return factureRepository.findByIdLivreurAndConfirmerNotNonTraiter(livreurId);
    }

    public BigDecimal sumMontantEntrepriseVerseLivreurByLivreurId(String livreurId) {
        List<Facture> factures = factureRepository.findByIdLivreurAndType(
                livreurId, Facture.FactureType.ENTREPRISE_VERSE_LIVREUR);
        BigDecimal total = BigDecimal.ZERO;
        for (Facture facture : factures) {
            if (facture.getMontant() != null) {
                total = total.add(facture.getMontant());
            }
        }
        return total;
    }

    public BigDecimal sumMontantLivreurVerseEntrepriseByLivreurId(String livreurId) {
        List<Facture> factures = factureRepository.findByIdLivreurAndType(
                livreurId, Facture.FactureType.LIVREUR_VERSE_ENTREPRISE);
        BigDecimal total = BigDecimal.ZERO;
        for (Facture facture : factures) {
            if (facture.getMontant() != null) {
                total = total.add(facture.getMontant());
            }
        }
        return total;
    }
}
