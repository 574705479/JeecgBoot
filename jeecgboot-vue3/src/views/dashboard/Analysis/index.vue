<template>
  <div class="cs-dashboard">
    <!-- ========== 第一行：实时数据统计卡片 ========== -->
    <a-row :gutter="[16, 16]" class="stat-row">
      <a-col :xs="12" :sm="8" :md="6" :lg="4" :xl="3" v-for="item in statCards" :key="item.key">
        <div class="stat-card" :style="{ borderTop: `3px solid ${item.color}` }">
          <div class="stat-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="stat-label">{{ item.label }}</div>
        </div>
      </a-col>
    </a-row>

    <!-- ========== 第二行：快速接入 + 基础信息 ========== -->
    <a-row :gutter="16" class="info-row">
      <!-- 左侧：快速接入 -->
      <a-col :xs="24" :lg="16">
        <a-card title="快速接入" :bordered="false" class="info-card">
          <div class="quick-access-content">
            <!-- 接入链接列表 -->
            <div class="access-links" v-if="accessLinks.length > 0">
              <div class="section-title">接入链接</div>
              <div v-for="(link, idx) in accessLinks" :key="idx" class="access-link-item">
                <a-tag color="blue">{{ link.domain }}</a-tag>
                <a-typography-paragraph :copyable="{ text: getFullUrl(idx) }" class="link-text">
                  {{ getFullUrl(idx) }}
                </a-typography-paragraph>
                <a-select
                  v-model:value="selectedAgentMap[idx]"
                  placeholder="默认分配"
                  allowClear
                  style="width: 140px; flex-shrink: 0"
                  size="small"
                >
                  <a-select-option v-for="agent in agentList" :key="agent.id" :value="agent.id">
                    {{ agent.nickname }}
                  </a-select-option>
                </a-select>
              </div>
            </div>
            <a-empty v-else description="请先在【域名配置】中配置域名，在【接入设置】中生成接入密钥" :image="simpleImage" />

            <a-divider v-if="accessLinks.length > 0" />

            <!-- 二维码 + 下载链接 -->
            <a-row :gutter="16" v-if="accessLinks.length > 0">
              <a-col :span="8">
                <div class="section-title">对话二维码</div>
                <div class="qr-wrapper">
                  <a-select
                    v-if="accessLinks.length > 1"
                    v-model:value="selectedQrLinkIdx"
                    size="small"
                    style="width: 140px; margin-bottom: 8px"
                  >
                    <a-select-option v-for="(link, idx) in accessLinks" :key="idx" :value="idx">
                      {{ link.domain }}
                    </a-select-option>
                  </a-select>
                  <a-tag v-else color="blue" style="margin-bottom: 8px">{{ accessLinks[0]?.domain }}</a-tag>
                  <QrCode :value="getFullUrl(selectedQrLinkIdx)" :width="140" />
                  <div class="qr-tip">扫码打开对话</div>
                </div>
              </a-col>
              <a-col :span="16" v-if="parsedDownloadLinks.length > 0">
                <div class="section-title">客户端下载</div>
                <div class="download-area">
                  <div class="download-buttons">
                    <a-button
                      v-for="(dl, dlIdx) in parsedDownloadLinks"
                      :key="dlIdx"
                      type="primary"
                      :href="dl.url"
                      target="_blank"
                    >
                      <template #icon><DesktopOutlined /></template>
                      {{ dl.label || '下载' }}
                    </a-button>
                  </div>
                </div>
              </a-col>
            </a-row>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧：基础信息 -->
      <a-col :xs="24" :lg="8">
        <a-card title="基础信息" :bordered="false" class="info-card">
          <a-descriptions :column="1" size="middle">
            <a-descriptions-item label="系统版本">
              <a-tag color="green">{{ licenseInfo.version }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="坐席数量">
              <span class="seat-count">
                <span class="seat-used">{{ stats.onlineAgents || 0 }}</span>
                <span class="seat-divider"> / </span>
                <span class="seat-total">{{ licenseInfo.maxAgents }}</span>
              </span>
            </a-descriptions-item>
            <a-descriptions-item label="有效期">
              <a-tag color="blue">{{ licenseInfo.expireDate }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="在线客服">
              <a-badge :status="stats.onlineAgents > 0 ? 'success' : 'default'" />
              {{ stats.onlineAgents || 0 }} 在线
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>

    <!-- ========== 第三行：坐席实时状态 ========== -->
    <a-card title="坐席实时状态" :bordered="false" class="agent-status-card">
      <a-table
        :columns="agentColumns"
        :dataSource="agentStatusList"
        :pagination="false"
        :loading="agentLoading"
        rowKey="agentId"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-badge
              :status="record.status === 1 ? 'success' : record.status === 2 ? 'warning' : record.status === 0 ? 'error' : 'default'"
              :text="getStatusText(record.status)"
            />
          </template>
          <template v-if="column.dataIndex === 'goodRate'">
            {{ record.goodRate }}%
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts" name="CsDashboardAnalysis">
import { onMounted, reactive, ref, computed, onUnmounted } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { DesktopOutlined } from '@ant-design/icons-vue';
import { Empty } from 'ant-design-vue';
import QrCode from '/@/components/Qrcode/src/Qrcode.vue';

defineOptions({ name: 'CsDashboardAnalysis' });

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;

// ==================== 统计数据 ====================
const stats = reactive({
  activeConversations: 0,
  avgResponseTime: 0,
  todayEffectiveConversations: 0,
  todayVisitors: 0,
  todayConversations: 0,
  todayLeaveMessages: 0,
  onlineAgents: 0,
  totalAgents: 0,
});

const statCards = computed(() => [
  { key: 'active', label: '实时对话量', value: stats.activeConversations, color: '#1890ff' },
  { key: 'avgResp', label: '平均响应时长(秒)', value: stats.avgResponseTime, color: '#722ed1' },
  { key: 'effective', label: '今日有效对话量', value: stats.todayEffectiveConversations, color: '#52c41a' },
  { key: 'visitors', label: '今日访客量', value: stats.todayVisitors, color: '#fa8c16' },
  { key: 'conversations', label: '今日对话量', value: stats.todayConversations, color: '#13c2c2' },
  { key: 'messages', label: '留言量', value: stats.todayLeaveMessages, color: '#eb2f96' },
  { key: 'agents', label: '在线客服', value: `${stats.onlineAgents}/${stats.totalAgents}`, color: '#2f54eb' },
]);

async function fetchStats() {
  try {
    const res = await defHttp.get({ url: '/cs/dashboard/stats' }, { isTransformResponse: false });
    const data = res?.result || res || {};
    Object.assign(stats, data);
  } catch (e) {
    console.error('[Dashboard] 获取统计数据失败', e);
  }
}

// ==================== 域名配置 + 接入链接 ====================
const domainConfig = reactive({
  domains: '',
  downloadUrl: '',
  downloadLinks: '',
});
const secretKey = ref('');
const agentList = ref<any[]>([]);
const selectedAgentMap = reactive<Record<number, string | undefined>>({});
const selectedQrLinkIdx = ref(0);

function getFullUrl(idx: number): string {
  const link = accessLinks.value[idx];
  if (!link) return '';
  const agentId = selectedAgentMap[idx];
  return agentId ? `${link.fullUrl}&agentId=${agentId}` : link.fullUrl;
}

const accessLinks = computed(() => {
  if (!domainConfig.domains || !secretKey.value) return [];
  const domains = domainConfig.domains.split('\n').map((d) => d.trim()).filter(Boolean);
  return domains.map((domain) => {
    const base = domain.startsWith('http') ? domain : `https://${domain}`;
    const fullUrl = `${base}/cs/userChat?key=${secretKey.value}`;
    return { domain, fullUrl };
  });
});

/** 解析下载链接列表，兼容旧 downloadUrl 字段 */
const parsedDownloadLinks = computed(() => {
  if (domainConfig.downloadLinks) {
    try {
      const parsed = typeof domainConfig.downloadLinks === 'string'
        ? JSON.parse(domainConfig.downloadLinks)
        : domainConfig.downloadLinks;
      if (Array.isArray(parsed) && parsed.length > 0) {
        return parsed.filter((item: any) => item.url);
      }
    } catch (e) {
      console.warn('[Dashboard] 解析 downloadLinks 失败', e);
    }
  }
  // 兼容旧字段
  if (domainConfig.downloadUrl) {
    return [{ label: 'PC客户端', url: domainConfig.downloadUrl }];
  }
  return [];
});

async function fetchDomainConfig() {
  try {
    const res = await defHttp.get({ url: '/cs/domain/get' }, { isTransformResponse: false });
    const data = res?.result || res || {};
    domainConfig.domains = data.domains || '';
    domainConfig.downloadUrl = data.downloadUrl || '';
    domainConfig.downloadLinks = data.downloadLinks || '';
  } catch (e) {
    console.error('[Dashboard] 获取域名配置失败', e);
  }
}

async function fetchSecretKey() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/visitor-access' }, { isTransformResponse: false });
    const data = res?.result || res || {};
    secretKey.value = data.secretKey || '';
  } catch (e) {
    console.error('[Dashboard] 获取接入密钥失败', e);
  }
}

