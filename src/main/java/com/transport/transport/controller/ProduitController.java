package com.transport.transport.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.transport.transport.model.Produit;
import com.transport.transport.service.ProduitService;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    @Autowired
    private ProduitService produitService;
    private final String uploadBaseDir = "C:/Users/Lenovo/Desktop/angular/transport/public/produits";
    private final Cloudinary cloudinary;

    public ProduitController(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    // GET : Liste de tous les produits
    @GetMapping
    public List<Produit> getAllProduits() {
        return produitService.getAllProduits();
    }

    // GET : Récupérer un produit par id
    @GetMapping("/{id}")
    public ResponseEntity<Produit> getProduitById(@PathVariable String  id) {
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
    public ResponseEntity<Produit> updateProduit(@PathVariable String  id, @RequestBody Produit produitDetails) {
        Produit updatedProduit = produitService.updateProduit(id, produitDetails);
        if (updatedProduit != null) {
            return ResponseEntity.ok(updatedProduit);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE : Supprimer un produit
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable String  id) {
        produitService.deleteProduit(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/commande/{idCommande}")
    public List<Produit> getProduitsByCommande(@PathVariable String  idCommande) {
        return produitService.getProduitsByCommandeId(idCommande);
    }


    @CrossOrigin(origins = {"http://localhost:4200", "https://yemchi-w-yji-back-1.onrender.com"}, allowCredentials = "true")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProduit(@RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Fichier vide"));
            }

            // Options utiles : dossier, type image, nom de fichier auto
            Map params = ObjectUtils.asMap(
                    "folder", "monapp/produits",     // <- change le chemin si tu veux
                    "resource_type", "image",
                    "use_filename", true,            // garde le nom d'origine si possible
                    "unique_filename", true,         // évite les collisions
                    "overwrite", false
            );

            Map res = cloudinary.uploader().upload(image.getBytes(), params);

            String url = (String) res.get("secure_url");
            String publicId = (String) res.get("public_id");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", url,            // à stocker dans ta DB (Produit.imageUrl)
                    "public_id", publicId  // utile si tu veux supprimer/modifier plus tard
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Erreur serveur: " + e.getMessage()));
        }
    }


    @PostMapping("/{target}") // target = produits | profile
    public Map<String, String> upload(@PathVariable String target,
                                      @RequestParam("file") MultipartFile file) throws IOException {
        Map params = ObjectUtils.asMap("folder", "monapp/" + target);
        Map uploadResult = cloudinary.uploader().upload(file.getBytes(), params);
        return Map.of("url", (String) uploadResult.get("secure_url"));
    }
    @PostMapping("/detect")
    public ResponseEntity<String> detectObject(@RequestParam("image") MultipartFile image) {

        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body("image n'est pas claire");
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "python/detect.py"
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();

            try (var os = process.getOutputStream()) {
                os.write(image.getBytes());
                os.flush();
            }

            String output = new String(
                    process.getInputStream().readAllBytes()
            ).trim();

            int exitCode = process.waitFor();
            System.out.println("[detectObject] exitCode=" + exitCode);
            System.out.println("[detectObject] output=" + output);

            if (output.isEmpty()) {
                output = "image n'est pas claire";
            }

            return ResponseEntity.ok(output);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("image n'est pas claire");
        }
    }



    

}
