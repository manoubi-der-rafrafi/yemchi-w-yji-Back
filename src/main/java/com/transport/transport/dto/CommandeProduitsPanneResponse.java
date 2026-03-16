package com.transport.transport.dto;

import java.util.List;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;

public record CommandeProduitsPanneResponse(
    Commande commande,
    List<Produit> produits
) {}
