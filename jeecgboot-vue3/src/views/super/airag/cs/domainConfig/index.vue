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

        <a-form-item label="下载链接">
          <div class="download-links-list">
            <div v-for="(item, idx) in downloadLinks" :key="idx" class="download-link-row">
              <a-input
                v-model:value="item.label"
                placeholder="标签（如 Windows x64）"
                style="width: 180px"
              />
              <a-input
                v-model:value="item.url"
                placeholder="下载链接 URL"
                style="flex: 1"
              />
              <a-button danger size="small" @click="removeDownloadLink(idx)">删除</a-button>
            </div>
            <a-button type="dashed" block @click="addDownloadLink" style="margin-top: 8px">
              + 添加下载链接
            </a-button>
          </div>
          <div class="form-tip">可配置多个客户端下载入口（如 Windows / Mac / Linux），将显示在首页快速接入区域。</div>
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
  remark: '',
});

// 动态下载链接列表
const downloadLinks = ref<{ label: string; url: string }[]>([]);

function addDownloadLink() {
  downloadLinks.value.push({ label: '', url: '' });
}

function removeDownloadLink(idx: number) {
  downloadLinks.value.splice(idx, 1);
}

/** 解析后端返回的下载链接数据，兼容旧 downloadUrl 字段 */
function parseDownloadLinks(data: any): { label: string; url: string }[] {
  // 优先使用新字段
  if (data.downloadLinks) {
    try {
      const parsed = typeof data.downloadLinks === 'string'
        ? JSON.parse(data.downloadLinks)
        : data.downloadLinks;
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed.map((item: any) => ({
          label: item.label || '',
          url: item.url || '',
        }));
      }
    } catch (e) {
      console.warn('解析 downloadLinks JSON 失败', e);
    }
  }
  // 兼容旧字段
  if (data.downloadUrl) {
    return [{ label: 'PC客户端', url: data.downloadUrl }];
  }
  return [];
}

async function fetchConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/domain/get' }, { isTransformResponse: false });
    const data = res?.result || res || {};
    formState.domains = data.domains || '';
    formState.remark = data.remark || '';
    downloadLinks.value = parseDownloadLinks(data);
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  saving.value = true;
  try {
    // 过滤掉标签和URL都为空的行
    const validLinks = downloadLinks.value.filter((item) => item.label.trim() || item.url.trim());
    const saveData = {
      ...formState,
      downloadLinks: validLinks.length > 0 ? JSON.stringify(validLinks) : '',
      // 兼容：同时清空旧字段
      downloadUrl: '',
    };
    await defHttp.post({ url: '/cs/domain/save', data: saveData });
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

.download-links-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.download-link-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
