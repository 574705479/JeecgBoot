<template>
  <div class="lp">
    <div class="lp-shell">
      <div class="lp-page-head">
        <div>
          <div class="lp-page-eyebrow">License Center</div>
          <h1>授权中心</h1>
          <p>统一查看当前系统授权状态、许可范围与关键操作。</p>
        </div>
      </div>

      <a-spin :spinning="loading">
        <template v-if="!licenseInfo.licensed">
          <div class="lp-empty-card">
            <div class="lp-empty-visual">
              <div class="lp-empty-icon"><LockOutlined /></div>
            </div>
            <div class="lp-empty-content">
              <div class="lp-empty-tag">授权状态</div>
              <h2>系统尚未激活许可证</h2>
              <p>当前系统未检测到有效授权信息，请联系管理员获取许可证密钥并完成激活。</p>
              <div class="lp-empty-points">
                <div class="lp-empty-point">激活后可恢复功能授权与配额能力识别</div>
                <div class="lp-empty-point">支持后续刷新授权与许可证注销操作</div>
              </div>
              <a-button type="primary" size="large" @click="router.push('/license/activate')">
                <template #icon><KeyOutlined /></template>
                去激活授权
              </a-button>
            </div>
          </div>
        </template>

        <template v-else>
          <section class="lp-hero" :class="heroStatusClass">
            <div class="lp-hero-top">
              <div class="lp-hero-main">
                <div class="lp-hero-badge">
                  <SafetyCertificateOutlined v-if="licenseInfo.status === 'ACTIVE'" />
                  <ClockCircleOutlined v-else-if="licenseInfo.status === 'EXPIRED'" />
                  <StopOutlined v-else-if="licenseInfo.status === 'REVOKED'" />
                  <PauseCircleOutlined v-else-if="licenseInfo.status === 'SUSPENDED'" />
                  <QuestionCircleOutlined v-else />
                </div>
                <div class="lp-hero-copy">
                  <div class="lp-hero-kicker">当前授权概览</div>
                  <div class="lp-hero-title">{{ statusText[licenseInfo.status] || '授权状态待确认' }}</div>
                  <div class="lp-hero-desc">{{ remainingText }}</div>
                </div>
              </div>

              <div class="lp-hero-actions">
                <div class="lp-status-pill">{{ statusText[licenseInfo.status] || '未知状态' }}</div>
                <a-button class="lp-refresh-btn" @click="handleRefresh" :loading="refreshing">
                  <template #icon><SyncOutlined /></template>
                  刷新授权
                </a-button>
              </div>
            </div>

            <div class="lp-hero-meta">
              <div class="lp-hero-meta-item">
                <span class="lp-hero-meta-label">客户</span>
                <span class="lp-hero-meta-value">{{ customerDisplay }}</span>
              </div>
              <div class="lp-hero-meta-item">
                <span class="lp-hero-meta-label">授权套餐</span>
                <span class="lp-hero-meta-value">{{ planDisplay }}</span>
              </div>
              <div class="lp-hero-meta-item">
                <span class="lp-hero-meta-label">到期时间</span>
                <span class="lp-hero-meta-value">{{ expireDisplay }}</span>
              </div>
              <div class="lp-hero-meta-item">
                <span class="lp-hero-meta-label">已授权功能</span>
                <span class="lp-hero-meta-value">{{ featureCount }}</span>
              </div>
              <div class="lp-hero-meta-item">
                <span class="lp-hero-meta-label">配额项</span>
                <span class="lp-hero-meta-value">{{ quotaCount }}</span>
              </div>
            </div>
          </section>

          <div v-if="statusNotice" class="lp-notice" :class="heroStatusClass">
            <WarningOutlined class="lp-notice-icon" />
            <div>
              <div class="lp-notice-title">{{ statusNotice.title }}</div>
              <div class="lp-notice-text">{{ statusNotice.text }}</div>
            </div>
          </div>

          <section class="lp-layout">
            <div class="lp-main">
              <div class="lp-card">
                <div class="lp-card-head">
                  <div class="lp-card-title-wrap">
                    <KeyOutlined class="lp-card-icon" />
                    <div>
                      <div class="lp-card-title">许可证信息</div>
                      <div class="lp-card-subtitle">查看当前许可证密钥，并用于排查或人工核验</div>
                    </div>
                  </div>
                  <a-tooltip title="复制许可证密钥">
                    <a-button type="text" size="small" @click="copyKey">
                      <template #icon><CopyOutlined /></template>
                    </a-button>
                  </a-tooltip>
                </div>

                <div class="lp-license-key">
                  <code class="lp-key-code">{{ licenseInfo.licenseKey || '--' }}</code>
                </div>

                <div class="lp-key-notes">
                  <div class="lp-key-note">
                    <span class="lp-key-note-title">展示说明</span>
                    <span class="lp-key-note-text">页面中的许可证密钥已脱敏处理，可直接复制用于核验。</span>
                  </div>
                  <div class="lp-key-note">
                    <span class="lp-key-note-title">刷新建议</span>
                    <span class="lp-key-note-text">如果套餐、功能或配额刚发生变更，建议刷新授权后再查看最新状态。</span>
                  </div>
                </div>
              </div>

              <div class="lp-card">
                <div class="lp-card-head lp-card-head--stack">
                  <div class="lp-card-title-wrap">
                    <DashboardOutlined class="lp-card-icon" />
                    <div>
                      <div class="lp-card-title">配额信息</div>
                      <div class="lp-card-subtitle">当前许可证定义的容量、实例和资源上限</div>
                    </div>
                  </div>
                </div>

                <div v-if="quotaList.length" class="lp-quota-grid">
                  <div v-for="item in quotaList" :key="item.key" class="lp-quota-card">
                    <div class="lp-quota-name">{{ item.name }}</div>
                    <div class="lp-quota-val">{{ item.limit === '不限' ? '∞' : item.limit }}</div>
                  </div>
                </div>
                <div v-else class="lp-no-data">当前许可证未返回配额数据</div>
              </div>
            </div>

            <div class="lp-side">
              <div class="lp-card">
                <div class="lp-card-head lp-card-head--stack">
                  <div class="lp-card-title-wrap">
                    <AppstoreOutlined class="lp-card-icon" />
                    <div>
                      <div class="lp-card-title">已授权功能</div>
                      <div class="lp-card-subtitle">当前环境可启用的功能模块与能力范围</div>
                    </div>
                  </div>
                </div>

                <div v-if="featureList.length" class="lp-feature-list">
                  <div v-for="f in featureList" :key="f.code" class="lp-feature-item">
                    <div class="lp-feature-icon">
                      <CheckCircleOutlined />
                    </div>
                    <div class="lp-feature-copy">
                      <div class="lp-feature-name">{{ f.name }}</div>
                      <div class="lp-feature-code">{{ f.code }}</div>
                    </div>
                  </div>
                </div>
                <div v-else class="lp-no-data">当前许可证未返回功能授权数据</div>
              </div>

              <div class="lp-card lp-action-card">
                <div class="lp-card-head lp-card-head--stack">
                  <div class="lp-card-title-wrap">
                    <IdcardOutlined class="lp-card-icon" />
                    <div>
                      <div class="lp-card-title">维护与风险操作</div>
                      <div class="lp-card-subtitle">刷新许可证状态，或在必要时注销当前授权</div>
                    </div>
                  </div>
                </div>

                <div class="lp-action-list">
                  <div class="lp-action-tip">
                    <ThunderboltOutlined />
                    <span>建议在授权续期、套餐变更或功能范围调整后执行一次刷新。</span>
                  </div>
                  <div class="lp-action-tip lp-action-tip--warn">
                    <WarningOutlined />
                    <span>注销授权后系统将回到未授权状态，请确认已具备新的激活密钥。</span>
                  </div>
                </div>

                <div class="lp-danger-box">
                  <div class="lp-danger-title">危险操作</div>
                  <div class="lp-danger-text">该操作会使当前系统失去已激活授权，部分功能与配额可能立即失效。</div>
                  <a-button danger @click="handleDeactivate">注销当前授权</a-button>
                </div>
              </div>
            </div>
          </section>
        </template>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import {
  AppstoreOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  DashboardOutlined,
  IdcardOutlined,
  KeyOutlined,
  LockOutlined,
  PauseCircleOutlined,
  QuestionCircleOutlined,
  SafetyCertificateOutlined,
  StopOutlined,
  SyncOutlined,
  ThunderboltOutlined,
  WarningOutlined,
} from '@ant-design/icons-vue';

