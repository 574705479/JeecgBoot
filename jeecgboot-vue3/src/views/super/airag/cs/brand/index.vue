<template>
  <div class="cs-brand-config">
    <div class="brand-layout">
      <div class="brand-form">
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
                <a-form-item label="Logo 图片（建议 PNG/SVG）" name="logoUrl">
                  <CropperUpload
                    v-model:value="formState.logoUrl"
                    :uploadApi="uploadImg"
                    :circled="true"
                    :aspectRatio="1"
                    btnText="上传 Logo"
                    :showInput="true"
                    inputPlaceholder="/logo.svg 或 https://..."
                    previewClass="brand-preview-circle"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="浏览器图标（建议 PNG/ICO/SVG）" name="faviconUrl">
                  <CropperUpload
                    v-model:value="formState.faviconUrl"
                    :uploadApi="uploadImg"
                    :aspectRatio="1"
                    btnText="上传图标"
                    :showInput="true"
                    inputPlaceholder="/logo.svg 或 https://..."
                    accept="image/png,image/x-icon,image/svg+xml"
                    previewClass="brand-preview-favicon"
                  />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="登录页背景图（建议 1920×1080 以上）" name="loginBgUrl">
                  <CropperUpload
                    v-model:value="formState.loginBgUrl"
                    :uploadApi="uploadImg"
                    :aspectRatio="16 / 9"
                    btnText="上传背景"
                    :showInput="true"
                    inputPlaceholder="可选，图片地址"
                    previewClass="brand-preview-wide"
                  />
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

      <div class="brand-preview">
        <div class="browser-frame">
          <div class="browser-chrome">
            <div class="browser-tab-bar">
              <div class="browser-dots">
                <span class="dot dot-red"></span>
                <span class="dot dot-yellow"></span>
                <span class="dot dot-green"></span>
              </div>
              <div class="browser-tab">
                <img :src="previewFaviconUrl" class="tab-favicon" />
                <span class="tab-title">登录 - {{ formState.appTitle || '客服系统' }}</span>
              </div>
            </div>
            <div class="browser-address-bar">
              <svg class="address-lock" viewBox="0 0 16 16" width="12" height="12"><path fill="#5a5a5a" d="M8 1a3.5 3.5 0 0 0-3.5 3.5V6H4a1 1 0 0 0-1 1v7a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V7a1 1 0 0 0-1-1h-.5V4.5A3.5 3.5 0 0 0 8 1zm2 5H6V4.5a2 2 0 1 1 4 0V6z"/></svg>
              <span class="address-text">yourdomain.com/login</span>
            </div>
          </div>
          <div ref="previewScaleRef" class="login-preview-scale" :style="{ height: `${750 * previewScale}px` }">
            <div class="login-preview-inner" :style="{ ...previewBgStyle, transform: `scale(${previewScale})` }">
              <div class="lp-card">
                <div class="lp-header">
                  <div class="lp-avatar-ring">
                    <img :src="previewLogoUrl" alt="logo" class="lp-logo-img" />
                  </div>
                  <div class="lp-title">{{ formState.appTitle || '客服系统' }}</div>
                  <div v-if="formState.appSubtitle" class="lp-subtitle">{{ formState.appSubtitle }}</div>
                </div>
                <div class="lp-form">
                  <div class="lp-input-group">
                    <div class="lp-input"><span class="lp-input-icon">👤</span><span class="lp-input-text">用户名</span></div>
                    <div class="lp-input"><span class="lp-input-icon">🔒</span><span class="lp-input-text">密码</span></div>
                    <div class="lp-input lp-input-captcha">
                      <span class="lp-input-icon">🛡️</span>
                      <span class="lp-input-text">验证码</span>
                      <div class="lp-captcha-box">ABCD</div>
                    </div>
                  </div>
                  <div class="lp-options"><span class="lp-checkbox">☑</span> 记住我</div>
                  <div class="lp-status"><span class="lp-status-dot"></span> 在线登录</div>
                  <div class="lp-btn">登 录</div>
                </div>
                <div class="lp-footer">Powered by {{ formState.appTitle || '客服系统' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="CsBrandConfigPage">
import { computed, onMounted, onBeforeUnmount, reactive, ref, nextTick } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { loadBrandConfig, applyBrandToDom, resolveBrandUrl } from '/@/utils/brand';
import { BRAND_STORAGE_KEY, DEFAULT_BRAND } from '/@/settings/brandSetting';
import { uploadImg } from '/@/api/sys/upload';
import CropperUpload from '/@/components/Cropper/src/CropperUpload.vue';

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

const previewLogoUrl = computed(() => {
  return resolveBrandUrl(formState.logoUrl || '/logo.svg');
});

const previewFaviconUrl = computed(() => {
  return resolveBrandUrl(formState.faviconUrl || '/logo.svg');
});

const previewBgStyle = computed(() => {
  const base: any = {
    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  };
  if (formState.loginBgUrl) {
    const bgUrl = resolveBrandUrl(formState.loginBgUrl);
    base.backgroundImage = `url(${bgUrl})`;
    base.backgroundSize = 'cover';
    base.backgroundPosition = 'center';
  }
  return base;
});

const previewScaleRef = ref<HTMLElement | null>(null);
const previewScale = ref(0.5);
let resizeObserver: ResizeObserver | null = null;

function updatePreviewScale() {
  const el = previewScaleRef.value;
  if (!el) return;
  const containerWidth = el.clientWidth;
  previewScale.value = containerWidth / 1200;
}

function setupResizeObserver() {
  const el = previewScaleRef.value;
  if (!el) return;
  resizeObserver = new ResizeObserver(() => updatePreviewScale());
  resizeObserver.observe(el);
  updatePreviewScale();
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
  await nextTick();
  setupResizeObserver();
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
});
</script>

<style lang="less" scoped>
.cs-brand-config {
  padding: 16px;
}

.brand-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.brand-form {
  flex: 0 0 520px;
  min-width: 0;
}

.brand-preview {
  flex: 1;
  min-width: 0;
  position: sticky;
  top: 16px;
}

/* ---- CropperUpload 预览类 ---- */
:deep(.brand-preview-circle) {
  border-radius: 50%;
  overflow: hidden;
  width: 80px;
  height: 80px;

  img {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    object-fit: cover;
  }
}

:deep(.brand-preview-favicon) {
  width: 48px;
  height: 48px;

  img {
    width: 48px;
    height: 48px;
    object-fit: contain;
  }
}

:deep(.brand-preview-wide) {
  max-width: 320px;
  max-height: 180px;

  img {
    max-width: 320px;
    max-height: 180px;
    object-fit: cover;
  }
}

/* ---- 浏览器 Chrome 模拟 ---- */
.browser-frame {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.browser-chrome {
  background: #e2e2e2;
  padding: 0;
  user-select: none;
}

.browser-tab-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px 0;
  background: #dedede;
}

.browser-dots {
  display: flex;
  gap: 6px;
  padding: 0 4px;
  flex-shrink: 0;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.dot-red { background: #ff5f57; }
.dot-yellow { background: #ffbd2e; }
.dot-green { background: #28c840; }

.browser-tab {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f1f1;
  border-radius: 8px 8px 0 0;
  padding: 6px 14px;
  max-width: 220px;
  min-width: 0;
}

.tab-favicon {
  width: 14px;
  height: 14px;
  object-fit: contain;
  flex-shrink: 0;
  border-radius: 2px;
}

.tab-title {
  font-size: 11px;
  color: #444;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
}

.browser-address-bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 12px;
  padding: 5px 10px;
  background: #fff;
  border-radius: 6px;
  margin-top: 6px;
  margin-bottom: 8px;
}

.address-lock {
  flex-shrink: 0;
  opacity: 0.6;
}

.address-text {
  font-size: 12px;
  color: #555;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ---- 登录页预览 ---- */
.login-preview-scale {
  width: 100%;
  overflow: hidden;
  position: relative;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-preview-inner {
  transform-origin: top left;
  width: 1200px;
  height: 750px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  top: 0;
  left: 0;
}

.lp-card {
  width: 380px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 36px 32px 26px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 2;
}

.lp-header {
  text-align: center;
  margin-bottom: 24px;
}

.lp-avatar-ring {
  width: 74px;
  height: 74px;
  margin: 0 auto 14px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 3px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
}

.lp-logo-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  background: #fff;
  display: block;
}

.lp-title {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.lp-subtitle {
  font-size: 12px;
  color: #888;
}

.lp-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lp-input-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.lp-input {
  display: flex;
  align-items: center;
  background: #f4f6fb;
  border-radius: 12px;
  padding: 0 14px;
  height: 44px;
}

.lp-input-icon {
  font-size: 14px;
  margin-right: 10px;
  opacity: 0.5;
}

.lp-input-text {
  color: #aaa;
  font-size: 14px;
}

.lp-input-captcha {
  .lp-input-text {
    flex: 1;
  }
}

.lp-captcha-box {
  width: 80px;
  height: 30px;
  background: #e8e8e8;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  color: #666;
  letter-spacing: 4px;
  font-family: monospace;
}

.lp-options {
  font-size: 13px;
  color: #666;
}

.lp-checkbox {
  color: #667eea;
  margin-right: 4px;
}

.lp-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #52c41a;
}

.lp-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #52c41a;
}

.lp-btn {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  text-align: center;
  padding: 12px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 8px;
  cursor: default;
}

.lp-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 11px;
  color: #bbb;
}
</style>
