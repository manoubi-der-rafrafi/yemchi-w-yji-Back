package com.transport.transport.dto;

import java.util.List;

public record TransporteurSecoursCommandesResponse(
    TransporteurInfo transporteurSecours,
    List<CommandeProduitsSecoursResponse> commandes
) {}