const router = useRouter();
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

const statusNoticeMap: Record<string, { title: string; text: string }> = {
  EXPIRED: {
    title: '授权已过期',
    text: '建议尽快更新许可证，避免功能受限或服务能力下降。',
  },
  SUSPENDED: {
    title: '授权已暂停',
    text: '当前许可证处于暂停状态，请确认套餐状态或联系管理员处理。',
  },
  REVOKED: {
    title: '授权已吊销',
    text: '当前许可证已被吊销，请更换有效许可证后重新激活。',
  },
  INACTIVE: {
    title: '授权未生效',
    text: '当前许可证尚未处于可用状态，请检查激活流程或服务器授权状态。',
  },
};

function formatDate(dateText: string) {
  if (!dateText) return '';
  return dateText.replace('T', ' ').replace(/:\d{2}$/, '');
}

const heroStatusClass = computed(() => `lp-hero--${(licenseInfo.value.status || 'inactive').toLowerCase()}`);

const daysRemaining = computed(() => {
  const expireDate = licenseInfo.value.expireDate;
  if (!expireDate) return null;
  try {
    const target = new Date(expireDate.replace('T', ' '));
    const diff = target.getTime() - Date.now();
    return Math.ceil(diff / 86400000);
  } catch {
    return null;
  }
});

