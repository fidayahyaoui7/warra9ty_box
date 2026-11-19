package com.sip.wbx.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.sip.wbx.entities.StoredFile;

public interface StoredFileRepository extends JpaRepository<StoredFile, String> {
}
