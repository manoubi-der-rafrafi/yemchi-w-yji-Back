package com.transport.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.transport.transport.model.Commande;
import com.transport.transport.model.TypeVehicule;

@Repository
public interface CommandeRepository extends MongoRepository<Commande, String> {
    Optional<Commande> findByClientIdAndStatut(String idClient, Commande.Statut statut);
    List<Commande> findByClientIdAndStatutOrderByDateDemandeDesc(String clientId, Commande.Statut statut);
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
List<Commande> findByTransporteurIdAndStatut(String transporteurId, Commande.Statut statut);
List<Commande> findByTransporteurIdAndStatutOrderByDateDemandeDesc(
        String transporteurId,
        Commande.Statut statut);
@org.springframework.data.mongodb.repository.Query(
    value = "{ 'transporteurId': ?0, 'statut': { '$nin': ?1 } }"
)
List<Commande> findByTransporteurIdAndStatutNotInOrderByDateDemandeDesc(
        String transporteurId,
        List<Commande.Statut> statuts);
List<Commande> findByTransporteurIdAndStatutAndModePaiement(
        String transporteurId,
        Commande.Statut statut,
        Commande.ModePaiement modePaiement);
List<Commande> findByTransporteurIdAndStatutAndModePaiementNot(
        String transporteurId,
        Commande.Statut statut,
        Commande.ModePaiement modePaiement);
List<Commande> findByTransporteurIdAndModePaiementOrderByDateDemandeDesc(
        String transporteurId,
        Commande.ModePaiement modePaiement);
List<Commande> findByTransporteurIdAndModePaiementNotOrderByDateDemandeDesc(
        String transporteurId,
        Commande.ModePaiement modePaiement);
List<Commande> findByTransporteurIdAndModePaiementAndStatutOrderByDateDemandeDesc(
        String transporteurId,
        Commande.ModePaiement modePaiement,
        Commande.Statut statut);
List<Commande> findByTransporteurIdAndModePaiementNotAndStatutOrderByDateDemandeDesc(
        String transporteurId,
        Commande.ModePaiement modePaiement,
        Commande.Statut statut);
@org.springframework.data.mongodb.repository.Query(
    value = "{ 'transporteurId': ?0, 'modePaiement': ?1, '$or': [ { 'sousZoneDepart': ?2 }, { 'sousZoneArrivee': ?2 } ] }"
)
List<Commande> findByTransporteurIdAndModePaiementAndSousZone(
        String transporteurId,
        Commande.ModePaiement modePaiement,
        Commande.SousZone sousZone,
        Sort sort);
@org.springframework.data.mongodb.repository.Query(
    value = "{ 'transporteurId': ?0, 'modePaiement': { '$ne': ?1 }, '$or': [ { 'sousZoneDepart': ?2 }, { 'sousZoneArrivee': ?2 } ] }"
)
List<Commande> findByTransporteurIdAndModePaiementNotAndSousZone(
        String transporteurId,
        Commande.ModePaiement modePaiement,
        Commande.SousZone sousZone,
        Sort sort);
@org.springframework.data.mongodb.repository.Query(
    value = "{ 'transporteurId': ?0, 'modePaiement': ?1, 'statut': ?2, '$or': [ { 'sousZoneDepart': ?3 }, { 'sousZoneArrivee': ?3 } ] }"
)
List<Commande> findByTransporteurIdAndModePaiementAndStatutAndSousZone(
        String transporteurId,
        Commande.ModePaiement modePaiement,
        Commande.Statut statut,
        Commande.SousZone sousZone,
        Sort sort);
@org.springframework.data.mongodb.repository.Query(
    value = "{ 'transporteurId': ?0, 'modePaiement': { '$ne': ?1 }, 'statut': ?2, '$or': [ { 'sousZoneDepart': ?3 }, { 'sousZoneArrivee': ?3 } ] }"
)
List<Commande> findByTransporteurIdAndModePaiementNotAndStatutAndSousZone(
        String transporteurId,
        Commande.ModePaiement modePaiement,
        Commande.Statut statut,
        Commande.SousZone sousZone,
        Sort sort);
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