const isPermanent = computed(() => !licenseInfo.value.expireDate);
const expireDisplay = computed(() => (isPermanent.value ? '永久有效' : expireDateText.value));
const customerDisplay = computed(() => licenseInfo.value.customerName || '未配置');
const planDisplay = computed(() => licenseInfo.value.planName || '未配置');
const expireDateText = computed(() => (licenseInfo.value.expireDate ? formatDate(licenseInfo.value.expireDate) : '永久有效'));

const remainingText = computed(() => {
  if (isPermanent.value) return '当前许可证为永久授权，无到期限制。';
  if (daysRemaining.value === null) return '授权到期时间待确认，请稍后刷新授权信息。';
  if (daysRemaining.value > 30) return `距离到期还有 ${daysRemaining.value} 天，当前授权状态稳定。`;
  if (daysRemaining.value > 0) return `授权临近到期，还剩 ${daysRemaining.value} 天，请提前安排续期。`;
  return '当前许可证已过期，请尽快更新许可证以恢复完整能力。';
});

const statusNotice = computed(() => statusNoticeMap[licenseInfo.value.status] || null);

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

const featureCount = computed(() => featureList.value.length);
const quotaCount = computed(() => quotaList.value.length);

function copyKey() {
  const key = licenseInfo.value.licenseKey;
  if (!key) return;
  if (!navigator?.clipboard?.writeText) {
    createMessage.warning('当前环境不支持自动复制');
    return;
  }
  navigator.clipboard
    .writeText(key)
    .then(() => {
      createMessage.success('已复制到剪贴板');
    })
    .catch(() => {
      createMessage.error('复制失败');
    });
}

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
    createMessage.success('授权信息已刷新');
    setTimeout(() => window.location.reload(), 600);
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
.lp {
  min-height: 100%;
  padding: 24px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.12), transparent 32%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.08), transparent 24%),
    linear-gradient(180deg, #f4f7fb 0%, #eef3f8 100%);
}

.lp-shell {
  max-width: 1240px;
  margin: 0 auto;
}

.lp-page-head {
  margin-bottom: 20px;
}

.lp-page-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #3b82f6;
}

.lp-page-head h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
  color: #0f172a;
}

.lp-page-head p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #64748b;
}

.lp-card,
.lp-empty-card,
.lp-notice {
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06);
}

.lp-card,
.lp-empty-card {
  background: rgba(255, 255, 255, 0.88);
  border-radius: 24px;
  backdrop-filter: blur(12px);
}

.lp-empty-card {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  align-items: center;
  padding: 36px;
}

