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

    // Historique simple d'un client
    public List<Commande> getHistoriqueClient(String  clientId) {
        utilisateurRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable"));
        return commandeRepository.findByClientIdOrderByDateDemandeDesc(clientId);
    }
    // Recuperer toutes les commandes
    public List<Commande> getAllCommandes() {
        return commandeRepository.findAll();
    }

    // Recuperer une commande par son id
    public Optional<Commande> getCommandeById(String  id) {
        return commandeRepository.findById(id);
    }

    // Ajouter une nouvelle commande
    public Commande createCommande(Commande commande) {
        return commandeRepository.save(commande);
    }

    // Mettre a jour une commande existante
    public Commande updateCommande(String id, Commande details) {
        return commandeRepository.findById(id).map(commande -> {
            // copie EXPLICITE de tous les champs que tu veux rendre modifiables
            if (details.getLocalisationDepart() != null) {
                commande.setLocalisationDepart(details.getLocalisationDepart());
            }
            if (details.getDestination() != null) {
                commande.setDestination(details.getDestination());
            }
            if (details.getDateDebut() != null) {
                commande.setDateDebut(details.getDateDebut());
            }
            if (details.getDateFin() != null) {
                commande.setDateFin(details.getDateFin());
            }
            if (details.getDateDemande() != null) {
                commande.setDateDemande(details.getDateDemande());
            }
            if (details.getStatut() != null) {
                if(details.getStatut() == Statut.confirmer)
                {
                    commande.setDateConfirmation(LocalDateTime.now());
                }
                commande.setStatut(details.getStatut());
            }
            if (details.getPrix() != null) {
                commande.setPrix(details.getPrix());
            }
            if (details.getModePaiement() != null) {
                commande.setModePaiement(details.getModePaiement());
            }
            if (details.getInstructions() != null) {
                commande.setInstructions(details.getInstructions());
            }

            if (details.getTelDepart() != null) {            // �o. ajout�
                commande.setTelDepart(details.getTelDepart());
            }
            if (details.getClientId() != null) {              // �o. s'assure que �a se copie
                commande.setClientId(details.getClientId());
            }
            if (details.getTransporteurId() != null) {
                commande.setTransporteurId(details.getTransporteurId());
            }
            if (details.getIdAmie() != null) {
                commande.setIdAmie(details.getIdAmie());
            }
            if (details.getTelArrivee() != null) {
                commande.setTelArrivee(details.getTelArrivee());
            }

            if (details.getLatitudeDepart() != null) {
                commande.setLatitudeDepart(details.getLatitudeDepart());
            }
            if (details.getLongitudeDepart() != null) {
                commande.setLongitudeDepart(details.getLongitudeDepart());
            }
            if (details.getLatitudeDestination() != null) {
                commande.setLatitudeDestination(details.getLatitudeDestination());
            }
            if (details.getLongitudeDestination() != null) {
                commande.setLongitudeDestination(details.getLongitudeDestination());
            }

            if (details.getDistanceKm() != null) {
                commande.setDistanceKm(details.getDistanceKm());
            }

            if (details.getSousZoneDepart() != null) {
                commande.setSousZoneDepart(details.getSousZoneDepart());
            }
            if (details.getSousZoneArrivee() != null) {
                commande.setSousZoneArrivee(details.getSousZoneArrivee());
            }

            if (details.getZonePrincipaleDepart() != null) {
                commande.setZonePrincipaleDepart(details.getZonePrincipaleDepart());
            }
            if (details.getZonePrincipaleArrivee() != null) {
                commande.setZonePrincipaleArrivee(details.getZonePrincipaleArrivee());
            }
            commande.setQrCodeDepartScanne(details.isQrCodeDepartScanne());
            if (details.getDateScanDepart() != null) {
                commande.setDateScanDepart(details.getDateScanDepart());
            }

            commande.setQrCodeReceptionScanne(details.isQrCodeReceptionScanne());
            if (details.getDateScanReception() != null) {
                commande.setDateScanReception(details.getDateScanReception());
            }

            // --- Met a jour la date de modification automatique ---
            commande.setMajLe(LocalDateTime.now());
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }

    public Commande updateCommandePatch(String id, Commande patch) {

    return commandeRepository.findById(id).map(existing -> {

        // Copy only non-null fields from patch → existing
        BeanWrapper srcWrapper = new BeanWrapperImpl(patch);
        BeanWrapper targetWrapper = new BeanWrapperImpl(existing);

        for (var pd : srcWrapper.getPropertyDescriptors()) {
            String field = pd.getName();

            // Skip technical fields that must NOT be patched
            if (field.equals("id") ||
                field.equals("createdAt") ||
                field.equals("majLe")) {
                continue;
            }

            Object newValue = srcWrapper.getPropertyValue(field);
            if (newValue != null) {
                targetWrapper.setPropertyValue(field, newValue);
            }
        }

        // Always update modification date
        existing.setMajLe(LocalDateTime.now());

        return commandeRepository.save(existing);

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
            commande.setDateDemande(LocalDateTime.now()); // �z? date actuelle
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
    // Variante triee :
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
