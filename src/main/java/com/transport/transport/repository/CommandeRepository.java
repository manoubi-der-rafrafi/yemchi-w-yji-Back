package com.transport.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.transport.transport.model.Commande;
import com.transport.transport.model.TypeVehicule;

@Repository
public interface CommandeRepository extends MongoRepository<Commande, String> {
    Optional<Commande> findByClientIdAndStatut(String idClient, Commande.Statut statut);
    List<Commande> findByClientIdOrderByDateDemandeDesc(String clientId);
    List<Commande> findByIdAmie(String idAmie);
    int countByIdAmie(String idAmie);
    int countByIdAmieAndStatut(String idAmie, Commande.Statut statut);
    List<Commande> findByZonePrincipaleDepartAndZonePrincipaleArrivee(Commande.Zone zoneDepart, Commande.Zone zoneArrivee);
    @org.springframework.data.mongodb.repository.Query(
    value = "{ 'statut': ?0, '$or': [ { 'zonePrincipaleDepart': ?1 }, { 'zonePrincipaleArrivee': ?1 } ] }"
)
List<Commande> findByStatutAndZonePrincipale(Commande.Statut statut, Commande.Zone zone, org.springframework.data.domain.Sort sort);
List<Commande> findByTransporteurIdOrderByDateDemandeDesc(String transporteurId);
    List<Commande> findBySousZoneDepartInAndSousZoneArriveeIn(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee);
    List<Commande> findBySousZoneDepartInAndSousZoneArriveeInAndVehicule(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee,
        TypeVehicule vehicule);
    List<Commande> findBySousZoneDepartInAndSousZoneArriveeInAndVehiculeAndStatut(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee,
        TypeVehicule vehicule,
        Commande.Statut statut);
    List<Commande> findByZonePrincipaleDepartAndZonePrincipaleArriveeAndVehicule(
        Commande.Zone zoneDepart,
        Commande.Zone zoneArrivee,
        TypeVehicule vehicule);

}
