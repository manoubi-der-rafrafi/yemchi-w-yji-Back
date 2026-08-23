package com.transport.transport.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StatutFinancierLivreurResponse(
        boolean configurationActive,
        boolean bloque,
        BigDecimal detteNette,
        BigDecimal detteProduits,
        BigDecimal detteCourses,
        BigDecimal creditCoursesEnLigne,
        BigDecimal paiementsLivreurAcceptes,
        BigDecimal versementsEntrepriseAcceptes,
        BigDecimal montantBlocage,
        BigDecimal pourcentageReglement,
        BigDecimal paiementMinimum,
        BigDecimal paiementRestant,
        BigDecimal cibleDette,
        LocalDateTime bloqueLe,
        String message) {
}
