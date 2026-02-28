<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <a-space>
        <a-select
          v-model:value="appPk"
          placeholder="选择应用"
          style="width: 200px"
          allow-clear
          @change="fetchList"
        >
          <a-select-option v-for="app in appList" :key="app.id" :value="app.id">
            {{ app.appName }}
          </a-select-option>
        </a-select>
      </a-space>
      <a-button type="primary" @click="openModal()">
        <PlusOutlined /> 新建套餐
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      @change="onTableChange"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'appName'">
          {{ getAppName(record.appPk) }}
        </template>
        <template v-if="column.key === 'quotas'">
          <a-tooltip>
            <template #title>
              <div v-for="(val, key) in (record.quotas || {})" :key="key">
                {{ getQuotaName(record.appPk, String(key)) }}: {{ val === 0 ? '不限' : val }}
              </div>
            </template>
            <span>{{ formatQuotas(record.appPk, record.quotas) }}</span>
          </a-tooltip>
        </template>
        <template v-if="column.key === 'features'">
          <a-tooltip v-for="f in (record.features || [])" :key="f" :title="getFeatureInfo(record.appPk, f).description || f">
            <a-tag size="small">{{ getFeatureInfo(record.appPk, f).name || f }}</a-tag>
          </a-tooltip>
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a-popconfirm title="确认删除该套餐？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalVisible"
      :title="editingPlan ? '编辑套餐' : '新建套餐'"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      width="700px"
      destroy-on-close
    >
      <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="所属应用" required>
          <a-select
            v-model:value="form.appPk"
            placeholder="选择应用"
            :disabled="!!editingPlan"
            @change="onAppChange"
          >
            <a-select-option v-for="app in appList" :key="app.id" :value="app.id">
              {{ app.appName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="套餐名称" required>
          <a-input v-model:value="form.planName" placeholder="请输入套餐名称" />
        </a-form-item>
        <a-form-item label="套餐代码" required>
          <a-input v-model:value="form.planCode" placeholder="如：basic, pro, enterprise" />
        </a-form-item>

        <!-- 配额可视化编辑 -->
        <a-form-item label="配额设置">
          <div v-if="currentQuotasDef.length === 0" style="color: #999">
            {{ form.appPk ? '该应用未定义配额' : '请先选择应用' }}
          </div>
          <div v-for="qDef in currentQuotasDef" :key="qDef.code" style="display: flex; align-items: center; margin-bottom: 8px; gap: 8px">
            <a-checkbox
              :checked="quotaEnabled[qDef.code] || false"
              @change="(e: any) => toggleQuota(qDef.code, e.target.checked, qDef.defaultValue)"
            />
            <a-tooltip :title="qDef.description">
              <span style="width: 130px; flex-shrink: 0">{{ qDef.name }}</span>
            </a-tooltip>
            <a-input-number
              v-model:value="quotaValues[qDef.code]"
              :disabled="!quotaEnabled[qDef.code]"
              :min="0"
              placeholder="0=不限"
              style="width: 140px"
              size="small"
            />
            <span style="color: #999; font-size: 12px">0=不限</span>
          </div>
        </a-form-item>

        <!-- 功能可视化编辑 -->
        <a-form-item label="功能模块">
          <div v-if="currentFeaturesDef.length === 0" style="color: #999">
            {{ form.appPk ? '该应用未定义功能' : '请先选择应用' }}
          </div>
          <div style="display: flex; flex-wrap: wrap; gap: 4px">
            <a-tooltip v-for="fDef in currentFeaturesDef" :key="fDef.code" :title="fDef.description">
              <a-checkable-tag
                :checked="form.features.includes(fDef.code)"
                @change="(checked: boolean) => toggleFeature(fDef.code, checked)"
              >
                {{ fDef.name }}
              </a-checkable-tag>
            </a-tooltip>
          </div>
        </a-form-item>

        <a-form-item label="排序">
          <a-input-number v-model:value="form.sortOrder" :min="0" />
        </a-form-item>
        <a-form-item label="状态">
          <a-switch v-model:checked="form.statusBool" checked-children="启用" un-checked-children="禁用" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { request } from '../../utils/request'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const appList = ref<any[]>([])
const appPk = ref<number | undefined>(undefined)
const modalVisible = ref(false)
const editingPlan = ref<any>(null)

const quotaEnabled = reactive<Record<string, boolean>>({})
const quotaValues = reactive<Record<string, number>>({})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '所属应用', key: 'appName', width: 150 },
  { title: '套餐名称', dataIndex: 'planName', key: 'planName', width: 150 },
  { title: '套餐代码', dataIndex: 'planCode', key: 'planCode', width: 120 },
  { title: '配额', key: 'quotas', width: 250, ellipsis: true },
  { title: '功能', key: 'features', width: 250 },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '状态', key: 'status', width: 80 },
  { title: '操作', key: 'action', width: 120 },
]

const form = reactive({
  appPk: undefined as number | undefined,
  planName: '',
  planCode: '',
  features: [] as string[],
  sortOrder: 0,
  statusBool: true,
})

function getApp(appPk: number) {
  return appList.value.find((a) => a.id === appPk)
}

