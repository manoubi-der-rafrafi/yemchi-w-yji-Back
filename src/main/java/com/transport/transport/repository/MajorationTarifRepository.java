package com.transport.transport.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.transport.transport.model.MajorationTarif;

public interface MajorationTarifRepository extends MongoRepository<MajorationTarif, String> {
    @Query("{ 'dateDebut': { $lte: ?0 }, 'dateFin': { $gt: ?0 } }")
    Optional<MajorationTarif> findFirstActiveAt(LocalDateTime dateReference);
    @Query("{ 'dateDebut': { $lt: ?1 }, 'dateFin': { $gt: ?0 } }")
    List<MajorationTarif> findOverlapping(LocalDateTime dateDebut, LocalDateTime dateFin);
    List<MajorationTarif> findAllByOrderByDateDebutDesc();
}
