<template>
  <div class="message-board-settings">
    <a-card title="留言板设置" :bordered="false">
      <a-spin :spinning="loading">
        <!-- 副标题 -->
        <div class="config-section">
          <div class="section-title">留言板副标题：</div>
          <a-input 
            v-model:value="config.subtitle" 
            placeholder="请输入留言板副标题" 
            style="max-width: 500px;"
            @change="debounceSave"
          />
          <div class="section-tip">当所有客服不在线时，访客端将显示留言板，此处设置留言板的副标题文案</div>
        </div>

        <a-divider />

        <!-- 留言表单字段配置 -->
        <div class="config-section">
          <div class="section-title">留言表单字段配置：</div>
          <div class="section-tip" style="margin-bottom: 12px;">配置访客留言时可填写的字段，以及各字段是否必填</div>
          <a-table 
            :dataSource="fieldList" 
            :columns="fieldColumns" 
            :pagination="false"
            bordered
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'label'">
                {{ record.label }}
              </template>
              <template v-if="column.dataIndex === 'show'">
                <a-switch 
                  v-model:checked="config.fields[record.key].show" 
                  size="small"
                  @change="onFieldChange(record.key)"
                />
              </template>
              <template v-if="column.dataIndex === 'required'">
                <a-switch 
                  v-model:checked="config.fields[record.key].required" 
                  size="small"
                  :disabled="!config.fields[record.key].show"
                  @change="saveConfig"
                />
              </template>
            </template>
          </a-table>
        </div>

        <a-divider />

        <a-button type="primary" @click="saveConfig" :loading="saving">保存设置</a-button>
      </a-spin>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { decryptTransport } from '../utils/csEncrypt';

const { createMessage: message } = useMessage();

const loading = ref(false);
const saving = ref(false);
let saveTimer: ReturnType<typeof setTimeout> | null = null;

const config = ref({
  subtitle: '客服不在线，请留言',
  fields: {
    name:   { show: true, required: true },
    phone:  { show: true, required: false },
    email:  { show: true, required: false },
    qq:     { show: false, required: false },
    wechat: { show: false, required: false },
    image:  { show: true, required: false },
  } as Record<string, { show: boolean; required: boolean }>,
});

const fieldList = [
  { key: 'name', label: '姓名' },
  { key: 'phone', label: '手机' },
  { key: 'email', label: '邮箱' },
  { key: 'qq', label: 'QQ' },
  { key: 'wechat', label: '微信' },
  { key: 'image', label: '图片' },
];

const fieldColumns = [
  { title: '字段', dataIndex: 'label', width: 120 },
  { title: '是否显示', dataIndex: 'show', width: 120, align: 'center' as const },
  { title: '是否必填', dataIndex: 'required', width: 120, align: 'center' as const },
];

onMounted(async () => {
  await loadConfig();
});

async function loadConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/message-board' });
    let rawData = res?.result || res;
    if (typeof rawData === 'string') {
      const decrypted = decryptTransport(rawData);
      try { rawData = JSON.parse(decrypted); } catch { rawData = null; }
    }
    const data = rawData;
    if (data) {
      config.value.subtitle = data.subtitle || '客服不在线，请留言';
      if (data.fields) {
        for (const key of Object.keys(config.value.fields)) {
          if (data.fields[key]) {
            config.value.fields[key] = {
              show: data.fields[key].show === true,
              required: data.fields[key].required === true,
            };
          }
        }
      }
    }
  } catch (e) {
    console.error('加载留言板设置失败', e);
  } finally {
    loading.value = false;
  }
}

function onFieldChange(key: string) {
  // 如果关闭显示，同时取消必填
  if (!config.value.fields[key].show) {
    config.value.fields[key].required = false;
  }
  saveConfig();
}

function debounceSave() {
  if (saveTimer) {
    clearTimeout(saveTimer);
  }
  saveTimer = setTimeout(() => saveConfig(), 500);
}

async function saveConfig() {
  saving.value = true;
  try {
    await defHttp.put({
      url: '/cs/agent/global/message-board',
      data: config.value,
    });
    message.success('保存成功');
  } catch (e) {
    console.error('保存留言板设置失败', e);
    message.error('保存失败');
  } finally {
    saving.value = false;
  }
}

onBeforeUnmount(() => {
  if (saveTimer) {
    clearTimeout(saveTimer);
    saveTimer = null;
  }
});
</script>

<style lang="less" scoped>
.message-board-settings {
  padding: 16px;
  max-width: 800px;

  .config-section {
    .section-title {
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 12px;
    }

    .section-tip {
      margin-top: 8px;
      color: #999;
      font-size: 12px;
    }
  }
}
</style>
