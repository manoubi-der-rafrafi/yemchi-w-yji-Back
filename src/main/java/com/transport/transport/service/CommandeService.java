package com.transport.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Commande.Statut;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class CommandeService {

    @Autowired
    private final CommandeRepository commandeRepository;
    private final UtilisateurRepository utilisateurRepository;

    public CommandeService(CommandeRepository commandeRepository, UtilisateurRepository utilisateurRepository) {
        this.commandeRepository = commandeRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // Historique simple d’un client
    public List<Commande> getHistoriqueClient(String  clientId) {
        utilisateurRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));
        return commandeRepository.findByClientIdOrderByDateDemandeDesc(clientId);
    }
    // Récupérer toutes les commandes
    public List<Commande> getAllCommandes() {
        return commandeRepository.findAll();
    }

    // Récupérer une commande par son id
    public Optional<Commande> getCommandeById(String  id) {
        return commandeRepository.findById(id);
    }

    // Ajouter une nouvelle commande
    public Commande createCommande(Commande commande) {
        return commandeRepository.save(commande);
    }

    // Mettre à jour une commande existante
    public Commande updateCommande(String id, Commande details) {
        return commandeRepository.findById(id).map(commande -> {
            // copie EXPLICITE de tous les champs que tu veux rendre modifiables
            commande.setLocalisationDepart(details.getLocalisationDepart());
            commande.setDestination(details.getDestination());
            commande.setDateDebut(details.getDateDebut());
            commande.setDateFin(details.getDateFin());
            commande.setDateDemande(details.getDateDemande());
            commande.setStatut(details.getStatut());
            commande.setPrix(details.getPrix());
            commande.setModePaiement(details.getModePaiement());
            commande.setInstructions(details.getInstructions());

            commande.setTelDepart(details.getTelDepart());            // ✅ ajouté
            commande.setClientId(details.getClientId());              // ✅ s’assure que ça se copie
            commande.setTransporteurId(details.getTransporteurId());
            commande.setIdAmie(details.getIdAmie());
            commande.setTelArrivee(details.getTelArrivee());

            commande.setLatitudeDepart(details.getLatitudeDepart());
            commande.setLongitudeDepart(details.getLongitudeDepart());
            commande.setLatitudeDestination(details.getLatitudeDestination());
            commande.setLongitudeDestination(details.getLongitudeDestination());

            commande.setDistanceKm(details.getDistanceKm());
            commande.setDistanceKm(details.getDistanceKm());


            commande.setSousZoneDepart(details.getSousZoneDepart());
            commande.setSousZoneArrivee(details.getSousZoneArrivee());

            commande.setZonePrincipaleDepart(details.getZonePrincipaleDepart());
            commande.setZonePrincipaleArrivee(details.getZonePrincipaleArrivee());
            commande.setQrCodeDepartScanne(details.isQrCodeDepartScanne());
            commande.setDateScanDepart(details.getDateScanDepart());

            commande.setQrCodeReceptionScanne(details.isQrCodeReceptionScanne());
            commande.setDateScanReception(details.getDateScanReception());

            // --- Met à jour la date de modification automatique ---
            commande.setMajLe(LocalDateTime.now());
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }

    // Supprimer une commande par son id
    public void deleteCommande(String  id) {
        commandeRepository.deleteById(id);
    }
    public Optional<Commande> getCommandeEnCoursByClientId(String  idClient) {
        return commandeRepository.findByClientIdAndStatut(idClient, Commande.Statut.en_cours);
    }
    public Commande confirmerCommande(String  id) {
        return commandeRepository.findById(id).map(commande -> {
            commande.setStatut(Commande.Statut.confirmer);
            commande.setDateDemande(LocalDateTime.now()); // ➝ date actuelle
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }
    public List<Commande> getByIdAmie(String idAmie) {
        return commandeRepository.findByIdAmie(idAmie);
    }
    public int countCommandesByIdAmie(String idAmie) {
        return commandeRepository.countByIdAmie(idAmie);
    }
    public int countCommandesByIdAmieAndStatutEnvoyee(String idAmie) {
        return commandeRepository.countByIdAmieAndStatut(idAmie, Commande.Statut.envoyee);
    }   
    public List<Commande> getCommandesByZonePrincipale(Commande.Zone zone) {
    return commandeRepository.findByZonePrincipaleDepartAndZonePrincipaleArrivee(zone, zone);
}
    public List<Commande> getCommandesByZonePrincipaleConfirmees(Commande.Zone zone) {
    //return commandeRepository.findByStatutAndZonePrincipale(Statut.confirmer, zone);
    // Variante triée :
     return commandeRepository.findByStatutAndZonePrincipale(
         Statut.confirmer, zone, Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public Commande assignerTransporteur(String idCommande, String idTransporteur) {
    return commandeRepository.findById(idCommande).map(commande -> {
        commande.setTransporteurId(idTransporteur);
        commande.setMajLe(LocalDateTime.now());
        commande.setStatut(Statut.en_route);
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}
public List<Commande> getCommandesByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdOrderByDateDemandeDesc(idTransporteur);
}
public List<Commande> getCommandesBySousZones(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee) {
    if (sousZonesDepart == null || sousZonesArrivee == null || sousZonesDepart.isEmpty() || sousZonesArrivee.isEmpty()) {
        throw new IllegalArgumentException("Les listes de sous-zones doivent etre renseignees");
    }
    return commandeRepository.findBySousZoneDepartInAndSousZoneArriveeIn(sousZonesDepart, sousZonesArrivee);
}


}
