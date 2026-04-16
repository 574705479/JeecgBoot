<template>
  <div class="storage-config-page">
    <a-card title="存储桶配置" :bordered="false">
      <a-alert
        type="info"
        show-icon
        class="mb-4"
        message="生效说明"
        :description="effectiveHint"
      />

      <a-spin :spinning="loading">
        <a-form
          ref="formRef"
          :model="form"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 14 }"
          @finish="onSave"
        >
          <a-form-item label="存储类型" name="storageType" :rules="[{ required: true, message: '请选择存储类型' }]">
            <a-radio-group v-model:value="form.storageType" @change="onTypeChange">
              <a-radio value="SYSTEM">系统存储（本地目录）</a-radio>
              <a-radio value="ALIYUN">阿里云 OSS</a-radio>
              <a-radio value="TENCENT">腾讯云 COS</a-radio>
            </a-radio-group>
          </a-form-item>

          <template v-if="form.storageType === 'ALIYUN'">
            <a-divider orientation="left">阿里云</a-divider>
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
          </template>

          <template v-if="form.storageType === 'TENCENT'">
            <a-divider orientation="left">腾讯云</a-divider>
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
          </template>

          <a-form-item v-if="form.storageType !== 'SYSTEM'" label="备注">
            <a-textarea v-model:value="form.remark" :rows="2" allow-clear />
          </a-form-item>

          <a-form-item
            v-if="form.storageType === 'ALIYUN'"
            label="全球加速"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-space align="start">
              <a-switch v-model:checked="form.aliyunTransferAccel" />
              <span class="text-gray-500 text-sm">默认关闭，开启后文件上传、下载将使用传输加速域名（请先在阿里云 OSS 控制台为该 Bucket 开启「传输加速」）。</span>
            </a-space>
          </a-form-item>

          <a-form-item
            v-if="form.storageType === 'TENCENT'"
            label="全球加速"
            :label-col="{ span: 6 }"
            :wrapper-col="{ span: 14 }"
          >
            <a-space align="start">
              <a-switch v-model:checked="form.tencentGlobalAccel" />
              <span class="text-gray-500 text-sm">默认关闭，开启后文件上传、下载将使用全球加速域名（请先在腾讯云 COS 控制台为该 Bucket 开启「全球加速」）。</span>
            </a-space>
          </a-form-item>

          <a-form-item v-if="vo.updateTime" :wrapper-col="{ offset: 6 }">
            <span class="text-gray-500 text-sm">最近保存：{{ vo.updateTime }} {{ vo.updateBy ? `（${vo.updateBy}）` : '' }}</span>
          </a-form-item>

          <a-form-item :wrapper-col="{ offset: 6 }">
            <a-space>
              <a-button type="primary" html-type="submit" :loading="saving">保存</a-button>
              <a-button @click="loadConfig">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import type { SelectProps } from 'ant-design-vue';
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import type { FormInstance } from 'ant-design-vue';
import { useMessage } from '/@/hooks/web/useMessage';
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

const effectiveHint = computed(() => {
  const s = vo.value.effectiveSource;
  if (s === 'database') {
    return '当前已保存数据库配置，上传与在线导入等将按此处「存储类型」执行。';
  }
  return `当前未保存数据库配置，全站沿用 application 中的 jeecg.uploadType（当前为：${vo.value.ymlUploadType || '-'}），上传根目录：${vo.value.ymlUploadPath || '-'}`;
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
.storage-config-page {
  padding: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}
.w-full {
  width: 100%;
}
</style>
