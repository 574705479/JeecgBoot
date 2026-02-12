<template>
  <div class="cs-domain-config">
    <a-card title="域名配置" :bordered="false">
      <a-form layout="vertical" :model="formState">
        <a-form-item label="域名列表" name="domains">
          <a-textarea
            v-model:value="formState.domains"
            placeholder="每行输入一个域名，例如：&#10;example.com&#10;cs.example.com&#10;https://www.example.com"
            :autoSize="{ minRows: 6, maxRows: 15 }"
          />
          <div class="form-tip">每行一个域名，系统将使用这些域名拼接接入密钥生成接入链接，显示在首页快速接入区域。</div>
        </a-form-item>

        <a-form-item label="桌面端下载链接" name="downloadUrl">
          <a-input
            v-model:value="formState.downloadUrl"
            placeholder="例如：https://download.example.com/cs-client.exe"
          />
          <div class="form-tip">PC客户端下载地址，将显示在首页快速接入区域。</div>
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="formState.remark"
            placeholder="可选备注信息"
            :autoSize="{ minRows: 2, maxRows: 5 }"
          />
        </a-form-item>

        <a-space>
          <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
          <a-button @click="handleReload" :loading="loading">刷新</a-button>
        </a-space>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts" name="CsDomainConfigPage">
import { onMounted, reactive, ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

defineOptions({ name: 'CsDomainConfigPage' });

const { createMessage } = useMessage();
const loading = ref(false);
const saving = ref(false);

const formState = reactive({
  domains: '',
  downloadUrl: '',
  remark: '',
});

async function fetchConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/domain/get' }, { isTransformResponse: false });
    const data = res?.result || res || {};
    formState.domains = data.domains || '';
    formState.downloadUrl = data.downloadUrl || '';
    formState.remark = data.remark || '';
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  saving.value = true;
  try {
    await defHttp.post({ url: '/cs/domain/save', data: { ...formState } });
    createMessage.success('保存成功');
  } catch (e) {
    createMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleReload() {
  await fetchConfig();
}

onMounted(async () => {
  await fetchConfig();
});
</script>

<style lang="less" scoped>
.cs-domain-config {
  padding: 16px;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}
</style>
