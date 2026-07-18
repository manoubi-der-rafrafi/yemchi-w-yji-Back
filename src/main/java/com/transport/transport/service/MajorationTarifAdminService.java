package com.transport.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.transport.transport.model.MajorationTarif;
import com.transport.transport.repository.MajorationTarifRepository;

@Service
public class MajorationTarifAdminService {
    private final MajorationTarifRepository repository;

    public MajorationTarifAdminService(MajorationTarifRepository repository) {
        this.repository = repository;
    }

    public List<MajorationTarif> list() {
        return repository.findAllByOrderByDateDebutDesc();
    }

    public MajorationTarif create(MajorationTarif majoration, String adminId) {
        if (majoration == null || majoration.getPourcentageAjout() == null
                || majoration.getPourcentageAjout().signum() < 0
                || majoration.getDateDebut() == null || majoration.getDateFin() == null
                || !majoration.getDateFin().isAfter(majoration.getDateDebut())
                || majoration.getDescriptionCause() == null || majoration.getDescriptionCause().isBlank()) {
            throw new IllegalArgumentException("Majoration invalide");
        }
        if (!repository.findOverlapping(majoration.getDateDebut(), majoration.getDateFin()).isEmpty()) {
            throw new IllegalArgumentException("Une majoration existe deja sur cette periode");
        }
        majoration.setId(null);
        majoration.setCreatedByAdminId(adminId);
        return repository.save(majoration);
    }
}
