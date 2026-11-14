package com.sip.wbx.repositories;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sip.wbx.entities.Achat;

public interface AchatRepository extends JpaRepository<Achat, Long> {
    List<Achat> findByUserId(Long userId);

    // garanties qui se terminent entre deux dates (rappel quotidien)
    List<Achat> findByDateFinGarantieBetween(LocalDate from, LocalDate to);
}
