package com.transport.transport.controller;

import com.transport.transport.model.Produit;
import com.transport.transport.service.ProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.CrossOrigin;
@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    @Autowired
    private ProduitService produitService;
    private final String uploadBaseDir = "C:/Users/Lenovo/Desktop/angular/transport/public/produits";

    // GET : Liste de tous les produits
    @GetMapping
    public List<Produit> getAllProduits() {
        return produitService.getAllProduits();
    }

    // GET : Récupérer un produit par id
    @GetMapping("/{id}")
    public ResponseEntity<Produit> getProduitById(@PathVariable Integer id) {
        Optional<Produit> produit = produitService.getProduitById(id);
        return produit.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST : Créer un nouveau produit
    @PostMapping
    public Produit createProduit(@RequestBody Produit produit) {
        return produitService.createProduit(produit);
    }

    // PUT : Modifier un produit existant
    @PutMapping("/{id}")
    public ResponseEntity<Produit> updateProduit(@PathVariable Integer id, @RequestBody Produit produitDetails) {
        Produit updatedProduit = produitService.updateProduit(id, produitDetails);
        if (updatedProduit != null) {
            return ResponseEntity.ok(updatedProduit);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE : Supprimer un produit
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Integer id) {
        produitService.deleteProduit(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/commande/{idCommande}")
    public List<Produit> getProduitsByCommande(@PathVariable Integer idCommande) {
        return produitService.getProduitsByCommandeId(idCommande);
    }


    @CrossOrigin(origins = {"http://localhost:4200", "http://192.168.1.155:4200"}, allowCredentials = "true")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProduit(@RequestParam("image") MultipartFile image) {
        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Fichier vide"));
            }

            // 1) Créer le dossier si nécessaire
            Path uploadDir = Paths.get(uploadBaseDir);
            Files.createDirectories(uploadDir);

            // 2) Nom de fichier unique pour éviter les collisions
            String original = StringUtils.cleanPath(image.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot).toLowerCase();
            if (!ext.matches("\\.(png|jpg|jpeg|webp|gif)$")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Extension non autorisée"));
            }
            String filename = UUID.randomUUID().toString() + ext;

            // 3) Sauvegarde réelle
            Path destination = uploadDir.resolve(filename);
            Files.copy(image.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            // 4) URL publique (voir WebMvcConfig ci-dessous)
            String publicUrl = "/produits/" + filename;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", publicUrl,
                    "message", "Fichier uploadé avec succès"
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Erreur serveur: " + e.getMessage()));
        }
    }
}
