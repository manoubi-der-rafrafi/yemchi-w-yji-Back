package com.transport.transport.dto.partner;

import java.math.BigDecimal;
import java.util.List;

import com.transport.transport.model.Commande;
import com.transport.transport.model.TypeVehicule;

public record PartnerCreateCommandeRequest(
        String externalOrderId,
        String instructions,
        BigDecimal prix,
        Commande.ModePaiement modePaiement,
        ContactPoint depart,
        ContactPoint arrivee,
        List<ProductItem> produits,
        TypeVehicule vehicule) {

    public record ContactPoint(
            String nom,
            String telephone,
            String adresse,
            Double latitude,
            Double longitude) {}

    public record ProductItem(
            String nom,
            String type,
            Integer quantite,
            BigDecimal poids,
            BigDecimal largeur,
            BigDecimal profondeur,
            BigDecimal hauteur,
            String description,
            String image1,
            String image2,
            String image3) {}
}
