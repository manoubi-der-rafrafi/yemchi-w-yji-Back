package com.transport.transport.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.transport.transport.model.Facture;
import com.transport.transport.service.FactureService;

@RestController
@RequestMapping("/api/factures")
public class FactureController {

    @Autowired
    private FactureService factureService;
    private final Cloudinary cloudinary;

    public FactureController(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    @PostMapping
    public Facture createFacture(@RequestBody Facture facture) {
        return factureService.createFacture(facture);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createFactureWithImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("montant") BigDecimal montant,
            @RequestParam("dateTimle") String dateTimle,
            @RequestParam("idLivreur") String idLivreur,
            @RequestParam("type") Facture.FactureType type,
            @RequestParam(value = "confirmer", required = false) Facture.ConfirmationStatut confirmer) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Fichier vide"));
            }

            Map params = ObjectUtils.asMap(
                    "folder", "monapp/facture",
                    "resource_type", "image",
                    "use_filename", true,
                    "unique_filename", true,
                    "overwrite", false
            );

            Map res = cloudinary.uploader().upload(image.getBytes(), params);
            String url = (String) res.get("secure_url");

            Facture facture = new Facture();
            facture.setMontant(montant);
            facture.setDateTimle(dateTimle);
            facture.setIdLivreur(idLivreur);
            facture.setType(type);
            facture.setConfirmer(confirmer != null ? confirmer : Facture.ConfirmationStatut.NON_TRAITER);
            facture.setImage(url);

            return ResponseEntity.ok(factureService.createFacture(facture));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Erreur serveur: " + e.getMessage()));
        }
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
