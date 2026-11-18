package com.sip.wbx.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sip.wbx.entities.Achat;

public interface AchatRepository extends JpaRepository<Achat, Long> {
    List<Achat> findByUserId(Long userId);

    // garanties qui se terminent entre deux dates
    List<Achat> findByDateFinGarantieBetween(LocalDate from, LocalDate to);

    // garanties qui se terminent entre deux dates et dont le rappel n'a pas encore été envoyé
    @Query("select a from Achat a where a.dateFinGarantie between :from and :to "
         + "and (a.rappelEnvoye is null or a.rappelEnvoye = false)")
    List<Achat> findRappelsAEnvoyer(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
