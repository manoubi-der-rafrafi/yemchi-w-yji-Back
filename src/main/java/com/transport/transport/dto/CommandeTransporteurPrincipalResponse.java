package com.transport.transport.dto;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;

import java.util.List;

public record CommandeTransporteurPrincipalResponse(
    Commande commande,
    List<Produit> produits,
    TransporteurInfo transporteur
) {}