.lp-empty-visual {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  border-radius: 24px;
  background: linear-gradient(135deg, #eff6ff 0%, #ecfeff 100%);
}

.lp-empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96px;
  height: 96px;
  border-radius: 28px;
  background: linear-gradient(135deg, #1d4ed8 0%, #0f766e 100%);
  color: #fff;
  font-size: 42px;
  box-shadow: 0 24px 40px rgba(29, 78, 216, 0.25);
}

.lp-empty-content h2 {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 700;
  color: #0f172a;
}

.lp-empty-content p {
  max-width: 560px;
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: #64748b;
}

.lp-empty-tag {
  display: inline-flex;
  align-items: center;
  height: 30px;
  margin-bottom: 16px;
  padding: 0 12px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.lp-empty-points {
  display: grid;
  gap: 10px;
  margin: 24px 0 28px;
}

.lp-empty-point {
  position: relative;
  padding-left: 18px;
  color: #334155;
  font-size: 14px;
}

.lp-empty-point::before {
  content: '';
  position: absolute;
  top: 9px;
  left: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6 0%, #14b8a6 100%);
}

.lp-hero {
  position: relative;
  overflow: hidden;
  margin-bottom: 18px;
  padding: 28px;
  border-radius: 28px;
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8 0%, #0f766e 100%);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.14);
}

.lp-hero::before,
.lp-hero::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.lp-hero::before {
  top: -48px;
  right: -40px;
  width: 220px;
  height: 220px;
  background: rgba(255, 255, 255, 0.08);
}

.lp-hero::after {
  right: 220px;
  bottom: -88px;
  width: 180px;
  height: 180px;
  background: rgba(255, 255, 255, 0.06);
}

.lp-hero--active {
  background: linear-gradient(135deg, #1d4ed8 0%, #0f766e 100%);
}

.lp-hero--expired {
  background: linear-gradient(135deg, #b45309 0%, #92400e 100%);
}

.lp-hero--revoked {
  background: linear-gradient(135deg, #b91c1c 0%, #7f1d1d 100%);
}

.lp-hero--suspended {
  background: linear-gradient(135deg, #ca8a04 0%, #a16207 100%);
}

.lp-hero--inactive {
  background: linear-gradient(135deg, #475569 0%, #334155 100%);
}

.lp-hero-top,
.lp-hero-meta {
  position: relative;
  z-index: 1;
}

.lp-hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.lp-hero-main {
  display: flex;
  gap: 18px;
  align-items: center;
  min-width: 0;
}

.lp-hero-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 34px;
  flex-shrink: 0;
}

.lp-hero-copy {
  min-width: 0;
}

.lp-hero-kicker {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.lp-hero-title {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.15;
  color: #fff;
}

.lp-hero-desc {
  max-width: 620px;
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.82);
}

.lp-hero-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: flex-end;
  flex-shrink: 0;
}

.lp-status-pill {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  font-size: 13px;
  font-weight: 600;
  color: #fff;
}

.lp-refresh-btn {
  height: 40px !important;
  padding: 0 16px !important;
  border: 1px solid rgba(255, 255, 255, 0.18) !important;
  border-radius: 12px !important;
  background: rgba(255, 255, 255, 0.12) !important;
  color: #fff !important;
  box-shadow: none !important;
}

.lp-refresh-btn:hover {
  background: rgba(255, 255, 255, 0.18) !important;
}

.lp-hero-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.lp-hero-meta-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.1);
}

.lp-hero-meta-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.68);
}

.lp-hero-meta-value {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
}

.lp-notice {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.8);
}

.lp-notice-icon {
  margin-top: 2px;
  font-size: 18px;
}