function getAppName(appPk: number) {
  return getApp(appPk)?.appName || appPk
}

function getQuotaName(appPk: number, code: string): string {
  const app = getApp(appPk)
  if (!app?.quotasDef) return code
  const def = app.quotasDef.find((d: any) => d.code === code)
  return def?.name || code
}

function getFeatureInfo(appPk: number, code: string): { name: string; description: string } {
  const app = getApp(appPk)
  if (!app?.featuresDef) return { name: code, description: '' }
  const def = app.featuresDef.find((d: any) => d.code === code)
  return { name: def?.name || code, description: def?.description || '' }
}

function formatQuotas(appPk: number, quotas: Record<string, any> | null): string {
  if (!quotas) return '-'
  const parts = Object.entries(quotas).map(([key, val]) => {
    const name = getQuotaName(appPk, key)
    return `${name}: ${val === 0 ? '不限' : val}`
  })
  const str = parts.join(', ')
  return str.length > 50 ? str.substring(0, 50) + '...' : str
}

const currentQuotasDef = computed(() => {
  if (!form.appPk) return []
  const app = getApp(form.appPk)
  return app?.quotasDef || []
})

const currentFeaturesDef = computed(() => {
  if (!form.appPk) return []
  const app = getApp(form.appPk)
  return app?.featuresDef || []
})

function onAppChange() {
  Object.keys(quotaEnabled).forEach(k => delete quotaEnabled[k])
  Object.keys(quotaValues).forEach(k => delete quotaValues[k])
  form.features = []
}

function toggleQuota(code: string, checked: boolean, defaultValue?: number) {
  quotaEnabled[code] = checked
  if (checked && quotaValues[code] === undefined) {
    quotaValues[code] = defaultValue ?? 0
  }
}

function toggleFeature(code: string, checked: boolean) {
  if (checked) {
    if (!form.features.includes(code)) form.features.push(code)
  } else {
    form.features = form.features.filter(f => f !== code)
  }
}

function openModal(plan?: any) {
  editingPlan.value = plan || null
  Object.keys(quotaEnabled).forEach(k => delete quotaEnabled[k])
  Object.keys(quotaValues).forEach(k => delete quotaValues[k])

  if (plan) {
    form.appPk = plan.appPk
    form.planName = plan.planName
    form.planCode = plan.planCode
    form.sortOrder = plan.sortOrder || 0
    form.statusBool = plan.status === 1
    const appDef = getApp(plan.appPk)
    const validQuotaCodes = new Set((appDef?.quotasDef || []).map((d: any) => d.code))
    const validFeatureCodes = new Set((appDef?.featuresDef || []).map((d: any) => d.code))
    form.features = (plan.features || []).filter((f: string) => validFeatureCodes.has(f))
    if (plan.quotas) {
      for (const [key, val] of Object.entries(plan.quotas)) {
        if (validQuotaCodes.has(key)) {
          quotaEnabled[key] = true
          quotaValues[key] = val as number
        }
      }
    }
  } else {
    form.appPk = appPk.value
    form.planName = ''
    form.planCode = ''
    form.features = []
    form.sortOrder = 0
    form.statusBool = true
  }
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.appPk) {
    message.warning('请选择应用')
    return
  }
  if (!form.planName.trim() || !form.planCode.trim()) {
    message.warning('请填写套餐名称和代码')
    return
  }

  const validQCodes = new Set(currentQuotasDef.value.map((d: any) => d.code))
  const validFCodes = new Set(currentFeaturesDef.value.map((d: any) => d.code))
  const quotas: Record<string, number> = {}
  for (const [code, enabled] of Object.entries(quotaEnabled)) {
    if (enabled && validQCodes.has(code)) quotas[code] = quotaValues[code] ?? 0
  }
  const filteredFeatures = form.features.filter(f => validFCodes.has(f))

  const payload = {
    appPk: form.appPk,
    planName: form.planName,
    planCode: form.planCode,
    quotas: Object.keys(quotas).length > 0 ? quotas : null,
    features: filteredFeatures.length > 0 ? filteredFeatures : null,
    sortOrder: form.sortOrder,
    status: form.statusBool ? 1 : 0,
  }

  submitting.value = true
  try {
    const res = editingPlan.value
      ? await request.put(`/admin/plan/${editingPlan.value.id}`, payload)
      : await request.post('/admin/plan', payload)
    if (res.data.code === 200) {
      message.success(editingPlan.value ? '更新成功' : '创建成功')
      modalVisible.value = false
      fetchList()
    } else {
      message.error(res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  const res = await request.delete(`/admin/plan/${id}`)
  if (res.data.code === 200) {
    message.success('删除成功')
    fetchList()
  } else {
    message.error(res.data.message)
  }
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

async function fetchAppList() {
  const res = await request.get('/admin/app/list', { params: { page: 1, size: 100 } })
  if (res.data.code === 200) {
    appList.value = res.data.data.records
  }
}

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/admin/plan/list', {
      params: {
        page: pagination.current,
        size: pagination.pageSize,
        appPk: appPk.value || undefined,
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

onMounted(async () => {
  await fetchAppList()
  fetchList()
})
</script>
