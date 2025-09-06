package com.transport.transport.repository;

import com.transport.transport.model.Ami;
import com.transport.transport.model.Utilisateur;
import com.transport.transport.model.StatutAmi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AmiRepository extends JpaRepository<Ami, Long> {

    List<Ami> findByDemandeurOrRecepteurAndStatut(Utilisateur demandeur, Utilisateur recepteur, StatutAmi statut);
    @Query("""
        select u
        from Utilisateur u
        where exists (
            select 1
            from Ami a
            where a.statut = com.transport.transport.model.StatutAmi.ACCEPTE
              and (
                 (a.demandeur = u and a.recepteur.id = :userId)
                 or
                 (a.recepteur = u and a.demandeur.id = :userId)
              )
        )
        """)
        List<Utilisateur> findAcceptedFriendsOf(@Param("userId") Long userId);
        boolean existsByDemandeurIdAndRecepteurIdAndStatut(Integer demandeurId, Integer recepteurId, StatutAmi statut);

        // Récupérer TOUTE relation (peu importe le sens)
        @Query("""
      select a from Ami a
      where (a.demandeur.id = :u1 and a.recepteur.id = :u2)
         or (a.demandeur.id = :u2 and a.recepteur.id = :u1)
    """)
    List<Ami> findAnyBetween(@Param("u1") Integer u1, @Param("u2") Integer u2);
    @Query("""
    select a.demandeur
    from Ami a
    where a.recepteur.id = :userId
      and a.statut = :statut
    order by a.id desc
""")
    List<Utilisateur> findPendingInvitationSenders(@Param("userId") Integer userId,
                                                   @Param("statut") StatutAmi statut);
    Optional<Ami> findByDemandeurIdAndRecepteurIdAndStatut(
            Integer demandeurId,
            Integer recepteurId,
            StatutAmi statut
    );

}
