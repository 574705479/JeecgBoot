<template>
  <PageWrapper contentFullHeight>
    <div class="storage-config-page">
      <div class="storage-config-inner">
        <header class="storage-config-header">
          <h1 class="storage-config-title">存储桶配置</h1>
          <p class="storage-config-subtitle">配置全站文件上传、在线导入等使用的对象存储与访问方式</p>
        </header>

        <a-card :bordered="false" class="storage-config-card">
          <div class="effective-status-bar">
            <a-tag :color="effectiveTagColor" class="effective-tag">{{ effectiveTagText }}</a-tag>
            <div class="effective-text">
              <p class="effective-primary">{{ effectivePrimary }}</p>
              <p v-if="effectiveSecondary" class="effective-secondary">{{ effectiveSecondary }}</p>
            </div>
          </div>

          <a-spin :spinning="loading">
            <a-form
              ref="formRef"
              :model="form"
              :label-col="formLayout.labelCol"
              :wrapper-col="formLayout.wrapperCol"
              @finish="onSave"
            >
              <!-- 无「存储类型」文案标签，仅保留三卡片切换 -->
              <a-form-item
                name="storageType"
                :rules="[{ required: true, message: '请选择存储类型' }]"
                :label-col="{ span: 0 }"
                :wrapper-col="{ span: 24 }"
                class="storage-type-form-item"
              >
                <a-radio-group v-model:value="form.storageType" class="storage-type-group" @change="onTypeChange">
                  <div class="storage-type-cards">
                    <a-radio value="SYSTEM" class="storage-type-option">
                      <div class="storage-type-card-inner">
                        <div class="storage-type-head">
                          <Icon icon="ant-design:folder-open-outlined" class="storage-type-icon" />
                          <span class="storage-type-title">系统存储</span>
                        </div>
                        <div class="storage-type-desc">本地目录，适合内网或单机部署</div>
                      </div>
                    </a-radio>
                    <a-radio value="ALIYUN" class="storage-type-option">
                      <div class="storage-type-card-inner">
                        <div class="storage-type-head">
                          <Icon icon="ant-design:cloud-outlined" class="storage-type-icon" />
                          <span class="storage-type-title">阿里云 OSS</span>
                        </div>
                        <div class="storage-type-desc">对象存储与 CDN 加速</div>
                      </div>
                    </a-radio>
                    <a-radio value="TENCENT" class="storage-type-option">
                      <div class="storage-type-card-inner">
                        <div class="storage-type-head">
                          <Icon icon="ant-design:cloud-server-outlined" class="storage-type-icon" />
                          <span class="storage-type-title">腾讯云 COS</span>
                        </div>
                        <div class="storage-type-desc">对象存储与全球加速</div>
                      </div>
                    </a-radio>
                  </div>
                </a-radio-group>
              </a-form-item>

              <div v-if="form.storageType === 'SYSTEM'" class="system-storage-hint">
                <Icon icon="ant-design:info-circle-outlined" class="system-storage-hint-icon" />
                <div>
                  <div class="system-storage-hint-title">系统存储</div>
                  <p class="system-storage-hint-body">
                    使用服务器本地目录存放上传文件，无需填写云账号信息。若需对接 OSS/COS，请在上方的存储方式中选择。
                  </p>
                </div>
              </div>

              <div v-if="form.storageType === 'ALIYUN'" class="cloud-panel">
                <a-card size="small" :bordered="true" class="cloud-section-card">
                  <template #title>
                    <span class="cloud-section-title">
                      <Icon icon="ant-design:cloud-outlined" class="cloud-section-title-icon" />
                      阿里云 OSS
                    </span>
                  </template>
                  <a-form-item label="地域" name="aliyunEndpoint" :rules="[{ required: true, message: '请选择地域' }]">
                    <a-select
                      v-model:value="form.aliyunEndpoint"
                      show-search
                      allow-clear
                      option-filter-prop="label"
                      placeholder="请选择地域，可搜索"
                      :options="aliyunRegionSelectOptions"
                      class="w-full"
                    />
                  </a-form-item>
                  <a-form-item label="Bucket" name="aliyunBucket" :rules="[{ required: true, message: '请输入 Bucket' }]">
                    <a-input v-model:value="form.aliyunBucket" allow-clear />
                  </a-form-item>
                  <a-form-item label="AccessKey ID" name="aliyunAccessKeyId" :rules="[{ required: true, message: '请输入 AccessKey ID' }]">
                    <a-input v-model:value="form.aliyunAccessKeyId" allow-clear />
                  </a-form-item>
                  <a-form-item label="AccessKey Secret">
                    <a-input-password v-model:value="form.aliyunAccessKeySecret" :placeholder="aliyunSecretPlaceholder" allow-clear />
                  </a-form-item>
                  <a-form-item label="角色ARN" name="aliyunRoleArn">
                    <a-input
                      v-model:value="form.aliyunRoleArn"
                      placeholder="请输入角色ARN，例如：acs:ram::1234567890:role/AliyunOSSRole"
                      allow-clear
                    />
                  </a-form-item>
                  <a-form-item label="静态/访问域名">
                    <a-input v-model:value="form.aliyunStaticDomain" placeholder="可选，CDN 或自定义域名，需以 http(s):// 开头" allow-clear />
                  </a-form-item>
                  <a-form-item label="全球加速">
                    <a-space align="start">
                      <a-switch v-model:checked="form.aliyunTransferAccel" />
                      <span class="hint-inline"
                        >默认关闭，开启后文件上传、下载将使用传输加速域名（请先在阿里云 OSS 控制台为该 Bucket 开启「传输加速」）。</span
                      >
                    </a-space>
                  </a-form-item>
                </a-card>
              </div>

              <div v-if="form.storageType === 'TENCENT'" class="cloud-panel">
                <a-card size="small" :bordered="true" class="cloud-section-card">
                  <template #title>
                    <span class="cloud-section-title">
                      <Icon icon="ant-design:cloud-server-outlined" class="cloud-section-title-icon" />
                      腾讯云 COS
                    </span>
                  </template>
                  <a-form-item label="地域" name="tencentRegion" :rules="[{ required: true, message: '请选择地域' }]">
                    <a-select
                      v-model:value="form.tencentRegion"
                      show-search
                      allow-clear
                      option-filter-prop="label"
                      placeholder="请选择地域，可搜索"
                      :options="tencentRegionSelectOptions"
                      class="w-full"
                    />
                  </a-form-item>
                  <a-form-item label="Bucket" name="tencentBucket" :rules="[{ required: true, message: '请输入 Bucket' }]">
                    <a-input v-model:value="form.tencentBucket" allow-clear />
                  </a-form-item>
                  <a-form-item label="SecretId" name="tencentSecretId" :rules="[{ required: true, message: '请输入 SecretId' }]">
                    <a-input v-model:value="form.tencentSecretId" allow-clear />
                  </a-form-item>
                  <a-form-item label="SecretKey">
                    <a-input-password v-model:value="form.tencentSecretKey" :placeholder="tencentSecretPlaceholder" allow-clear />
                  </a-form-item>
                  <a-form-item label="自定义域名">
                    <a-input v-model:value="form.tencentDomain" placeholder="可选，CDN 域名" allow-clear />
                  </a-form-item>
                  <a-form-item label="全球加速">
                    <a-space align="start">
                      <a-switch v-model:checked="form.tencentGlobalAccel" />
                      <span class="hint-inline"
                        >默认关闭，开启后文件上传、下载将使用全球加速域名（请先在腾讯云 COS 控制台为该 Bucket 开启「全球加速」）。</span
                      >
                    </a-space>
                  </a-form-item>
                </a-card>
              </div>

              <div class="storage-config-footer">
                <a-form-item v-if="vo.updateTime" :wrapper-col="footerMetaCol" class="footer-meta-item">
                  <span class="footer-meta"
                    >最近保存：{{ vo.updateTime }}{{ vo.updateBy ? `（${vo.updateBy}）` : '' }}</span
                  >
                </a-form-item>

                <a-form-item :wrapper-col="footerActionsCol" class="footer-actions-item">
                  <a-space>
                    <a-button
                      type="primary"
                      html-type="submit"
                      class="storage-primary-btn"
                      :loading="saving"
                      preIcon="ant-design:save-outlined"
                      >保存</a-button
                    >
                    <a-button @click="loadConfig" preIcon="ant-design:undo-outlined">重置</a-button>
                  </a-space>
                </a-form-item>
              </div>
            </a-form>
          </a-spin>
        </a-card>
      </div>
    </div>
  </PageWrapper>
