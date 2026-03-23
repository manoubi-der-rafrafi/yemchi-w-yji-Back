package com.transport.transport.dto;

public record TransporteurPanneInfo(
    String id,
    String nom,
    String prenom,
    String telephone,
    String image,
    double latitude,
    double longitude,
    String etatIncident
) {}
