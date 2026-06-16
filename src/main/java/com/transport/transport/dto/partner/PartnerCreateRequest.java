package com.transport.transport.dto.partner;

import java.util.List;

public record PartnerCreateRequest(
        String externalBusinessId,
        String externalOwnerUserId,
        String businessName,
        List<String> scopes) {}
