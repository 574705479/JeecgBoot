<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px; gap: 12px">
      <a-space>
        <a-select v-model:value="serverId" style="width: 280px" show-search placeholder="选择服务器" @change="onServerChange">
          <a-select-option v-for="item in serverOptions" :key="item.id" :value="item.id">
            {{ item.serverName }} ({{ item.ip }})
          </a-select-option>
        </a-select>
        <a-button @click="handleRefresh" :disabled="!serverId" :loading="loading">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
        <a-button @click="syncStatusAction" :disabled="!serverId" :loading="syncing">
          <template #icon><SyncOutlined /></template>
          同步状态
        </a-button>
      </a-space>
      <a-space>
        <a-button @click="composeUploadOpen = true" :disabled="!serverId">
          <template #icon><UploadOutlined /></template>
          上传Compose文件
        </a-button>
        <a-button @click="exportComposeAction" :disabled="!serverId || list.length === 0">
          <template #icon><DownloadOutlined /></template>
          导出Compose文件
        </a-button>
        <a-button type="primary" @click="addServiceOpen = true" :disabled="!serverId" ghost>
          <template #icon><PlusOutlined /></template>
          添加服务
        </a-button>
        <a-button type="primary" :disabled="!serverId || selectedRowKeys.length === 0" @click="batchUpdateAction">
          <template #icon><CloudUploadOutlined /></template>
          批量更新
        </a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :span="16" style="min-width: 0">
        <a-empty
          v-if="!loading && list.length === 0"
          description="暂无Docker服务配置"
        >
          <template #description>
            <p>暂无Docker服务配置</p>
            <p style="color: #999; font-size: 12px;">
              请点击"上传Compose文件"按钮，上传 docker-compose.yml 文件来添加服务配置
            </p>
          </template>
          <a-button type="primary" @click="composeUploadOpen = true" :disabled="!serverId">
            <template #icon><UploadOutlined /></template>
            上传Compose文件
          </a-button>
        </a-empty>

        <a-table
          v-else
          :columns="columns"
          :data-source="list"
          :loading="loading"
          :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
          row-key="id"
          :pagination="false"
          :scroll="{ x: 1100 }"
          bordered
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'imageFull'">
              {{ record.imageName }}:{{ record.currentVersion }}
            </template>
            <template v-else-if="column.key === 'targetVersion'">
              <a-input v-model:value="record.targetVersion" style="width: 100%" size="small" @blur="changeVersion(record)" />
            </template>
            <template v-else-if="column.key === 'useParamsMode'">
              <a-tag :color="record.useParamsMode === 1 ? 'blue' : 'default'">
                {{ record.useParamsMode === 1 ? '开启' : '关闭' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'green' : record.status === 0 ? 'red' : 'orange'">
                {{ statusMap[record.status] || '未知' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space :size="4">
                <a-tooltip title="详细配置">
                  <a-button type="link" size="small" @click="openDetailModal(record)" style="padding: 0 4px;">
                    <SettingOutlined style="font-size: 16px;" />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="启动">
                  <a-button type="link" size="small" @click="runCommand(record, 'start')" style="padding: 0 4px;">
                    <PlayCircleOutlined style="font-size: 16px;" />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="停止">
                  <a-button type="link" size="small" danger @click="runCommand(record, 'stop')" style="padding: 0 4px;">
                    <PauseCircleOutlined style="font-size: 16px;" />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="重启">
                  <a-button type="link" size="small" @click="runCommand(record, 'restart')" style="padding: 0 4px;">
                    <ReloadOutlined style="font-size: 16px;" />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="更新镜像">
                  <a-button type="link" size="small" @click="runCommand(record, 'update')" style="padding: 0 4px;">
                    <CloudUploadOutlined style="font-size: 16px;" />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="查看日志">
                  <a-button type="link" size="small" @click="runCommand(record, 'logs')" style="padding: 0 4px;">
                    <FileTextOutlined style="font-size: 16px;" />
                  </a-button>
                </a-tooltip>
                <a-tooltip title="移除服务">
                  <a-button type="link" size="small" danger @click="handleRemoveService(record)" style="padding: 0 4px;">
                    <DeleteOutlined style="font-size: 16px;" />
                  </a-button>
                </a-tooltip>
              </a-space>
            </template>
          </template>
        </a-table>
      </a-col>
      <a-col :span="8">
        <DockerTaskManager
          v-if="serverId"
          ref="taskManagerRef"
          :server-id="serverId"
          @taskComplete="handleTaskComplete"
        />
      </a-col>
    </a-row>

    <!-- 日志查看Modal -->
    <a-modal
      v-model:open="logVisible"
      :title="`${logServiceName} - 容器日志 (实时)`"
      width="75%"
      :footer="null"
      :bodyStyle="{ padding: '12px', height: '70vh', overflow: 'hidden', display: 'flex', flexDirection: 'column' }"
      @afterClose="stopLogStream"
    >
      <div class="log-container">
        <div class="log-header">
          <a-space>
            <a-tag :color="logStreaming ? 'green' : 'blue'">{{ logServiceName }}</a-tag>
            <a-tag v-if="logStreaming" color="success">
              <span class="streaming-dot"></span>
              实时推送中
            </a-tag>
            <span class="log-info">{{ logStreaming ? '实时日志流' : '最近100行日志' }}</span>
            <a-button v-if="!logStreaming" size="small" type="primary" @click="startLogStream">
              <template #icon><PlayCircleOutlined /></template>
              开始实时推送
            </a-button>
            <a-button v-else size="small" danger @click="stopLogStream">
              <template #icon><PauseCircleOutlined /></template>
              停止推送
            </a-button>
            <a-button size="small" @click="logContent = ''">清空</a-button>
            <a-switch v-model:checked="autoScroll" checked-children="自动滚动" un-checked-children="手动滚动" />
          </a-space>
        </div>
        <div class="log-content" ref="logContentRef">
          <div v-if="loadingLogs" class="log-loading">
            <a-spin size="large" tip="正在加载日志..." />
          </div>
          <pre v-else-if="logContent && logContent.trim()">{{ logContent }}</pre>
          <div v-else class="empty-log">
            <a-empty description="暂无日志内容" />
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 子组件 -->
    <DockerComposeUpload
      v-if="serverId"
      v-model:open="composeUploadOpen"
      :serverId="serverId"
      @success="handleRefresh"
    />
    <DockerServiceAdd
      v-if="serverId"
      v-model:open="addServiceOpen"
      :serverId="serverId"
      @success="handleRefresh"
    />
    <DockerServiceDetail
      v-model:open="detailOpen"
      :serviceData="currentServiceDetail"
      @success="handleRefresh"
    />
  </div>
</template>

<script setup lang="ts">
import { h, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Modal, message } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SyncOutlined,
  CloudUploadOutlined,
  UploadOutlined,
  DownloadOutlined,
  PlayCircleOutlined,
  PauseCircleOutlined,
  FileTextOutlined,
  SettingOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import {
  deleteDockerService,
  executeDockerAsync,
  executeDockerCommand,
  exportComposeFile,
  listDockerServiceByServerId,
  listServerInfo,
  syncDockerStatus,
  updateDockerVersion,
} from '../../api/server'
import DockerTaskManager from './components/DockerTaskManager.vue'
import DockerComposeUpload from './components/DockerComposeUpload.vue'
import DockerServiceAdd from './components/DockerServiceAdd.vue'
import DockerServiceDetail from './components/DockerServiceDetail.vue'

const route = useRoute()
const loading = ref(false)
const syncing = ref(false)

const serverId = ref<number>()
const serverOptions = ref<any[]>([])
const list = ref<any[]>([])
const selectedRowKeys = ref<number[]>([])

const composeUploadOpen = ref(false)
const addServiceOpen = ref(false)
const detailOpen = ref(false)
const currentServiceDetail = ref<any>({})
const taskManagerRef = ref<any>(null)

const logVisible = ref(false)
const logContent = ref('')
const logServiceName = ref('')
const currentLogServiceId = ref<number>(0)
const logStreaming = ref(false)
const autoScroll = ref(true)
const logContentRef = ref<HTMLElement | null>(null)
const loadingLogs = ref(false)
let logStreamInterval: number | null = null

const statusMap: Record<number, string> = { 0: '停止', 1: '运行中', 2: '异常' }

const columns = [
  { title: '服务名称', dataIndex: 'serviceName', key: 'serviceName', width: 120 },
  { title: '容器名称', dataIndex: 'containerName', key: 'containerName', width: 120 },
  { title: '当前镜像', key: 'imageFull', width: 200, ellipsis: true },
  { title: '目标版本', key: 'targetVersion', width: 120 },
  { title: 'PARAMS模式', key: 'useParamsMode', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '重启策略', dataIndex: 'restartPolicy', key: 'restartPolicy', width: 100 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 260 },
]

function onSelectChange(keys: number[]) { selectedRowKeys.value = keys }
function onServerChange() { loadServices(); }

async function loadServerOptions() {
  try {
    const res = await listServerInfo({ page: 1, size: 200 })
    if (res.data.code === 200) serverOptions.value = res.data.data.records
    else message.error(res.data.message || '加载服务器列表失败')
  } catch (err: any) {
    message.error('加载服务器列表失败: ' + (err.message || err))
  }
}

const originalVersions = ref<Map<number, string>>(new Map())

async function loadServices() {
  if (!serverId.value) return
  loading.value = true
  try {
    const res = await listDockerServiceByServerId(serverId.value)
    if (res.data.code === 200) {
      list.value = res.data.data || []
      originalVersions.value.clear()
      list.value.forEach(s => originalVersions.value.set(s.id, s.targetVersion || ''))
    } else {
      message.error(res.data.message || '加载服务列表失败')
    }
  } catch (err: any) {
    message.error('加载服务列表失败: ' + (err.message || err))
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  loadServices()
}

function handleTaskComplete() {
  loadServices()
  if (taskManagerRef.value) taskManagerRef.value.refresh()
}

async function syncStatusAction() {
  if (!serverId.value) return
  syncing.value = true
  try {
    const res = await syncDockerStatus(serverId.value)
    if (res.data.code === 200) {
      message.success(res.data.data || '同步成功')
      loadServices()
    } else {
      message.error(res.data.message)
    }
  } finally {
    syncing.value = false
  }
}

async function changeVersion(record: any) {
  const orig = originalVersions.value.get(record.id)
  if (record.targetVersion === orig) return
  try {
    const res = await updateDockerVersion(record.id, record.targetVersion)
    if (res.data.code === 200) {
      originalVersions.value.set(record.id, record.targetVersion)
    } else {
      message.error(res.data.message || '更新版本失败')
    }
  } catch (err: any) {
    message.error('更新版本失败: ' + (err.message || err))
  }
}

function openDetailModal(record: any) {
  currentServiceDetail.value = JSON.parse(JSON.stringify(record))
  detailOpen.value = true
}

function runCommand(record: any, commandType: string) {
  if (commandType === 'logs') {
    currentLogServiceId.value = record.id
    logServiceName.value = record.serviceName
    logContent.value = ''
    logVisible.value = true
    loadingLogs.value = true

    executeDockerCommand({ serviceId: record.id, commandType: 'logs', serverId: serverId.value! })
      .then(res => {
        if (!logVisible.value) return
        if (res.data.code === 200) {
          logContent.value = res.data.data || '暂无日志'
          startLogStream()
        } else {
          logContent.value = ''
          message.error(res.data.message || '获取日志失败')
        }
      })
      .catch(err => {
        if (!logVisible.value) return
        logContent.value = ''
        message.error('获取日志失败: ' + (err.message || err))
      })
      .finally(() => { loadingLogs.value = false })
    return
  }

  const commandName: Record<string, string> = {
    start: '启动', stop: '停止', restart: '重启', update: '更新',
  }
  Modal.confirm({
    title: `确认${commandName[commandType] || '执行'}`,
    content: `是否${commandName[commandType] || commandType}服务: ${record.serviceName}?`,
    onOk: async () => {
      const res = await executeDockerAsync({ serviceIds: [record.id], commandType, serverId: serverId.value! })
      if (res.data.code === 200) {
        message.success('任务已加入队列，请查看右侧任务管理器')
        if (taskManagerRef.value) {
          taskManagerRef.value.refresh()
          const taskId = res.data.data?.taskId
          if (taskId) {
            setTimeout(() => { taskManagerRef.value.scrollToTask(taskId) }, 500)
          }
        }
      } else {
        message.error(res.data.message)
      }
    },
  })
}

function scrollToBottom() {
  setTimeout(() => {
    if (logContentRef.value) {
      const pre = logContentRef.value.querySelector('pre')
      if (pre) pre.scrollTop = pre.scrollHeight
    }
  }, 100)
}

async function refreshLogs() {
  if (!currentLogServiceId.value) return
  try {
    const res = await executeDockerCommand({ serviceId: currentLogServiceId.value, commandType: 'logs', serverId: serverId.value! })
    if (res.data.code === 200 && res.data.data !== logContent.value) {
      logContent.value = res.data.data || '暂无日志'
      if (autoScroll.value) scrollToBottom()
    }
  } catch (err: any) {
    console.error('获取日志失败:', err)
  }
}

function startLogStream() {
  if (logStreaming.value) return
  logStreaming.value = true
  refreshLogs()
  logStreamInterval = window.setInterval(refreshLogs, 2000)
}

function stopLogStream() {
  if (logStreamInterval) { clearInterval(logStreamInterval); logStreamInterval = null }
  logStreaming.value = false
}

function handleRemoveService(record: any) {
  const num1 = Math.floor(Math.random() * 9) + 1
  const num2 = Math.floor(Math.random() * 9) + 1
  const correctAnswer = num1 + num2
  let inputValue = ''

  Modal.confirm({
    title: '移除服务确认',
    content: () => h('div', [
      h('p', { style: 'color: #ff4d4f; font-weight: bold; margin-bottom: 12px;' },
        '警告：此操作将从系统中移除服务配置，但不会停止或删除容器。'),
      h('p', { style: 'margin-bottom: 12px;' }, ['服务名称：', h('strong', record.serviceName)]),
      h('p', { style: 'margin-bottom: 8px;' }, '请回答以下问题以确认删除：'),
      h('p', { style: 'font-size: 16px; font-weight: bold; margin-bottom: 8px;' }, `${num1} + ${num2} = ?`),
      h('input', {
        id: 'mathAnswer',
        type: 'number',
        placeholder: '请输入答案',
        style: 'width: 100%; padding: 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px;',
        onInput: (e: any) => { inputValue = e.target.value },
        onKeypress: (e: KeyboardEvent) => {
          if (e.key === 'Enter') {
            const okBtn = document.querySelector('.ant-modal-confirm-btns .ant-btn-primary') as HTMLButtonElement
            if (okBtn) okBtn.click()
          }
        },
      }),
    ]),
    okText: '确认移除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    onOk: async () => {
      const userAnswer = parseInt(inputValue)
      if (!inputValue || isNaN(userAnswer)) { message.error('请输入答案'); return Promise.reject() }
      if (userAnswer !== correctAnswer) { message.error(`答案错误！正确答案是 ${correctAnswer}`); return Promise.reject() }
      try {
        const res = await deleteDockerService(record.id)
        if (res.data.code === 200) { message.success('服务已移除'); loadServices() }
        else { message.error(res.data.message || '移除失败'); return Promise.reject() }
      } catch (error: any) {
        message.error('移除失败: ' + (error.message || error))
        return Promise.reject()
      }
    },
  })

  setTimeout(() => {
    const input = document.getElementById('mathAnswer') as HTMLInputElement
    if (input) input.focus()
  }, 100)
}

async function batchUpdateAction() {
  const selectedServices = list.value.filter(s => selectedRowKeys.value.includes(s.id))
  Modal.confirm({
    title: '确认批量更新',
    content: () => h('div', [
      h('p', `即将批量更新以下 ${selectedServices.length} 个服务：`),
      h('div', { style: 'margin: 8px 0; max-height: 200px; overflow-y: auto;' },
        selectedServices.map(s => h('div', { key: s.id, style: 'padding: 2px 0; color: #1890ff;' }, `• ${s.serviceName}`))
      ),
    ]),
    onOk: async () => {
      const res = await executeDockerAsync({ serviceIds: selectedRowKeys.value, commandType: 'update', serverId: serverId.value! })
      if (res.data.code === 200) {
        message.success('批量任务已加入队列，请查看右侧任务管理器')
        selectedRowKeys.value = []
        if (taskManagerRef.value) {
          taskManagerRef.value.refresh()
          const taskId = res.data.data?.taskId
          if (taskId) setTimeout(() => { taskManagerRef.value.scrollToTask(taskId) }, 500)
        }
      } else {
        message.error(res.data.message)
      }
    },
  })
}

async function exportComposeAction() {
  if (!serverId.value) return
  try {
    const res = await exportComposeFile(serverId.value)
    const blob = new Blob([res.data], { type: 'application/x-yaml' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `docker-compose-${serverId.value}.yml`
    a.click()
    window.URL.revokeObjectURL(url)
  } catch (err: any) {
    message.error('导出Compose文件失败: ' + (err.message || err))
  }
}

onMounted(async () => {
  await loadServerOptions()
  const fromQuery = Number(route.query.serverId || 0)
  if (fromQuery) serverId.value = fromQuery
  if (!serverId.value && serverOptions.value.length > 0) serverId.value = serverOptions.value[0].id
  await loadServices()
})

onBeforeUnmount(() => {
  stopLogStream()
})
</script>

<style scoped lang="less">
.log-container {
  display: flex;
  flex-direction: column;
  height: 100%;

  .log-header {
    padding: 8px 0 12px 0;
    border-bottom: 1px solid #f0f0f0;
    margin-bottom: 12px;
    flex-shrink: 0;

    .log-info {
      color: #666;
      font-size: 12px;
    }

    .streaming-dot {
      display: inline-block;
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background-color: #52c41a;
      margin-right: 4px;
      animation: blink 1.5s infinite;
    }
  }

  .log-content {
    background: #1e1e1e;
    border-radius: 6px;
    overflow: hidden;
    flex: 1;
    display: flex;
    flex-direction: column;

    .log-loading {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #1e1e1e;
      color: #d4d4d4;
    }

    pre {
      background: #1e1e1e;
      color: #d4d4d4;
      padding: 16px;
      margin: 0;
      font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
      font-size: 13px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-word;
      overflow-y: auto;
      flex: 1;

      &::-webkit-scrollbar { width: 10px; }
      &::-webkit-scrollbar-track { background: #2d2d2d; border-radius: 5px; }
      &::-webkit-scrollbar-thumb {
        background: #555;
        border-radius: 5px;
        &:hover { background: #777; }
      }
    }

    .empty-log {
      padding: 60px;
      text-align: center;
      background: #1e1e1e;
      color: #999;
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
