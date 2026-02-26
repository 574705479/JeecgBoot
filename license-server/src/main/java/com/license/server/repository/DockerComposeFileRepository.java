package com.license.server.repository;

import com.license.server.entity.DockerComposeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface DockerComposeFileRepository extends JpaRepository<DockerComposeFile, Long>, JpaSpecificationExecutor<DockerComposeFile> {
    Optional<DockerComposeFile> findFirstByServerIdOrderByCreateTimeDesc(Long serverId);
}
