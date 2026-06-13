package com.transport.transport.controller;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.ProduitService;

@RestController
@RequestMapping("/api/produits")
public class ProduitController {

    @Autowired
    private ProduitService produitService;
    @Autowired
    private CommandeRepository commandeRepository;
    private final String uploadBaseDir = "C:/Users/Lenovo/Desktop/angular/transport/public/produits";
    private final Cloudinary cloudinary;
    private final AuthorizationService authorizationService;

    @Value("${PYTHON_URL:http://127.0.0.1:8000}")
    private String pythonUrl;

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ProduitController(Cloudinary cloudinary, AuthorizationService authorizationService) {
        this.cloudinary = cloudinary;
        this.authorizationService = authorizationService;
    }

    // GET : Liste de tous les produits
    @GetMapping
    public List<Produit> getAllProduits(Authentication authentication) {
        var current = authorizationService.currentUser(authentication);
        if (authorizationService.isAdmin(current)) {
            return produitService.getAllProduits();
        }
        List<String> commandeIds = commandeRepository.findAll()
                .stream()
                .filter(c -> authorizationService.canAccessCommande(current, c))
                .map(c -> c.getId())
                .collect(Collectors.toList());
        if (commandeIds.isEmpty()) {
            return List.of();
        }
        return produitService.getProduitsByCommandeIds(commandeIds);
    }

    // GET : Récupérer un produit par id
    @GetMapping("/{id}")
    public ResponseEntity<Produit> getProduitById(@PathVariable String  id, Authentication authentication) {
        Optional<Produit> produit = produitService.getProduitById(id);
        produit.ifPresent(p -> requireProduitCommandeAccess(p, authentication));
        return produit.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST : Créer un nouveau produit
    @PostMapping
    public Produit createProduit(@RequestBody Produit produit, Authentication authentication) {
        requireProduitCommandeAccess(produit, authentication);
        return produitService.createProduit(produit);
    }

    // PUT : Modifier un produit existant
    @PutMapping("/{id}")
    public ResponseEntity<Produit> updateProduit(@PathVariable String  id, @RequestBody Produit produitDetails,
                                                 Authentication authentication) {
        produitService.getProduitById(id).ifPresent(p -> requireProduitCommandeAccess(p, authentication));
        if (produitDetails.getCommandeId() != null && !produitDetails.getCommandeId().isBlank()) {
            requireProduitCommandeAccess(produitDetails, authentication);
        }
        Produit updatedProduit = produitService.updateProduit(id, produitDetails);
        if (updatedProduit != null) {
            return ResponseEntity.ok(updatedProduit);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DELETE : Supprimer un produit
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable String  id, Authentication authentication) {
        produitService.getProduitById(id).ifPresent(p -> requireProduitCommandeAccess(p, authentication));
        produitService.deleteProduit(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/commande/{idCommande}")
    public List<Produit> getProduitsByCommande(@PathVariable String  idCommande, Authentication authentication) {
        commandeRepository.findById(idCommande)
                .ifPresent(commande -> authorizationService.requireCommandeAccess(commande, authentication));
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

        long startMs = System.currentTimeMillis();
        try {
            if (image.isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"object\":\"objet inconnu\",\"median_weight_kg\":0.0}");
            }

            System.out.println("[detectObject] start size=" + image.getSize()
                    + " contentType=" + image.getContentType()
                    + " filename=" + image.getOriginalFilename());

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(pythonUrl + "/detect"))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(image.getBytes()))
                    .build();

            HttpResponse<String> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            long durationMs = System.currentTimeMillis() - startMs;
            System.out.println("[detectObject] status=" + resp.statusCode() + " durationMs=" + durationMs);

            String output = resp.body() == null ? "" : resp.body().trim();

            if (resp.statusCode() == 503) {
                System.out.println("[detectObject] model not ready");
                return ResponseEntity.status(503).body("modele non disponible");
            }

            if (resp.statusCode() >= 400) {
                String message = output.isEmpty() ? "image n'est pas claire" : output;
                if ("No weight values found in search results".equals(message)) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"object\":\"objet inconnu\",\"median_weight_kg\":0.01}");
                }
                if ("image n'est pas claire".equals(message)) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"object\":\"objet inconnu\",\"median_weight_kg\":0.0}");
                }
                return ResponseEntity.status(resp.statusCode()).body(message);
            }

            if (output.isEmpty() || "image n'est pas claire".equals(output)) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"object\":\"objet inconnu\",\"median_weight_kg\":0.0}");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(output);

        } catch (HttpTimeoutException e) {
            long durationMs = System.currentTimeMillis() - startMs;
            System.out.println("[detectObject] timeout durationMs=" + durationMs + " msg=" + e.getMessage());
            return ResponseEntity.status(504).body("service detection trop lent (timeout)");
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            System.out.println("[detectObject] failed durationMs=" + durationMs);
            e.printStackTrace();
            return ResponseEntity.status(500).body("erreur interne detection");
        }
    }

    private void requireProduitCommandeAccess(Produit produit, Authentication authentication) {
        String commandeId = produit != null ? produit.getCommandeId() : null;
        if (commandeId == null || commandeId.isBlank()) {
            return;
        }
        commandeRepository.findById(commandeId)
                .ifPresent(commande -> authorizationService.requireCommandeAccess(commande, authentication));
    }
}
