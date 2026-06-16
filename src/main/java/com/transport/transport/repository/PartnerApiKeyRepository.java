package com.transport.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.PartnerApiKey;

public interface PartnerApiKeyRepository extends MongoRepository<PartnerApiKey, String> {
    Optional<PartnerApiKey> findByKeyPrefix(String keyPrefix);
    List<PartnerApiKey> findByPartnerId(String partnerId);
}