async function fetchAgentList() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/list', params: { pageNo: 1, pageSize: 100 } }, { isTransformResponse: false });
    const data = res?.result || res || {};
    agentList.value = data.records || [];
  } catch (e) {
    console.error('[Dashboard] 获取客服列表失败', e);
  }
}

// ==================== 授权信息 ====================
const licenseInfo = reactive({
  version: 'V1.0.0',
  maxAgents: '不限' as string | number,
  expireDate: '永久授权',
});

async function fetchLicenseInfo() {
  try {
    const res = await defHttp.get(
      { url: '/license/status' },
      { isTransformResponse: false }
    );
    const data = res?.result || res || {};
    if (data.licensed) {
      const maxCsAgents = data.quotas?.max_cs_agents;
      licenseInfo.maxAgents = maxCsAgents && maxCsAgents > 0 ? maxCsAgents : '不限';
      licenseInfo.expireDate = data.expireDate
        ? data.expireDate.substring(0, 10)
        : '永久授权';
    }
  } catch {}
}

// ==================== 坐席实时状态 ====================
const agentStatusList = ref<any[]>([]);
const agentLoading = ref(false);

const agentColumns = [
  { title: '客服账号', dataIndex: 'userId', key: 'userId', width: 120 },
  { title: '客服昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '正在对话', dataIndex: 'currentSessions', key: 'currentSessions', width: 90, align: 'center' },
  { title: '好评量', dataIndex: 'goodCount', key: 'goodCount', width: 80, align: 'center' },
  { title: '好评率', dataIndex: 'goodRate', key: 'goodRate', width: 80, align: 'center' },
  { title: '在线时长', dataIndex: 'onlineDuration', key: 'onlineDuration', width: 100, align: 'center' },
  { title: '坐席状态', dataIndex: 'status', key: 'status', width: 100, align: 'center' },
];

function getStatusText(status: number) {
  const map: Record<number, string> = { 0: '离线', 1: '在线', 2: '忙碌', 3: '隐身' };
  return map[status] || '未知';
}

async function fetchAgentStatus() {
  agentLoading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/dashboard/agent-status' }, { isTransformResponse: false });
    agentStatusList.value = res?.result || res || [];
  } catch (e) {
    console.error('[Dashboard] 获取坐席状态失败', e);
  } finally {
    agentLoading.value = false;
  }
}