.lp-notice-title {
  margin-bottom: 4px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.lp-notice-text {
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.lp-notice.lp-hero--expired,
.lp-notice.lp-hero--suspended {
  border-color: rgba(245, 158, 11, 0.22);
  background: rgba(255, 251, 235, 0.92);
  color: #92400e;
}

.lp-notice.lp-hero--revoked {
  border-color: rgba(239, 68, 68, 0.22);
  background: rgba(254, 242, 242, 0.95);
  color: #991b1b;
}

.lp-notice.lp-hero--inactive {
  border-color: rgba(100, 116, 139, 0.2);
  background: rgba(248, 250, 252, 0.95);
  color: #334155;
}

.lp-notice.lp-hero--expired .lp-notice-title,
.lp-notice.lp-hero--suspended .lp-notice-title,
.lp-notice.lp-hero--revoked .lp-notice-title,
.lp-notice.lp-hero--inactive .lp-notice-title {
  color: inherit;
}

.lp-notice.lp-hero--expired .lp-notice-text,
.lp-notice.lp-hero--suspended .lp-notice-text,
.lp-notice.lp-hero--revoked .lp-notice-text,
.lp-notice.lp-hero--inactive .lp-notice-text {
  color: inherit;
  opacity: 0.85;
}

.lp-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.lp-main,
.lp-side {
  display: grid;
  gap: 18px;
  align-content: start;
}

.lp-card {
  padding: 22px;
}

.lp-card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.lp-card-head--stack {
  margin-bottom: 20px;
}

.lp-card-title-wrap {
  display: flex;
  gap: 12px;
  min-width: 0;
}

.lp-card-icon {
  margin-top: 2px;
  font-size: 18px;
  color: #2563eb;
}

.lp-card-title {
  font-size: 17px;
  font-weight: 700;
  color: #0f172a;
}

.lp-card-subtitle {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.lp-license-key {
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fbff 0%, #eff6ff 100%);
}

.lp-key-code {
  display: block;
  overflow-x: auto;
  font-family: 'SF Mono', 'Fira Code', 'Cascadia Code', ui-monospace, monospace;
  font-size: 14px;
  line-height: 1.8;
  color: #1e293b;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.lp-key-notes {
  display: grid;
  gap: 12px;
}

.lp-key-note {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fafc;
}

.lp-key-note-title {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: #64748b;
}

.lp-key-note-text {
  font-size: 14px;
  line-height: 1.7;
  color: #0f172a;
}

.lp-quota-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.lp-quota-card {
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease;
}

.lp-quota-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
}

.lp-quota-name {
  margin-bottom: 10px;
  font-size: 13px;
  color: #64748b;
}

.lp-quota-val {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.1;
  color: #0f172a;
}

.lp-feature-list {
  display: grid;
  gap: 12px;
}

.lp-feature-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fafc;
  transition:
    transform 0.2s ease,
    background 0.2s ease;
}

.lp-feature-item:hover {
  transform: translateY(-1px);
  background: #f1f5f9;
}

.lp-feature-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: rgba(16, 185, 129, 0.12);
  color: #10b981;
  flex-shrink: 0;
}

.lp-feature-copy {
  min-width: 0;
}

.lp-feature-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.lp-feature-code {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  word-break: break-all;
}

.lp-action-card {
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.96) 100%);
}

.lp-action-list {
  display: grid;
  gap: 12px;
}

.lp-action-tip {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 14px 16px;
  border-radius: 16px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 13px;
  line-height: 1.7;
}

.lp-action-tip--warn {
  background: #fff7ed;
  color: #c2410c;
}

.lp-danger-box {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid rgba(239, 68, 68, 0.16);
  border-radius: 20px;
  background: linear-gradient(180deg, #fff5f5 0%, #fff1f2 100%);
}

.lp-danger-title {
  font-size: 15px;
  font-weight: 700;
  color: #991b1b;
}

.lp-danger-text {
  margin: 8px 0 16px;
  font-size: 13px;
  line-height: 1.7;
  color: #b91c1c;
}

.lp-no-data {
  padding: 28px 12px;
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
}

@media (max-width: 1180px) {
  .lp-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .lp-empty-card {
    grid-template-columns: 1fr;
    padding: 28px;
  }

  .lp-empty-visual {
    min-height: 180px;
  }

  .lp-hero-top {
    flex-direction: column;
  }

  .lp-hero-actions {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }
}

@media (max-width: 640px) {
  .lp {
    padding: 16px;
  }

  .lp-page-head h1 {
    font-size: 26px;
  }

  .lp-hero {
    padding: 22px 18px;
  }

  .lp-hero-main {
    align-items: flex-start;
  }

  .lp-hero-badge {
    width: 58px;
    height: 58px;
    border-radius: 18px;
    font-size: 26px;
  }

  .lp-hero-title {
    font-size: 28px;
  }

  .lp-hero-actions,
  .lp-hero-meta,
  .lp-quota-grid {
    grid-template-columns: 1fr;
  }

  .lp-hero-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .lp-status-pill,
  .lp-refresh-btn {
    width: 100%;
    justify-content: center;
  }

  .lp-card,
  .lp-empty-card {
    border-radius: 20px;
  }
}
</style>
