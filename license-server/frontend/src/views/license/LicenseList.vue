<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <a-space>
        <a-select
          v-model:value="filters.appPk"
          placeholder="选择应用"
          style="width: 160px"
          allow-clear
          @change="fetchList"
        >
          <a-select-option v-for="app in appList" :key="app.id" :value="app.id">
            {{ app.appName }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.customerId"
          placeholder="选择客户"
          style="width: 160px"
          allow-clear
          show-search
          :filter-option="filterCustomer"
          @change="fetchList"
        >
          <a-select-option v-for="c in customerList" :key="c.id" :value="c.id">
            {{ c.customerName }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.status"
          placeholder="状态"
          style="width: 120px"
          allow-clear
          @change="fetchList"
        >
          <a-select-option value="INACTIVE">未激活</a-select-option>
          <a-select-option value="ACTIVE">活跃</a-select-option>
          <a-select-option value="SUSPENDED">已暂停</a-select-option>
          <a-select-option value="REVOKED">已吊销</a-select-option>
          <a-select-option value="EXPIRED">已过期</a-select-option>
        </a-select>
        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索 license key"
          style="width: 220px"
          @search="fetchList"
          allow-clear
        />
      </a-space>
      <a-button type="primary" @click="$router.push('/license/create')">
        <PlusOutlined /> 创建许可证
      </a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="pagination"
      @change="onTableChange"
      row-key="id"
      :scroll="{ x: 1400 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'licenseKey'">
          <a @click="$router.push(`/license/${record.id}`)">
            {{ record.licenseKey }}
          </a>
        </template>
        <template v-if="column.key === 'appName'">
          {{ getAppName(record.appPk) }}
        </template>
        <template v-if="column.key === 'customerName'">
          {{ getCustomerName(record.customerId) }}
        </template>
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColorMap[record.status] || 'default'">
            {{ statusTextMap[record.status] || record.status }}
          </a-tag>
        </template>
        <template v-if="column.key === 'expireDate'">
          {{ formatDate(record.expireDate) }}
        </template>
        <template v-if="column.key === 'lastHeartbeat'">
          {{ formatDate(record.lastHeartbeat) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="$router.push(`/license/${record.id}`)">详情</a>
            <a v-if="record.status !== 'REVOKED'" @click="$router.push(`/license/${record.id}?edit=1`)">编辑</a>
            <template v-if="record.status === 'ACTIVE'">
              <a-popconfirm title="确认暂停？" @confirm="changeStatus(record.id, 'suspend')">
                <a style="color: #faad14">暂停</a>
              </a-popconfirm>
            </template>
            <template v-if="record.status === 'SUSPENDED'">
              <a-popconfirm title="确认恢复？" @confirm="changeStatus(record.id, 'restore')">
                <a style="color: #52c41a">恢复</a>
              </a-popconfirm>
            </template>
            <template v-if="record.status !== 'REVOKED'">
              <a-popconfirm title="确认吊销？此操作不可撤销！" @confirm="changeStatus(record.id, 'revoke')">
                <a style="color: #ff4d4f">吊销</a>
              </a-popconfirm>
            </template>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { request } from '../../utils/request'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref<any[]>([])
const appList = ref<any[]>([])
const customerList = ref<any[]>([])

const filters = reactive({
  appPk: undefined as number | undefined,
  customerId: undefined as number | undefined,
  status: undefined as string | undefined,
  keyword: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
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

const columns = [
  { title: 'License Key', key: 'licenseKey', width: 220, ellipsis: true },
  { title: '应用', key: 'appName', width: 120 },
  { title: '客户', key: 'customerName', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: '过期时间', key: 'expireDate', width: 170 },
  { title: '最后心跳', key: 'lastHeartbeat', width: 170 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const },
]

function formatDate(val: string) {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

function getAppName(appPk: number) {
  return appList.value.find((a) => a.id === appPk)?.appName || appPk
}

function getCustomerName(id: number) {
  if (!id) return '-'
  return customerList.value.find((c) => c.id === id)?.customerName || id
}

function filterCustomer(input: string, option: any) {
  return option.children?.[0]?.children?.toLowerCase().includes(input.toLowerCase())
}

async function changeStatus(id: number, action: string) {
  const res = await request.post(`/admin/license/${id}/${action}`)
  if (res.data.code === 200) {
    message.success('操作成功')
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

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/admin/license/list', {
      params: {
        page: pagination.current,
        size: pagination.pageSize,
        appPk: filters.appPk || undefined,
        customerId: filters.customerId || undefined,
        status: filters.status || undefined,
        keyword: filters.keyword || undefined,
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

async function fetchSelects() {
  const [appRes, custRes] = await Promise.all([
    request.get('/admin/app/list', { params: { page: 1, size: 100 } }),
    request.get('/admin/customer/list', { params: { page: 1, size: 500 } }),
  ])
  if (appRes.data.code === 200) appList.value = appRes.data.data.records
  if (custRes.data.code === 200) customerList.value = custRes.data.data.records
}

onMounted(async () => {
  await fetchSelects()
  fetchList()
})
</script>
