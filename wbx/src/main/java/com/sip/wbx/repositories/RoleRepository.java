package com.sip.wbx.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sip.wbx.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByLibelle(String libelle);
}