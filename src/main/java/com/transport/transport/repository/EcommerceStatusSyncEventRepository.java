package com.transport.transport.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.EcommerceStatusSyncEvent;

public interface EcommerceStatusSyncEventRepository
        extends MongoRepository<EcommerceStatusSyncEvent, String> {

    List<EcommerceStatusSyncEvent>
            findTop50ByCompletedAtIsNullAndFailedPermanentlyAtIsNullAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                    LocalDateTime now);
}
