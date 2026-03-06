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
        row-key="region"
        bordered
        size="middle"
        style="margin-top: 16px"
      />
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

const columns = [
  { title: '地区', dataIndex: 'region', width: 200 },
  { title: '访客数量', dataIndex: 'visitorCount', width: 120, align: 'center' as const,
    sorter: (a: any, b: any) => (a.visitorCount || 0) - (b.visitorCount || 0),
    defaultSortOrder: 'descend' as const,
  },
];

async function handleQuery() {
  if (!dateRange.value || dateRange.value.length < 2) return;
  loading.value = true;
  try {
    const res = await defHttp.get({
      url: '/cs/statistics/visitor-region',
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
