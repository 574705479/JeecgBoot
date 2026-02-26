package com.license.server.repository;

import com.license.server.entity.DockerTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DockerTaskRepository extends JpaRepository<DockerTask, Long>, JpaSpecificationExecutor<DockerTask> {
    Optional<DockerTask> findFirstByServerIdAndStatusInOrderByCreateTimeDesc(Long serverId, List<Integer> statuses);

    List<DockerTask> findTop20ByServerIdOrderByCreateTimeDesc(Long serverId);
}
