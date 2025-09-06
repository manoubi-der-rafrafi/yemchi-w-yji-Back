package com.transport.transport.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ami")
public class Ami {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_demandeur", nullable = false)
    private Utilisateur demandeur;

    @ManyToOne
    @JoinColumn(name = "utilisateur_recepteur", nullable = false)
    private Utilisateur recepteur;

    @Enumerated(EnumType.STRING)
    private StatutAmi statut = StatutAmi.EN_ATTENTE;

    @Column(name = "cree_le", updatable = false, insertable = false)
    private LocalDateTime creeLe;

    @Column(name = "maj_le", insertable = false)
    private LocalDateTime majLe;

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utilisateur getDemandeur() { return demandeur; }
    public void setDemandeur(Utilisateur demandeur) { this.demandeur = demandeur; }

    public Utilisateur getRecepteur() { return recepteur; }
    public void setRecepteur(Utilisateur recepteur) { this.recepteur = recepteur; }

    public StatutAmi getStatut() { return statut; }
    public void setStatut(StatutAmi statut) { this.statut = statut; }
}
