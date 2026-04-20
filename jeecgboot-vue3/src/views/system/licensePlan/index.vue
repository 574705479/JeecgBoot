<template>
  <div class="pp">
    <div class="pp-shell">
      <div class="pp-page-head">
        <div>
          <div class="pp-page-eyebrow">Products &amp; Pricing</div>
          <h1>产品和价格</h1>
          <p>了解各套餐的功能范围与服务内容，选择最适合您团队的方案。</p>
        </div>
      </div>

      <a-spin :spinning="loading">
        <template v-if="plans.length === 0 && !loading">
          <div class="pp-empty">
            <AppstoreOutlined class="pp-empty-icon" />
            <div class="pp-empty-title">暂无套餐信息</div>
            <div class="pp-empty-desc">当前没有可用的套餐说明，请联系管理员配置。</div>
          </div>
        </template>

        <div v-else class="pp-grid">
          <div
            v-for="plan in plans"
            :key="plan.planCode"
            class="pp-card"
            :class="{ 'pp-card--active': currentPlanCode === plan.planCode }"
          >
            <div v-if="currentPlanCode === plan.planCode" class="pp-card-badge">
              <CheckCircleOutlined /> 当前套餐
            </div>
            <div class="pp-card-header">
              <h2 class="pp-card-title">{{ plan.planName }}</h2>
            </div>
            <div class="pp-card-body">
              <div v-if="plan.description" class="pp-rich-content" v-html="plan.description" v-cse-html></div>
              <div v-else class="pp-no-desc">暂无功能说明</div>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { AppstoreOutlined, CheckCircleOutlined } from '@ant-design/icons-vue';

const loading = ref(true);
const plans = ref<any[]>([]);
const currentPlanCode = ref('');

async function loadPlans() {
  try {
    const res = await defHttp.get({ url: '/license/plans' }, { errorMessageMode: 'none' });
    plans.value = Array.isArray(res) ? res : [];
  } catch {
    plans.value = [];
  }
}

async function loadCurrentPlan() {
  try {
    const res = await defHttp.get({ url: '/license/status' }, { errorMessageMode: 'none' });
    if (res?.licensed && res?.planName) {
      const matched = plans.value.find((p: any) => p.planName === res.planName);
      if (matched) {
        currentPlanCode.value = matched.planCode;
      }
    }
  } catch {
    // ignore
  }
}

onMounted(async () => {
  loading.value = true;
  await loadPlans();
  await loadCurrentPlan();
  loading.value = false;
});
</script>

<style scoped>
.pp {
  min-height: 100vh;
  background: linear-gradient(180deg, #f0f5ff 0%, #f8fafc 40%, #ffffff 100%);
  padding: 32px;
}

.pp-shell {
  max-width: 1200px;
  margin: 0 auto;
}

.pp-page-head {
  margin-bottom: 32px;
}

.pp-page-eyebrow {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #3b82f6;
}

.pp-page-head h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
  color: #0f172a;
}

.pp-page-head p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #64748b;
}

.pp-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06);
}

.pp-empty-icon {
  font-size: 48px;
  color: #94a3b8;
  margin-bottom: 16px;
}

.pp-empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 8px;
}

.pp-empty-desc {
  font-size: 14px;
  color: #94a3b8;
}

.pp-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.pp-card {
  position: relative;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 20px;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.06);
  transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
  overflow: hidden;
}

.pp-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 48px rgba(15, 23, 42, 0.1);
}

.pp-card--active {
  border-color: #3b82f6;
  box-shadow: 0 8px 32px rgba(59, 130, 246, 0.12), 0 0 0 1px #3b82f6;
}

.pp-card--active:hover {
  box-shadow: 0 16px 48px rgba(59, 130, 246, 0.18), 0 0 0 1px #3b82f6;
}

.pp-card-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.3);
}

.pp-card-header {
  padding: 28px 28px 0;
}

.pp-card-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.pp-card-body {
  padding: 20px 28px 28px;
}

.pp-no-desc {
  font-size: 14px;
  color: #94a3b8;
  font-style: italic;
}

/* Rich text content rendering */
.pp-rich-content {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
}

.pp-rich-content :deep(h1) {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 12px;
  line-height: 1.3;
}

.pp-rich-content :deep(h2) {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin: 16px 0 8px;
  line-height: 1.4;
}

.pp-rich-content :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 12px 0 6px;
}

.pp-rich-content :deep(p) {
  margin: 0 0 8px;
}

.pp-rich-content :deep(ul),
.pp-rich-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.pp-rich-content :deep(li) {
  margin-bottom: 4px;
}

.pp-rich-content :deep(strong) {
  font-weight: 600;
  color: #0f172a;
}

.pp-rich-content :deep(hr) {
  border: none;
  border-top: 1px solid #e2e8f0;
  margin: 16px 0;
}

.pp-rich-content :deep(blockquote) {
  margin: 8px 0;
  padding: 8px 16px;
  border-left: 3px solid #3b82f6;
  background: #f8fafc;
  border-radius: 0 8px 8px 0;
}

.pp-rich-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
}

.pp-rich-content :deep(th),
.pp-rich-content :deep(td) {
  border: 1px solid #e2e8f0;
  padding: 8px 12px;
  text-align: left;
}

.pp-rich-content :deep(th) {
  background: #f1f5f9;
  font-weight: 600;
}
</style>
