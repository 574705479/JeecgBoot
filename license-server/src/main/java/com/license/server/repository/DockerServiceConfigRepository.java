package com.license.server.repository;

import com.license.server.entity.DockerServiceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DockerServiceConfigRepository extends JpaRepository<DockerServiceConfig, Long>, JpaSpecificationExecutor<DockerServiceConfig> {
    List<DockerServiceConfig> findByServerIdAndDelFlagOrderByCreateTimeAsc(Long serverId, Integer delFlag);

    List<DockerServiceConfig> findByServerIdOrderByCreateTimeAsc(Long serverId);

    Optional<DockerServiceConfig> findByServerIdAndServiceName(Long serverId, String serviceName);
}
