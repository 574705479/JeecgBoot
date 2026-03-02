<template>
  <div class="task-manager">
    <div class="task-header">
      <span class="title">任务管理器</span>
      <a-badge :count="runningTaskCount" :overflow-count="99">
        <span class="badge-text">运行中</span>
      </a-badge>
    </div>

    <div class="task-list">
      <div v-for="task in runningTasks" :key="task.id" class="task-item running">
        <div class="task-title">
          <LoadingOutlined class="task-icon running-icon" />
          <span class="task-type">{{ getTaskTypeName(task.taskType) }}</span>
          <a-tag color="processing">执行中</a-tag>
        </div>
        <div class="task-info">
          <div class="service-names">
            <template v-if="task.serviceNames">
              <a-tag v-for="name in getVisibleNames(task)" :key="name" size="small">{{ name }}</a-tag>
              <a-tag v-if="getHiddenCount(task) > 0 && !expandedTasks.has(task.id)"
                color="blue" size="small" style="cursor: pointer" @click="expandedTasks.add(task.id)">
                +{{ getHiddenCount(task) }}
              </a-tag>
              <a-tag v-if="expandedTasks.has(task.id) && parseServiceNames(task.serviceNames).length > 3"
                size="small" style="cursor: pointer" @click="expandedTasks.delete(task.id)">
                收起
              </a-tag>
            </template>
            <span v-else class="placeholder-text">处理中...</span>
          </div>
          <div class="current-service" v-if="task.currentService">
            正在执行: {{ task.currentService }}
          </div>
          <a-progress
            :percent="task.progressPercent || 0"
            :status="'active'"
            :show-info="true"
          />
          <div class="task-stats">
            成功: {{ task.successCount || 0 }} / 失败: {{ task.failCount || 0 }} / 总数: {{ task.totalCount || 0 }}
          </div>
        </div>
      </div>

      <div
        v-for="task in recentTasks"
        :key="task.id"
        :ref="el => setTaskRef(task.id, el)"
        class="task-item"
        :class="[getTaskStatusClass(task.status), { 'highlight-new-task': task.id === highlightTaskId }]"
      >
        <div class="task-title">
          <component :is="getTaskIcon(task.status)" class="task-icon" :class="getTaskIconClass(task.status)" />
          <span class="task-type">{{ getTaskTypeName(task.taskType) }}</span>
          <a-tag :color="getTaskStatusColor(task.status)">{{ getTaskStatusText(task.status) }}</a-tag>
        </div>
        <div class="task-info">
          <div class="service-names">
            <template v-if="task.serviceNames">
              <a-tag v-for="name in getVisibleNames(task)" :key="name" size="small">{{ name }}</a-tag>
              <a-tag v-if="getHiddenCount(task) > 0 && !expandedTasks.has(task.id)"
                color="blue" size="small" style="cursor: pointer" @click="expandedTasks.add(task.id)">
                +{{ getHiddenCount(task) }}
              </a-tag>
              <a-tag v-if="expandedTasks.has(task.id) && parseServiceNames(task.serviceNames).length > 3"
                size="small" style="cursor: pointer" @click="expandedTasks.delete(task.id)">
                收起
              </a-tag>
            </template>
            <span v-else class="placeholder-text">无服务信息</span>
          </div>
          <div class="task-stats" :class="{ 'has-error': task.status === 3 || task.failCount > 0 }">
            成功: <span class="success-count">{{ task.successCount || 0 }}</span> /
            失败: <span class="fail-count" :class="{ 'highlight-error': task.failCount > 0 }">{{ task.failCount || 0 }}</span> /
            总数: {{ task.totalCount || 0 }}
          </div>
          <div class="task-time">
            {{ formatTime(task.createTime) }}
            <span v-if="task.finishTime"> - {{ formatTime(task.finishTime) }}</span>
          </div>
          <div v-if="task.failCount > 0 && task.resultDetail" class="error-detail">
            <div class="error-header">
              <CloseCircleOutlined style="margin-right: 4px;" />
              失败详情 ({{ task.failCount }} 个失败)
            </div>
            <div class="error-content">
              {{ parseErrorDetail(task.resultDetail) }}
            </div>
          </div>
          <div v-else-if="task.errorMessage" class="error-message">
            <CloseCircleOutlined style="margin-right: 4px;" />
            {{ task.errorMessage }}
          </div>
          <div v-else-if="task.failCount > 0" class="error-message">
            <CloseCircleOutlined style="margin-right: 4px;" />
            执行失败，但未获取到详细错误信息。请查看服务器日志。
          </div>
        </div>
      </div>

      <a-empty v-if="runningTasks.length === 0 && recentTasks.length === 0" description="暂无任务" :image="Empty.PRESENTED_IMAGE_SIMPLE" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { Empty } from 'ant-design-vue'
