<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <a-space>
        <a-input-search
          v-model:value="keyword"
          placeholder="搜索服务器/任务/日志"
          style="width: 320px"
          allow-clear
          @search="fetchList"
        />
        <a-select v-model:value="status" style="width: 140px" allow-clear placeholder="任务状态" @change="fetchList">
          <a-select-option :value="1">成功</a-select-option>
          <a-select-option :value="0">失败</a-select-option>
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
      :scroll="{ x: 1400 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'green' : 'red'">
            {{ record.status === 1 ? '成功' : '失败' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'time'">
          {{ formatTime(record.startTime) }} ~ {{ formatTime(record.endTime) }}
        </template>
        <template v-if="column.key === 'log'">
          <a-typography-paragraph :ellipsis="{ rows: 2, expandable: true, symbol: '展开' }" style="margin-bottom: 0">
            {{ record.log || '-' }}
          </a-typography-paragraph>
        </template>
        <template v-if="column.key === 'action'">
          <a-popconfirm title="确认删除该日志？" @confirm="removeLog(record.id)">
            <a style="color: #ff4d4f">删除</a>
          </a-popconfirm>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { deleteServerLog, listServerLogs } from '../../api/server'

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const status = ref<number>()

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '服务器', dataIndex: 'server', key: 'server', width: 180 },
  { title: '任务', dataIndex: 'task', key: 'task', width: 120 },
  { title: '状态', key: 'status', width: 90 },
  { title: '开始/结束时间', key: 'time', width: 320 },
  { title: '时长', dataIndex: 'duration', key: 'duration', width: 120 },
  { title: '执行日志', key: 'log' },
  { title: '操作', key: 'action', width: 80, fixed: 'right' as const },
]

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function fetchList() {
  loading.value = true
  try {
    const res = await listServerLogs({
      page: pagination.current,
      size: pagination.pageSize,
      keyword: keyword.value || undefined,
      status: status.value,
    })
    if (res.data.code === 200) {
      list.value = res.data.data.records
      pagination.total = res.data.data.total
    }
  } finally {
    loading.value = false
  }
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

async function removeLog(id: number) {
  const res = await deleteServerLog(id)
  if (res.data.code === 200) {
    message.success('删除成功')
    fetchList()
  } else {
    message.error(res.data.message)
  }
}

onMounted(fetchList)
</script>
