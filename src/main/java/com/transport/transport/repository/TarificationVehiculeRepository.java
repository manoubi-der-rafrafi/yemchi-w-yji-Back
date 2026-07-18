package com.transport.transport.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.transport.transport.model.TarificationVehicule;
import com.transport.transport.model.TypeVehicule;

public interface TarificationVehiculeRepository extends MongoRepository<TarificationVehicule, String> {
    Optional<TarificationVehicule> findFirstByTypeVehiculeAndDateFinIsNullAndDateDebutLessThanEqualOrderByDateDebutDesc(
            TypeVehicule typeVehicule,
            LocalDateTime dateReference);
    List<TarificationVehicule> findByTypeVehiculeAndDateFinIsNull(TypeVehicule typeVehicule);
    List<TarificationVehicule> findAllByOrderByDateDebutDesc();
}
