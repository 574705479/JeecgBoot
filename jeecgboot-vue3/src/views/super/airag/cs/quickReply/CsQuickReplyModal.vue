<template>
  <BasicModal
    v-bind="$attrs"
    @register="registerModal"
    :title="isUpdate ? '编辑快捷回复' : '新增快捷回复'"
    @ok="handleSubmit"
    width="700px"
  >
    <a-form :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
      <a-form-item label="标题/关键词" required>
        <a-input v-model:value="formData.title" placeholder="用于搜索的关键词" />
      </a-form-item>

      <a-form-item label="消息类型">
        <a-select v-model:value="formData.msgType" @change="handleMsgTypeChange">
          <a-select-option :value="0">文本</a-select-option>
          <a-select-option :value="1">图片</a-select-option>
          <a-select-option :value="2">文件</a-select-option>
          <a-select-option :value="5">富文本</a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="回复内容" required>
        <!-- 文本 -->
        <a-textarea
          v-if="formData.msgType === 0"
          v-model:value="formData.content"
          :rows="4"
          placeholder="快捷回复的文本内容"
        />
        <!-- 富文本 -->
        <div v-else-if="formData.msgType === 5">
          <JEditor v-model:value="formData.content" />
        </div>
        <!-- 图片 -->
        <div v-else-if="formData.msgType === 1">
          <a-upload
            :showUploadList="false"
            accept="image/*"
            :beforeUpload="beforeUploadFile"
            :customRequest="handleCustomUpload"
          >
            <a-button size="small">
              <upload-outlined /> 上传图片
            </a-button>
          </a-upload>
          <a-spin :spinning="uploading" size="small">
            <a-input
              v-model:value="formData.content"
              placeholder="或直接输入图片URL地址"
              style="margin-top: 8px"
            />
          </a-spin>
          <div v-if="formData.content" style="margin-top: 8px">
            <a-image :src="resolvePreviewUrl(formData.content)" :width="120" fallback="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mN8/+F/PQAJpAN12QOABAAAAABJRU5ErkJggg==" />
          </div>
        </div>
        <!-- 文件 -->
        <div v-else-if="formData.msgType === 2">
          <a-upload
            :showUploadList="false"
            :beforeUpload="beforeUploadFile"
            :customRequest="handleCustomUpload"
          >
            <a-button size="small">
              <upload-outlined /> 上传文件
            </a-button>
          </a-upload>
          <a-spin :spinning="uploading" size="small">
            <a-input
              v-model:value="formData.content"
              placeholder="或直接输入文件URL地址"
              style="margin-top: 8px"
            />
          </a-spin>
          <div v-if="formData.content" style="margin-top: 4px; color: #666; font-size: 12px">
            已上传: {{ formData.content.split('/').pop() }}
          </div>
        </div>
      </a-form-item>

      <a-form-item label="所属客服">
        <a-select v-model:value="formData.agentId" placeholder="不选则为公共回复" allowClear>
          <a-select-option v-for="opt in agentOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </a-select-option>
        </a-select>
      </a-form-item>

      <a-form-item label="排序">
        <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
      </a-form-item>

      <a-form-item label="快捷键">
        <a-input
          :value="formData.shortcutKey"
          placeholder="点击此处后按下快捷键组合"
          readonly
          @keydown="captureShortcut"
          @click="shortcutInputFocused = true"
          @blur="shortcutInputFocused = false"
          :class="{ 'shortcut-listening': shortcutInputFocused }"
        >
          <template #suffix>
            <close-circle-outlined
              v-if="formData.shortcutKey"
              style="cursor: pointer; color: #999"
              @click.stop="formData.shortcutKey = ''"
            />
          </template>
        </a-input>
        <div v-if="shortcutInputFocused" style="margin-top: 4px; color: #1890ff; font-size: 12px">
          正在监听键盘输入，请按下快捷键组合（如 Ctrl+1、Alt+Q）...
        </div>
        <div style="margin-top: 4px; color: #999; font-size: 12px; line-height: 1.5">
          <div>· 仅在工作台输入框聚焦时生效</div>
          <div>· 避免使用浏览器常用快捷键（Ctrl+C/V/A/Z/X/S/F 等）</div>
          <div>· 建议使用 Alt+数字 或 Ctrl+数字 组合</div>
          <div>· 多个快捷回复配置相同快捷键时，优先匹配第一个</div>
        </div>
      </a-form-item>

      <a-form-item label="状态">
        <a-radio-group v-model:value="formData.status">
          <a-radio :value="1">启用</a-radio>
          <a-radio :value="0">禁用</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </BasicModal>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { UploadOutlined, CloseCircleOutlined } from '@ant-design/icons-vue';
import JEditor from '/@/components/Form/src/jeecg/components/JEditor.vue';
import { computeFileMd5 } from '../utils/fileHash';
import { withImageCache } from '/@/utils/file/imageCache';
import { isCseUrl } from '/@/utils/cse/cseUrl';

const emit = defineEmits(['success', 'register']);
const { createMessage } = useMessage();