</template>

<script setup lang="ts">
import type { SelectProps } from 'ant-design-vue';
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import type { FormInstance } from 'ant-design-vue';
import { useMessage } from '/@/hooks/web/useMessage';
import { PageWrapper } from '@/components/Page';
import Icon from '@/components/Icon';
import { useBreakpoint } from '/@/hooks/event/useBreakpoint';
import { sizeEnum } from '/@/enums/breakpointEnum';
import {
  getStorageConfig,
  saveStorageConfig,
  testStorageConnection,
  type StorageConfigSaveDTO,
  type StorageConfigVO,
} from './storageConfig.api';
import {
  ALIYUN_REGION_GROUPS,
  mergeUnknownRegion,
  normalizeAliyunEndpoint,
  normalizeTencentRegion,
  TENCENT_REGION_GROUPS,
} from './storageRegionData';

defineOptions({ name: 'SystemStorageConfig' });

const { screenRef } = useBreakpoint();

const { createMessage } = useMessage();
const loading = ref(false);
const saving = ref(false);
const vo = ref<StorageConfigVO>({});
const formRef = ref<FormInstance>();
const form = reactive<StorageConfigSaveDTO & { remark?: string }>({
  storageType: 'SYSTEM',
  remark: '',
  aliyunEndpoint: '',
  aliyunBucket: '',
  aliyunAccessKeyId: '',
  aliyunAccessKeySecret: '',
  aliyunStaticDomain: '',
  tencentRegion: '',
  tencentBucket: '',
  tencentSecretId: '',
  tencentSecretKey: '',
  tencentDomain: '',
  aliyunTransferAccel: false,
  aliyunRoleArn: '',
  tencentGlobalAccel: false,
});

