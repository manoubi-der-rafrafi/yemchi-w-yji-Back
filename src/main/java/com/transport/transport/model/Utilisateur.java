package com.transport.transport.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "utilisateur")
public class Utilisateur {

    @Id
    private String id;

    private String nom;
    private String prenom;
    private LocalDate dateNaissance;

    private String email;
    private String motDePasse;
    private String telephone;
    private String identifiant;
    private String phoneCountryCode;
    private String phoneDialCode;

    /**
     * Indique si l'email a été vérifié.
     * Utilisé pour le flux d'inscription/validation.
     */
    private Boolean isEmailVerified = false;

    private Role role ;

    private String adresse;

    /** chemin/URL vers l'image de profil */
    private String image;
    private String imageCarteIdentiteFace;
    private String imageCarteIdentiteArriere;
    private String imagePermis;
    private String imageCarteGrise;
    private String imageAssurance;
    private TypeVehicule typeVehicule;

    private Statut statut = Statut.actif;

    @CreatedDate
    private LocalDateTime dateCreation;

    private LocalDateTime lastSeen;
    private boolean online;
    private double latitude;
    private double longitude;
    private SousZone sousZone;
    private Zone zone;
    private Map<String, List<String>> zoneDepart;
    @JsonAlias("zoneArriver")
    private Map<String, List<String>> zoneAriver;
    public enum Role { client, transporteur, admin }
    public enum Statut { actif, inactif, banni }


    public Utilisateur() {}

    // --- Getters/Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }
    public String getPhoneCountryCode() { return phoneCountryCode; }
    public void setPhoneCountryCode(String phoneCountryCode) { this.phoneCountryCode = phoneCountryCode; }
    public String getPhoneDialCode() { return phoneDialCode; }
    public void setPhoneDialCode(String phoneDialCode) { this.phoneDialCode = phoneDialCode; }

    public Boolean getIsEmailVerified() { return isEmailVerified; }
    public void setIsEmailVerified(Boolean isEmailVerified) { this.isEmailVerified = isEmailVerified; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getImageCarteIdentiteFace() { return imageCarteIdentiteFace; }
    public void setImageCarteIdentiteFace(String imageCarteIdentiteFace) { this.imageCarteIdentiteFace = imageCarteIdentiteFace; }

    public String getImageCarteIdentiteArriere() { return imageCarteIdentiteArriere; }
    public void setImageCarteIdentiteArriere(String imageCarteIdentiteArriere) { this.imageCarteIdentiteArriere = imageCarteIdentiteArriere; }

    public String getImagePermis() { return imagePermis; }
    public void setImagePermis(String imagePermis) { this.imagePermis = imagePermis; }

    public String getImageCarteGrise() { return imageCarteGrise; }
    public void setImageCarteGrise(String imageCarteGrise) { this.imageCarteGrise = imageCarteGrise; }

    public String getImageAssurance() { return imageAssurance; }
    public void setImageAssurance(String imageAssurance) { this.imageAssurance = imageAssurance; }

    public TypeVehicule getTypeVehicule() { return typeVehicule; }
    public void setTypeVehicule(TypeVehicule typeVehicule) { this.typeVehicule = typeVehicule; }

    public Statut getStatut() { return statut; }
    public void setStatut(Statut statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public SousZone getSousZone() { return sousZone; }
    public void setSousZone(SousZone sousZone) { this.sousZone = sousZone; }

    public Zone getZone() { return zone; }
    public void setZone(Zone zone) { this.zone = zone; }

    public Map<String, List<String>> getZoneDepart() { return zoneDepart; }
    public void setZoneDepart(Map<String, List<String>> zoneDepart) { this.zoneDepart = zoneDepart; }

    public Map<String, List<String>> getZoneAriver() { return zoneAriver; }
    public void setZoneAriver(Map<String, List<String>> zoneAriver) { this.zoneAriver = zoneAriver; }


    // --- Enum pour les grandes zones (régions principales) ---
public enum Zone {
    GRAND_TUNIS,
    COTIER_NORD,
    NORD_EST,
    NORD_OUEST,
    CENTRE_EST,
    CENTRE,
    CENTRE_OUEST,
    SAHEL,
    SFAX,
    SUD_EST,
    SUD_OUEST,
    INTERIEUR
}

// --- Enum pour les sous-zones (zones détaillées pour scooters) ---
public enum SousZone {
    // Grand Tunis
    TUNIS,
    TUNIS_CENTRE,
    ARIANA,
    ARIANA_NORD,
    BEN_AROUS,
    BEN_AROUS_SUD,
    MANOUBA,
    MANOUBA_OUEST,

    // Côtier Nord
    BIZERTE,
    BIZERTE_METRO,
    NABEUL,
    NABEUL_HAMMAMET,
    KELIBIA_MENZEL_TEMIME,

    // Nord Ouest
    BEJA,
    JENDOUBA,
    KEF,
    SILIANA,

    // Centre Est
    SOUSSE,
    MONASTIR,
    MAHDIA,

    // Centre / Centre Ouest
    ZAGHOUAN,
    // Sfax
    SFAX,
    KAIROUAN,
    KASSERINE,
    SIDI_BOUZID,

    // Sud Est
    GABES,
    MEDENINE,
    TATAOUINE,
    DJERBA_ZARZIS,

    // Intérieur
    // Sud Ouest
    GAFSA,
    TOZEUR,
    KEBILI
}

}
