package com.transport.transport.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.transport.transport.model.Demande;
import com.transport.transport.repository.DemandeRepository;

@Service
public class DemandeService {

    private final DemandeRepository demandeRepository;

    public DemandeService(DemandeRepository demandeRepository) {
        this.demandeRepository = demandeRepository;
    }

    public Demande creerDemande(Demande demande) {
        demande.setId(null);
        demande.setDateReponse(null);
        demande.setReponse(false);
        demande.setDateDemande(LocalDateTime.now());
        return demandeRepository.save(demande);
    }

    public Demande accepterDemande(String idDemande) {
        return traiterDemande(idDemande, true);
    }

    public Demande refuserDemande(String idDemande) {
        return traiterDemande(idDemande, false);
    }

    private Demande traiterDemande(String idDemande, boolean decision) {
        Demande demande = demandeRepository.findById(idDemande)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable: " + idDemande));

        if (demande.getDateReponse() != null) {
            throw new IllegalStateException("Demande déjà traitée");
        }

        demande.setReponse(decision);
        demande.setDateReponse(LocalDateTime.now());
        return demandeRepository.save(demande);
    }
}
