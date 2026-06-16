package com.transport.transport.dto.partner;

public record PartnerProvisionResponse(
        String partnerId,
        String externalBusinessId,
        String businessName,
        String keyPrefix,
        String apiKey) {}
