<template>
  <div style="max-width: 800px">
    <a-page-header title="创建许可证" @back="$router.back()" />

    <a-form
      :model="form"
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 16 }"
      @finish="handleSubmit"
    >
      <a-form-item label="应用" name="appPk" :rules="[{ required: true, message: '请选择应用' }]">
        <a-select
          v-model:value="form.appPk"
          placeholder="选择应用"
          @change="onAppChange"
        >
          <a-select-option v-for="app in appList" :key="app.id" :value="app.id">
            {{ app.appName }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="客户" name="customerId">
        <a-select
          v-model:value="form.customerId"
          placeholder="选择客户（可选）"
          allow-clear
          show-search
          :filter-option="filterOption"
        >
          <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">
            {{ c.customerName }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="套餐" name="planId">
        <a-select
          v-model:value="form.planId"
          placeholder="选择套餐（可选，会自动填充配额和功能）"
          allow-clear
          @change="onPlanChange"
        >
          <a-select-option v-for="p in planList" :key="p.id" :value="p.id">
            {{ p.planName }} ({{ p.planCode }})
          </a-select-option>
        </a-select>
        <div style="color: #999; font-size: 12px; margin-top: 4px">
          切换套餐将覆盖当前配额和功能设置
        </div>
      </a-form-item>

      <a-form-item label="过期时间" name="expireDate" :rules="[{ required: true, message: '请选择过期时间' }]">
        <a-date-picker
          v-model:value="form.expireDate"
          show-time
          format="YYYY-MM-DD HH:mm:ss"
          style="width: 100%"
          placeholder="选择过期时间"
        />
      </a-form-item>

      <a-form-item label="配额">
        <div v-if="selectedApp?.quotasDef?.length">
          <a-row :gutter="16">
            <a-col
              :span="12"
              v-for="def in selectedApp.quotasDef"
              :key="def.code"
              style="margin-bottom: 8px"
            >
              <div style="margin-bottom: 4px">
                <a-tooltip :title="def.description">
                  <span style="cursor: help; border-bottom: 1px dashed #999">
                    {{ def.name || def.code }}（{{ def.code }}）
                  </span>
                </a-tooltip>
              </div>
              <a-input-number
                v-if="def.type === 'number'"
                v-model:value="form.quotas[def.code]"
                :min="0"
                style="width: 100%"
                :placeholder="`输入 ${def.name || def.code}（0=不限）`"
              />
              <a-input
                v-else
                v-model:value="form.quotas[def.code]"
                :placeholder="`输入 ${def.name || def.code}`"
              />
            </a-col>
          </a-row>
        </div>
        <a-textarea
          v-else
          v-model:value="form.quotasStr"
          :rows="4"
          placeholder='应用未定义配额项，可手动以 JSON 格式录入，如：{"maxUsers": 100, "maxProjects": 10}'
        />
      </a-form-item>

      <a-form-item label="功能列表">
        <div v-if="selectedApp?.featuresDef?.length">
          <a-checkbox-group v-model:value="form.features" style="width: 100%">
            <a-row>
              <a-col
                :span="8"
                v-for="def in selectedApp.featuresDef"
                :key="def.code"
                style="margin-bottom: 8px"
              >
                <a-tooltip :title="def.description">
                  <a-checkbox :value="def.code">
                    {{ def.name || def.code }}
                  </a-checkbox>
                </a-tooltip>
              </a-col>
            </a-row>
          </a-checkbox-group>
        </div>
        <a-select
          v-else
          v-model:value="form.features"
          mode="tags"
          placeholder="应用未定义功能项，可手动输入功能标识后回车"
        />
      </a-form-item>

      <a-form-item label="IP 白名单">
        <a-select
          v-model:value="form.allowedIps"
          mode="tags"
          placeholder="输入 IP 地址后回车（留空不限制）"
        />
        <div style="color: #999; font-size: 12px; margin-top: 4px">
          支持格式：单个 IP（如 192.168.1.100）、CIDR 网段（如 10.0.0.0/24）。逐个输入后按回车添加，留空表示不限制 IP。
        </div>
      </a-form-item>

      <a-divider>域名与下载配置</a-divider>

      <a-form-item label="域名列表">
        <a-textarea
          v-model:value="form.domains"
          :autoSize="{ minRows: 4, maxRows: 10 }"
          placeholder="每行输入一个域名，例如：&#10;cs.example.com&#10;cs-backup.example.com"
        />
        <a-alert
          type="error"
          show-icon
          banner
          style="margin-top: 6px; padding: 4px 12px; font-size: 12px"
          message="必填：不配置则桌面客户端（Electron）激活后会提示「所有业务域名均无法访问」，连不上后端。多写几个等于备用线路，自动选最快的一个。"
        />
      </a-form-item>

      <a-form-item label="下载链接">
        <div style="display: flex; flex-direction: column; gap: 8px">
          <div
            v-for="(item, idx) in form.downloadLinks"
            :key="idx"
            style="display: flex; align-items: center; gap: 8px"
          >
            <a-input
              v-model:value="item.label"
              placeholder="标签（如 Windows x64）"
              style="width: 180px"
            />
            <a-input
              v-model:value="item.url"
              placeholder="下载链接 URL"
              style="flex: 1"
            />
            <a-button danger size="small" @click="form.downloadLinks.splice(idx, 1)">
              删除
            </a-button>
          </div>
          <a-button
            type="dashed"
            block
            @click="form.downloadLinks.push({ label: '', url: '' })"
          >
            + 添加下载链接
          </a-button>
        </div>
        <a-alert
          type="warning"
          show-icon
          banner
          style="margin-top: 6px; padding: 4px 12px; font-size: 12px"
          message="建议配置：用于在浏览器 Dashboard 显示桌面客户端安装包下载入口（Windows / Mac / Linux）。不配置不影响浏览器访问，仅是没有桌面客户端下载按钮。"
        />
      </a-form-item>

      <a-form-item label="备注">
        <a-textarea v-model:value="form.remark" :rows="3" placeholder="备注信息" />
      </a-form-item>

      <a-form-item :wrapper-col="{ offset: 5, span: 16 }">
        <a-space>
          <a-button type="primary" html-type="submit" :loading="submitting">
            创建许可证
          </a-button>
          <a-button @click="$router.back()">取消</a-button>
        </a-space>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { request } from '../../utils/request'
import type { Dayjs } from 'dayjs'

const router = useRouter()
const submitting = ref(false)
const appList = ref<any[]>([])
const customerList = ref<any[]>([])
const planList = ref<any[]>([])
const previousPlanId = ref<number | undefined>(undefined)

const form = reactive({
  appPk: undefined as number | undefined,
  customerId: undefined as number | undefined,
  planId: undefined as number | undefined,
  expireDate: null as Dayjs | null,
  quotas: {} as Record<string, any>,
  quotasStr: '',
  features: [] as string[],
  allowedIps: [] as string[],
  remark: '',
  domains: '',
  downloadLinks: [] as { label: string; url: string }[],
})

const selectedApp = computed(() => {
  return appList.value.find((a) => a.id === form.appPk)
})

function filterOption(input: string, option: any) {
  return option.children?.[0]?.children?.toLowerCase().includes(input.toLowerCase())
}

async function onAppChange() {
  form.planId = undefined
  previousPlanId.value = undefined
  form.features = []
  if (form.appPk) {
    const app = appList.value.find((a: any) => a.id === form.appPk)
    if (app?.quotasDef) {
      const defaults: Record<string, any> = {}
      app.quotasDef.forEach((d: any) => {
        if (d.defaultValue !== undefined) defaults[d.code] = d.defaultValue
      })
      form.quotas = defaults
    } else {
      form.quotas = {}
    }
    const res = await request.get(`/admin/plan/by-app/${form.appPk}`)
    if (res.data.code === 200) {
      planList.value = res.data.data || []
    }
  } else {
    form.quotas = {}
    planList.value = []
  }
}

function applyPlan(planId: number | undefined) {
  const plan = planList.value.find((p) => p.id === planId)
  if (!plan) return
  const app = selectedApp.value
  const validQuotaCodes = new Set((app?.quotasDef || []).map((d: any) => d.code))
  const validFeatureCodes = new Set((app?.featuresDef || []).map((d: any) => d.code))
  if (plan.quotas) {
    const filtered: Record<string, any> = {}
    for (const [k, v] of Object.entries(plan.quotas)) {
      if (validQuotaCodes.has(k)) filtered[k] = v
    }
    form.quotas = filtered
    form.quotasStr = JSON.stringify(filtered, null, 2)
  }
  if (plan.features) {
    form.features = plan.features.filter((f: string) => validFeatureCodes.has(f))
  }
}

function onPlanChange(newPlanId: number | undefined) {
  if (!newPlanId) {
    previousPlanId.value = undefined
    return
  }
  const hasExistingData =
    Object.keys(form.quotas).length > 0 || form.features.length > 0
  if (!hasExistingData) {
    applyPlan(newPlanId)
    previousPlanId.value = newPlanId
    return
  }
  Modal.confirm({
    title: '切换套餐',
    content: '切换套餐将覆盖当前配额和功能设置，确定要继续吗？',
    onOk() {
      applyPlan(newPlanId)
      previousPlanId.value = newPlanId
    },
    onCancel() {
      form.planId = previousPlanId.value
    },
  })
}

async function confirmEmptyDomains(domains: string): Promise<boolean> {
  if (domains.trim().length > 0) return true
  return new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: '未配置接入域名',
      width: 480,
      content: h('div', [
        h(
          'p',
          { style: 'margin: 0 0 8px' },
          '当前许可证未填写"域名列表"，桌面客户端（Electron）激活后将无法连接到后端服务，会提示"所有业务域名均无法访问"。',
        ),
        h('p', { style: 'margin: 0' }, '是否仍要继续保存？'),
      ]) as any,
      okText: '仍然保存',
      okButtonProps: { danger: true },
      cancelText: '返回填写',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

async function handleSubmit() {
  let quotas = form.quotas
  if (form.quotasStr.trim() && !selectedApp.value?.quotasDef?.length) {
    try {
      quotas = JSON.parse(form.quotasStr)
    } catch {
      message.error('配额 JSON 格式不正确')
      return
    }
  }

  if (selectedApp.value?.quotasDef?.length) {
    const validCodes = new Set(selectedApp.value.quotasDef.map((d: any) => d.code))
    const filtered: Record<string, any> = {}
    for (const [k, v] of Object.entries(quotas)) {
      if (validCodes.has(k)) filtered[k] = v
    }
    quotas = filtered
  }
  const validFeatureCodes = new Set((selectedApp.value?.featuresDef || []).map((d: any) => d.code))
  const filteredFeatures = selectedApp.value?.featuresDef?.length
    ? form.features.filter(f => validFeatureCodes.has(f))
    : form.features

  if (!(await confirmEmptyDomains(form.domains))) return

  const domainConfig: Record<string, any> = {}
  if (form.domains.trim()) domainConfig.domains = form.domains
  const cleanLinks = form.downloadLinks.filter(
    (i) => i.label.trim() || i.url.trim(),
  )
  if (cleanLinks.length) domainConfig.downloadLinks = cleanLinks

  const payload: any = {
    appPk: form.appPk,
    customerId: form.customerId,
    planId: form.planId,
    expireDate: form.expireDate?.format('YYYY-MM-DDTHH:mm:ss'),
    quotas,
    features: filteredFeatures,
    allowedIps: form.allowedIps.length ? form.allowedIps : null,
    remark: form.remark,
  }
  if (Object.keys(domainConfig).length) payload.domainConfig = domainConfig

  submitting.value = true
  try {
    const res = await request.post('/admin/license', payload)
    if (res.data.code === 200) {
      message.success('许可证创建成功')
      router.push('/license')
    } else {
      message.error(res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  const [appRes, custRes] = await Promise.all([
    request.get('/admin/app/list', { params: { page: 1, size: 100 } }),
    request.get('/admin/customer/list', { params: { page: 1, size: 500 } }),
  ])
  if (appRes.data.code === 200) appList.value = appRes.data.data.records
  if (custRes.data.code === 200) customerList.value = custRes.data.data.records
})
</script>
