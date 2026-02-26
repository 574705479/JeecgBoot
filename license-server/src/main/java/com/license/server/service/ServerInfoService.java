package com.license.server.service;

import com.license.server.entity.ServerInfo;
import com.license.server.repository.ServerInfoRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerInfoService {

    private final ServerInfoRepository serverInfoRepository;

    public Page<ServerInfo> list(int page, int size, String keyword) {
        return serverInfoRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("serverName"), like),
                        cb.like(root.get("ip"), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, Math.min(size, 100), Sort.by(Sort.Direction.ASC, "sortOrder").and(Sort.by(Sort.Direction.DESC, "createTime"))));
    }

    public List<ServerInfo> treeList(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return serverInfoRepository.findByDelFlagOrderBySortOrderAscCreateTimeAsc(0);
        }
        String k = keyword.trim().toLowerCase();
        return serverInfoRepository.findByDelFlagOrderBySortOrderAscCreateTimeAsc(0).stream()
                .filter(item -> (item.getServerName() != null && item.getServerName().toLowerCase().contains(k))
                        || (item.getIp() != null && item.getIp().toLowerCase().contains(k)))
                .toList();
    }

    public ServerInfo getById(Long id) {
        ServerInfo info = serverInfoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("服务器不存在"));
        if (info.getDelFlag() != null && info.getDelFlag() == 1) {
            throw new IllegalArgumentException("服务器不存在");
        }
        return info;
    }

    @Transactional
    public ServerInfo create(ServerInfo data) {
        data.setId(null);
        data.setPublicKey(data.getPublicKey() == null ? 0 : data.getPublicKey());
        data.setDelFlag(0);
        return serverInfoRepository.save(data);
    }

    @Transactional
    public ServerInfo update(Long id, ServerInfo data) {
        ServerInfo old = getById(id);
        old.setParentId(data.getParentId());
        old.setServerName(data.getServerName());
        old.setServerType(data.getServerType());
        old.setConnectionType(data.getConnectionType());
        old.setCloudVendor(data.getCloudVendor());
        old.setIp(data.getIp());
        old.setSshPort(data.getSshPort());
        old.setUsername(data.getUsername());
        old.setPassword(data.getPassword());
        old.setPrivateKey(data.getPrivateKey());
        old.setPrivateKeyPath(data.getPrivateKeyPath());
        old.setPublicKey(data.getPublicKey());
        old.setPublicKeyValue(data.getPublicKeyValue());
        old.setMsUser(data.getMsUser());
        old.setMsPwd(data.getMsPwd());
        old.setMsPort(data.getMsPort());
        old.setDatabaseName(data.getDatabaseName());
        old.setDockerApiPort(data.getDockerApiPort());
        old.setSpLink(data.getSpLink());
        old.setStatus(data.getStatus());
        old.setSortOrder(data.getSortOrder());
        return serverInfoRepository.save(old);
    }

    @Transactional
    public void softDelete(Long id) {
        ServerInfo info = getById(id);
        info.setDelFlag(1);
        serverInfoRepository.save(info);
    }

    @Transactional
    public void softDeleteBatch(List<Long> ids) {
        for (Long id : ids) {
            softDelete(id);
        }
    }
}
