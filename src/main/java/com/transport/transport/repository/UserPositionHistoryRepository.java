package com.transport.transport.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.transport.transport.model.UserPositionHistory;

public interface UserPositionHistoryRepository extends MongoRepository<UserPositionHistory, String> {
}
