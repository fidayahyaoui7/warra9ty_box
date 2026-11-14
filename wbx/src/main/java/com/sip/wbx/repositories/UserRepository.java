package com.sip.wbx.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sip.wbx.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}