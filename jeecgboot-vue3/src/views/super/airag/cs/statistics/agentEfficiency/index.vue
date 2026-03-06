<template>
  <div class="cs-stat-page">
    <a-card>
      <a-space class="query-bar" :size="16">
        <span>时间范围：</span>
        <a-range-picker v-model:value="dateRange" :allow-clear="false" />
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
        <template #bodyCell="{ column, index }">
          <template v-if="column.dataIndex === 'index'">{{ index + 1 }}</template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import dayjs from 'dayjs';

const dateRange = ref<[dayjs.Dayjs, dayjs.Dayjs]>([dayjs().subtract(7, 'day'), dayjs()]);
const loading = ref(false);
const dataSource = ref<any[]>([]);

function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) return '0分';
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}小时${m}分`;
  return `${m}分`;
}

const columns = [
  { title: '序号', dataIndex: 'index', width: 60, align: 'center' as const },
  { title: '客服姓名', dataIndex: 'agentName', width: 120 },
  { title: '有效对话量', dataIndex: 'effectiveCount', width: 100, align: 'center' as const },
  { title: '首次未响应会话数', dataIndex: 'noResponseCount', width: 130, align: 'center' as const },
  { title: '对话总量', dataIndex: 'totalCount', width: 100, align: 'center' as const },
  { title: '隐身时长', dataIndex: 'invisibleDuration', width: 110, align: 'center' as const,
    customRender: ({ text }: any) => formatDuration(Number(text || 0))
  },
  { title: '离线时长', dataIndex: 'offlineDuration', width: 110, align: 'center' as const,
    customRender: ({ text }: any) => formatDuration(Number(text || 0))
  },
  { title: '及时回复率', dataIndex: 'timelyReplyRate', width: 110, align: 'center' as const,
    customRender: ({ text }: any) => `${text ?? 0}%`
  },
];

async function handleQuery() {
  if (!dateRange.value || dateRange.value.length < 2) return;
  loading.value = true;
  try {
    const res = await defHttp.get({
      url: '/cs/statistics/agent-efficiency',
      params: {
        startDate: dateRange.value[0].format('YYYY-MM-DD'),
        endDate: dateRange.value[1].format('YYYY-MM-DD'),
      },
    });
    dataSource.value = res || [];
  } catch (e) {
    console.error('查询失败', e);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.cs-stat-page { padding: 16px; }
.query-bar { margin-bottom: 8px; display: flex; align-items: center; }
</style>
