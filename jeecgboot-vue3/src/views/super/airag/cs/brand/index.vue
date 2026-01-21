<template>
  <div class="cs-brand-config">
    <a-card title="品牌配置" :bordered="false">
      <a-form layout="vertical" :model="formState">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="系统名称" name="appTitle" required>
              <a-input :value="formState.appTitle" placeholder="例如：客服系统" @update:value="val => (formState.appTitle = val)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="系统简称" name="appShortTitle">
              <a-input :value="formState.appShortTitle" placeholder="例如：客服系统" @update:value="val => (formState.appShortTitle = val)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="登录页副标题" name="appSubtitle">
              <a-input :value="formState.appSubtitle" placeholder="例如：欢迎使用客服系统" @update:value="val => (formState.appSubtitle = val)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="加载页文案" name="loadingTitle">
              <a-input :value="formState.loadingTitle" placeholder="例如：客服系统" @update:value="val => (formState.loadingTitle = val)" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Logo 图片" name="logoUrl">
              <a-space align="start">
                <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'logoUrl')">
                  <a-button>上传Logo</a-button>
                </a-upload>
                <a-input :value="formState.logoUrl" placeholder="/logo.svg 或 https://..." @update:value="val => (formState.logoUrl = val)" />
              </a-space>
              <div v-if="formState.logoUrl" class="brand-preview">
                <img :src="getPreviewUrl(formState.logoUrl)" alt="logo" />
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="浏览器图标" name="faviconUrl">
              <a-space align="start">
                <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'faviconUrl')">
                  <a-button>上传图标</a-button>
                </a-upload>
                <a-input :value="formState.faviconUrl" placeholder="/logo.svg 或 https://..." @update:value="val => (formState.faviconUrl = val)" />
              </a-space>
              <div v-if="formState.faviconUrl" class="brand-preview">
                <img :src="getPreviewUrl(formState.faviconUrl)" alt="favicon" />
              </div>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="登录页背景图" name="loginBgUrl">
              <a-space align="start">
                <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'loginBgUrl')">
                  <a-button>上传背景</a-button>
                </a-upload>
                <a-input :value="formState.loginBgUrl" placeholder="可选，图片地址" @update:value="val => (formState.loginBgUrl = val)" />
              </a-space>
              <div v-if="formState.loginBgUrl" class="brand-preview brand-preview--wide">
                <img :src="getPreviewUrl(formState.loginBgUrl)" alt="login-bg" />
              </div>
            </a-form-item>
          </a-col>
        </a-row>
        <a-space>
          <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
          <a-button danger @click="handleReset" :loading="saving">恢复默认</a-button>
          <a-button @click="handleReload" :loading="loading">刷新</a-button>
        </a-space>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts" name="CsBrandConfigPage">
import { onMounted, reactive, ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { loadBrandConfig, applyBrandToDom, resolveBrandUrl } from '/@/utils/brand';
import { BRAND_STORAGE_KEY, DEFAULT_BRAND } from '/@/settings/brandSetting';
import { uploadImg } from '/@/api/sys/upload';

defineOptions({ name: 'CsBrandConfigPage' });

const { createMessage } = useMessage();
const loading = ref(false);
const saving = ref(false);

const formState = reactive({
  appTitle: '',
  appShortTitle: '',
  appSubtitle: '',
  logoUrl: '',
  faviconUrl: '',
  loginBgUrl: '',
  loadingTitle: '',
});

async function handleUpload(info: any, field: keyof typeof formState) {
  const file = info?.file;
  if (!file) return;
  try {
    const res: any = await uploadImg({ file }, () => {});
    const data = res?.result || res;
    const url = data?.url || data?.fileUrl || data?.path || data?.message;
    if (!url) {
      createMessage.error('上传失败：未获取到文件地址');
      return;
    }
    (formState as any)[field] = url;
    createMessage.success('上传成功');
  } catch (e) {
    createMessage.error('上传失败');
  }
}

function getPreviewUrl(url: string) {
  return resolveBrandUrl(url);
}

async function fetchConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/brand/get' }, { isTransformResponse: false });
    const data = res?.result || res || {};
    formState.appTitle = data.appTitle || '';
    formState.appShortTitle = data.appShortTitle || '';
    formState.appSubtitle = data.appSubtitle || '';
    formState.logoUrl = data.logoUrl || '';
    formState.faviconUrl = data.faviconUrl || '';
    formState.loginBgUrl = data.loginBgUrl || '';
    formState.loadingTitle = data.loadingTitle || '';
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  if (!formState.appTitle) {
    createMessage.warning('系统名称不能为空');
    return;
  }
  saving.value = true;
  try {
    await defHttp.post({ url: '/cs/brand/save', data: { ...formState } });
    const brand = {
      title: formState.appTitle,
      shortTitle: formState.appShortTitle,
      subtitle: formState.appSubtitle,
      logoUrl: formState.logoUrl,
      faviconUrl: formState.faviconUrl,
      loginBgUrl: formState.loginBgUrl,
      loadingTitle: formState.loadingTitle,
    };
    (window as any).__APP_BRAND__ = Object.assign({}, (window as any).__APP_BRAND__ || {}, brand);
    window.localStorage.setItem(BRAND_STORAGE_KEY, JSON.stringify(brand));
    applyBrandToDom(brand);
    createMessage.success('保存成功');
  } finally {
    saving.value = false;
  }
}

async function handleReload() {
  await fetchConfig();
}

async function handleReset() {
  formState.appTitle = DEFAULT_BRAND.title;
  formState.appShortTitle = DEFAULT_BRAND.shortTitle;
  formState.appSubtitle = DEFAULT_BRAND.subtitle;
  formState.logoUrl = DEFAULT_BRAND.logoUrl;
  formState.faviconUrl = DEFAULT_BRAND.faviconUrl;
  formState.loginBgUrl = DEFAULT_BRAND.loginBgUrl;
  formState.loadingTitle = DEFAULT_BRAND.loadingTitle;
  await handleSave();
}

onMounted(async () => {
  await fetchConfig();
  await loadBrandConfig();
});
</script>

<style lang="less" scoped>
.cs-brand-config {
  padding: 16px;
}

.brand-preview {
  margin-top: 8px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 6px;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fafafa;
}

.brand-preview img {
  max-width: 100%;
  max-height: 100%;
}

.brand-preview--wide {
  width: 320px;
  height: 120px;
}
</style>
