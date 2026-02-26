<template>
  <div>
    <div style="margin-bottom: 16px">
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
          v-model:value="filters.action"
          placeholder="操作类型"
          style="width: 140px"
          allow-clear
          @change="fetchList"
        >
          <a-select-option value="ACTIVATE">激活</a-select-option>
          <a-select-option value="HEARTBEAT">心跳</a-select-option>
          <a-select-option value="CREATE">创建</a-select-option>
          <a-select-option value="SUSPEND">暂停</a-select-option>
          <a-select-option value="REVOKE">吊销</a-select-option>
          <a-select-option value="RESTORE">恢复</a-select-option>
          <a-select-option value="EXTEND">延期</a-select-option>
          <a-select-option value="UPDATE_CONTENT">编辑内容</a-select-option>
          <a-select-option value="UPDATE_IPS">更新IP</a-select-option>
          <a-select-option value="DEACTIVATE">注销</a-select-option>
          <a-select-option value="DELETE">删除</a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.result"
          placeholder="结果"
          style="width: 120px"
          allow-clear
          @change="fetchList"
        >
          <a-select-option value="SUCCESS">成功</a-select-option>
          <a-select-option value="FAILED">失败</a-select-option>
        </a-select>
        <a-button @click="fetchList">刷新</a-button>
      </a-space>
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
        <template v-if="column.key === 'appName'">
          {{ getAppName(record.appPk) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-tag>{{ actionLabelMap[record.action] || record.action }}</a-tag>
        </template>
        <template v-if="column.key === 'result'">
          <a-tag :color="resultColorMap[record.result] || 'default'">
            {{ resultLabelMap[record.result] || record.result }}
          </a-tag>
        </template>
        <template v-if="column.key === 'createTime'">
          {{ formatDate(record.createTime) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { request } from '../../utils/request'
import dayjs from 'dayjs'

const loading = ref(false)
const list = ref<any[]>([])
const appList = ref<any[]>([])

const filters = reactive({
  appPk: undefined as number | undefined,
  action: undefined as string | undefined,
  result: undefined as string | undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const actionLabelMap: Record<string, string> = {
  ACTIVATE: '激活',
  HEARTBEAT: '心跳',
  CREATE: '创建',
  SUSPEND: '暂停',
  REVOKE: '吊销',
  RESTORE: '恢复',
  EXTEND: '延期',
  UPDATE_CONTENT: '编辑内容',
  UPDATE_IPS: '更新IP',
  DEACTIVATE: '注销',
  DELETE: '删除',
}

const resultLabelMap: Record<string, string> = {
  SUCCESS: '成功',
  FAILED: '失败',
}

const resultColorMap: Record<string, string> = {
  SUCCESS: 'green',
  FAILED: 'red',
}

const columns = [
  { title: '应用', key: 'appName', width: 120 },
  { title: 'License ID', dataIndex: 'licenseId', key: 'licenseId', width: 100 },
  { title: '操作类型', key: 'action', width: 100 },
  { title: '客户端 IP', dataIndex: 'clientIp', key: 'clientIp', width: 140 },
  { title: '结果', key: 'result', width: 80 },
  { title: '消息', dataIndex: 'message', key: 'message', ellipsis: true },
  { title: '操作时间', key: 'createTime', width: 170 },
]

function formatDate(val: string) {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function getAppName(appPk: number) {
  if (!appPk) return '-'
  return appList.value.find((a) => a.id === appPk)?.appName || appPk
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/admin/log/list', {
      params: {
        page: pagination.current,
        size: pagination.pageSize,
        appPk: filters.appPk || undefined,
        action: filters.action || undefined,
        result: filters.result || undefined,
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
  const appRes = await request.get('/admin/app/list', { params: { page: 1, size: 100 } })
  if (appRes.data.code === 200) appList.value = appRes.data.data.records
  fetchList()
})
</script>
