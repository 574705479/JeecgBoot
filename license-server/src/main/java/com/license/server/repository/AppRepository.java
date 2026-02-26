package com.license.server.repository;

import com.license.server.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppRepository extends JpaRepository<App, Long>, JpaSpecificationExecutor<App> {

    Optional<App> findByAppId(String appId);

    boolean existsByAppId(String appId);

    List<App> findBySecretRotateAtBeforeAndAppSecretOldIsNotNull(LocalDateTime time);
}
