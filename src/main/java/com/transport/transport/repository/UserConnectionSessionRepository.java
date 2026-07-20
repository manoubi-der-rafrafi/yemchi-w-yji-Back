package com.transport.transport.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.transport.transport.model.UserConnectionSession;

public interface UserConnectionSessionRepository extends MongoRepository<UserConnectionSession, String> {
  Optional<UserConnectionSession> findFirstByUserIdAndDisconnectedAtIsNullOrderByConnectedAtDesc(String userId);
}
