<template>
  <div class="cs-stat-page">
    <a-card>
      <a-space class="query-bar" :size="16">
        <span>查询日期：</span>
        <a-date-picker v-model:value="queryDate" :allow-clear="false" />
        <a-button type="primary" :loading="loading" @click="handleQuery">查询</a-button>
      </a-space>
      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="false"
        row-key="agentId"
        bordered
        size="middle"
        style="margin-top: 16px"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'action'">
            <a-button type="link" size="small" @click="showDetail(record)">详情</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:open="detailVisible"
      :title="`${detailAgent} - ${queryDate?.format('YYYY-MM-DD')} 出勤详情`"
      :footer="null"
      width="700px"
    >
      <a-table
        :columns="detailColumns"
        :data-source="detailData"
        :loading="detailLoading"
        :pagination="false"
        row-key="startTime"
        bordered
        size="small"
      >
        <template #bodyCell="{ column, text }">
          <template v-if="column.dataIndex === 'statusText'">
            <a-tag :color="getStatusColor(text)">{{ text }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import dayjs from 'dayjs';

const queryDate = ref<dayjs.Dayjs>(dayjs());
const loading = ref(false);
const dataSource = ref<any[]>([]);

const detailVisible = ref(false);
const detailLoading = ref(false);
const detailData = ref<any[]>([]);
const detailAgent = ref('');

function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) return '0分';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}小时${m}分`;
  return `${m}分`;
}

function getStatusColor(status: string) {
  if (status === '在线') return 'green';
  if (status === '离线') return 'red';
  if (status === '隐身') return 'orange';
  return 'default';
}

function formatTime(val: string) {
  if (!val) return '-';
  return dayjs(val).format('HH:mm:ss');
}

const columns = [
  { title: '客服姓名', dataIndex: 'agentName', width: 120 },
  { title: '在线时长', dataIndex: 'onlineDuration', width: 110, align: 'center' as const,
    customRender: ({ text }: any) => formatDuration(Number(text || 0))
  },
  { title: '离线时长', dataIndex: 'offlineDuration', width: 110, align: 'center' as const,
    customRender: ({ text }: any) => formatDuration(Number(text || 0))
  },
  { title: '隐身时长', dataIndex: 'invisibleDuration', width: 110, align: 'center' as const,
    customRender: ({ text }: any) => formatDuration(Number(text || 0))
  },
  { title: '最早登录时间', dataIndex: 'earliestLogin', width: 120, align: 'center' as const,
    customRender: ({ text }: any) => formatTime(text)
  },
  { title: '最晚离线时间', dataIndex: 'latestLogout', width: 120, align: 'center' as const,
    customRender: ({ text }: any) => formatTime(text)
  },
  { title: '操作', dataIndex: 'action', width: 80, align: 'center' as const },
];

const detailColumns = [
  { title: '状态', dataIndex: 'statusText', width: 80, align: 'center' as const },
  { title: '开始时间', dataIndex: 'startTime', width: 160,
    customRender: ({ text }: any) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'
  },
  { title: '结束时间', dataIndex: 'endTime', width: 160,
    customRender: ({ text }: any) => text ? dayjs(text).format('YYYY-MM-DD HH:mm:ss') : '-'
  },
  { title: '持续时长', dataIndex: 'durationSeconds', width: 120, align: 'center' as const,
    customRender: ({ text }: any) => formatDuration(Number(text || 0))
  },
];

async function handleQuery() {
  if (!queryDate.value) return;
  loading.value = true;
  try {
    const res = await defHttp.get({
      url: '/cs/statistics/attendance',
      params: { queryDate: queryDate.value.format('YYYY-MM-DD') },
    });
    dataSource.value = res || [];
  } catch (e) {
    console.error('查询失败', e);
  } finally {
    loading.value = false;
  }
}

async function showDetail(record: any) {
  detailAgent.value = record.agentName || '';
  detailVisible.value = true;
  detailLoading.value = true;
  try {
    const res = await defHttp.get({
      url: '/cs/statistics/attendance/detail',
      params: {
        agentId: record.agentId,
        queryDate: queryDate.value.format('YYYY-MM-DD'),
      },
    });
    detailData.value = res || [];
  } catch (e) {
    console.error('查询详情失败', e);
  } finally {
    detailLoading.value = false;
  }
}
</script>

<style scoped>
.cs-stat-page { padding: 16px; }
.query-bar { margin-bottom: 8px; display: flex; align-items: center; }
</style>
