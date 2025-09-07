package com.transport.transport.repository;

import com.transport.transport.model.Ami;
import com.transport.transport.model.StatutAmi;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface AmiRepository extends MongoRepository<Ami, String> {

    // 1) All relations of a user (either side) filtered by statut
    @Query("{ 'statut': ?1, $or: [ { 'demandeurId': ?0 }, { 'recepteurId': ?0 } ] }")
    List<Ami> findByUserIdAndStatut(Integer userId, StatutAmi statut);

    // 2) Any relation between two users (regardless of direction)
    @Query("{ $or: [ { 'demandeurId': ?0, 'recepteurId': ?1 }, { 'demandeurId': ?1, 'recepteurId': ?0 } ] }")
    List<Ami> findAnyBetween(String u1, String u2);

    // 3) Existence check in one direction with a given statut
    boolean existsByDemandeurIdAndRecepteurIdAndStatut(String demandeurId,
                                                       String recepteurId,
                                                       StatutAmi statut);

    // 4) Find one relation in one direction with a given statut
    Optional<Ami> findByDemandeurIdAndRecepteurIdAndStatut(String demandeurId,
                                                           String recepteurId,
                                                           StatutAmi statut);

    // 5) Pending invitations a user HAS RECEIVED (sorted newest first)
    @Query(value = "{ 'recepteurId': ?0, 'statut': ?1 }", sort = "{ '_id': -1 }")
    List<Ami> findPendingInvitationsForUser(String userId, StatutAmi statut);

    // 6) Accepted relations of a user (either side)
    @Query("{ 'statut': 'ACCEPTE', $or: [ { 'demandeurId': ?0 }, { 'recepteurId': ?0 } ] }")
    List<Ami> findAcceptedRelationsOf(String userId);
    private String otherSideId(Ami a, String me) {
        if (Objects.equals(a.getDemandeurId(), me)) return a.getRecepteurId();
        if (Objects.equals(a.getRecepteurId(), me)) return a.getDemandeurId();
        return null;
    }
}
