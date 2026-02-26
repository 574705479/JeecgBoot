package com.license.server.service;

import com.license.server.entity.DockerTask;
import com.license.server.repository.DockerTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DockerTaskService {

    private final DockerTaskRepository dockerTaskRepository;

    @Transactional
    public DockerTask createTask(String taskType, Long serverId, List<Long> serviceIds, String serviceNames) {
        DockerTask task = new DockerTask();
        task.setTaskType(taskType);
        task.setServerId(serverId);
        task.setServiceIds(serviceIds);
        task.setServiceNames(serviceNames);
        task.setStatus(0);
        task.setTotalCount(serviceIds == null ? 0 : serviceIds.size());
        task.setSuccessCount(0);
        task.setFailCount(0);
        task.setProgressPercent(0);
        return dockerTaskRepository.save(task);
    }

    @Transactional
    public void updateProgress(Long taskId, String currentService, int successCount, int failCount) {
        DockerTask task = getById(taskId);
        int total = task.getTotalCount() == null ? 0 : task.getTotalCount();
        int progress = total == 0 ? 0 : (int) (((successCount + failCount) * 100.0) / total);
        task.setStatus(1);
        task.setCurrentService(currentService);
        task.setSuccessCount(successCount);
        task.setFailCount(failCount);
        task.setProgressPercent(Math.min(100, Math.max(0, progress)));
        dockerTaskRepository.save(task);
    }

    @Transactional
    public void completeTask(Long taskId, int successCount, int failCount, List<String> resultDetail) {
        DockerTask task = getById(taskId);
        task.setStatus(2);
        task.setSuccessCount(successCount);
        task.setFailCount(failCount);
        task.setProgressPercent(100);
        task.setResultDetail(resultDetail);
        task.setFinishTime(LocalDateTime.now());
        dockerTaskRepository.save(task);
    }

    @Transactional
    public void failTask(Long taskId, String errorMessage) {
        DockerTask task = getById(taskId);
        task.setStatus(3);
        task.setErrorMessage(errorMessage);
        task.setFinishTime(LocalDateTime.now());
        dockerTaskRepository.save(task);
    }

    public DockerTask getById(Long id) {
        return dockerTaskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    }

    public DockerTask getRunningTaskByServerId(Long serverId) {
        return dockerTaskRepository.findFirstByServerIdAndStatusInOrderByCreateTimeDesc(serverId, List.of(0, 1)).orElse(null);
    }

    public List<DockerTask> getRecentTasksByServerId(Long serverId, int limit) {
        List<DockerTask> tasks = dockerTaskRepository.findTop20ByServerIdOrderByCreateTimeDesc(serverId);
        return tasks.stream().limit(Math.max(1, Math.min(limit, 20))).toList();
    }
}
