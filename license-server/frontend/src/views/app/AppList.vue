<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索应用名称"
        style="width: 300px"
        @search="fetchList"
        allow-clear
      />
      <a-button type="primary" @click="openModal()">
        <PlusOutlined /> 新建应用
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      @change="onTableChange"
      row-key="id"
      :scroll="{ x: 1200 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'appId'">
          <span>{{ record.appId }}</span>
          <a-button type="link" size="small" @click="copyText(record.appId)">
            <CopyOutlined />
          </a-button>
        </template>
        <template v-if="column.key === 'appSecret'">
          <span>{{ maskSecret(record.appSecret) }}</span>
          <a-button type="link" size="small" @click="copyText(record.appSecret)">
            <CopyOutlined />
          </a-button>
        </template>
        <template v-if="column.key === 'createTime'">
          {{ formatDate(record.createTime) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a @click="viewKeys(record)">密钥</a>
            <a-popconfirm title="确认轮换 appSecret？" @confirm="rotateSecret(record.id)">
              <a style="color: #faad14">轮换</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑应用弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="editingApp ? '编辑应用' : '新建应用'"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      width="900px"
      destroy-on-close
    >
      <a-tabs v-model:activeKey="activeTab">
        <!-- 基本信息 -->
        <a-tab-pane key="basic" tab="基本信息">
          <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 19 }">
            <a-form-item label="应用名称" required>
              <a-input v-model:value="form.appName" placeholder="请输入应用名称" />
            </a-form-item>
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="2" />
            </a-form-item>
            <a-form-item label="状态">
              <a-switch v-model:checked="form.statusBool" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
          </a-form>
        </a-tab-pane>

        <!-- 配额定义 -->
        <a-tab-pane key="quotas" tab="配额定义">
          <div style="margin-bottom: 12px; display: flex; gap: 8px">
            <a-button type="primary" size="small" @click="addQuotaRow">
              <PlusOutlined /> 添加配额
            </a-button>
            <a-button size="small" @click="openJsonImport('quotas')">
              <ImportOutlined /> JSON 导入
            </a-button>
          </div>
          <a-table
            :columns="quotaDefColumns"
            :data-source="quotasDefData"
            :pagination="false"
            row-key="_uid"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'code'">
                <a-input v-model:value="record.code" size="small" placeholder="如 max_seats" />
              </template>
              <template v-if="column.key === 'name'">
                <a-input v-model:value="record.name" size="small" placeholder="如 最大在线用户数" />
              </template>
              <template v-if="column.key === 'defaultValue'">
                <a-input-number v-model:value="record.defaultValue" size="small" :min="0" style="width: 100%" />
              </template>
              <template v-if="column.key === 'description'">
                <a-input v-model:value="record.description" size="small" placeholder="说明" />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" danger size="small" @click="quotasDefData.splice(index, 1)">
                  <DeleteOutlined />
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <!-- 功能定义 -->
        <a-tab-pane key="features" tab="功能定义">
          <div style="margin-bottom: 12px; display: flex; gap: 8px">
            <a-button type="primary" size="small" @click="addFeatureRow">
              <PlusOutlined /> 添加功能
            </a-button>
            <a-button size="small" @click="openJsonImport('features')">
              <ImportOutlined /> JSON 导入
            </a-button>
          </div>
          <a-table
            :columns="featureDefColumns"
            :data-source="featuresDefData"
            :pagination="false"
            row-key="_uid"
            size="small"
            bordered
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'code'">
                <a-input v-model:value="record.code" size="small" placeholder="如 airag" />
              </template>
              <template v-if="column.key === 'name'">
                <a-input v-model:value="record.name" size="small" placeholder="如 AI应用平台" />
              </template>
              <template v-if="column.key === 'description'">
                <a-input v-model:value="record.description" size="small" placeholder="说明" />
              </template>
              <template v-if="column.key === 'action'">
                <a-button type="link" danger size="small" @click="featuresDefData.splice(index, 1)">
                  <DeleteOutlined />
                </a-button>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-modal>

    <!-- JSON 导入弹窗 -->
    <a-modal
      v-model:open="jsonImportVisible"
      :title="jsonImportTarget === 'quotas' ? '导入配额定义 JSON' : '导入功能定义 JSON'"
      @ok="handleJsonImport"
      width="600px"
      destroy-on-close
    >
      <a-alert
        :message="jsonImportTarget === 'quotas'
          ? '粘贴配额定义 JSON 数组，每项需包含 code, name 字段。导入将覆盖现有数据。'
          : '粘贴功能定义 JSON 数组，每项需包含 code, name 字段。导入将覆盖现有数据。'"
        type="info"
        show-icon
        style="margin-bottom: 12px"
      />
      <a-textarea
        v-model:value="jsonImportText"
        :rows="12"
        placeholder='[{"code":"max_seats","name":"最大在线用户数","type":"number","defaultValue":10,"description":"..."}]'
      />
    </a-modal>

    <!-- 密钥信息弹窗 -->
    <a-modal
      v-model:open="keysVisible"
      title="应用密钥信息"
      :footer="null"
      width="700px"
    >
      <template v-if="currentApp">
        <div v-if="!currentApp.publicKey" style="text-align: center; padding: 24px 0">
          <p style="color: #999; margin-bottom: 16px">该应用尚未生成 RSA 密钥对</p>
          <a-popconfirm title="确认生成 RSA 密钥对？如已有密钥将被覆盖。" @confirm="generateKeys(currentApp.id)">
            <a-button type="primary" :loading="generatingKeys">
              <KeyOutlined /> 生成密钥对
            </a-button>
          </a-popconfirm>
        </div>
        <a-descriptions v-else bordered :column="1" size="small">
          <a-descriptions-item label="App ID">
            <div style="display: flex; align-items: center; gap: 8px">
              <code>{{ currentApp.appId }}</code>
              <a-button size="small" @click="copyText(currentApp.appId)">
                <CopyOutlined /> 复制
              </a-button>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="App Secret">
            <div style="display: flex; align-items: center; gap: 8px">
              <code>{{ currentApp.appSecret }}</code>
              <a-button size="small" @click="copyText(currentApp.appSecret)">
                <CopyOutlined /> 复制
              </a-button>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="Public Key">
            <div style="display: flex; align-items: center; gap: 8px">
              <a-textarea :value="currentApp.publicKey" :rows="4" readonly style="flex: 1" />
              <a-button size="small" @click="copyText(currentApp.publicKey)">
                <CopyOutlined /> 复制
              </a-button>
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="操作">
            <a-popconfirm title="重新生成将使现有公钥失效，客户端需更新公钥文件。确认？" @confirm="generateKeys(currentApp.id)">
              <a-button size="small" danger :loading="generatingKeys">
                重新生成密钥对
              </a-button>
            </a-popconfirm>
          </a-descriptions-item>
        </a-descriptions>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, CopyOutlined, DeleteOutlined, ImportOutlined, KeyOutlined } from '@ant-design/icons-vue'
