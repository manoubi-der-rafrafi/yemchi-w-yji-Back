package com.transport.transport.controller;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.service.MailService;
import com.transport.transport.service.UtilisateurService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
@RestController
@RequestMapping("/api/utilisateur")
public class UtilisateurController {



    @Autowired
    private UtilisateurRepository utilisateurRepository;
    private UtilisateurService utilisateurService;
  private final Cloudinary cloudinary;
  private final AuthenticationManager authManager;
  private final JwtEncoder jwtEncoder;
  private final JwtDecoder jwtDecoder;
  private final PasswordEncoder passwordEncoder;
  private final GoogleIdTokenVerifier googleVerifier;
  private final MailService mailService;
  @Value("${app.verification.base-url:http://localhost:3000/verify-email}")
  private String verificationBaseUrl;
    // LOGIN : POST /api/utilisateur/login
    private final String uploadBaseDir = "C:/Users/Lenovo/Desktop/angular/transport/public/profil";
    public UtilisateurController(UtilisateurService utilisateurService, Cloudinary cloudinary,
    AuthenticationManager authManager,
      JwtEncoder jwtEncoder,
      JwtDecoder jwtDecoder,
      PasswordEncoder passwordEncoder,
      GoogleIdTokenVerifier googleVerifier,
      MailService mailService) {
        this.utilisateurService = utilisateurService;
        this.cloudinary = cloudinary;
        this.authManager = authManager;
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.passwordEncoder = passwordEncoder;
    this.googleVerifier = googleVerifier;
    this.mailService = mailService;

    }


    @PostMapping("/login/google")
    public ResponseEntity<?> loginWithGoogle(@RequestBody GoogleLoginRequest req) {
      String tokenFromRequest = (req != null) ? req.resolvedToken() : null;
      if (tokenFromRequest == null || tokenFromRequest.isBlank()) {
        return ResponseEntity.badRequest().body("Token Google manquant");
      }

      try {
        GoogleIdToken idToken = googleVerifier.verify(tokenFromRequest);
        if (idToken == null) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token Google invalide");
        }

        Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        if (email == null) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email Google manquante");
        }
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
          return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email Google non vérifiée");
        }

        Utilisateur user = utilisateurRepository.findByEmailIgnoreCase(email)
            .orElseGet(() -> {
              Utilisateur u = new Utilisateur();
              u.setEmail(email);
              u.setPrenom((String) payload.get("given_name"));
              u.setNom((String) payload.get("family_name"));
              u.setImage((String) payload.get("picture"));
              u.setRole(Utilisateur.Role.client);
              u.setStatut(Utilisateur.Statut.actif);
              return utilisateurService.saveUtilisateur(u);
            });

        if (user.getStatut() == Utilisateur.Statut.banni) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Compte banni");
        }

        String role = (user.getRole() != null) ? user.getRole().name() : "CLIENT";

        Instant now = Instant.now();
        long expiry = 604800; // 7 jours
        var claims = JwtClaimsSet.builder()
            .issuer("transport")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expiry))
            .subject(email)
            .claim("uid", user.getId())
            .claim("roles", List.of("ROLE_" + role.toUpperCase()))
            .build();

        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        user.setMotDePasse(null);

        return ResponseEntity.ok(new LoginResponse(token, user));
      } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Erreur login Google: " + e.getMessage());
      }
    }

    @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest req) {
    try {
      // 1) Authentifier via AuthenticationManager (utilise BCrypt de ta SecurityConfig)
      Authentication auth = authManager.authenticate(
          new UsernamePasswordAuthenticationToken(req.email(), req.motDePasse())
      );

      // 2) Charger l'utilisateur pour la réponse
      Utilisateur user = utilisateurRepository.findByEmailIgnoreCase(req.email())
          .orElse(null);
      if (user == null) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
      }

      // 3) Construire un JWT
      Instant now = Instant.now();
      long expiry = 604800; // 1h
      var claims = JwtClaimsSet.builder()
          .issuer("transport")
          .issuedAt(now)
          .expiresAt(now.plusSeconds(expiry))
          .subject(req.email())
          .claim("uid", user.getId())
          .claim("roles", auth.getAuthorities().stream()
              .map(GrantedAuthority::getAuthority).toList())
          .build();

      var header = JwsHeader.with(MacAlgorithm.HS256).build();
      String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

      // 4) Ne jamais exposer le mot de passe
      user.setMotDePasse(null);

      return ResponseEntity.ok(new LoginResponse(token, user));
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect");
    }
  }
