<template>
  <div class="data-cleanup-container">
    <a-card title="数据清理" :bordered="false">
      <a-spin :spinning="loading">
        <!-- 总开关 -->
        <div class="config-section">
          <div class="section-title">
            自动清理
            <a-switch v-model:checked="config.enabled" @change="saveConfig" style="margin-left: 12px;" />
          </div>
          <div class="setting-tips">
            <span class="desc-icon">●</span>
            开启后，系统将在每天凌晨 3:00 自动执行数据清理
          </div>
        </div>

        <a-divider />

        <!-- 第1项: 对话记录 -->
        <div class="config-section">
          <div class="section-title">对话记录</div>
          <div class="section-body">
            <div class="section-desc">
              删除已结束超过指定天数的客服会话及 AI 对话的所有记录，包括消息内容、协作者记录
            </div>
            <div class="section-warn">
              清理后"会话记录"中无法查看历史详情，AI 对话列表中超期对话消失，对应时间段的统计数据也将不可查看
            </div>
            <div class="inline-setting">
              <span class="setting-label">保留天数：</span>
              <a-input-number
                v-model:value="config.conversationDays"
                :min="1"
                :max="9999"
                style="width: 120px;"
                @change="saveConfig"
              />
              <span class="setting-unit">天</span>
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 第2项: 日志与访客 -->
        <div class="config-section">
          <div class="section-title">日志与访客</div>
          <div class="section-body">
            <div class="section-desc">
              删除超过指定天数的客服登录日志、状态日志，以及长期不活跃且无星标的访客资料
            </div>
            <div class="section-warn">
              "登录日志"无法查看超期记录，"出勤统计"超期数据丢失；不活跃访客再次访问时会创建新记录，之前的备注和标签丢失
            </div>
            <div class="inline-setting">
              <span class="setting-label">保留天数：</span>
              <a-input-number
                v-model:value="config.logAndVisitorDays"
                :min="1"
                :max="9999"
                style="width: 120px;"
                @change="saveConfig"
              />
              <span class="setting-unit">天</span>
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 第3项: 缓存与辅助数据 -->
        <div class="config-section">
          <div class="section-title">缓存与辅助数据</div>
          <div class="section-body">
            <div class="section-desc">
              删除超过指定天数的 IP 地理缓存、文件秒传记录和已回复的访客留言
            </div>
            <div class="section-warn">
              IP 缓存清理后需重新查询（无业务影响）；文件秒传记录清理后相同文件需重新上传（已发送的文件链接不受影响）；访客无法查看超期的历史留言回复
            </div>
            <div class="inline-setting">
              <span class="setting-label">保留天数：</span>
              <a-input-number
                v-model:value="config.cacheDays"
                :min="1"
                :max="9999"
                style="width: 120px;"
                @change="saveConfig"
              />
              <span class="setting-unit">天</span>
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 操作按钮 -->
        <div class="action-section">
          <a-space>
            <a-button type="primary" danger :loading="triggerLoading" @click="handleTrigger">
              立即清理
            </a-button>
            <a-button @click="showLogModal = true">
              清理日志
            </a-button>
          </a-space>
        </div>
      </a-spin>
    </a-card>

    <!-- 清理日志弹窗 -->
    <a-modal
      v-model:open="showLogModal"
      title="清理日志"
      :width="900"
      :footer="null"
      @cancel="showLogModal = false"
    >
      <a-table
        :columns="logColumns"
        :data-source="logData"
        :loading="logLoading"
        :pagination="logPagination"
        @change="handleLogPageChange"
        rowKey="id"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'triggerType'">
            <a-tag :color="record.triggerType === 'manual' ? 'blue' : 'green'">
              {{ record.triggerType === 'manual' ? '手动' : '定时' }}
            </a-tag>
          </template>
          <template v-if="column.dataIndex === 'durationMs'">
            {{ record.durationMs != null ? (record.durationMs / 1000).toFixed(1) + 's' : '-' }}
          </template>
          <template v-if="column.dataIndex === 'resultJson'">
            <a-button type="link" size="small" @click="showResultDetail(record)">查看详情</a-button>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 清理结果详情弹窗 -->
    <a-modal
      v-model:open="showResultModal"
      title="清理详情"
      :width="500"
      :footer="null"
    >
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item v-for="(value, key) in resultDetail" :key="key" :label="resultLabels[key] || key">
          {{ value }} 条
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { Modal } from 'ant-design-vue';

const { createMessage: msg } = useMessage();

const loading = ref(false);
const triggerLoading = ref(false);
let saveTimer: ReturnType<typeof setTimeout> | null = null;

const config = ref({
  enabled: true,
  conversationDays: 90,
  logAndVisitorDays: 90,
  cacheDays: 180,
});

