<template>
  <div class="cs-statistics-page">
    <!-- 统计卡片 -->
    <a-row :gutter="16" class="stat-cards">
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日会话数" :value="stats.todayConversations" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="今日消息数" :value="stats.todayMessages" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="在线客服" :value="stats.onlineAgents" suffix="人" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="平均满意度" :value="stats.avgSatisfaction" suffix="分" :precision="1" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="16" class="chart-row">
      <a-col :span="12">
        <a-card title="会话趋势">
          <div ref="conversationChartRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="客服工作量">
          <div ref="agentChartRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" class="chart-row">
      <a-col :span="12">
        <a-card title="会话来源分布">
          <div ref="sourceChartRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="满意度分布">
          <div ref="satisfactionChartRef" style="height: 300px;"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 客服排行榜 -->
    <a-card title="客服排行榜" class="ranking-card">
      <a-table :dataSource="agentRanking" :columns="rankingColumns" :pagination="false" size="small" />
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';

const stats = ref({
  todayConversations: 0,
  todayMessages: 0,
  onlineAgents: 0,
  avgSatisfaction: 0,
});

const agentRanking = ref([
  { rank: 1, name: '客服小王', conversations: 58, messages: 320, satisfaction: 4.8, avgResponse: '15秒' },
  { rank: 2, name: '客服小李', conversations: 52, messages: 280, satisfaction: 4.6, avgResponse: '18秒' },
  { rank: 3, name: '客服小张', conversations: 45, messages: 250, satisfaction: 4.5, avgResponse: '20秒' },
]);

const rankingColumns = [
  { title: '排名', dataIndex: 'rank', width: 60 },
  { title: '客服', dataIndex: 'name', width: 100 },
  { title: '会话数', dataIndex: 'conversations', width: 80 },
  { title: '消息数', dataIndex: 'messages', width: 80 },
  { title: '满意度', dataIndex: 'satisfaction', width: 80 },
  { title: '平均响应', dataIndex: 'avgResponse', width: 100 },
];

const conversationChartRef = ref<HTMLElement>();
const agentChartRef = ref<HTMLElement>();
const sourceChartRef = ref<HTMLElement>();
const satisfactionChartRef = ref<HTMLElement>();

let charts: echarts.ECharts[] = [];

onMounted(() => {
  loadStats();
  initCharts();
});

onUnmounted(() => {
  charts.forEach(chart => chart.dispose());
});

function loadStats() {
  // TODO: 从API加载统计数据
  stats.value = {
    todayConversations: 128,
    todayMessages: 1580,
    onlineAgents: 5,
    avgSatisfaction: 4.6,
  };
}

function initCharts() {
  // 会话趋势图
  if (conversationChartRef.value) {
    const chart = echarts.init(conversationChartRef.value);
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
      },
      yAxis: { type: 'value' },
      series: [
        { name: '会话数', type: 'line', data: [120, 132, 101, 134, 90, 230, 210], smooth: true }
      ]
    });
    charts.push(chart);
  }

  // 客服工作量图
  if (agentChartRef.value) {
    const chart = echarts.init(agentChartRef.value);
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['客服A', '客服B', '客服C', '客服D', '客服E']
      },
      yAxis: { type: 'value' },
      series: [
        { name: '会话数', type: 'bar', data: [58, 52, 45, 38, 32] }
      ]
    });
    charts.push(chart);
  }

  // 来源分布图
  if (sourceChartRef.value) {
    const chart = echarts.init(sourceChartRef.value);
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: '60%',
        data: [
          { value: 45, name: '网页' },
          { value: 30, name: 'APP' },
          { value: 15, name: '微信' },
          { value: 10, name: '其他' }
        ]
      }]
    });
    charts.push(chart);
  }

  // 满意度分布图
  if (satisfactionChartRef.value) {
    const chart = echarts.init(satisfactionChartRef.value);
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: [
          { value: 60, name: '非常满意' },
          { value: 25, name: '满意' },
          { value: 10, name: '一般' },
          { value: 3, name: '不满意' },
          { value: 2, name: '非常不满意' }
        ]
      }]
    });
    charts.push(chart);
  }
}
</script>

<style lang="less" scoped>
.cs-statistics-page {
  padding: 16px;
}

.stat-cards {
  margin-bottom: 16px;
}

.chart-row {
  margin-bottom: 16px;
}

.ranking-card {
  margin-top: 16px;
}
</style>
