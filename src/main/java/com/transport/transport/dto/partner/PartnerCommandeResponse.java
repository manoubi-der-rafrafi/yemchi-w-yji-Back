package com.transport.transport.dto.partner;

import java.util.List;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Produit;

public record PartnerCommandeResponse(
        Commande commande,
        List<Produit> produits) {}