const isNarrow = computed(() => {
  const s = screenRef.value;
  return s === sizeEnum.XS || s === sizeEnum.SM;
});

const formLayout = computed(() =>
  isNarrow.value
    ? { labelCol: { span: 24 }, wrapperCol: { span: 24 } }
    : { labelCol: { span: 6 }, wrapperCol: { span: 14 } },
);

const footerMetaCol = computed(() =>
  isNarrow.value ? { span: 24 } : { offset: 6, span: 14 },
);

const footerActionsCol = computed(() =>
  isNarrow.value ? { span: 24 } : { offset: 6, span: 14 },
);

const effectiveTagText = computed(() =>
  vo.value.effectiveSource === 'database' ? '数据库生效' : '未保存·沿用 YML',
);

const effectiveTagColor = computed(() => (vo.value.effectiveSource === 'database' ? 'success' : 'warning'));

const effectivePrimary = computed(() => {
  if (vo.value.effectiveSource === 'database') {
    return '当前已保存数据库配置，上传与在线导入等将按此处「存储类型」执行。';
  }
  return '当前未保存数据库配置，全站沿用 application 中的 jeecg.uploadType。';
});

const effectiveSecondary = computed(() => {
  if (vo.value.effectiveSource === 'database') {
    return '';
  }
  return `jeecg.uploadType：${vo.value.ymlUploadType || '-'}；上传根目录：${vo.value.ymlUploadPath || '-'}`;
});

