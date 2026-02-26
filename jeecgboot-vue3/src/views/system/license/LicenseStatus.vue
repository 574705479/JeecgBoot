<template>
  <div class="license-status-page">
    <a-spin :spinning="loading">
      <template v-if="!licenseInfo.licensed">
        <a-card>
          <a-result status="warning" title="系统未授权" sub-title="请联系管理员获取许可证密钥">
            <template #extra>
              <a-button type="primary" @click="$router.push('/license/activate')">去激活</a-button>
            </template>
          </a-result>
        </a-card>
      </template>

      <template v-else>
        <a-row :gutter="16" style="margin-bottom: 16px">
          <a-col :span="6">
            <a-card size="small">
              <div class="stat-card">
                <span class="stat-title">授权状态</span>
                <span class="stat-value" :style="{ color: statusColor }">{{ statusText[licenseInfo.status] || licenseInfo.status }}</span>
              </div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card size="small">
              <div class="stat-card">
                <span class="stat-title">到期时间</span>
                <span class="stat-value">{{ licenseInfo.expireDate || '永久' }}</span>
              </div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card size="small">
              <div class="stat-card">
                <span class="stat-title">客户名称</span>
                <span class="stat-value">{{ licenseInfo.customerName || '-' }}</span>
              </div>
            </a-card>
          </a-col>
          <a-col :span="6">
            <a-card size="small">
              <div class="stat-card">
                <span class="stat-title">授权套餐</span>
                <span class="stat-value">{{ licenseInfo.planName || '-' }}</span>
              </div>
            </a-card>
          </a-col>
        </a-row>

        <a-row :gutter="16" style="margin-bottom: 16px">
          <a-col :span="12">
            <a-card title="配额信息" size="small">
              <div v-if="quotaList.length">
                <div v-for="item in quotaList" :key="item.key" class="quota-item">
                  <span class="quota-label">{{ item.name }}</span>
                  <span class="quota-value">{{ item.limit }}</span>
                </div>
              </div>
              <a-empty v-else :image="simpleImage" description="无配额数据" />
            </a-card>
          </a-col>
          <a-col :span="12">
            <a-card title="授权功能" size="small">
              <div v-if="featureList.length" class="feature-tags">
                <a-tag v-for="f in featureList" :key="f.code" color="blue">{{ f.name }}</a-tag>
              </div>
              <a-empty v-else :image="simpleImage" description="无功能数据" />
            </a-card>
          </a-col>
        </a-row>

        <a-card title="许可证信息" size="small" style="margin-bottom: 16px">
          <a-descriptions :column="2" size="small">
            <a-descriptions-item label="许可证密钥">
              {{ licenseInfo.licenseKey }}
            </a-descriptions-item>
            <a-descriptions-item label="授权状态">
              <a-tag :color="statusColor">
                {{ statusText[licenseInfo.status] || licenseInfo.status }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="客户名称">
              {{ licenseInfo.customerName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="授权套餐">
              {{ licenseInfo.planName || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="到期时间">
              {{ licenseInfo.expireDate || '永久' }}
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-space>
          <a-button type="primary" ghost @click="handleRefresh" :loading="refreshing">刷新授权</a-button>
          <a-button danger @click="handleDeactivate">注销授权</a-button>
        </a-space>
      </template>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { Empty } from 'ant-design-vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
const { createConfirm, createMessage } = useMessage();
const loading = ref(true);
const refreshing = ref(false);
const licenseInfo = ref<Record<string, any>>({});

const statusText: Record<string, string> = {
  ACTIVE: '已激活',
  INACTIVE: '未激活',
  SUSPENDED: '已暂停',
  REVOKED: '已吊销',
  EXPIRED: '已过期',
};

const statusColorMap: Record<string, string> = {
  ACTIVE: '#52c41a',
  INACTIVE: '#d9d9d9',
  SUSPENDED: '#faad14',
  REVOKED: '#ff4d4f',
  EXPIRED: '#ff7a45',
};

const statusColor = computed(() => statusColorMap[licenseInfo.value.status] || '#d9d9d9');

const quotaList = computed(() => {
  const quotas = licenseInfo.value.quotas;
  const names = licenseInfo.value.quotaNames || {};
  if (!quotas) return [];
  return Object.entries(quotas).map(([key, value]) => ({
    key,
    name: names[key] || key,
    limit: value === 0 ? '不限' : value,
  }));
});

const featureList = computed(() => {
  const features = licenseInfo.value.features;
  const names = licenseInfo.value.featureNames || {};
  if (!features || !features.length) return [];
  return features.map((code: string) => ({
    code,
    name: names[code] || code,
  }));
});

async function loadStatus() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/license/status' }, { errorMessageMode: 'none' });
    licenseInfo.value = res || {};
  } catch {
    licenseInfo.value = { licensed: false };
  } finally {
    loading.value = false;
  }
}

async function handleRefresh() {
  refreshing.value = true;
  try {
    await defHttp.post({ url: '/license/refresh' });
    createMessage.success('授权信息已刷新，页面即将重新加载...');
    setTimeout(() => {
      window.location.reload();
    }, 800);
  } catch (e: any) {
    createMessage.error(e?.message || '刷新失败');
    refreshing.value = false;
  }
}

async function handleDeactivate() {
  createConfirm({
    title: '确认注销',
    content: '注销后系统将进入未授权状态，确认继续？',
    iconType: 'warning',
    onOk: async () => {
      try {
        await defHttp.post({ url: '/license/deactivate' });
        createMessage.success('已注销授权');
        await loadStatus();
      } catch (e: any) {
        createMessage.error(e?.message || '操作失败');
      }
    },
  });
}

onMounted(loadStatus);
</script>

<style scoped>
.license-status-page {
  padding: 24px;
}

.quota-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.quota-item:last-child {
  border-bottom: none;
}

.quota-label {
  color: #666;
}

.quota-value {
  font-weight: 600;
  color: #1890ff;
}

.feature-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-title {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.45);
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  line-height: 1.2;
}
</style>
