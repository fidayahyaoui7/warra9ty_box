package com.sip.wbx.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sip.wbx.entities.Reclamation;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {
    List<Reclamation> findByAchatUserId(Long userId);
}