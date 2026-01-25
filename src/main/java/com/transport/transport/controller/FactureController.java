package com.transport.transport.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.transport.model.Facture;
import com.transport.transport.service.FactureService;

@RestController
@RequestMapping("/api/factures")
public class FactureController {

    @Autowired
    private FactureService factureService;

    @PostMapping
    public Facture createFacture(@RequestBody Facture facture) {
        return factureService.createFacture(facture);
    }

    @GetMapping("/livreur/{livreurId}")
    public ResponseEntity<List<Facture>> listByLivreurId(@PathVariable String livreurId) {
        return ResponseEntity.ok(factureService.listByLivreurId(livreurId));
    }

    @GetMapping("/livreur/{livreurId}/sum-entreprise-verse-livreur")
    public ResponseEntity<BigDecimal> sumMontantEntrepriseVerseLivreurByLivreurId(
            @PathVariable String livreurId) {
        BigDecimal total = factureService.sumMontantEntrepriseVerseLivreurByLivreurId(livreurId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/livreur/{livreurId}/sum-livreur-verse-entreprise")
    public ResponseEntity<BigDecimal> sumMontantLivreurVerseEntrepriseByLivreurId(
            @PathVariable String livreurId) {
        BigDecimal total = factureService.sumMontantLivreurVerseEntrepriseByLivreurId(livreurId);
        return ResponseEntity.ok(total);
    }
}
