package com.license.server.repository;

import com.license.server.entity.ServerInfoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ServerInfoLogRepository extends JpaRepository<ServerInfoLog, Long>, JpaSpecificationExecutor<ServerInfoLog> {
}
