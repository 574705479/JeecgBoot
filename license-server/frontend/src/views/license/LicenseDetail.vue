<template>
  <div>
    <a-page-header title="许可证详情" @back="$router.back()">
      <template #extra>
        <a-space>
          <a-button
            type="primary"
            @click="openEditModal"
            :disabled="license?.status === 'REVOKED'"
          >
            编辑
          </a-button>
          <a-button v-if="license?.status === 'ACTIVE'" @click="handleAction('suspend')" type="default">
            暂停
          </a-button>
          <a-button v-if="license?.status === 'SUSPENDED'" @click="handleAction('restore')" type="primary">
            恢复
          </a-button>
          <a-button v-if="license?.status !== 'REVOKED'" danger @click="handleAction('revoke')">
            吊销
          </a-button>
          <a-button
            v-if="license?.status !== 'REVOKED' && license?.status !== 'INACTIVE'"
            @click="showExtendModal = true" type="primary" ghost>
            延期
          </a-button>
          <a-popconfirm title="确认删除此许可证？" @confirm="handleDelete">
            <a-button danger type="primary">删除</a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </a-page-header>

    <a-spin :spinning="loading">
      <a-descriptions bordered :column="2" size="small" v-if="license">
        <a-descriptions-item label="License Key" :span="2">
          <code style="font-size: 14px">{{ license.licenseKey }}</code>
          <a-button type="link" size="small" @click="copyText(license.licenseKey)">
            <CopyOutlined /> 复制
          </a-button>
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusColorMap[license.status] || 'default'">
            {{ statusTextMap[license.status] || license.status }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="应用">
          {{ appName }}
        </a-descriptions-item>
        <a-descriptions-item label="客户">
          {{ customerName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="套餐">
          {{ planName || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="签发时间">
          {{ formatDate(license.issueDate) }}
        </a-descriptions-item>
        <a-descriptions-item label="过期时间">
          {{ formatDate(license.expireDate) }}
        </a-descriptions-item>
        <a-descriptions-item label="激活时间">
          {{ formatDate(license.activatedAt) }}
        </a-descriptions-item>
        <a-descriptions-item label="激活 IP">
          {{ license.activatedIp || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="最后心跳">
          {{ formatDate(license.lastHeartbeat) }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ formatDate(license.createTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="回调地址" :span="2">
          <span v-if="license.callbackUrl" style="word-break: break-all">{{ license.callbackUrl }}</span>
          <span v-else style="color: #999">未注册</span>
        </a-descriptions-item>
        <a-descriptions-item label="备注" :span="2">
          {{ license.remark || '-' }}
        </a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <a-row :gutter="24" v-if="license">
        <a-col :span="12">
          <a-card title="配额 (Quotas)" size="small">
            <div v-if="license.quotas && Object.keys(license.quotas).length">
              <div v-for="(val, key) in license.quotas" :key="key" style="margin-bottom: 4px">
                <a-tooltip :title="getQuotaDescription(String(key))">
                  <span style="color: #666">{{ getQuotaName(String(key)) }}：</span>
                </a-tooltip>
                <strong>{{ val === 0 ? '不限' : val }}</strong>
              </div>
            </div>
            <a-empty v-else description="无配额数据" />
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="功能 (Features)" size="small">
            <div v-if="license.features?.length">
              <a-tooltip v-for="f in license.features" :key="f" :title="getFeatureDescription(f)">
                <a-tag color="blue" style="margin-bottom: 4px">
                  {{ getFeatureName(f) }}
                </a-tag>
              </a-tooltip>
            </div>
            <a-empty v-else description="无功能数据" />
          </a-card>
        </a-col>
      </a-row>

      <a-divider />

      <a-card title="IP 白名单" size="small" v-if="license">
        <template #extra>
          <a-button type="link" size="small" @click="showIpEditor = !showIpEditor">
            {{ showIpEditor ? '取消' : '编辑' }}
          </a-button>
        </template>
        <div v-if="!showIpEditor">
          <template v-if="license.allowedIps?.length">
            <a-tag v-for="ip in license.allowedIps" :key="ip" style="margin-bottom: 4px">
              {{ ip }}
            </a-tag>
          </template>
          <span v-else style="color: #999">未设置 IP 白名单（不限制）</span>
        </div>
        <div v-else>
          <a-select
            v-model:value="editIps"
            mode="tags"
            placeholder="输入 IP 后回车"
            style="width: 100%; margin-bottom: 4px"
          />
          <div style="color: #999; font-size: 12px; margin-bottom: 8px">
            支持格式：单个 IP（如 192.168.1.100）、CIDR 网段（如 10.0.0.0/24）。逐个输入后按回车添加，留空表示不限制 IP。
          </div>
          <a-button type="primary" size="small" @click="saveIps" :loading="savingIps">
            保存
          </a-button>
        </div>
      </a-card>
    </a-spin>

    <a-modal
      v-model:open="showExtendModal"
      title="延长有效期"
      @ok="handleExtend"
      :confirm-loading="extending"
    >
      <a-form-item label="新的过期时间">
        <a-date-picker
          v-model:value="newExpireDate"
          show-time
          format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
        />
      </a-form-item>
    </a-modal>

    <a-modal
      v-model:open="showEditModal"
      title="编辑许可证内容"
      :width="800"
      @ok="handleEditSave"
      :confirm-loading="editSaving"
      ok-text="保存"
      cancel-text="取消"
    >
      <a-form layout="vertical">
        <a-form-item label="套餐">
          <a-select
            v-model:value="editForm.planId"
            placeholder="选择套餐（可选）"
            allow-clear
            style="width: 100%"
            @change="onEditPlanChange"
          >
            <a-select-option v-for="p in planList" :key="p.id" :value="p.id">
              {{ p.planName }}
            </a-select-option>
          </a-select>
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            切换套餐将覆盖当前配额和功能设置
          </div>
        </a-form-item>

        <a-form-item label="配额">
          <div v-if="quotasDef.length">
            <a-row :gutter="16">
              <a-col :span="12" v-for="qd in quotasDef" :key="qd.code" style="margin-bottom: 8px">
                <div style="margin-bottom: 4px">
                  <a-tooltip :title="qd.description">
                    <span>{{ qd.name }}（{{ qd.code }}）</span>
                  </a-tooltip>
                </div>
                <a-input-number
                  v-model:value="editForm.quotas[qd.code]"
                  :min="0"
                  style="width: 100%"
                  :placeholder="qd.valueType === 'integer' ? '0 表示不限' : ''"
                />
              </a-col>
            </a-row>
          </div>
          <a-empty v-else description="应用未定义配额" />
        </a-form-item>

        <a-form-item label="功能模块">
          <div v-if="featuresDef.length">
            <a-checkbox-group v-model:value="editForm.features" style="width: 100%">
              <a-row>
                <a-col :span="8" v-for="fd in featuresDef" :key="fd.code" style="margin-bottom: 8px">
                  <a-tooltip :title="fd.description">
                    <a-checkbox :value="fd.code">{{ fd.name }}</a-checkbox>
                  </a-tooltip>
                </a-col>
              </a-row>
            </a-checkbox-group>
          </div>
          <a-empty v-else description="应用未定义功能模块" />
        </a-form-item>

        <a-form-item label="IP 白名单">
          <a-select
            v-model:value="editForm.allowedIps"
            mode="tags"
            placeholder="输入 IP 后回车添加"
            style="width: 100%"
          />
          <div style="color: #999; font-size: 12px; margin-top: 4px">
            支持格式：单个 IP（如 192.168.1.100）、CIDR 网段（如 10.0.0.0/24）。留空表示不限制 IP。
          </div>
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea v-model:value="editForm.remark" :rows="3" placeholder="备注信息" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { CopyOutlined } from '@ant-design/icons-vue'
import { request } from '../../utils/request'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const license = ref<any>(null)
const appData = ref<any>(null)
const appName = ref('')
const customerName = ref('')
const planName = ref('')
const showIpEditor = ref(false)
const editIps = ref<string[]>([])
const savingIps = ref(false)
const showExtendModal = ref(false)
const newExpireDate = ref<Dayjs | null>(null)
const extending = ref(false)

const showEditModal = ref(false)
const editSaving = ref(false)
const planList = ref<any[]>([])
const quotasDef = ref<any[]>([])
const featuresDef = ref<any[]>([])
const editForm = reactive({
  planId: null as number | null,
  quotas: {} as Record<string, number>,
  features: [] as string[],
  allowedIps: [] as string[],
  remark: '',
})

const statusColorMap: Record<string, string> = {
  INACTIVE: 'default',
  ACTIVE: 'green',
  SUSPENDED: 'orange',
  REVOKED: 'red',
  EXPIRED: 'volcano',
}
const statusTextMap: Record<string, string> = {
  INACTIVE: '未激活',
  ACTIVE: '活跃',
  SUSPENDED: '已暂停',
  REVOKED: '已吊销',
  EXPIRED: '已过期',
}

function formatDate(val: string) {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch {
    message.error('复制失败')
  }
}

async function fetchDetail() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await request.get(`/admin/license/${id}`)
    if (res.data.code === 200) {
      license.value = res.data.data
      editIps.value = [...(license.value.allowedIps || [])]

      if (license.value.appPk) {
        const appRes = await request.get(`/admin/app/${license.value.appPk}`)
        if (appRes.data.code === 200) {
          appData.value = appRes.data.data
          appName.value = appRes.data.data.appName
        }
      }
      if (license.value.customerId) {
        const custRes = await request.get(`/admin/customer/${license.value.customerId}`)
        if (custRes.data.code === 200) customerName.value = custRes.data.data.customerName
      }
      if (license.value.planId) {
        const planRes = await request.get(`/admin/plan/${license.value.planId}`)
        if (planRes.data.code === 200) planName.value = planRes.data.data.planName
      }
    }
  } finally {
    loading.value = false
  }
}

async function handleAction(action: string) {
  const id = route.params.id
  const res = await request.post(`/admin/license/${id}/${action}`)
  if (res.data.code === 200) {
    message.success('操作成功')
    fetchDetail()
  } else {
    message.error(res.data.message)
  }
}

async function handleExtend() {
  if (!newExpireDate.value) {
    message.warning('请选择新的过期时间')
    return
  }
  extending.value = true
  try {
    const id = route.params.id
    const res = await request.post(`/admin/license/${id}/extend`, {
      expireDate: newExpireDate.value.format('YYYY-MM-DDTHH:mm:ss'),
    })
    if (res.data.code === 200) {
      message.success('延期成功')
      showExtendModal.value = false
      fetchDetail()
    } else {
      message.error(res.data.message)
    }
  } finally {
    extending.value = false
  }
}

async function handleDelete() {
  const id = route.params.id
  const res = await request.delete(`/admin/license/${id}`)
  if (res.data.code === 200) {
    message.success('删除成功')
    router.push('/license')
  } else {
    message.error(res.data.message)
  }
}

function getQuotaName(code: string): string {
  const def = appData.value?.quotasDef?.find((d: any) => d.code === code)
  return def?.name || code
}

function getQuotaDescription(code: string): string {
  const def = appData.value?.quotasDef?.find((d: any) => d.code === code)
  return def?.description || ''
}

function getFeatureName(code: string): string {
  const def = appData.value?.featuresDef?.find((d: any) => d.code === code)
  return def?.name || code
}

function getFeatureDescription(code: string): string {
  const def = appData.value?.featuresDef?.find((d: any) => d.code === code)
  return def?.description || ''
}

async function saveIps() {
  savingIps.value = true
  try {
    const id = route.params.id
    const res = await request.put(`/admin/license/${id}/ips`, { allowedIps: editIps.value })
    if (res.data.code === 200) {
      message.success('IP 白名单已更新')
      showIpEditor.value = false
      fetchDetail()
    } else {
      message.error(res.data.message)
    }
  } finally {
    savingIps.value = false
  }
}

async function openEditModal() {
  if (!license.value || !appData.value) return
  quotasDef.value = appData.value.quotasDef || []
  featuresDef.value = appData.value.featuresDef || []

  editForm.planId = license.value.planId || null
  editForm.remark = license.value.remark || ''
  editForm.allowedIps = [...(license.value.allowedIps || [])]
  const validFeatureCodes = new Set(featuresDef.value.map((d: any) => d.code))
  editForm.features = (license.value.features || []).filter((f: string) => validFeatureCodes.has(f))

  const q: Record<string, number> = {}
  for (const qd of quotasDef.value) {
    q[qd.code] = license.value.quotas?.[qd.code] ?? 0
  }
  editForm.quotas = q

  if (appData.value?.id) {
    try {
      const res = await request.get(`/admin/plan/by-app/${appData.value.id}`)
      if (res.data.code === 200) planList.value = res.data.data || []
    } catch {
      planList.value = []
    }
  }

  showEditModal.value = true
}

function onEditPlanChange(planId: number | null) {
  if (!planId) return
  const plan = planList.value.find((p: any) => p.id === planId)
  if (!plan) return
  Modal.confirm({
    title: '切换套餐',
    content: '切换套餐将覆盖当前配额和功能设置，确定要继续吗？',
    onOk() {
      if (plan.quotas) {
        const q: Record<string, number> = {}
        for (const qd of quotasDef.value) {
          q[qd.code] = plan.quotas[qd.code] ?? 0
        }
        editForm.quotas = q
      }
      if (plan.features) {
        const validFCodes = new Set(featuresDef.value.map((d: any) => d.code))
        editForm.features = plan.features.filter((f: string) => validFCodes.has(f))
      }
    },
    onCancel() {
      editForm.planId = license.value?.planId || null
    },
  })
}

async function handleEditSave() {
  editSaving.value = true
  try {
    const id = route.params.id
    const body: any = {
      quotas: editForm.quotas,
      features: editForm.features,
      planId: editForm.planId,
      allowedIps: editForm.allowedIps,
      remark: editForm.remark,
    }
    const res = await request.put(`/admin/license/${id}`, body)
    if (res.data.code === 200) {
      message.success('许可证内容已更新')
      showEditModal.value = false
      fetchDetail()
    } else {
      message.error(res.data.message)
    }
  } finally {
    editSaving.value = false
  }
}

onMounted(async () => {
  await fetchDetail()
  if (route.query.edit === '1' && license.value?.status !== 'REVOKED') {
    openEditModal()
  }
})
</script>

<style scoped>
pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  margin: 0;
}
</style>
