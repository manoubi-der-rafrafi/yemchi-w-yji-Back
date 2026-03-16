package com.transport.transport.dto;

import java.util.List;

public record TransporteurPanneCommandesResponse(
    TransporteurPanneInfo transporteur,
    List<CommandeProduitsPanneResponse> commandes
) {}
