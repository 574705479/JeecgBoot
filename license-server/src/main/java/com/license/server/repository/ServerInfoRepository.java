package com.license.server.repository;

import com.license.server.entity.ServerInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ServerInfoRepository extends JpaRepository<ServerInfo, Long>, JpaSpecificationExecutor<ServerInfo> {
    List<ServerInfo> findByDelFlagOrderBySortOrderAscCreateTimeAsc(Integer delFlag);
}