import { LoadingOutlined, CheckCircleOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons-vue'
import { getRecentDockerTasks } from '../../../api/server'
import dayjs from 'dayjs'

const props = defineProps({
  serverId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits(['taskComplete'])

const runningTasks = ref<any[]>([])
const recentTasks = ref<any[]>([])
const pollingInterval = ref<any>(null)
const highlightTaskId = ref<number | null>(null)
const taskRefs = ref<Map<number, any>>(new Map())
const expandedTasks = reactive(new Set<number>())
const pendingTimers: number[] = []

const runningTaskCount = computed(() => runningTasks.value.length)

const parseServiceNames = (names: string): string[] => {
  if (!names) return []
  return names.split(',').map(s => s.trim()).filter(Boolean)
}

const COLLAPSE_THRESHOLD = 3

const getVisibleNames = (task: any): string[] => {
  const all = parseServiceNames(task.serviceNames)
  if (expandedTasks.has(task.id) || all.length <= COLLAPSE_THRESHOLD) return all
  return all.slice(0, COLLAPSE_THRESHOLD)
}

const getHiddenCount = (task: any): number => {
  const all = parseServiceNames(task.serviceNames)
  if (expandedTasks.has(task.id) || all.length <= COLLAPSE_THRESHOLD) return 0
  return all.length - COLLAPSE_THRESHOLD
}

const setTaskRef = (taskId: number, el: any) => {
  if (el) {
    taskRefs.value.set(taskId, el)
  } else {
    taskRefs.value.delete(taskId)
  }
}

const scrollToTask = (taskId: number) => {
  highlightTaskId.value = taskId
  const t1 = window.setTimeout(() => {
    const taskEl = taskRefs.value.get(taskId)
    if (taskEl) {
      taskEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }
  }, 300)
  const t2 = window.setTimeout(() => {
    highlightTaskId.value = null
  }, 3000)
  pendingTimers.push(t1, t2)
}

const getTaskTypeName = (taskType: string) => {
  const typeMap: Record<string, string> = {
    start: '启动服务',
    stop: '停止服务',
    restart: '重启服务',
    update: '更新镜像',
    batch_update: '批量更新',
  }
  return typeMap[taskType] || taskType
}

const getTaskStatusText = (status: number) => {
  const statusMap: Record<number, string> = { 0: '待处理', 1: '处理中', 2: '已完成', 3: '失败' }
  return statusMap[status] || '未知'
}

const getTaskStatusColor = (status: number) => {
  const colorMap: Record<number, string> = { 0: 'default', 1: 'processing', 2: 'success', 3: 'error' }
  return colorMap[status] || 'default'
}

const getTaskIcon = (status: number) => {
  const iconMap: Record<number, any> = { 0: ClockCircleOutlined, 1: LoadingOutlined, 2: CheckCircleOutlined, 3: CloseCircleOutlined }
  return iconMap[status] || ClockCircleOutlined
}

const getTaskIconClass = (status: number) => {
  const classMap: Record<number, string> = { 0: 'pending-icon', 1: 'running-icon', 2: 'success-icon', 3: 'error-icon' }
  return classMap[status] || ''
}

const getTaskStatusClass = (status: number) => {
  const classMap: Record<number, string> = { 0: 'pending', 1: 'running', 2: 'completed', 3: 'failed' }
  return classMap[status] || ''
}

const formatTime = (time: string) => {
  if (!time) return ''
  return dayjs(time).format('HH:mm:ss')
}

const parseErrorDetail = (resultDetail: string) => {
  if (!resultDetail) return '无详细信息'
  try {
    const details = JSON.parse(resultDetail)
    if (Array.isArray(details)) {
      const failures = details.filter((item: string) =>
        item.includes('失败') || item.includes('错误') || item.includes('异常') ||
        item.includes('操作失败') || item.includes('command not found')
      )
      const displayItems = failures.length > 0 ? failures : details
      return displayItems.map((item: string) => {
        if (item.includes(':')) {
          const parts = item.split(':')
          const serviceName = parts[0]!.trim()
          const errorMsg = parts.slice(1).join(':').trim()
          return `• ${serviceName}\n  └─ ${errorMsg}`
        }
        return `• ${item}`
      }).join('\n\n')
    }
    return resultDetail
  } catch {
    return resultDetail
  }
}

const POLL_ACTIVE = 3000
const POLL_IDLE = 15000

const loadTasks = async () => {
  try {
    const res = await getRecentDockerTasks(props.serverId, 20)
    const allTasks = res.data.code === 200 ? (res.data.data || []) : []

    const oldRunningCount = runningTasks.value.length
    runningTasks.value = allTasks.filter((task: any) => task.status === 0 || task.status === 1)
    recentTasks.value = allTasks.filter((task: any) => task.status === 2 || task.status === 3).slice(0, 5)

    if (oldRunningCount > 0 && runningTasks.value.length < oldRunningCount) {
      emit('taskComplete')
    }

    reschedulePolling()
  } catch (error: any) {
    console.error('加载任务失败', error)
  }
}

const reschedulePolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
  }
  const interval = runningTasks.value.length > 0 ? POLL_ACTIVE : POLL_IDLE
  pollingInterval.value = setInterval(() => {
    loadTasks()
  }, interval)
}

const startPolling = () => {
  loadTasks()
}

const stopPolling = () => {
  if (pollingInterval.value) {
    clearInterval(pollingInterval.value)
    pollingInterval.value = null
  }
}

onMounted(() => {
  startPolling()
})

onUnmounted(() => {
  stopPolling()
  pendingTimers.forEach(t => clearTimeout(t))
  pendingTimers.length = 0
})

defineExpose({
  refresh: loadTasks,
  scrollToTask,
})
</script>

<style lang="less" scoped>
.task-manager {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-left: 1px solid #e8e8e8;

  .task-header {
    padding: 16px;
    background: #fff;
    border-bottom: 1px solid #e8e8e8;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .title {
      font-size: 16px;
      font-weight: 500;
    }

    .badge-text {
      font-size: 12px;
      color: #666;
    }
  }

  .task-list {
    flex: 1;
    overflow-y: auto;
    padding: 12px;

    .task-item {
      background: #fff;
      border-radius: 4px;
      padding: 12px;
      margin-bottom: 12px;
      border: 1px solid #e8e8e8;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      &.running {
        border-color: #1890ff;
        background: #e6f7ff;
      }

      &.completed {
        border-color: #52c41a;
      }

      &.failed {
        border-color: #ff4d4f;
        background: #fff1f0;
        border-width: 2px;
        box-shadow: 0 0 8px rgba(255, 77, 79, 0.15);
      }

      &.highlight-new-task {
        animation: highlight-pulse 2s ease-in-out;
        border-color: #1890ff;
        box-shadow: 0 0 12px rgba(24, 144, 255, 0.4);
      }

      @keyframes highlight-pulse {
        0%, 100% { background: #ffffff; }
        50% { background: #e6f7ff; }
      }

      .task-title {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        .task-icon {
          font-size: 16px;
          &.running-icon { color: #1890ff; }
          &.success-icon { color: #52c41a; }
          &.error-icon { color: #ff4d4f; }
          &.pending-icon { color: #faad14; }
        }

        .task-type {
          font-weight: 500;
          flex: 1;
        }
      }

      .task-info {
        .service-names {
          display: flex;
          flex-wrap: wrap;
          gap: 4px;
          margin-bottom: 4px;

          .ant-tag {
            margin: 0;
            font-size: 11px;
          }

          .placeholder-text {
            font-size: 12px;
            color: #999;
          }
        }

        .current-service {
          font-size: 12px;
          color: #1890ff;
          margin-bottom: 8px;
        }

        .task-stats {
          font-size: 12px;
          color: #999;
          margin-top: 8px;

          &.has-error { color: #666; font-weight: 500; }
          .success-count { color: #52c41a; font-weight: 500; }
          .fail-count {
            &.highlight-error { color: #ff4d4f; font-weight: 700; font-size: 14px; }
          }
        }

        .task-time {
          font-size: 12px;
          color: #999;
          margin-top: 4px;
        }

        .error-detail {
          margin-top: 8px;
          padding: 8px;
          background: #fff;
          border: 1px solid #ffccc7;
          border-left: 3px solid #ff4d4f;
          border-radius: 2px;

          .error-header {
            font-size: 12px;
            color: #ff4d4f;
            font-weight: 600;
            margin-bottom: 6px;
            display: flex;
            align-items: center;
          }

          .error-content {
            font-size: 12px;
            color: #333;
            line-height: 1.6;
            white-space: pre-line;
            max-height: 150px;
            overflow-y: auto;
            padding: 4px;
            background: #fafafa;
            border-radius: 2px;
          }
        }

        .error-message {
          font-size: 12px;
          color: #ff4d4f;
          margin-top: 8px;
          padding: 6px 8px;
          background: #fff2f0;
          border: 1px solid #ffccc7;
          border-left: 3px solid #ff4d4f;
          border-radius: 2px;
          display: flex;
          align-items: flex-start;
          line-height: 1.5;
        }
      }
    }
  }
}
</style>
