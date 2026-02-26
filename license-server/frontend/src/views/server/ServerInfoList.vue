<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px; gap: 12px">
      <a-space>
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索服务器名称/IP"
          style="width: 320px"
          allow-clear
          @search="fetchList"
        />
        <a-button @click="fetchList">刷新</a-button>
      </a-space>
      <a-space>
        <a-button type="primary" @click="openEditModal()">
          <PlusOutlined /> 新增
        </a-button>
        <a-button :disabled="selectedRowKeys.length === 0" @click="openSqlModal">
          批量执行SQL
        </a-button>
        <a-button :disabled="selectedRowKeys.length === 0" @click="openShellModal">
          批量执行Shell
        </a-button>
        <a-popconfirm
          title="确认删除选中服务器？"
          :disabled="selectedRowKeys.length === 0"
          @confirm="batchDelete"
        >
          <a-button danger :disabled="selectedRowKeys.length === 0">批量删除</a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
      row-key="id"
      @change="onTableChange"
      :scroll="{ x: 1500 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'serverType'">
          {{ record.serverType === 2 ? '云数据库' : '服务器' }}
        </template>
        <template v-if="column.key === 'connectionType'">
          {{ record.connectionType === 2 ? '直连' : 'SSH连接' }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '正常' : '异常' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openEditModal(record)">编辑</a>
            <a @click="handleConfigKey(record)" v-if="record.connectionType === 1">配置公钥</a>
            <a @click="openTerminal(record)" v-if="record.connectionType === 1">终端</a>
            <a @click="goDocker(record)" v-if="record.connectionType === 1 && record.serverType !== 2">Docker管理</a>
            <a-popconfirm title="确认删除该服务器？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="editVisible"
      :title="editing.id ? '编辑服务器' : '新增服务器'"
      :confirm-loading="submitting"
      @ok="submitEdit"
      width="900px"
      destroy-on-close
    >
      <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="服务器名称" required>
              <a-input v-model:value="editing.serverName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="IP/域名" required>
              <a-input v-model:value="editing.ip" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="服务器类型">
              <a-select v-model:value="editing.serverType">
                <a-select-option :value="1">服务器</a-select-option>
                <a-select-option :value="2">云数据库</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="连接方式">
              <a-select v-model:value="editing.connectionType">
                <a-select-option :value="1">SSH连接</a-select-option>
                <a-select-option :value="2">直连</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="editing.connectionType === 1">
            <a-form-item label="SSH端口">
              <a-input-number v-model:value="editing.sshPort" :min="1" :max="65535" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="editing.connectionType === 1">
            <a-form-item label="SSH用户">
              <a-input v-model:value="editing.username" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="editing.connectionType === 1">
            <a-form-item label="SSH密码">
              <a-input-password v-model:value="editing.password" />
            </a-form-item>
          </a-col>
          <a-col :span="12" v-if="editing.connectionType === 1">
            <a-form-item label="私钥路径">
              <a-input v-model:value="editing.privateKeyPath" placeholder="可选：如 /root/.ssh/id_rsa" />
            </a-form-item>
          </a-col>
          <a-col :span="24" v-if="editing.connectionType === 1">
            <a-form-item label="私钥内容">
              <a-textarea v-model:value="editing.privateKey" :rows="4" placeholder="可选：直接粘贴私钥内容" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="MySQL用户">
              <a-input v-model:value="editing.msUser" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="MySQL密码">
              <a-input-password v-model:value="editing.msPwd" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="MySQL端口">
              <a-input-number v-model:value="editing.msPort" :min="1" :max="65535" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="数据库名">
              <a-input v-model:value="editing.databaseName" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="超链接">
              <a-textarea v-model:value="editing.spLink" :rows="2" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="sqlVisible"
      title="批量执行SQL"
      :confirm-loading="sqlExecuting"
      @ok="submitSql"
      width="800px"
      destroy-on-close
    >
      <a-alert type="warning" show-icon message="SQL将在选中服务器上执行，请谨慎操作" style="margin-bottom: 12px" />
      <a-textarea v-model:value="sqlContent" :rows="12" placeholder="请输入SQL，可多条以分号分隔" />
    </a-modal>

    <a-modal
      v-model:open="shellVisible"
      title="批量执行Shell命令"
      :confirm-loading="shellExecuting"
      @ok="submitShell"
      width="800px"
      destroy-on-close
    >
      <a-alert type="warning" show-icon message="命令将在选中服务器上执行，请谨慎操作" style="margin-bottom: 12px" />
      <a-textarea v-model:value="shellContent" :rows="12" placeholder="请输入Shell命令" />
    </a-modal>

    <ServerTerminalModal
      v-model="terminalVisible"
      :server-id="terminalServerId"
      :server-name="terminalServerName"
      :server-ip="terminalServerIp"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import ServerTerminalModal from './components/ServerTerminalModal.vue'
import {
  batchDeleteServerInfo,
  connectServer,
  deleteServerInfo,
  executeShellCommand,
  executeSql,
  generateSshKey,
  listServerInfo,
  saveServerInfo,
} from '../../api/server'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const sqlExecuting = ref(false)
const shellExecuting = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const selectedRowKeys = ref<number[]>([])

const editVisible = ref(false)
const sqlVisible = ref(false)
const shellVisible = ref(false)
const terminalVisible = ref(false)
const terminalServerId = ref<number | null>(null)
const terminalServerName = ref('')
const terminalServerIp = ref('')
const sqlContent = ref('')
const shellContent = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const editing = reactive<any>({
  id: null,
  serverName: '',
  serverType: 1,
  connectionType: 1,
  cloudVendor: '',
  ip: '',
  sshPort: 22,
  username: '',
  password: '',
  privateKey: '',
  privateKeyPath: '',
  msUser: '',
  msPwd: '',
  msPort: 3306,
  databaseName: 'im_platform',
  spLink: '',
  status: 0,
})

const columns = [
  { title: '服务器名称', dataIndex: 'serverName', key: 'serverName', width: 180 },
  { title: '服务器类型', key: 'serverType', width: 100 },
  { title: '连接方式', key: 'connectionType', width: 100 },
  { title: '云服务商', dataIndex: 'cloudVendor', key: 'cloudVendor', width: 120 },
  { title: 'IP/域名', dataIndex: 'ip', key: 'ip', width: 180 },
  { title: '状态', key: 'status', width: 90 },
  { title: 'SSH端口', dataIndex: 'sshPort', key: 'sshPort', width: 90 },
  { title: 'SSH用户', dataIndex: 'username', key: 'username', width: 100 },
  { title: 'MySQL端口', dataIndex: 'msPort', key: 'msPort', width: 100 },
  { title: 'MySQL用户', dataIndex: 'msUser', key: 'msUser', width: 100 },
  { title: '操作', key: 'action', width: 260, fixed: 'right' as const },
]

function resetEditing() {
  Object.assign(editing, {
    id: null,
    serverName: '',
    serverType: 1,
    connectionType: 1,
    cloudVendor: '',
    ip: '',
    sshPort: 22,
    username: '',
    password: '',
    privateKey: '',
    privateKeyPath: '',
    msUser: '',
    msPwd: '',
    msPort: 3306,
    databaseName: 'im_platform',
    spLink: '',
    status: 0,
  })
}

function openEditModal(record?: any) {
  if (!record) {
    resetEditing()
  } else {
    Object.assign(editing, JSON.parse(JSON.stringify(record)))
  }
  editVisible.value = true
}

async function submitEdit() {
  if (!editing.serverName || !editing.ip) {
    message.warning('服务器名称和IP/域名不能为空')
    return
  }
  submitting.value = true
  try {
    const isEdit = !!editing.id
    const payload = JSON.parse(JSON.stringify(editing))
    const res = await saveServerInfo(payload, isEdit)
    if (res.data.code === 200) {
      message.success(isEdit ? '更新成功' : '新增成功')
      editVisible.value = false
      fetchList()
    } else {
      message.error(res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listServerInfo({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: keyword.value || undefined,
    })
    if (res.data.code === 200) {
      list.value = res.data.data.records
      pagination.total = res.data.data.total
      refreshServerStatus()
    }
  } finally {
    loading.value = false
  }
}

async function refreshServerStatus() {
  const records = list.value || []
  if (!records.length) return
  await Promise.allSettled(
    records.map(async (record: any) => {
      try {
        const res = await connectServer(record.id)
        record.status = res?.data?.code === 200 ? 1 : 0
      } catch {
        record.status = 0
      }
    }),
  )
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

function onSelectChange(keys: number[]) {
  selectedRowKeys.value = keys
}

async function handleDelete(id: number) {
  const res = await deleteServerInfo(id)
  if (res.data.code === 200) {
    message.success('删除成功')
    fetchList()
  } else {
    message.error(res.data.message)
  }
}

async function batchDelete() {
  const res = await batchDeleteServerInfo(selectedRowKeys.value)
  if (res.data.code === 200) {
    message.success('批量删除成功')
    selectedRowKeys.value = []
    fetchList()
  } else {
    message.error(res.data.message)
  }
}

async function handleConfigKey(record: any) {
  const res = await generateSshKey(record.id)
  if (res.data.code === 200) {
    message.success('公钥配置成功')
    fetchList()
  } else {
    message.error(res.data.message)
  }
}

function goDocker(record: any) {
  router.push(`/server/docker?serverId=${record.id}&serverName=${encodeURIComponent(record.serverName)}`)
}

function openTerminal(record: any) {
  terminalServerId.value = record.id
  terminalServerName.value = record.serverName || ''
  terminalServerIp.value = record.ip || ''
  terminalVisible.value = true
}

function openSqlModal() {
  sqlContent.value = ''
  sqlVisible.value = true
}

function openShellModal() {
  shellContent.value = ''
  shellVisible.value = true
}

async function submitSql() {
  if (!sqlContent.value.trim()) {
    message.warning('请输入SQL')
    return
  }
  sqlExecuting.value = true
  try {
    const res = await executeSql(selectedRowKeys.value, sqlContent.value)
    if (res.data.code === 200) {
      message.success('执行完成')
      sqlVisible.value = false
    } else {
      message.error(res.data.message)
    }
  } finally {
    sqlExecuting.value = false
  }
}

async function submitShell() {
  if (!shellContent.value.trim()) {
    message.warning('请输入Shell命令')
    return
  }
  shellExecuting.value = true
  try {
    const res = await executeShellCommand(selectedRowKeys.value, shellContent.value)
    if (res.data.code === 200) {
      message.success('执行完成')
      shellVisible.value = false
    } else {
      message.error(res.data.message)
    }
  } finally {
    shellExecuting.value = false
  }
}

onMounted(fetchList)
</script>