const resultLabels: Record<string, string> = {
  conversation: '客服会话',
  csMessage: '客服消息',
  collaborator: '协作者记录',
  aiMessage: 'AI 对话消息',
  aiRedisKey: 'AI 会话缓存',
  softDeletedMessage: '软删除消息',
  loginLog: '登录日志',
  statusLog: '状态日志',
  visitor: '不活跃访客',
  ipGeoCache: 'IP 地理缓存',
  fileHash: '文件哈希记录',
  leaveMessage: '已回复留言',
};

// ==================== 配置 ====================

onMounted(async () => {
  await loadConfig();
});

async function loadConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/data-cleanup' });
    const data = res?.result || res;
    if (data) {
      config.value = {
        enabled: data.enabled !== false,
        conversationDays: data.conversationDays ?? 90,
        logAndVisitorDays: data.logAndVisitorDays ?? 90,
        cacheDays: data.cacheDays ?? 180,
      };
    }
  } catch (e) {
    console.error('加载清理配置失败', e);
  } finally {
    loading.value = false;
  }
}

function saveConfig() {
  if (saveTimer) clearTimeout(saveTimer);
  saveTimer = setTimeout(async () => {
    try {
      await defHttp.put({ url: '/cs/agent/global/data-cleanup', data: config.value });
      msg.success('保存成功');
    } catch (e) {
      console.error('保存清理配置失败', e);
      msg.error('保存失败');
    }
  }, 500);
}

// ==================== 手动触发 ====================

function handleTrigger() {
  Modal.confirm({
    title: '确认立即清理',
    content: '将按当前配置的保留天数执行数据清理，清理后数据不可恢复。确定继续？',
    okText: '确认清理',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      triggerLoading.value = true;
      try {
        const res = await defHttp.post({ url: '/cs/agent/global/data-cleanup/trigger' }, { timeout: 300000 });
        const data = res?.result || res;
        if (data && Object.keys(data).length > 0) {
          resultDetail.value = data;
          showResultModal.value = true;
          msg.success('清理完成');
        } else {
          msg.warning('清理任务正在执行中，请稍后查看清理日志');
        }
      } catch (e: any) {
        msg.error(e?.message || '清理失败');
      } finally {
        triggerLoading.value = false;
      }
    },
  });
}

// ==================== 清理日志 ====================

const showLogModal = ref(false);
const logLoading = ref(false);
const logData = ref<any[]>([]);
const logPagination = reactive({ current: 1, pageSize: 10, total: 0 });

const logColumns = [
  { title: '触发方式', dataIndex: 'triggerType', width: 80 },
  { title: '开始时间', dataIndex: 'startTime', width: 170 },
  { title: '耗时', dataIndex: 'durationMs', width: 80 },
  { title: '触发人', dataIndex: 'createBy', width: 100 },
  { title: '详情', dataIndex: 'resultJson', width: 100 },
];

const showResultModal = ref(false);
const resultDetail = ref<Record<string, number>>({});

import { watch } from 'vue';

watch(showLogModal, (val) => {
  if (val) {
    logPagination.current = 1;
    loadLogs();
  }
});

async function loadLogs() {
  logLoading.value = true;
  try {
    const res = await defHttp.get({
      url: '/cs/agent/global/data-cleanup/logs',
      params: { pageNo: logPagination.current, pageSize: logPagination.pageSize },
    });
    const data = res?.result || res;
    logData.value = data?.records || [];
    logPagination.total = data?.total || 0;
  } catch (e) {
    console.error('加载清理日志失败', e);
  } finally {
    logLoading.value = false;
  }
}

function handleLogPageChange(pag: any) {
  logPagination.current = pag.current;
  logPagination.pageSize = pag.pageSize;
  loadLogs();
}

function showResultDetail(record: any) {
  try {
    resultDetail.value = JSON.parse(record.resultJson || '{}');
    showResultModal.value = true;
  } catch {
    resultDetail.value = {};
    showResultModal.value = true;
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
.data-cleanup-container {
  padding: 16px;
  max-width: 900px;

  .config-section {
    .section-title {
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 12px;
      display: flex;
      align-items: center;
    }

    .section-body {
      padding-left: 16px;
    }

    .section-desc {
      color: #666;
      font-size: 13px;
      margin-bottom: 6px;
      line-height: 1.6;
    }

    .section-warn {
      color: #fa8c16;
      font-size: 12px;
      margin-bottom: 12px;
      line-height: 1.6;

      &::before {
        content: '⚠ ';
      }
    }

    .inline-setting {
      display: flex;
      align-items: center;
      gap: 8px;

      .setting-label {
        font-size: 14px;
      }

      .setting-unit {
        font-size: 13px;
        color: #666;
      }
    }

    .setting-tips {
      color: #999;
      font-size: 12px;
      margin-top: 4px;

      .desc-icon {
        margin-right: 4px;
      }
    }
  }

  .action-section {
    display: flex;
    justify-content: flex-start;
  }
}
</style>
