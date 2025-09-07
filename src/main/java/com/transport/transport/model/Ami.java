package com.transport.transport.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ami")
public class Ami {

    @Id
    private String id;  // Mongo uses String/ObjectId

    // store only the IDs of users instead of @ManyToOne
    private String demandeurId;
    private String recepteurId;

    private StatutAmi statut = StatutAmi.EN_ATTENTE;

    private LocalDateTime creeLe = LocalDateTime.now();
    private LocalDateTime majLe;

    // --- Getters & Setters ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDemandeurId() { return demandeurId; }
    public void setDemandeurId(String demandeurId) { this.demandeurId = demandeurId; }

    public String getRecepteurId() { return recepteurId; }
    public void setRecepteurId(String recepteurId) { this.recepteurId = recepteurId; }

    public StatutAmi getStatut() { return statut; }
    public void setStatut(StatutAmi statut) { this.statut = statut; }

    public LocalDateTime getCreeLe() { return creeLe; }
    public void setCreeLe(LocalDateTime creeLe) { this.creeLe = creeLe; }

    public LocalDateTime getMajLe() { return majLe; }
    public void setMajLe(LocalDateTime majLe) { this.majLe = majLe; }
}
