package com.transport.transport.dto;

public class TransporteurInfo {
    private String id;
    private String nom;
    private String prenom;
    private String telephone;
    private String image;
    private double latitude;
    private double longitude;

    public TransporteurInfo() {}

    public TransporteurInfo(
        String id,
        String nom,
        String prenom,
        String telephone,
        String image,
        double latitude,
        double longitude
    ) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.telephone = telephone;
        this.image = image;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
