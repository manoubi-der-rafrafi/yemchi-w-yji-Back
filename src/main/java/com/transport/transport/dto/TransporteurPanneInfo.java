package com.transport.transport.dto;

public record TransporteurPanneInfo(
    String id,
    String nom,
    String prenom,
    String telephone,
    double latitude,
    double longitude
) {}
