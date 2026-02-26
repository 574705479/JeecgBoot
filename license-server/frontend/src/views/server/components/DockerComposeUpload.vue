<template>
  <a-modal
    v-model:open="modalVisible"
    :title="uploadSuccess ? '解析结果预览' : '上传 Docker Compose 文件'"
    width="800px"
    @ok="handleOk"
    @cancel="handleCancel"
    :confirmLoading="uploading"
    :okText="uploadSuccess ? '确定' : '上传并解析'"
    destroyOnClose
  >
    <a-form :label-col="{ span: 4 }" :wrapper-col="{ span: 20 }">
      <a-form-item label="上传方式">
        <a-radio-group v-model:value="uploadType">
          <a-radio value="file">文件上传</a-radio>
          <a-radio value="text">文本粘贴</a-radio>
        </a-radio-group>
      </a-form-item>

      <a-form-item label="Compose文件" v-if="uploadType === 'file'">
        <a-upload
          :before-upload="beforeUpload"
          :file-list="fileList"
          @remove="handleRemove"
          accept=".yml,.yaml"
        >
          <a-button>
            <template #icon><UploadOutlined /></template>
            选择文件
          </a-button>
        </a-upload>
        <div class="upload-tip">支持 .yml 和 .yaml 文件</div>
      </a-form-item>

      <a-form-item label="文件内容" v-if="uploadType === 'text'">
        <a-textarea
          v-model:value="fileContent"
          :rows="15"
          placeholder="请粘贴 docker-compose.yml 文件内容"
          class="compose-textarea"
        />
      </a-form-item>

      <a-form-item label="预览" v-if="previewServices.length > 0">
        <a-table
          :columns="previewColumns"
          :dataSource="previewServices"
          :pagination="false"
          size="small"
          bordered
        >
          <template #bodyCell="{ column, text }">
            <template v-if="column.key === 'image'">
              <a-tag color="blue">{{ text }}</a-tag>
            </template>
          </template>
        </a-table>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import { uploadCompose, syncDockerStatus } from '../../../api/server'

const props = defineProps({
  open: {
    type: Boolean,
    default: false,
  },
  serverId: {
    type: Number,
    required: true,
  },
})

const emit = defineEmits(['update:open', 'success'])

const modalVisible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val),
})

const uploadType = ref('file')
const fileList = ref<any[]>([])
const fileContent = ref('')
const uploading = ref(false)
const uploadSuccess = ref(false)
const previewServices = ref<any[]>([])

const previewColumns = [
  { title: '服务名称', dataIndex: 'serviceName', key: 'serviceName' },
  { title: '镜像', dataIndex: 'imageName', key: 'image' },
  { title: '版本', dataIndex: 'currentVersion', key: 'version' },
]

watch(() => props.open, (val) => {
  if (!val) {
    uploadType.value = 'file'
    fileList.value = []
    fileContent.value = ''
    previewServices.value = []
    uploadSuccess.value = false
  }
})

const beforeUpload = (file: any) => {
  fileList.value = [file]
  const reader = new FileReader()
  reader.onload = (e) => {
    fileContent.value = e.target?.result as string
  }
  reader.readAsText(file)
  return false
}

const handleRemove = () => {
  fileList.value = []
  fileContent.value = ''
}

const handleSubmit = async () => {
  if (!fileContent.value) {
    message.warning('请上传文件或粘贴内容')
    return
  }

  uploading.value = true
  try {
    const res = await uploadCompose({
      serverId: props.serverId,
      fileName: fileList.value[0]?.name || 'docker-compose.yml',
      fileContent: fileContent.value,
    })

    if (res.data.code === 200) {
      const data = res.data.data || {}
      previewServices.value = data.services || []
      message.success(`解析成功，共 ${data.servicesCount || 0} 个服务`)
      uploadSuccess.value = true
    } else {
      message.error(res.data.message || '上传失败')
    }
  } catch (error: any) {
    message.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const handleOk = async () => {
  if (uploadSuccess.value) {
    try {
      message.loading({ content: '正在同步容器状态...', key: 'syncStatus', duration: 0 })
      const res = await syncDockerStatus(props.serverId)
      if (res.data.code === 200) {
        message.success({ content: '容器状态同步成功', key: 'syncStatus', duration: 2 })
      } else {
        message.error({ content: '同步容器状态失败', key: 'syncStatus', duration: 3 })
      }
    } catch (error: any) {
      message.error({ content: '同步容器状态失败: ' + (error.message || error), key: 'syncStatus', duration: 3 })
    }
    emit('success')
    emit('update:open', false)
  } else {
    handleSubmit()
  }
}

const handleCancel = () => {
  emit('update:open', false)
}
</script>

<style scoped lang="less">
.upload-tip {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.compose-textarea {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}
</style>
