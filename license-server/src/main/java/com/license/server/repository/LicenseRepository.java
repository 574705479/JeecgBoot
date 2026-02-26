package com.license.server.repository;

import com.license.server.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long>, JpaSpecificationExecutor<License> {

    Optional<License> findByLicenseKeyAndDelFlag(String licenseKey, Integer delFlag);

    long countByAppPkAndStatusAndDelFlag(Long appPk, String status, Integer delFlag);

    long countByCustomerIdAndStatusInAndDelFlag(Long customerId, List<String> statuses, Integer delFlag);

    @Modifying
    @Query("UPDATE License l SET l.status = 'EXPIRED', l.updateTime = CURRENT_TIMESTAMP WHERE l.expireDate < :now AND l.status = 'ACTIVE' AND l.delFlag = 0")
    int expireActiveLicenses(LocalDateTime now);

    @Query("SELECT l FROM License l WHERE l.expireDate BETWEEN :start AND :end AND l.status = 'ACTIVE' AND l.delFlag = 0")
    List<License> findExpiringLicenses(LocalDateTime start, LocalDateTime end);

    @Query("SELECT l FROM License l WHERE l.status = 'ACTIVE' AND l.lastHeartbeat < :cutoff AND l.delFlag = 0")
    List<License> findHeartbeatLostLicenses(LocalDateTime cutoff);

    long countByAppPkAndDelFlag(Long appPk, Integer delFlag);

    @Query("SELECT l.status, COUNT(l) FROM License l WHERE l.appPk = :appPk AND l.delFlag = 0 GROUP BY l.status")
    List<Object[]> countByAppPkGroupByStatus(Long appPk);

    @Query("SELECT l.status, COUNT(l) FROM License l WHERE l.delFlag = 0 GROUP BY l.status")
    List<Object[]> countGroupByStatus();
}
