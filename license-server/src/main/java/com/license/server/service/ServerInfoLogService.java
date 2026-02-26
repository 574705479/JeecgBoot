package com.license.server.service;

import com.license.server.entity.ServerInfoLog;
import com.license.server.repository.ServerInfoLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerInfoLogService {

    private final ServerInfoLogRepository serverInfoLogRepository;

    public Page<ServerInfoLog> list(int page, int size, String keyword, Integer status) {
        return serverInfoLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("server"), like),
                        cb.like(root.get("task"), like),
                        cb.like(root.get("log"), like)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createTime")));
    }

    @Transactional
    public void createLog(Long serverId, String server, String task, String content, boolean success, LocalDateTime startAt, Long operatorId) {
        ServerInfoLog log = new ServerInfoLog();
        log.setServerId(serverId);
        log.setServer(server);
        log.setTask(task);
        log.setLog(content);
        log.setStatus(success ? 1 : 0);
        log.setStartTime(startAt);
        log.setEndTime(LocalDateTime.now());
        log.setDuration(Duration.between(startAt, log.getEndTime()).toMillis() + " ms");
        log.setOperatorId(operatorId);
        serverInfoLogRepository.save(log);
    }

    @Transactional
    public ServerInfoLog create(ServerInfoLog log) {
        return serverInfoLogRepository.save(log);
    }

    public ServerInfoLog getById(Long id) {
        return serverInfoLogRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("日志不存在"));
    }

    @Transactional
    public void deleteById(Long id) {
        serverInfoLogRepository.deleteById(id);
    }
}