const aliyunSecretPlaceholder = computed(() =>
  vo.value.aliyunSecretConfigured ? '已配置密钥，留空则不修改' : '请输入 AccessKey Secret',
);
const tencentSecretPlaceholder = computed(() =>
  vo.value.tencentSecretKeyConfigured ? '已配置密钥，留空则不修改' : '请输入 SecretKey',
);

const aliyunRegionSelectOptions = computed<SelectProps['options']>(() => {
  const merged = mergeUnknownRegion(ALIYUN_REGION_GROUPS, form.aliyunEndpoint);
  return merged.map((g) => ({
    label: g.group,
    options: g.options.map((o) => ({ label: o.label, value: o.value })),
  }));
});

const tencentRegionSelectOptions = computed<SelectProps['options']>(() => {
  const merged = mergeUnknownRegion(TENCENT_REGION_GROUPS, form.tencentRegion);
  return merged.map((g) => ({
    label: g.group,
    options: g.options.map((o) => ({ label: o.label, value: o.value })),
  }));
});

function onTypeChange() {
  form.aliyunAccessKeySecret = '';
  form.tencentSecretKey = '';
  void nextTick(() => formRef.value?.clearValidate());
}

async function loadConfig() {
  loading.value = true;
  try {
    const data = await getStorageConfig();
    vo.value = data || {};
    form.storageType = (data?.storageType as string) || 'SYSTEM';
    form.aliyunEndpoint = normalizeAliyunEndpoint(data?.aliyunEndpoint);
    form.aliyunBucket = data?.aliyunBucket || '';
    form.aliyunAccessKeyId = data?.aliyunAccessKeyId || '';
    form.aliyunStaticDomain = data?.aliyunStaticDomain || '';
    form.tencentRegion = normalizeTencentRegion(data?.tencentRegion);
    form.tencentBucket = data?.tencentBucket || '';
    form.tencentSecretId = data?.tencentSecretId || '';
    form.tencentDomain = data?.tencentDomain || '';
    form.aliyunTransferAccel = data?.aliyunTransferAccel ?? false;
    form.aliyunRoleArn = data?.aliyunRoleArn || '';
    form.tencentGlobalAccel = data?.tencentGlobalAccel ?? false;
    form.remark = data?.remark || '';
    form.aliyunAccessKeySecret = '';
    form.tencentSecretKey = '';
    await nextTick();
    formRef.value?.clearValidate();
  } catch {
    createMessage.error('加载配置失败');
  } finally {
    loading.value = false;
  }
}

async function onSave() {
  saving.value = true;
  try {
    await testStorageConnection({ ...form });
    await saveStorageConfig({ ...form });
    createMessage.success('保存成功');
    await loadConfig();
  } catch (e: any) {
    createMessage.error(e?.message || '操作失败');
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadConfig();
});
</script>

<style scoped>
/* 对齐 DESIGN.md：浅底 #f5f5f7、主色 #0071e3、标题色 #1d1d1f；本页局部覆盖 Ant Design 主色，避免改全局主题 */
/* 抵消 PageWrapper 内容区 margin，使浅灰背景铺满可视区域 */
.storage-config-page {
  min-height: 100%;
  margin: -16px;
  padding: 24px 16px 32px;
  background: #f5f5f7;
  box-sizing: border-box;
}

.storage-config-inner {
  max-width: min(980px, 100%);
  margin: 0 auto;
}

.storage-config-header {
  margin-bottom: 24px;
}

.storage-config-title {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  line-height: 1.19;
  letter-spacing: 0.2px;
  color: #1d1d1f;
}

.storage-config-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.43;
  letter-spacing: -0.2px;
  color: rgba(0, 0, 0, 0.48);
}

.storage-config-card {
  border-radius: 8px;
  box-shadow: rgba(0, 0, 0, 0.22) 3px 5px 30px 0;
}

.effective-status-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  align-items: flex-start;
  padding: 16px;
  margin-bottom: 24px;
  border-radius: 8px;
  background: #fafafc;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.effective-tag {
  flex-shrink: 0;
  margin: 0;
}

