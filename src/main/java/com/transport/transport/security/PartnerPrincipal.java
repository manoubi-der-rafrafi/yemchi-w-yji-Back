package com.transport.transport.security;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class PartnerPrincipal {

    private final String partnerId;
    private final String externalBusinessId;
    private final String businessName;
    private final Set<String> scopes;
    private final String keyPrefix;

    public PartnerPrincipal(
            String partnerId,
            String externalBusinessId,
            String businessName,
            Set<String> scopes,
            String keyPrefix) {
        this.partnerId = partnerId;
        this.externalBusinessId = externalBusinessId;
        this.businessName = businessName;
        this.scopes = Collections.unmodifiableSet(new LinkedHashSet<>(scopes));
        this.keyPrefix = keyPrefix;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public String getBusinessName() {
        return businessName;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }
}
