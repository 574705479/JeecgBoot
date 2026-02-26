<template>
  <div>
    <div style="display: flex; justify-content: space-between; margin-bottom: 16px">
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索客户名称"
        style="width: 300px"
        @search="fetchList"
        allow-clear
      />
      <a-button type="primary" @click="openModal()">
        <PlusOutlined /> 新建客户
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
        <template v-if="column.key === 'createTime'">
          {{ formatDate(record.createTime) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a @click="openModal(record)">编辑</a>
            <a-popconfirm title="确认删除该客户？" @confirm="handleDelete(record.id)">
              <a style="color: #ff4d4f">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalVisible"
      :title="editingItem ? '编辑客户' : '新建客户'"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      width="550px"
      destroy-on-close
    >
      <a-form :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="客户名称" required>
          <a-input v-model:value="form.customerName" placeholder="请输入客户名称" />
        </a-form-item>
        <a-form-item label="联系人">
          <a-input v-model:value="form.contactName" placeholder="联系人姓名" />
        </a-form-item>
        <a-form-item label="联系电话">
          <a-input v-model:value="form.contactPhone" placeholder="联系电话" />
        </a-form-item>
        <a-form-item label="联系邮箱">
          <a-input v-model:value="form.contactEmail" placeholder="联系邮箱" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="form.remark" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { request } from '../../utils/request'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const modalVisible = ref(false)
const editingItem = ref<any>(null)

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '客户名称', dataIndex: 'customerName', key: 'customerName', width: 200 },
  { title: '联系人', dataIndex: 'contactName', key: 'contactName', width: 120 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 150 },
  { title: '联系邮箱', dataIndex: 'contactEmail', key: 'contactEmail', width: 200 },
  { title: '创建时间', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 120 },
]

const form = reactive({
  customerName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  remark: '',
})

function formatDate(val: string) {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

function openModal(item?: any) {
  editingItem.value = item || null
  if (item) {
    form.customerName = item.customerName
    form.contactName = item.contactName || ''
    form.contactPhone = item.contactPhone || ''
    form.contactEmail = item.contactEmail || ''
    form.remark = item.remark || ''
  } else {
    form.customerName = ''
    form.contactName = ''
    form.contactPhone = ''
    form.contactEmail = ''
    form.remark = ''
  }
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.customerName.trim()) {
    message.warning('请输入客户名称')
    return
  }
  submitting.value = true
  try {
    const res = editingItem.value
      ? await request.put(`/admin/customer/${editingItem.value.id}`, form)
      : await request.post('/admin/customer', form)
    if (res.data.code === 200) {
      message.success(editingItem.value ? '更新成功' : '创建成功')
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
  const res = await request.delete(`/admin/customer/${id}`)
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

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/admin/customer/list', {
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
