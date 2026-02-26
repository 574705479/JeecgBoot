<template>
  <div>
    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="6">
        <a-card>
          <a-statistic title="许可证总数" :value="stats.total" :loading="loading">
            <template #prefix><KeyOutlined style="color: #1890ff" /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="活跃许可证" :value="stats.active" :loading="loading">
            <template #prefix><CheckCircleOutlined style="color: #52c41a" /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="已暂停" :value="stats.suspended" :loading="loading">
            <template #prefix><PauseCircleOutlined style="color: #faad14" /></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="已吊销" :value="stats.revoked" :loading="loading">
            <template #prefix><CloseCircleOutlined style="color: #ff4d4f" /></template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :span="12">
        <a-card title="即将过期的许可证" :loading="loading" size="small">
          <a-table
            :columns="expiringColumns"
            :data-source="expiringList"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'expireDate'">
                {{ formatDate(record.expireDate) }}
              </template>
              <template v-if="column.key === 'action'">
                <a @click="goDetail(record.id)">查看</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="心跳丢失的许可证" :loading="loading" size="small">
          <a-table
            :columns="heartbeatColumns"
            :data-source="heartbeatLostList"
            :pagination="false"
            size="small"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'lastHeartbeat'">
                {{ formatDate(record.lastHeartbeat) }}
              </template>
              <template v-if="column.key === 'action'">
                <a @click="goDetail(record.id)">查看</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  KeyOutlined,
  CheckCircleOutlined,
  PauseCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons-vue'
import { request } from '../../utils/request'
import dayjs from 'dayjs'

const router = useRouter()
const loading = ref(false)

const stats = ref({
  total: 0,
  active: 0,
  suspended: 0,
  revoked: 0,
  inactive: 0,
  expired: 0,
})
const expiringList = ref<any[]>([])
const heartbeatLostList = ref<any[]>([])

const expiringColumns = [
  { title: 'License Key', dataIndex: 'licenseKey', key: 'licenseKey', ellipsis: true },
  { title: '过期时间', key: 'expireDate', width: 180 },
  { title: '操作', key: 'action', width: 80 },
]

const heartbeatColumns = [
  { title: 'License Key', dataIndex: 'licenseKey', key: 'licenseKey', ellipsis: true },
  { title: '最后心跳', key: 'lastHeartbeat', width: 180 },
  { title: '操作', key: 'action', width: 80 },
]

function formatDate(val: string) {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

function goDetail(id: number) {
  router.push(`/license/${id}`)
}

async function fetchData() {
  loading.value = true
  try {
    const [statsRes, expiringRes, heartbeatRes] = await Promise.all([
      request.get('/admin/dashboard/stats'),
      request.get('/admin/dashboard/expiring'),
      request.get('/admin/dashboard/heartbeat-lost'),
    ])
    if (statsRes.data.code === 200) {
      const data = statsRes.data.data
      stats.value.total = data.total || 0
      const byStatus = data.byStatus || {}
      stats.value.active = byStatus['ACTIVE'] || 0
      stats.value.suspended = byStatus['SUSPENDED'] || 0
      stats.value.revoked = byStatus['REVOKED'] || 0
      stats.value.inactive = byStatus['INACTIVE'] || 0
      stats.value.expired = byStatus['EXPIRED'] || 0
    }
    if (expiringRes.data.code === 200) {
      expiringList.value = expiringRes.data.data || []
    }
    if (heartbeatRes.data.code === 200) {
      heartbeatLostList.value = heartbeatRes.data.data || []
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>
