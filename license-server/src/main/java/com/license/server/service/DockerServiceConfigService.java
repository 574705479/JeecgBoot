package com.license.server.service;

import com.license.server.entity.DockerComposeFile;
import com.license.server.entity.DockerServiceConfig;
import com.license.server.repository.DockerComposeFileRepository;
import com.license.server.repository.DockerServiceConfigRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DockerServiceConfigService {

    private final DockerServiceConfigRepository dockerServiceConfigRepository;
    private final DockerComposeFileRepository dockerComposeFileRepository;

    public Page<DockerServiceConfig> list(int page, int size, Long serverId) {
        return dockerServiceConfigRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (serverId != null) {
                predicates.add(cb.equal(root.get("serverId"), serverId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createTime")));
    }

    public List<DockerServiceConfig> listByServerId(Long serverId) {
        return dockerServiceConfigRepository.findByServerIdAndDelFlagOrderByCreateTimeAsc(serverId, 0);
    }

    public DockerServiceConfig getById(Long id) {
        DockerServiceConfig config = dockerServiceConfigRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Docker服务不存在"));
        if (config.getDelFlag() != null && config.getDelFlag() == 1) {
            throw new IllegalArgumentException("Docker服务不存在");
        }
        return config;
    }

    @Transactional
    public DockerServiceConfig create(DockerServiceConfig config) {
        Optional<DockerServiceConfig> existing = dockerServiceConfigRepository
                .findByServerIdAndServiceName(config.getServerId(), config.getServiceName());
        if (existing.isPresent()) {
            DockerServiceConfig old = existing.get();
            if (old.getDelFlag() != null && old.getDelFlag() == 1) {
                copyFields(config, old);
                old.setDelFlag(0);
                return dockerServiceConfigRepository.save(old);
            }
            throw new IllegalArgumentException("服务名称已存在: " + config.getServiceName());
        }
        config.setId(null);
        config.setDelFlag(0);
        return dockerServiceConfigRepository.save(config);
    }

    @Transactional
    public DockerServiceConfig update(Long id, DockerServiceConfig data) {
        DockerServiceConfig old = getById(id);
        old.setServiceName(data.getServiceName());
        old.setContainerName(data.getContainerName());
        old.setHostname(data.getHostname());
        old.setImageName(data.getImageName());
        old.setCurrentVersion(data.getCurrentVersion());
        old.setTargetVersion(data.getTargetVersion());
        old.setPorts(data.getPorts());
        old.setEnvironment(data.getEnvironment());
        old.setVolumes(data.getVolumes());
        old.setDependsOn(data.getDependsOn());
        old.setNetworks(data.getNetworks());
        old.setRestartPolicy(data.getRestartPolicy());
        old.setCommand(data.getCommand());
        old.setExtraConfig(data.getExtraConfig());
        old.setUseParamsMode(data.getUseParamsMode());
        old.setStatus(data.getStatus());
        return dockerServiceConfigRepository.save(old);
    }

    @Transactional
    public void softDelete(Long id) {
        DockerServiceConfig old = getById(id);
        old.setDelFlag(1);
        dockerServiceConfigRepository.save(old);
    }

    @Transactional
    public void replaceByServerId(Long serverId, List<DockerServiceConfig> configs) {
        // 查出该服务器所有记录（含软删除的），按 serviceName 索引
        List<DockerServiceConfig> allExisting = dockerServiceConfigRepository.findByServerIdOrderByCreateTimeAsc(serverId);
        Map<String, DockerServiceConfig> existingMap = new LinkedHashMap<>();
        for (DockerServiceConfig e : allExisting) {
            existingMap.put(e.getServiceName(), e);
        }

        Set<String> newServiceNames = configs.stream()
                .map(DockerServiceConfig::getServiceName)
                .collect(Collectors.toSet());

        // 对每个新配置：如果已有同名记录则更新复用，否则新建
        List<DockerServiceConfig> toSave = new ArrayList<>();
        for (DockerServiceConfig config : configs) {
            config.setServerId(serverId);
            DockerServiceConfig old = existingMap.get(config.getServiceName());
            if (old != null) {
                copyFields(config, old);
                old.setDelFlag(0);
                toSave.add(old);
            } else {
                config.setId(null);
                config.setDelFlag(0);
                toSave.add(config);
            }
        }

        // 不在新列表中的旧记录做软删除
        for (DockerServiceConfig e : allExisting) {
            if (!newServiceNames.contains(e.getServiceName()) && (e.getDelFlag() == null || e.getDelFlag() == 0)) {
                e.setDelFlag(1);
                toSave.add(e);
            }
        }

        dockerServiceConfigRepository.saveAll(toSave);
    }

    private void copyFields(DockerServiceConfig src, DockerServiceConfig dest) {
        dest.setContainerName(src.getContainerName());
        dest.setHostname(src.getHostname());
        dest.setImageName(src.getImageName());
        dest.setCurrentVersion(src.getCurrentVersion());
        dest.setTargetVersion(src.getTargetVersion());
        dest.setPorts(src.getPorts());
        dest.setEnvironment(src.getEnvironment());
        dest.setVolumes(src.getVolumes());
        dest.setDependsOn(src.getDependsOn());
        dest.setNetworks(src.getNetworks());
        dest.setRestartPolicy(src.getRestartPolicy());
        dest.setCommand(src.getCommand());
        dest.setExtraConfig(src.getExtraConfig());
        dest.setUseParamsMode(src.getUseParamsMode());
    }

    @Transactional
    public DockerComposeFile saveComposeFile(Long serverId, String fileName, String fileContent, String filePath, String version, int parseStatus, String parseError) {
        DockerComposeFile compose = new DockerComposeFile();
        compose.setServerId(serverId);
        compose.setFileName(fileName == null || fileName.isBlank() ? "docker-compose.yml" : fileName);
        compose.setFileContent(fileContent);
        compose.setFilePath(filePath);
        compose.setVersion(version);
        compose.setParseStatus(parseStatus);
        compose.setParseError(parseError);
        return dockerComposeFileRepository.save(compose);
    }

    public DockerComposeFile getLatestComposeByServerId(Long serverId) {
        return dockerComposeFileRepository.findFirstByServerIdOrderByCreateTimeDesc(serverId).orElse(null);
    }
}
