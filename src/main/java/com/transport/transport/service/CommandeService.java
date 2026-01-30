package com.transport.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.transport.transport.model.Commande;
import com.transport.transport.model.Commande.Statut;
import com.transport.transport.model.TypeVehicule;
import com.transport.transport.repository.CommandeRepository;
import com.transport.transport.repository.UtilisateurRepository;

@Service
public class CommandeService {

    private static final Logger logger = LoggerFactory.getLogger(CommandeService.class);

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
        logger.info("updateCommande id={} vehicule={}", id, details.getVehicule());
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
                    commande.setDateDemande(LocalDateTime.now());
                }
                commande.setStatut(details.getStatut());
            }
            if (details.getPrix() != null) {
                commande.setPrix(details.getPrix());
            }
            if (details.getPoids() != null) {
                commande.setPoids(details.getPoids());
            }
            if (details.getVolume() != null) {
                commande.setVolume(details.getVolume());
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
            if (details.getVehicule() != null) {
                commande.setVehicule(details.getVehicule());
            }
            // --- Met a jour la date de modification automatique ---
            commande.setMajLe(LocalDateTime.now());
            return commandeRepository.save(commande);
        }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
    }

    public Commande updateCommandePatch(String id, Commande patch) {

    logger.info("updateCommandePatch id={} vehicule={}", id, patch.getVehicule());
    return commandeRepository.findById(id).map(new Function<Commande, Commande>() {
        @Override
        public Commande apply(Commande existing) {
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
        }
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
public List<Commande> getCommandesByZonePrincipaleAndVehicule(
        Commande.Zone zone,
        TypeVehicule vehicule) {
    if (zone == null) {
        throw new IllegalArgumentException("La zone est obligatoire");
    }
    if (vehicule == null) {
        throw new IllegalArgumentException("Le type de vehicule est obligatoire");
    }
    return commandeRepository.findByZonePrincipaleDepartAndZonePrincipaleArriveeAndVehicule(
            zone, zone, vehicule);
}
public Commande assignerTransporteur(String idCommande, String idTransporteur) {
    return commandeRepository.findById(idCommande).map(commande -> {
        if (commande.getTransporteurId() != null) {
            throw new IllegalStateException("Commande deja assignee a un transporteur");
        }
        commande.setTransporteurId(idTransporteur);
        commande.setMajLe(LocalDateTime.now());
        commande.setStatut(Statut.en_route);
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}
public List<Commande> getCommandesByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdOrderByDateDemandeDesc(idTransporteur);
}
public List<Commande> getCommandesLivreesByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdAndStatutOrderByDateDemandeDesc(
            idTransporteur, Commande.Statut.livree);
}
public BigDecimal getSommePrixCommandesLivreesByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatut(
            idTransporteur, Commande.Statut.livree);
    BigDecimal total = BigDecimal.ZERO;
    for (Commande commande : commandes) {
        if (commande.getPrix() != null) {
            total = total.add(commande.getPrix());
        }
    }
    return total;
}
public BigDecimal getSommePrixCommandesLivreesEnLigneByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatutAndModePaiement(
            idTransporteur, Commande.Statut.livree, Commande.ModePaiement.EN_LIGNE);
    BigDecimal total = BigDecimal.ZERO;
    for (Commande commande : commandes) {
        if (commande.getPrix() != null) {
            total = total.add(commande.getPrix());
        }
    }
    return total;
}
public BigDecimal getSommePrixCommandesLivreesHorsLigneByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatutAndModePaiementNot(
            idTransporteur, Commande.Statut.livree, Commande.ModePaiement.EN_LIGNE);
    BigDecimal total = BigDecimal.ZERO;
    for (Commande commande : commandes) {
        if (commande.getPrix() != null) {
            total = total.add(commande.getPrix());
        }
    }
    return total;
}
public List<Commande> getCommandesEnLigneByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdAndModePaiementOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE);
}
public List<Commande> getCommandesHorsLigneByTransporteur(String idTransporteur) {
    return commandeRepository.findByTransporteurIdAndModePaiementNotOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE);
}
public List<Commande> getCommandesEnLigneByTransporteur(String idTransporteur, Commande.Statut statut) {
    if (statut == null) {
        return getCommandesEnLigneByTransporteur(idTransporteur);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementAndStatutOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE, statut);
}
public List<Commande> getCommandesHorsLigneByTransporteur(String idTransporteur, Commande.Statut statut) {
    if (statut == null) {
        return getCommandesHorsLigneByTransporteur(idTransporteur);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementNotAndStatutOrderByDateDemandeDesc(
            idTransporteur, Commande.ModePaiement.EN_LIGNE, statut);
}
public List<Commande> getCommandesEnLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone) {
    return commandeRepository.findByTransporteurIdAndModePaiementAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public List<Commande> getCommandesHorsLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone) {
    return commandeRepository.findByTransporteurIdAndModePaiementNotAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public List<Commande> getCommandesEnLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone,
        Commande.Statut statut) {
    if (statut == null) {
        return getCommandesEnLigneByTransporteurAndSousZone(idTransporteur, sousZone);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementAndStatutAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            statut,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public List<Commande> getCommandesHorsLigneByTransporteurAndSousZone(
        String idTransporteur,
        Commande.SousZone sousZone,
        Commande.Statut statut) {
    if (statut == null) {
        return getCommandesHorsLigneByTransporteurAndSousZone(idTransporteur, sousZone);
    }
    return commandeRepository.findByTransporteurIdAndModePaiementNotAndStatutAndSousZone(
            idTransporteur,
            Commande.ModePaiement.EN_LIGNE,
            statut,
            sousZone,
            Sort.by(Sort.Direction.DESC, "dateDemande"));
}
public Map<String, Float> getPourcentageRevenuParSousZoneLivreeByTransporteur(String idTransporteur) {
    List<Commande> commandes = commandeRepository.findByTransporteurIdAndStatut(
            idTransporteur, Commande.Statut.livree);
    Map<Commande.SousZone, BigDecimal> totalParSousZone = new EnumMap<>(Commande.SousZone.class);
    BigDecimal total = BigDecimal.ZERO;

    for (Commande commande : commandes) {
        if (commande.getPrix() == null) {
            continue;
        }
        Commande.SousZone sousZone = commande.getSousZoneArrivee();
        if (sousZone == null) {
            continue;
        }
        totalParSousZone.merge(sousZone, commande.getPrix(), BigDecimal::add);
        total = total.add(commande.getPrix());
    }

    Map<String, Float> resultats = new LinkedHashMap<>();
    if (total.compareTo(BigDecimal.ZERO) == 0) {
        return resultats;
    }
    for (Map.Entry<Commande.SousZone, BigDecimal> entry : totalParSousZone.entrySet()) {
        BigDecimal pourcentage = entry.getValue()
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 2, RoundingMode.HALF_UP);
        resultats.put(entry.getKey().name(), pourcentage.floatValue());
    }
    return resultats;
}
public List<Commande> getCommandesBySousZones(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee) {
    if (sousZonesDepart == null || sousZonesArrivee == null || sousZonesDepart.isEmpty() || sousZonesArrivee.isEmpty()) {
        throw new IllegalArgumentException("Les listes de sous-zones doivent etre renseignees");
    }
    return commandeRepository.findBySousZoneDepartInAndSousZoneArriveeIn(sousZonesDepart, sousZonesArrivee);
}
public List<Commande> getCommandesBySousZonesAndVehicule(
        List<Commande.SousZone> sousZonesDepart,
        List<Commande.SousZone> sousZonesArrivee,
        TypeVehicule vehicule) {
    if (sousZonesDepart == null || sousZonesArrivee == null || sousZonesDepart.isEmpty() || sousZonesArrivee.isEmpty()) {
        throw new IllegalArgumentException("Les listes de sous-zones doivent etre renseignees");
    }
    if (vehicule == null) {
        throw new IllegalArgumentException("Le type de vehicule est obligatoire");
    }
    return commandeRepository.findBySousZoneDepartInAndSousZoneArriveeInAndVehiculeAndStatut(
            sousZonesDepart, sousZonesArrivee, vehicule, Commande.Statut.confirmer);
}

public Commande marquerDepartScanne(String id) {
    return commandeRepository.findById(id).map(commande -> {
        commande.marquerDepartScanne();
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}

public Commande marquerReceptionScanne(String id) {
    return commandeRepository.findById(id).map(commande -> {
        commande.marquerReceptionScanne();
        commande.setStatut(Statut.livree);
        commande.setMajLe(LocalDateTime.now());
        return commandeRepository.save(commande);
    }).orElseThrow(() -> new IllegalArgumentException("Commande introuvable"));
}


}