// ==================== 自动刷新 ====================
let refreshTimer: ReturnType<typeof setInterval> | null = null;

onMounted(async () => {
  await Promise.all([fetchStats(), fetchDomainConfig(), fetchSecretKey(), fetchAgentList(), fetchAgentStatus(), fetchLicenseInfo()]);
  // 每30秒自动刷新统计数据和坐席状态
  refreshTimer = setInterval(() => {
    fetchStats();
    fetchAgentStatus();
  }, 30000);
});

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = null;
  }
});
</script>

<style lang="less" scoped>
.cs-dashboard {
  padding: 16px;
}

// ==================== 第一行：统计卡片 ====================
.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.3s;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  }
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
}

// ==================== 第二行：快速接入 + 基础信息 ====================
.info-row {
  margin-bottom: 16px;
}

.info-card {
  height: 100%;

  :deep(.ant-card-body) {
    padding: 16px;
  }
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.access-link-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  .link-text {
    margin-bottom: 0;
    flex: 1;
    font-size: 13px;
    word-break: break-all;
  }
}

.qr-wrapper {
  text-align: center;
}

.qr-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #8c8c8c;
}

.download-area {
  .download-buttons {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.seat-count {
  font-size: 16px;
  font-weight: 600;
}

.seat-used {
  color: #1890ff;
}

.seat-divider {
  color: #d9d9d9;
}

.seat-total {
  color: #8c8c8c;
}

// ==================== 第三行：坐席状态 ====================
.agent-status-card {
  :deep(.ant-card-body) {
    padding: 0 16px 16px;
  }
}
</style>
