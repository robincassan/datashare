package com.datashare;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, String> {

    List<FileEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<FileEntity> findByExpiresAtBefore(LocalDateTime dateTime);

    Optional<FileEntity> findByDownloadToken(String downloadToken);

    Optional<FileEntity> findByIdAndUserId(String id, String userId);
}
