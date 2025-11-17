package com.transport.transport.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import com.transport.transport.model.Demande;
import com.transport.transport.service.DemandeService;

@RestController
@RequestMapping("/api/demandes")
public class DemandeController {

    private final DemandeService demandeService;
    private final Cloudinary cloudinary;

    public DemandeController(DemandeService demandeService, Cloudinary cloudinary) {
        this.demandeService = demandeService;
        this.cloudinary = cloudinary;
    }

    @GetMapping("/numero/existe")
    public ResponseEntity<Map<String, Object>> numeroExiste(@RequestParam String numero) {
        boolean existe = demandeService.numeroExiste(numero);
        return ResponseEntity.ok(Map.of(
                "numero", numero,
                "existe", existe
        ));
    }

    @PostMapping
    public ResponseEntity<?> creerDemande(@RequestBody Demande demande) {
        try {
            return ResponseEntity.ok(demandeService.creerDemande(demande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/accepter")
    public ResponseEntity<?> accepterDemande(@PathVariable("id") String idDemande) {
        try {
            return ResponseEntity.ok(demandeService.accepterDemande(idDemande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/refuser")
    public ResponseEntity<?> refuserDemande(@PathVariable("id") String idDemande) {
        try {
            return ResponseEntity.ok(demandeService.refuserDemande(idDemande));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @PostMapping(value = "/uploadDocuments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocuments(@RequestParam("image") MultipartFile image) {
        try {
            if (image == null || image.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Fichier vide"));
            }

            // (Optionnel) petit contrôle de type MIME côté serveur
            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Le fichier doit être une image"));
            }

            // Paramètres Cloudinary
            Map<String, Object> params = ObjectUtils.asMap(
                    "folder", "monapp/Document",  // <- change en "monapp/produits" si besoin
                    "resource_type", "image",
                    "use_filename", true,       // garde (en partie) le nom d'origine
                    "unique_filename", true,    // évite les collisions
                    "overwrite", false
            );

            // Upload direct en mémoire (pas d’écriture disque)
            @SuppressWarnings("unchecked")
            Map<String, Object> res = cloudinary.uploader().upload(image.getBytes(), params);

            String url = (String) res.get("secure_url");
            String publicId = (String) res.get("public_id");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "url", url,           // => à stocker dans ta DB (ex: Produit.imageUrl ou User.photoUrl)
                    "public_id", publicId // => utile pour supprimer/mettre à jour plus tard
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Erreur serveur: " + e.getMessage()));
        }
    }
    
}