const isUpdate = ref(false);
const recordId = ref('');
const uploading = ref(false);
const shortcutInputFocused = ref(false);

const agentOptions = ref<{ label: string; value: string }[]>([]);

const defaultFormData = () => ({
  title: '',
  content: '',
  msgType: 0,
  agentId: undefined as string | undefined,
  sort: 0,
  shortcutKey: '',
  status: 1,
});

const formData = reactive(defaultFormData());

async function loadAgentList() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/list', params: { pageSize: 1000 } });
    const records = res.records || res || [];
    agentOptions.value = records.map((agent: any) => ({
      label: agent.nickname || agent.username || agent.id,
      value: agent.id,
    }));
  } catch (e) {
    console.error('加载客服列表失败', e);
  }
}

onMounted(() => {
  loadAgentList();
});

const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
  Object.assign(formData, defaultFormData());
  isUpdate.value = !!data?.isUpdate;

  if (data?.record) {
    recordId.value = data.record.id;
    Object.assign(formData, {
      title: data.record.title || '',
      content: data.record.content || '',
      msgType: data.record.msgType ?? 0,
      agentId: data.record.agentId || undefined,
      sort: data.record.sort ?? 0,
      shortcutKey: data.record.shortcutKey || '',
      status: data.record.status ?? 1,
    });
  }
});

function handleMsgTypeChange() {
  formData.content = '';
}

function resolvePreviewUrl(url: string) {
  if (!url) return '';
  // CSE 加密图片：走 withImageCache 解密为 blob URL
  if (isCseUrl(url)) return withImageCache(url);
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url;
  return `${window.location.origin}/${url.replace(/^\//, '')}`;
}

const UPLOAD_MAX_SIZE = 20 * 1024 * 1024;

function beforeUploadFile(file: File) {
  if (file.size > UPLOAD_MAX_SIZE) {
    createMessage.warning('文件大小不能超过 20MB');
    return false;
  }
  return true;
}

function handleCustomUpload(options: any) {
  const file = options.file as File;
  uploadFile(file);
}

async function uploadFile(file: File) {
  uploading.value = true;
  try {
    const md5 = await computeFileMd5(file);

    const checkRes = await defHttp.post(
      { url: '/airag/chat/checkHash', params: { md5, fileSize: file.size } },
      { joinParamsToUrl: true },
    );

    if (checkRes?.exists) {
      formData.content = checkRes.url || '';
      createMessage.success('文件秒传成功');
      return;
    }

    const isReturn = (fileInfo: any) => {
      try {
        if (fileInfo.code === 0) {
          const url = fileInfo.message;
          if (url) {
            formData.content = url;
            createMessage.success('上传成功');
          }
        } else {
          createMessage.error(fileInfo.message || `${file.name} 上传失败`);
        }
      } catch (error) {
        console.error('上传处理失败', error);
        createMessage.error(`${file.name} 上传失败`);
      }
    };
    await defHttp.uploadFile({ url: '/airag/chat/upload' }, { file, data: { md5 } }, { success: isReturn });
  } catch (e) {
    console.error('上传失败', e);
    createMessage.error(`${file.name} 上传失败`);
  } finally {
    uploading.value = false;
  }
}

function captureShortcut(e: KeyboardEvent) {
  e.preventDefault();
  e.stopPropagation();

  const modKeys = ['Control', 'Alt', 'Shift', 'Meta'];
  if (modKeys.includes(e.key)) return;

  if (!e.ctrlKey && !e.altKey && !e.metaKey) {
    if (e.key === 'Backspace' || e.key === 'Delete') {
      formData.shortcutKey = '';
      return;
    }
    createMessage.info('请配合 Ctrl、Alt 或 Shift 修饰键使用');
    return;
  }

  const parts: string[] = [];
  if (e.ctrlKey) parts.push('Ctrl');
  if (e.altKey) parts.push('Alt');
  if (e.shiftKey) parts.push('Shift');
  const key = e.key.length === 1 ? e.key.toUpperCase() : e.key;
  parts.push(key);
  formData.shortcutKey = parts.join('+');
}

async function handleSubmit() {
  if (!formData.title?.trim()) {
    createMessage.warning('请输入标题/关键词');
    return;
  }
  if (formData.msgType !== 5 && !formData.content?.trim()) {
    createMessage.warning('请输入回复内容');
    return;
  }
  try {
    setModalProps({ confirmLoading: true });
    const submitData = { ...formData };
    if (isUpdate.value) {
      await defHttp.put({ url: '/cs/quickReply/edit', data: { ...submitData, id: recordId.value } });
    } else {
      await defHttp.post({ url: '/cs/quickReply/add', data: submitData });
    }
    createMessage.success(isUpdate.value ? '编辑成功' : '新增成功');
    closeModal();
    emit('success');
  } finally {
    setModalProps({ confirmLoading: false });
  }
}
</script>

<style scoped>
.shortcut-listening {
  border-color: #1890ff !important;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2) !important;
}
</style>