import { request } from '../../utils/request'
import dayjs from 'dayjs'

let uidCounter = 0
function genUid() { return ++uidCounter }

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const modalVisible = ref(false)
const keysVisible = ref(false)
const generatingKeys = ref(false)
const editingApp = ref<any>(null)
const currentApp = ref<any>(null)
const activeTab = ref('basic')

const quotasDefData = ref<any[]>([])
const featuresDefData = ref<any[]>([])

const jsonImportVisible = ref(false)
const jsonImportTarget = ref<'quotas' | 'features'>('quotas')
const jsonImportText = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '应用名称', dataIndex: 'appName', key: 'appName', width: 150 },
  { title: 'App ID', key: 'appId', width: 220 },
  { title: 'App Secret', key: 'appSecret', width: 200 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 160, fixed: 'right' as const },
]

const quotaDefColumns = [
  { title: '标识 (code)', key: 'code', width: 150 },
  { title: '名称', key: 'name', width: 140 },
  { title: '默认值', key: 'defaultValue', width: 100 },
  { title: '说明', key: 'description' },
  { title: '', key: 'action', width: 50 },
]

const featureDefColumns = [
  { title: '标识 (code)', key: 'code', width: 160 },
  { title: '名称', key: 'name', width: 160 },
  { title: '说明', key: 'description' },
  { title: '', key: 'action', width: 50 },
]

const form = reactive({
  appName: '',
  remark: '',
  statusBool: true,
})

