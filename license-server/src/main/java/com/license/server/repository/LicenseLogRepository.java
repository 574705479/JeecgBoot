package com.license.server.repository;

import com.license.server.entity.LicenseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface LicenseLogRepository extends JpaRepository<LicenseLog, Long>, JpaSpecificationExecutor<LicenseLog> {

    @Modifying
    @Query("DELETE FROM LicenseLog l WHERE l.createTime < :before")
    int deleteOlderThan(LocalDateTime before);
}