public static record GoogleLoginRequest(
    @com.fasterxml.jackson.annotation.JsonProperty("idToken") String idToken,
    @com.fasterxml.jackson.annotation.JsonProperty("id_token") String idTokenSnake
) {
  public String resolvedToken() {
    return (idToken != null && !idToken.isBlank()) ? idToken : idTokenSnake;
  }
}
public static record LoginRequest(String email, String motDePasse) {}
  public static record LoginResponse(String token, Utilisateur user) {}

    // Récupérer un utilisateur par ID (GET /api/utilisateur/id/1)
    @GetMapping("/id/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable String  id) {
        Utilisateur user = utilisateurRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        user.setMotDePasse(null);
        return ResponseEntity.ok(user);
    }

    // (Optionnel) Liste de tous les utilisateurs (GET /api/utilisateur/all)
    @GetMapping("/all")
    public ResponseEntity<?> getAllUtilisateurs() {
        return ResponseEntity.ok(utilisateurRepository.findAll());
    }
    @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody Utilisateur payload) {
    // Email unique ?
    if (utilisateurRepository.findByEmailIgnoreCase(payload.getEmail()).isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Email déjà utilisé");
    }

    // Encoder le mot de passe
    if (payload.getMotDePasse() == null || payload.getMotDePasse().isBlank()) {
      return ResponseEntity.badRequest().body("Mot de passe requis");
    }
    payload.setMotDePasse(passwordEncoder.encode(payload.getMotDePasse()));

    // Valeurs par défaut (statut/role), gérées aussi par UtilisateurService.saveUtilisateur(...)
    if (payload.getStatut() == null) {
      payload.setStatut(Utilisateur.Statut.actif);
    }
    if (payload.getRole() == null) {
      payload.setRole(Utilisateur.Role.client);
    }

    Utilisateur saved = utilisateurRepository.save(payload);

    // Générer un token pour auto-login (facultatif)
    Instant now = Instant.now();
    long expiry = 604800;
    var claims = JwtClaimsSet.builder()
        .issuer("transport")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(expiry))
        .subject(saved.getEmail())
        .claim("uid", saved.getId())
        .claim("roles", List.of("ROLE_" + saved.getRole().name().toUpperCase()))
        .build();
    var header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    saved.setMotDePasse(null);
    return ResponseEntity.status(HttpStatus.CREATED).body(new LoginResponse(token, saved));
  }

  @PostMapping("/register/email")
  public ResponseEntity<?> createWithEmailOnly(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    if (email == null || email.isBlank()) {
      return ResponseEntity.badRequest().body("Email requis");
    }
    return utilisateurRepository.findByEmailIgnoreCase(email)
        .map(existing -> {
          boolean dejaVerifie = Boolean.TRUE.equals(existing.getVerifier());
          if (dejaVerifie) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email deja utilise");
          }
          String token = buildVerificationToken(existing);
          String url = buildVerificationUrl(token);
          mailService.sendVerificationEmail(existing.getEmail(), url);
          existing.setMotDePasse(null);
          return ResponseEntity.ok(Map.of("user", existing, "verificationUrl", url));
        })
        .orElseGet(() -> {
          Utilisateur created = utilisateurService.createUserWithEmail(email);
          String token = buildVerificationToken(created);
          String url = buildVerificationUrl(token);
          mailService.sendVerificationEmail(created.getEmail(), url);
          created.setMotDePasse(null);
          return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("user", created, "verificationUrl", url));
        });
  }

  private String buildVerificationToken(Utilisateur user) {
    Instant now = Instant.now();
    var claims = JwtClaimsSet.builder()
        .issuer("transport")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300)) // 5 minutes
        .subject(user.getId())
        .claim("email", user.getEmail())
        .claim("purpose", "verify_email")
        .build();
    var header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private String buildVerificationUrl(String token) {
    String base = (verificationBaseUrl == null || verificationBaseUrl.isBlank())
        ? "http://localhost:3000/verify-email"
        : verificationBaseUrl;
    String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
    if (base.contains("?")) {
      return base + "&token=" + encoded;
    }
    return base + "?token=" + encoded;
  }

  @GetMapping("/verify-email")
  public ResponseEntity<Object> verifyEmail(@RequestParam("token") String token) {
    if (token == null || token.isBlank()) {
      return ResponseEntity.badRequest().body("Token manquant");
    }
    try {
      Jwt jwt = jwtDecoder.decode(token);
      Object purpose = jwt.getClaim("purpose");
      if (purpose == null || !"verify_email".equals(purpose.toString())) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token invalide");
      }
      String userId = jwt.getSubject();
      if (userId == null || userId.isBlank()) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Token invalide");
      }

      return utilisateurRepository.findById(userId)
          .map(u -> {
            u.setVerifier(true);
            utilisateurRepository.save(u);
            u.setMotDePasse(null);
            return ResponseEntity.ok().body((Object) Map.of("message", "Email verifie", "user", u));
          })
          .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body((Object) Map.of("error", "Utilisateur non trouve")));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body((Object) Map.of("error", "Token invalide ou expire"));
    }
  }

  @GetMapping("/email-verification-status")
  public ResponseEntity<Object> emailVerificationStatus(@RequestParam("email") String email) {
    if (email == null || email.isBlank()) {
      return ResponseEntity.badRequest().body((Object) Map.of("error", "Email manquant"));
    }
    boolean verified = utilisateurService.isEmailVerified(email);
    return ResponseEntity.ok((Object) Map.of("email", email, "verified", verified));
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
                    if (updated.getImageCarteIdentiteFace() != null) user.setImageCarteIdentiteFace(updated.getImageCarteIdentiteFace());
                    if (updated.getImageCarteIdentiteArriere() != null) user.setImageCarteIdentiteArriere(updated.getImageCarteIdentiteArriere());
                    if (updated.getImagePermis() != null)     user.setImagePermis(updated.getImagePermis());
                    if (updated.getImageCarteGrise() != null) user.setImageCarteGrise(updated.getImageCarteGrise());
                    if (updated.getImageAssurance() != null)  user.setImageAssurance(updated.getImageAssurance());

                    // Optionnel : si tu autorises aussi ces champs à être mis à jour partiellement
                    if (updated.getEmail() != null)          user.setEmail(updated.getEmail());
                    if (updated.getRole() != null)           user.setRole(updated.getRole());
                    if (updated.getStatut() != null)         user.setStatut(updated.getStatut());
                    if (updated.getVerifier() != null)       user.setVerifier(updated.getVerifier());

                    if (updated.getSousZone() != null)       user.setSousZone(updated.getSousZone());
                    if (updated.getZone() != null)           user.setZone(updated.getZone());

                    // Nouveaux champs: latitude / longitude (si tu utilises Double)
                    if (updated.getLatitude() != 0.0) user.setLatitude(updated.getLatitude());
                    if (updated.getLongitude() != 0.0) user.setLongitude(updated.getLongitude());

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
                    "folder", "monapp/profil",  // <- change en "monapp/produits" si besoin
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
    @PutMapping("/{id}/localisation")
    public ResponseEntity<?> updateLocalisation(
            @PathVariable("id") String userId,
            @RequestBody Map<String, Object> body) {

        // 1) Extraire latitude/longitude depuis le JSON du corps
        Double lat = null, lng = null;
        try {
            Object latObj = body.get("latitude");
            Object lngObj = body.get("longitude");
            if (latObj != null) lat = Double.valueOf(latObj.toString());
            if (lngObj != null) lng = Double.valueOf(lngObj.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur de format: latitude/longitude doivent être numériques.");
        }

        // 2) Appeler TA méthode de service (qui fait les validations et la sauvegarde)
        try {
            Utilisateur updated = utilisateurService.updateLocalisation(userId, lat, lng);
            return ResponseEntity.ok(updated);
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur serveur: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/zone/{zone}")
public ResponseEntity<Utilisateur> updateZone(
    @PathVariable String id,
    @PathVariable Utilisateur.Zone zone
) {
  Utilisateur updated = utilisateurService.updateZone(id, zone);
  return ResponseEntity.ok(updated);
}

// PUT /api/utilisateur/{id}/sous-zone/TUNIS_CENTRE
@PutMapping("/{id}/sous-zone/{sousZone}")
public ResponseEntity<Utilisateur> updateSousZone(
    @PathVariable String id,
    @PathVariable Utilisateur.SousZone sousZone
) {
  Utilisateur updated = utilisateurService.updateSousZone(id, sousZone);
  return ResponseEntity.ok(updated);
}




}
