package com.transport.transport.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.transport.transport.model.Utilisateur;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.FactureService;
import com.transport.transport.security.ImageUploadValidator;

@RestController
@RequestMapping("/api/factures")
public class FactureController {

    @Autowired
    private FactureService factureService;
    private final Cloudinary cloudinary;
    private final AuthorizationService authorizationService;

    public FactureController(Cloudinary cloudinary, AuthorizationService authorizationService) {
        this.cloudinary = cloudinary;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public Facture createFacture(@RequestBody Facture facture, Authentication authentication) {
        authorizationService.requireTransporteurOrAdmin(authentication);
        Utilisateur current = authorizationService.currentUser(authentication);
        secureNewFacture(facture, current);
        return factureService.createFacture(facture);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createFactureWithImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("montant") BigDecimal montant,
            @RequestParam("dateTimle") String dateTimle,
            @RequestParam("idLivreur") String idLivreur,
            @RequestParam("type") Facture.FactureType type,
            @RequestParam(value = "confirmer", required = false) String confirmer,
            Authentication authentication) {
        try {
            authorizationService.requireTransporteurOrAdmin(authentication);
            Utilisateur current = authorizationService.currentUser(authentication);
            validateFactureValues(montant, type);
            ImageUploadValidator.validate(image, ImageUploadValidator.STANDARD_MAX_BYTES);

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
            Facture.ConfirmationStatut statut = Facture.ConfirmationStatut.fromString(confirmer);
            if (statut != null) {
                facture.setConfirmer(statut);
            }
            facture.setImage(url);
            secureNewFacture(facture, current);

            return ResponseEntity.ok(factureService.createFacture(facture));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("success", false, "message", e.getReason()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Erreur interne lors de l'upload"));
        }
    }

    @GetMapping("/livreur/{livreurId}")
    public ResponseEntity<List<Facture>> listByLivreurId(@PathVariable String livreurId, Authentication authentication) {
        authorizationService.requireSelfOrAdmin(livreurId, authentication);
        return ResponseEntity.ok(factureService.listByLivreurId(livreurId));
    }

    @GetMapping("/livreur/{livreurId}/sum-entreprise-verse-livreur")
    public ResponseEntity<BigDecimal> sumMontantEntrepriseVerseLivreurByLivreurId(
            @PathVariable String livreurId,
            Authentication authentication) {
        authorizationService.requireSelfOrAdmin(livreurId, authentication);
        BigDecimal total = factureService.sumMontantEntrepriseVerseLivreurByLivreurId(livreurId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/livreur/{livreurId}/sum-livreur-verse-entreprise")
    public ResponseEntity<BigDecimal> sumMontantLivreurVerseEntrepriseByLivreurId(
            @PathVariable String livreurId,
            Authentication authentication) {
        authorizationService.requireSelfOrAdmin(livreurId, authentication);
        BigDecimal total = factureService.sumMontantLivreurVerseEntrepriseByLivreurId(livreurId);
        return ResponseEntity.ok(total);
    }

    private void secureNewFacture(Facture facture, Utilisateur current) {
        if (facture == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Facture requise");
        }
        validateFactureValues(facture.getMontant(), facture.getType());

        // Une creation ne doit jamais devenir une mise a jour Mongo via un id fourni par le client.
        facture.setId(null);
        facture.setDateTimle(Instant.now().toString());
        if (!authorizationService.isAdmin(current)) {
            facture.setIdLivreur(current.getId());
            facture.setType(Facture.FactureType.LIVREUR_VERSE_ENTREPRISE);
            facture.setConfirmer(Facture.ConfirmationStatut.NON_TRAITER);
        } else if (facture.getIdLivreur() == null || facture.getIdLivreur().isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Livreur requis");
        }
    }

    private void validateFactureValues(BigDecimal montant, Facture.FactureType type) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0
                || montant.compareTo(new BigDecimal("1000000")) > 0) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Montant de facture invalide");
        }
        if (type == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Type de facture requis");
        }
    }
}