.effective-text {
  flex: 1;
  min-width: 200px;
}

.effective-primary {
  margin: 0;
  font-size: 14px;
  line-height: 1.47;
  color: #1d1d1f;
}

.effective-secondary {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.47;
  color: rgba(0, 0, 0, 0.48);
}

.storage-type-form-item {
  margin-bottom: 24px;
}

.storage-type-form-item :deep(.ant-form-item-label) {
  display: none;
}

.storage-type-group {
  width: 100%;
}

.storage-type-cards {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  align-items: stretch;
  gap: 8px;
}

.storage-type-group :deep(.storage-type-option) {
  display: inline-flex;
  width: auto;
  max-width: 100%;
  min-height: 0;
  margin: 0;
  padding: 0;
  align-items: center;
}

.storage-type-group :deep(.storage-type-option .ant-radio) {
  align-self: center;
  margin-top: 0;
  top: 0;
}

.storage-type-card-inner {
  margin-left: 8px;
  box-sizing: border-box;
  width: 208px;
  max-width: min(208px, calc(100vw - 48px));
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background 0.2s;
}

.storage-type-head {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 24px;
}

.storage-type-group :deep(.storage-type-option.ant-radio-wrapper-checked .storage-type-card-inner) {
  border-color: #0071e3;
  background: rgba(0, 113, 227, 0.06);
  box-shadow: 0 0 0 1px rgba(0, 113, 227, 0.2);
}

.storage-type-group :deep(.storage-type-option:hover .storage-type-card-inner) {
  border-color: rgba(0, 113, 227, 0.45);
}

.storage-type-icon {
  flex-shrink: 0;
  font-size: 20px;
  line-height: 1;
  color: rgba(0, 0, 0, 0.55);
}

.storage-type-group :deep(.storage-type-option.ant-radio-wrapper-checked .storage-type-icon) {
  color: #0071e3;
}

.storage-type-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.25;
  color: #1d1d1f;
}

.storage-type-desc {
  margin-top: 6px;
  padding-left: 0;
  font-size: 12px;
  line-height: 1.33;
  color: rgba(0, 0, 0, 0.48);
}

@media (max-width: 576px) {
  .storage-type-cards {
    flex-direction: column;
    align-items: stretch;
  }

  .storage-type-group :deep(.storage-type-option) {
    width: 100%;
  }

  .storage-type-card-inner {
    width: 100%;
    max-width: none;
  }
}

.system-storage-hint {
  display: flex;
  gap: 12px;
  padding: 16px;
  margin-bottom: 24px;
  border-radius: 8px;
  background: #fafafc;
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.system-storage-hint-icon {
  flex-shrink: 0;
  font-size: 18px;
  color: #0071e3;
  margin-top: 2px;
}

.system-storage-hint-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
}

.system-storage-hint-body {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.47;
  color: rgba(0, 0, 0, 0.55);
}

.cloud-panel {
  margin-bottom: 8px;
}

.cloud-section-card {
  background: #fafafc !important;
  border-radius: 8px !important;
  border-color: rgba(0, 0, 0, 0.06) !important;
}

.cloud-section-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
}

.cloud-section-title-icon {
  font-size: 18px;
  color: rgba(0, 0, 0, 0.55);
}

.hint-inline {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.48);
  line-height: 1.47;
}

.storage-config-footer {
  margin-top: 8px;
  padding-top: 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.footer-meta-item,
.footer-actions-item {
  margin-bottom: 0;
}

.footer-meta-item + .footer-actions-item {
  margin-top: 12px;
}

.footer-meta {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.48);
}

.w-full {
  width: 100%;
}

.storage-primary-btn {
  background: #0071e3 !important;
  border-color: #0071e3 !important;
}

.storage-primary-btn:hover,
.storage-primary-btn:focus {
  background: #0077ed !important;
  border-color: #0077ed !important;
}

.storage-primary-btn:focus-visible {
  outline: 2px solid #0071e3;
  outline-offset: 2px;
}
</style>
