package com.transport.transport.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.transport.transport.dto.TransporteurPanneCommandesResponse;
import com.transport.transport.dto.UserPosition;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.repository.UtilisateurRepository;
import com.transport.transport.service.AuthorizationService;
import com.transport.transport.service.MailService;
import com.transport.transport.service.MailService.MailDeliveryException;
import com.transport.transport.service.MailService.MailTransportException;
import com.transport.transport.service.UtilisateurService;
@RestController
@RequestMapping("/api/utilisateur")
public class UtilisateurController {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurController.class);


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
  private final AuthorizationService authorizationService;
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
      MailService mailService,
      AuthorizationService authorizationService) {
        this.utilisateurService = utilisateurService;
        this.cloudinary = cloudinary;
        this.authManager = authManager;
    this.jwtEncoder = jwtEncoder;
    this.jwtDecoder = jwtDecoder;
    this.passwordEncoder = passwordEncoder;
    this.googleVerifier = googleVerifier;
    this.mailService = mailService;
    this.authorizationService = authorizationService;

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
        if (user.getDateCreation() == null) {
          return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Inscription incomplÈte");
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
      if (user.getDateCreation() == null) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Inscription incomplÈte");
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
  public static record SendEmailRequest(String to, String subject, String body, Boolean html) {}

    // Récupérer un utilisateur par ID (GET /api/utilisateur/id/1)
    @GetMapping("/id/{id}")
    public ResponseEntity<?> getUtilisateurById(@PathVariable String  id, Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        if (!authorizationService.canAccessUserData(current, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acces refuse");
        }
        Utilisateur user = utilisateurRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        user.setMotDePasse(null);
        return ResponseEntity.ok(user);
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
    payload.setStatut(Utilisateur.Statut.actif);
    payload.setRole(Utilisateur.Role.client);
    payload.setIsEmailVerified(false);
    if (payload.getDateCreation() == null) {
      payload.setDateCreation(Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
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
    String traceId = UUID.randomUUID().toString();
    String email = body.get("email");
    logger.info("traceId={} register/email start email={}", traceId, maskEmail(email));
    if (email == null || email.isBlank()) {
      logger.warn("traceId={} register/email missing email", traceId);
      return buildTextErrorResponse(HttpStatus.BAD_REQUEST, traceId, "REG_EMAIL_MISSING", "Email requis");
    }
    try {
      return utilisateurRepository.findByEmailIgnoreCase(email)
        .map(existing -> {
          boolean dejaVerifie = Boolean.TRUE.equals(existing.getIsEmailVerified());
          boolean hasDateCreation = existing.getDateCreation() != null;
          logger.info(
              "traceId={} register/email existing user email={} verified={} hasDateCreation={}",
              traceId,
              maskEmail(existing.getEmail()),
              dejaVerifie,
              hasDateCreation);
          if (dejaVerifie && hasDateCreation) {
            logger.warn("traceId={} register/email conflict existing verified account email={}",
                traceId, maskEmail(existing.getEmail()));
            return buildTextErrorResponse(
                HttpStatus.CONFLICT,
                traceId,
                "REG_EMAIL_ALREADY_EXISTS",
                "Email deja existe");
          }
          if (dejaVerifie && !hasDateCreation) {
            existing.setIsEmailVerified(null);
            existing = utilisateurRepository.save(existing);
            logger.info("traceId={} register/email reset verification state email={}",
                traceId, maskEmail(existing.getEmail()));
          }
          String token = buildVerificationToken(existing);
          String url = buildVerificationUrl(token);
          logger.info("traceId={} register/email sending verification email existingUser=true email={}",
              traceId, maskEmail(existing.getEmail()));
          mailService.sendVerificationEmail(
              existing.getEmail(),
              url,
              "Merci de confirmer votre adresse email pour activer votre compte."
          );
          logger.info("traceId={} register/email verification email sent email={}",
              traceId, maskEmail(existing.getEmail()));
          existing.setMotDePasse(null);
          return ResponseEntity.ok()
              .headers(traceHeaders(traceId, null))
              .body(Map.of("user", existing));
        })
        .orElseGet(() -> {
          Utilisateur created = utilisateurService.createUserWithEmail(email);
          String token = buildVerificationToken(created);
          String url = buildVerificationUrl(token);
          logger.info("traceId={} register/email sending verification email existingUser=false email={}",
              traceId, maskEmail(created.getEmail()));
          mailService.sendVerificationEmail(
              created.getEmail(),
              url,
              "Merci de confirmer votre adresse email pour terminer la création de votre compte."
          );
          logger.info("traceId={} register/email verification email sent email={}",
              traceId, maskEmail(created.getEmail()));
          created.setMotDePasse(null);
          return ResponseEntity.status(HttpStatus.CREATED)
              .headers(traceHeaders(traceId, null))
              .body(Map.of("user", created));
        });
    } catch (MailDeliveryException e) {
      return buildMailFailureResponse(
          e,
          traceId,
          "REGISTER_EMAIL_PROVIDER_ERROR",
          "Echec de l'envoi de l'email de verification");
    } catch (MailTransportException e) {
      logger.error("traceId={} register/email transport failure email={}", traceId, maskEmail(email), e);
      return buildJsonErrorResponse(
          HttpStatus.BAD_GATEWAY,
          traceId,
          "REGISTER_EMAIL_TRANSPORT_ERROR",
          "Echec reseau lors de l'envoi de l'email de verification",
          Map.of("cause", e.getMessage()));
    } catch (IllegalStateException e) {
      logger.error("traceId={} register/email config failure email={}", traceId, maskEmail(email), e);
      return buildJsonErrorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          traceId,
          "REGISTER_EMAIL_CONFIG_ERROR",
          e.getMessage(),
          null);
    } catch (RuntimeException e) {
      MailDeliveryException nested = findMailDeliveryException(e);
      if (nested != null) {
        return buildMailFailureResponse(
            nested,
            traceId,
            "REGISTER_EMAIL_PROVIDER_ERROR",
            "Echec de l'envoi de l'email de verification");
      }
      logger.error("traceId={} register/email unexpected failure email={}", traceId, maskEmail(email), e);
      return buildJsonErrorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          traceId,
          "REGISTER_EMAIL_INTERNAL_ERROR",
          e.getMessage(),
          null);
    }
  }

  @PostMapping("/register/complete")
  public ResponseEntity<?> registerStep1Identity(@RequestBody Map<String, String> body) {
    String email = body.get("email");
    String nom = body.get("nom");
    String prenom = body.get("prenom");
    if (email == null || email.isBlank()) {
      return ResponseEntity.badRequest().body("Email requis");
    }
    if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank()) {
      return ResponseEntity.badRequest().body("Nom et prenom requis");
    }
    return utilisateurRepository.findByEmailIgnoreCase(email)
        .map(u -> {
          if (!Boolean.TRUE.equals(u.getIsEmailVerified())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("Email non verifie");
          }
          u.setNom(nom);
          u.setPrenom(prenom);
          utilisateurRepository.save(u);
          u.setMotDePasse(null);
          return ResponseEntity.ok(u);
        })
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouve"));
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
            u.setIsEmailVerified(true);
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

  @PostMapping("/send-email")
  public ResponseEntity<?> sendEmail(@RequestBody SendEmailRequest req) {
    String traceId = UUID.randomUUID().toString();
    if (req == null || req.to() == null || req.to().isBlank()) {
      logger.warn("traceId={} send-email missing recipient", traceId);
      return buildTextErrorResponse(
          HttpStatus.BAD_REQUEST,
          traceId,
          "SEND_EMAIL_MISSING_RECIPIENT",
          "Destinataire requis");
    }
    if (req.subject() == null || req.subject().isBlank()) {
      logger.warn("traceId={} send-email missing subject to={}", traceId, maskEmail(req.to()));
      return buildTextErrorResponse(
          HttpStatus.BAD_REQUEST,
          traceId,
          "SEND_EMAIL_MISSING_SUBJECT",
          "Sujet requis");
    }
    if (req.body() == null || req.body().isBlank()) {
      logger.warn("traceId={} send-email missing body to={}", traceId, maskEmail(req.to()));
      return buildTextErrorResponse(
          HttpStatus.BAD_REQUEST,
          traceId,
          "SEND_EMAIL_MISSING_BODY",
          "Contenu requis");
    }
    boolean isHtml = Boolean.TRUE.equals(req.html());
    try {
      logger.info("traceId={} send-email start to={} subject={} html={}",
          traceId, maskEmail(req.to()), req.subject(), isHtml);
      mailService.sendEmail(req.to(), req.subject(), req.body(), isHtml);
      logger.info("traceId={} send-email success to={} subject={}",
          traceId, maskEmail(req.to()), req.subject());
      return ResponseEntity.ok()
          .headers(traceHeaders(traceId, null))
          .body(Map.of("success", true));
    } catch (MailDeliveryException e) {
      return buildMailFailureResponse(
          e,
          traceId,
          "SEND_EMAIL_PROVIDER_ERROR",
          "Echec de l'envoi de l'email");
    } catch (MailTransportException e) {
      logger.error("traceId={} send-email transport failure to={}", traceId, maskEmail(req.to()), e);
      return buildJsonErrorResponse(
          HttpStatus.BAD_GATEWAY,
          traceId,
          "SEND_EMAIL_TRANSPORT_ERROR",
          "Echec reseau lors de l'envoi de l'email",
          Map.of("cause", e.getMessage()));
    } catch (IllegalStateException e) {
      logger.error("traceId={} send-email config failure to={}", traceId, maskEmail(req.to()), e);
      return buildJsonErrorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          traceId,
          "SEND_EMAIL_CONFIG_ERROR",
          e.getMessage(),
          null);
    } catch (RuntimeException e) {
      MailDeliveryException nested = findMailDeliveryException(e);
      if (nested != null) {
        return buildMailFailureResponse(
            nested,
            traceId,
            "SEND_EMAIL_PROVIDER_ERROR",
            "Echec de l'envoi de l'email");
      }
      logger.error("traceId={} send-email unexpected failure to={}", traceId, maskEmail(req.to()), e);
      return buildJsonErrorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          traceId,
          "SEND_EMAIL_INTERNAL_ERROR",
          e.getMessage(),
          null);
    }
  }

  private ResponseEntity<Map<String, Object>> buildMailFailureResponse(
      MailDeliveryException e,
      String traceId,
      String genericErrorCode,
      String prefixMessage) {
    String providerMessage = sanitizeProviderMessage(e.getProviderResponseBody());
    String errorCode = resolveProviderErrorCode(genericErrorCode, e.getStatusCode());
    StringBuilder message = new StringBuilder(prefixMessage);
    if (!providerMessage.isBlank()) {
      message.append(": ").append(providerMessage);
    }
    if (e.getStatusCode() == 401 || e.getStatusCode() == 403) {
      message.append(". Verifiez RESEND_API_KEY et RESEND_FROM. ")
          .append("Avec onboarding@resend.dev, Resend peut refuser l'envoi vers des adresses externes.");
    }
    logger.error("traceId={} mail provider failure errorCode={} providerStatus={} providerMessage={}",
        traceId, errorCode, e.getStatusCode(), providerMessage);
    return buildJsonErrorResponse(
        HttpStatus.BAD_GATEWAY,
        traceId,
        errorCode,
        message.toString(),
        Map.of("providerStatus", e.getStatusCode()));
  }

  private ResponseEntity<String> buildTextErrorResponse(
      HttpStatus status,
      String traceId,
      String errorCode,
      String message) {
    return ResponseEntity.status(status)
        .headers(traceHeaders(traceId, errorCode))
        .body(message);
  }

  private ResponseEntity<Map<String, Object>> buildJsonErrorResponse(
      HttpStatus status,
      String traceId,
      String errorCode,
      String message,
      Map<String, Object> extraFields) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", message);
    body.put("errorCode", errorCode);
    body.put("traceId", traceId);
    if (extraFields != null && !extraFields.isEmpty()) {
      body.putAll(extraFields);
    }
    return ResponseEntity.status(status)
        .headers(traceHeaders(traceId, errorCode))
        .body(body);
  }

  private HttpHeaders traceHeaders(String traceId, String errorCode) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Trace-Id", traceId);
    if (errorCode != null && !errorCode.isBlank()) {
      headers.add("X-Error-Code", errorCode);
    }
    return headers;
  }

  private String resolveProviderErrorCode(String genericErrorCode, int providerStatus) {
    if (providerStatus == 401 || providerStatus == 403) {
      return genericErrorCode + "_AUTH";
    }
    if (providerStatus == 429) {
      return genericErrorCode + "_RATE_LIMIT";
    }
    if (providerStatus >= 500) {
      return genericErrorCode + "_UPSTREAM";
    }
    return genericErrorCode + "_REJECTED";
  }

  private String sanitizeProviderMessage(String providerMessage) {
    if (providerMessage == null) {
      return "";
    }
    return providerMessage.replaceAll("\\s+", " ").trim();
  }

  private MailDeliveryException findMailDeliveryException(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof MailDeliveryException mailDeliveryException) {
        return mailDeliveryException;
      }
      current = current.getCause();
    }
    return null;
  }

  private String maskEmail(String email) {
    if (email == null || email.isBlank()) {
      return "<empty>";
    }
    int atIndex = email.indexOf('@');
    if (atIndex <= 0) {
      return "***";
    }
    String localPart = email.substring(0, atIndex);
    String domain = email.substring(atIndex);
    if (localPart.length() == 1) {
      return localPart + "***" + domain;
    }
    return localPart.substring(0, Math.min(2, localPart.length())) + "***" + domain;
  }
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateUtilisateur(@PathVariable String  id,
                                                    @RequestBody Utilisateur updated,
                                                    Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        boolean isAdmin = authorizationService.isAdmin(current);
        if (!isAdmin && !current.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acces refuse");
        }
        return utilisateurRepository.findById(id)
                .map(user -> {
                    // ⚠️ On met à jour uniquement si la nouvelle valeur est fournie (!= null)
                    if (updated.getNom() != null)            user.setNom(updated.getNom());
                    if (updated.getPrenom() != null)         user.setPrenom(updated.getPrenom());
                    if (updated.getAdresse() != null)        user.setAdresse(updated.getAdresse());
                    if (updated.getTelephone() != null)      user.setTelephone(updated.getTelephone());
                    if (updated.getIdentifiant() != null)    user.setIdentifiant(updated.getIdentifiant());
                    if (updated.getPhoneCountryCode() != null) user.setPhoneCountryCode(updated.getPhoneCountryCode());
                    if (updated.getPhoneDialCode() != null)    user.setPhoneDialCode(updated.getPhoneDialCode());
                    if (updated.getDateNaissance() != null)  user.setDateNaissance(updated.getDateNaissance());
                    if (updated.getImage() != null)          user.setImage(updated.getImage()); // URL seulement si envoyée
                    if (updated.getImageCarteIdentiteFace() != null) user.setImageCarteIdentiteFace(updated.getImageCarteIdentiteFace());
                    if (updated.getImageCarteIdentiteArriere() != null) user.setImageCarteIdentiteArriere(updated.getImageCarteIdentiteArriere());
                    if (updated.getImagePermis() != null)     user.setImagePermis(updated.getImagePermis());
                    if (updated.getImageCarteGrise() != null) user.setImageCarteGrise(updated.getImageCarteGrise());
                    if (updated.getImageAssurance() != null)  user.setImageAssurance(updated.getImageAssurance());

                    // Optionnel : si tu autorises aussi ces champs à être mis à jour partiellement
                    if (isAdmin && updated.getEmail() != null)          user.setEmail(updated.getEmail());
                    if (isAdmin && updated.getRole() != null)           user.setRole(updated.getRole());
                    if (isAdmin && updated.getStatut() != null)         user.setStatut(updated.getStatut());
                    if (isAdmin && updated.getIsEmailVerified() != null)       user.setIsEmailVerified(updated.getIsEmailVerified());

                    if (updated.getSousZone() != null)       user.setSousZone(updated.getSousZone());
                    if (updated.getZone() != null)           user.setZone(updated.getZone());
                    if (updated.getZoneDepart() != null)     user.setZoneDepart(updated.getZoneDepart());
                    if (updated.getZoneAriver() != null)     user.setZoneAriver(updated.getZoneAriver());

                    // Nouveaux champs: latitude / longitude (si tu utilises Double)
                    if (updated.getLatitude() != 0.0) user.setLatitude(updated.getLatitude());
                    if (updated.getLongitude() != 0.0) user.setLongitude(updated.getLongitude());
                    if (updated.getEtatIncident() != null) user.setEtatIncident(updated.getEtatIncident());

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
    public ResponseEntity<?> me(@RequestParam(required = false) String  id, Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        if (id == null || id.isBlank()) {
            id = current.getId();
        }
        if (!authorizationService.isAdmin(current) && !current.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acces refuse");
        }
        Utilisateur user = utilisateurRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utilisateur non trouvé");
        }
        user.setMotDePasse(null);
        return ResponseEntity.ok(user);
    }
    // Dans UtilisateurController

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatusPost(@PathVariable String  id, @RequestBody Map<String, String> body,
                                              Authentication authentication) {
        Utilisateur current = authorizationService.currentUser(authentication);
        if (!authorizationService.isAdmin(current) && !current.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acces refuse");
        }
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
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        authorizationService.requireSelfOrAdmin(userId, authentication);

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
    @PathVariable Utilisateur.Zone zone,
    Authentication authentication
) {
  authorizationService.requireSelfOrAdmin(id, authentication);
  Utilisateur updated = utilisateurService.updateZone(id, zone);
  return ResponseEntity.ok(updated);
}

// PUT /api/utilisateur/{id}/sous-zone/TUNIS_CENTRE
@PutMapping("/{id}/sous-zone/{sousZone}")
public ResponseEntity<Utilisateur> updateSousZone(
    @PathVariable String id,
    @PathVariable Utilisateur.SousZone sousZone,
    Authentication authentication
) {
  authorizationService.requireSelfOrAdmin(id, authentication);
  Utilisateur updated = utilisateurService.updateSousZone(id, sousZone);
  return ResponseEntity.ok(updated);
}

public static record UpdateZonesRequest(
    Map<String, List<String>> zoneDepart,
    Map<String, List<String>> zoneAriver
) {}

public static record DeclarerAccidentRequest(
    Map<String, Integer> produitsAffectes,
    List<String> produitsNonAffectes
) {}

// PUT /api/utilisateur/{id}/zones-depart-arriver
@PutMapping("/{id}/zones-depart-arriver")
public ResponseEntity<Utilisateur> updateZoneAriverDepart(
    @PathVariable String id,
    @RequestBody UpdateZonesRequest body,
    Authentication authentication
) {
  authorizationService.requireSelfOrAdmin(id, authentication);
  Utilisateur updated = utilisateurService.updateZonesDepartAriver(id, body.zoneDepart(), body.zoneAriver());
  return ResponseEntity.ok(updated);
}

@PutMapping("/{id}/etat-incident/panne")
public ResponseEntity<?> marquerTransporteurEnPanne(@PathVariable String id, Authentication authentication) {
  try {
    authorizationService.requireSelfOrAdmin(id, authentication);
    Utilisateur updated = utilisateurService.marquerEnPanne(id);
    return ResponseEntity.ok(updated);
  } catch (ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
  }
}

@PutMapping("/{id}/etat-incident/accident")
public ResponseEntity<?> marquerTransporteurEnAccident(@PathVariable String id, Authentication authentication) {
  try {
    authorizationService.requireSelfOrAdmin(id, authentication);
    Utilisateur updated = utilisateurService.marquerEnAccident(id);
    return ResponseEntity.ok(updated);
  } catch (ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
  }
}

@PutMapping("/{id}/etat-incident/rien")
public ResponseEntity<?> reinitialiserEtatIncidentTransporteur(@PathVariable String id, Authentication authentication) {
  try {
    authorizationService.requireSelfOrAdmin(id, authentication);
    Utilisateur updated = utilisateurService.reinitialiserEtatIncident(id);
    return ResponseEntity.ok(updated);
  } catch (ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
  }
}

@PutMapping("/{id}/etat-incident/accident/produits")
public ResponseEntity<?> declarerAccidentAvecProduits(
    @PathVariable String id,
    @RequestBody DeclarerAccidentRequest body,
    Authentication authentication) {
  try {
    authorizationService.requireSelfOrAdmin(id, authentication);
    Utilisateur updated = utilisateurService.declarerAccidentAvecProduits(
        id,
        body == null ? null : body.produitsAffectes(),
        body == null ? null : body.produitsNonAffectes());
    return ResponseEntity.ok(updated);
  } catch (ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason());
  }
}

public static record PositionsRequest(List<String> ids) {}

// POST /api/utilisateur/positions
@PostMapping("/positions")
public ResponseEntity<?> getPositions(@RequestBody PositionsRequest body, Authentication authentication) {
  if (body == null || body.ids() == null || body.ids().isEmpty()) {
    return ResponseEntity.badRequest().body("Liste d'identifiants requise");
  }
  List<String> allowedIds = authorizationService.filterAllowedUserIds(authentication, body.ids())
      .stream()
      .toList();
  List<UserPosition> positions = utilisateurService.getPositionsByUserIds(allowedIds);
  return ResponseEntity.ok(positions);
}

@GetMapping("/transporteurs/panne/commandes")
public ResponseEntity<List<TransporteurPanneCommandesResponse>> getTransporteursEnPanneAvecCommandes(
    Authentication authentication) {
  return ResponseEntity.ok(
      utilisateurService.getTransporteursEnPanneAvecCommandes(
          authentication != null ? authentication.getName() : null));
}




}
