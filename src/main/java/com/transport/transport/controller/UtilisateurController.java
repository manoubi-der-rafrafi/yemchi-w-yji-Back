package com.transport.transport.controller;

import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.service.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/utilisateur")
public class UtilisateurController {

    @Autowired
    private UtilisateurRepository utilisateurRepository;
    private UtilisateurService utilisateurService;
    // LOGIN : POST /api/utilisateur/login
    private final String uploadBaseDir = "C:/Users/Lenovo/Desktop/angular/transport/public/profil";
    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Utilisateur utilisateur) {
        Utilisateur user = utilisateurRepository.findByEmail(utilisateur.getEmail());
        if (user == null || !user.getMotDePasse().equals(utilisateur.getMotDePasse())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
        }
        return ResponseEntity.ok(user);
    }

    // Récupérer un utilisateur par ID (GET /api/utilisateur/id/1)
    @GetMapping("/id/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable String  id) {
        Utilisateur user = utilisateurRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        return ResponseEntity.ok(user);
    }

    // (Optionnel) Liste de tous les utilisateurs (GET /api/utilisateur/all)
    @GetMapping("/all")
    public ResponseEntity<?> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }
    @PostMapping("/register")
    public Utilisateur createUtilisateur(@RequestBody Utilisateur utilisateur) {
        // tu peux ajouter des vérifications ici (ex: email déjà utilisé)
        return utilisateurService.saveUtilisateur(utilisateur);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUtilisateur(@PathVariable String  id,
                                                    @RequestBody Utilisateur updated) {
        return utilisateurRepository.findById(id)
                .map(user -> {
                    // ⚠️ On met à jour uniquement si la nouvelle valeur est fournie (!= null)
                    if (updated.getNom() != null)            user.setNom(updated.getNom());
                    if (updated.getPrenom() != null)         user.setPrenom(updated.getPrenom());
                    if (updated.getAdresse() != null)        user.setAdresse(updated.getAdresse());
                    if (updated.getTelephone() != null)      user.setTelephone(updated.getTelephone());
                    if (updated.getDateNaissance() != null)  user.setDateNaissance(updated.getDateNaissance());
                    if (updated.getImage() != null)          user.setImage(updated.getImage()); // URL seulement si envoyée

                    // Optionnel : si tu autorises aussi ces champs à être mis à jour partiellement
                    if (updated.getEmail() != null)          user.setEmail(updated.getEmail());
                    if (updated.getRole() != null)           user.setRole(updated.getRole());
                    if (updated.getStatut() != null)         user.setStatut(updated.getStatut());

                    utilisateurRepository.save(user);

                    // On ne renvoie jamais le mot de passe
                    user.setMotDePasse(null);

                    return ResponseEntity.ok((Object) user);
                })
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body((Object) "Utilisateur non trouvé"));
    }


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
            String publicUrl = "profil/" + filename;

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
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestParam String  id) {
        // Version simple sans sécurité/JWT: on passe ?id=...
        Utilisateur user = utilisateurRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        user.setMotDePasse(null);
        return ResponseEntity.ok(user);
    }
    // Dans UtilisateurController

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatusPost(@PathVariable String  id, @RequestBody Map<String, String> body) {
        String statut = body.get("statut"); // "actif" | "inactif" | "banni"
        return utilisateurRepository.findById(id)
                .map(u -> {
                    try {
                        Utilisateur.Statut s = Utilisateur.Statut.valueOf(statut);
                        u.setStatut(s);
                        utilisateurRepository.save(u);
                        u.setMotDePasse(null);
                        return ResponseEntity.ok(u);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body("Statut invalide");
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé"));
    }
    @GetMapping("/search/numero")
    public ResponseEntity<?> chercherParNumero(@RequestParam String numero) {
        return utilisateurService.chercherParNumero(numero)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/search/email")
    public ResponseEntity<?> chercherParEmail(@RequestParam String email) {
        return utilisateurService.chercherParEmail(email)
                .map(ResponseEntity::ok)                  // 200 + objet Utilisateur
                .orElse(ResponseEntity.notFound().build());// 404 si rien
    }
}
