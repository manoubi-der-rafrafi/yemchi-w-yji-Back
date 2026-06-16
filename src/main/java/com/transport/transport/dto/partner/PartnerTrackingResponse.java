package com.transport.transport.dto.partner;

import com.transport.transport.dto.TransporteurInfo;

public record PartnerTrackingResponse(
        String externalOrderId,
        String transportOrderId,
        String statut,
        TransporteurInfo transporteur,
        Double destinationLatitude,
        Double destinationLongitude,
        Long estimatedArrivalMinutes) {}