function formatDate(val: string) {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

function maskSecret(secret: string) {
  if (!secret) return ''
  return secret.substring(0, 8) + '****' + secret.substring(secret.length - 4)
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

function addQuotaRow() {
  quotasDefData.value.push({ _uid: genUid(), code: '', name: '', type: 'number', defaultValue: 0, description: '' })
}

function addFeatureRow() {
  featuresDefData.value.push({ _uid: genUid(), code: '', name: '', description: '' })
}

function openJsonImport(target: 'quotas' | 'features') {
  jsonImportTarget.value = target
  jsonImportText.value = ''
  jsonImportVisible.value = true
}

function handleJsonImport() {
  const text = jsonImportText.value.trim()
  if (!text) {
    message.warning('请粘贴 JSON 内容')
    return
  }
  let parsed: any[]
  try {
    parsed = JSON.parse(text)
  } catch {
    message.error('JSON 格式不正确')
    return
  }
  if (!Array.isArray(parsed)) {
    message.error('JSON 必须是数组格式')
    return
  }
  for (const item of parsed) {
    if (!item.code) {
      message.error('每项必须包含 code 字段')
      return
    }
  }

  if (jsonImportTarget.value === 'quotas') {
    quotasDefData.value = parsed.map(item => ({
      _uid: genUid(),
      code: item.code || '',
      name: item.name || '',
      type: item.type || 'number',
      defaultValue: item.defaultValue ?? 0,
      description: item.description || '',
    }))
  } else {
    featuresDefData.value = parsed.map(item => ({
      _uid: genUid(),
      code: item.code || '',
      name: item.name || '',
      description: item.description || '',
    }))
  }
  jsonImportVisible.value = false
  message.success(`已导入 ${parsed.length} 条记录`)
}

function openModal(app?: any) {
  editingApp.value = app || null
  activeTab.value = 'basic'
  if (app) {
    form.appName = app.appName
    form.remark = app.remark || ''
    form.statusBool = app.status === 1
    quotasDefData.value = (app.quotasDef || []).map((item: any) => ({ ...item, _uid: genUid() }))
    featuresDefData.value = (app.featuresDef || []).map((item: any) => ({ ...item, _uid: genUid() }))
  } else {
    form.appName = ''
    form.remark = ''
    form.statusBool = true
    quotasDefData.value = []
    featuresDefData.value = []
  }
  modalVisible.value = true
}

function viewKeys(app: any) {
  currentApp.value = app
  keysVisible.value = true
}

function validateDefs(): boolean {
  const quotaCodes = quotasDefData.value.map(r => r.code).filter(Boolean)
  const quotaDup = quotaCodes.find((c, i) => quotaCodes.indexOf(c) !== i)
  if (quotaDup) {
    message.error(`配额定义中存在重复的 code: ${quotaDup}`)
    activeTab.value = 'quotas'
    return false
  }
  for (const row of quotasDefData.value) {
    if (!row.code || !row.name) {
      message.error('配额定义每项的 code 和名称不能为空')
      activeTab.value = 'quotas'
      return false
    }
  }

  const featureCodes = featuresDefData.value.map(r => r.code).filter(Boolean)
  const featureDup = featureCodes.find((c, i) => featureCodes.indexOf(c) !== i)
  if (featureDup) {
    message.error(`功能定义中存在重复的 code: ${featureDup}`)
    activeTab.value = 'features'
    return false
  }
  for (const row of featuresDefData.value) {
    if (!row.code || !row.name) {
      message.error('功能定义每项的 code 和名称不能为空')
      activeTab.value = 'features'
      return false
    }
  }
  return true
}

async function handleSubmit() {
  if (!form.appName.trim()) {
    message.warning('请输入应用名称')
    activeTab.value = 'basic'
    return
  }
  if (!validateDefs()) return

  const quotasDef = quotasDefData.value.map(({ _uid, ...rest }) => rest)
  const featuresDef = featuresDefData.value.map(({ _uid, ...rest }) => rest)

  const payload = {
    appName: form.appName,
    quotasDef: quotasDef.length > 0 ? quotasDef : null,
    featuresDef: featuresDef.length > 0 ? featuresDef : null,
    remark: form.remark,
    status: form.statusBool ? 1 : 0,
  }

  submitting.value = true
  try {
    const res = editingApp.value
      ? await request.put(`/admin/app/${editingApp.value.id}`, payload)
      : await request.post('/admin/app', payload)
    if (res.data.code === 200) {
      message.success(editingApp.value ? '更新成功' : '创建成功')
      modalVisible.value = false
      fetchList()
    } else {
      message.error(res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

async function rotateSecret(id: number) {
  const res = await request.post(`/admin/app/${id}/rotate-secret`)
  if (res.data.code === 200) {
    message.success('密钥轮换成功')
    fetchList()
  } else {
    message.error(res.data.message)
  }
}

async function generateKeys(id: number) {
  generatingKeys.value = true
  try {
    const res = await request.post(`/admin/app/${id}/generate-keys`)
    if (res.data.code === 200) {
      message.success('RSA 密钥对生成成功')
      currentApp.value = res.data.data
      fetchList()
    } else {
      message.error(res.data.message)
    }
  } catch (e: any) {
    message.error('生成密钥对失败')
  } finally {
    generatingKeys.value = false
  }
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/admin/app/list', {
      params: {
        page: pagination.current,
        size: pagination.pageSize,
        keyword: keyword.value || undefined,
      },
    })
    if (res.data.code === 200) {
      list.value = res.data.data.records
      pagination.total = res.data.data.total
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>
