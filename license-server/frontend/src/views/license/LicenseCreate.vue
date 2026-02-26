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
          <a-row :gutter="[16, 8]" v-for="def in selectedApp.quotasDef" :key="def.code">
            <a-col :span="8">
              <a-tooltip :title="def.description">
                <span style="cursor: help; border-bottom: 1px dashed #999">{{ def.name || def.code }}</span>
              </a-tooltip>
            </a-col>
            <a-col :span="16">
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
          placeholder='JSON 格式，如：{"maxUsers": 100, "maxProjects": 10}'
        />
      </a-form-item>

      <a-form-item label="功能列表">
        <div v-if="selectedApp?.featuresDef?.length">
          <a-checkbox-group v-model:value="form.features">
            <a-tooltip v-for="def in selectedApp.featuresDef" :key="def.code" :title="def.description">
              <a-checkbox :value="def.code">
                {{ def.name || def.code }}
              </a-checkbox>
            </a-tooltip>
          </a-checkbox-group>
        </div>
        <a-select
          v-else
          v-model:value="form.features"
          mode="tags"
          placeholder="输入功能标识后回车"
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
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { request } from '../../utils/request'
import type { Dayjs } from 'dayjs'

const router = useRouter()
const submitting = ref(false)
const appList = ref<any[]>([])
const customerList = ref<any[]>([])
const planList = ref<any[]>([])

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
})

const selectedApp = computed(() => {
  return appList.value.find((a) => a.id === form.appPk)
})

function filterOption(input: string, option: any) {
  return option.children?.[0]?.children?.toLowerCase().includes(input.toLowerCase())
}

async function onAppChange() {
  form.planId = undefined
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

function onPlanChange() {
  const plan = planList.value.find((p) => p.id === form.planId)
  if (plan) {
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

  const payload = {
    appPk: form.appPk,
    customerId: form.customerId,
    planId: form.planId,
    expireDate: form.expireDate?.format('YYYY-MM-DDTHH:mm:ss'),
    quotas,
    features: filteredFeatures,
    allowedIps: form.allowedIps.length ? form.allowedIps : null,
    remark: form.remark,
  }

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
