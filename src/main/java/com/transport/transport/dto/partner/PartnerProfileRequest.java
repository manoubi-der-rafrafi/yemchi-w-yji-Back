package com.transport.transport.dto.partner;

import java.util.List;

public record PartnerProfileRequest(
        String businessName,
        String logoUrl,
        String phone,
        List<String> phoneNumbers,
        String email,
        String address,
        String facebookUrl,
        String instagramUrl,
        String tiktokUrl,
        Double latitude,
        Double longitude) {}
