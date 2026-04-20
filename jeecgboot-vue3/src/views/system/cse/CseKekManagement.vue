<template>
  <PageWrapper contentFullHeight>
    <div class="cse-mgmt-page">
      <div class="cse-mgmt-inner">
        <header class="cse-mgmt-header">
          <h1 class="cse-mgmt-title">文件加密管理</h1>
          <p class="cse-mgmt-subtitle">
            CSE 文件加密总开关、白名单/黑名单可视化配置，以及主密钥（KEK）的生成、激活、归档与备份。
            所有写操作均需输入超管登录密码作为二次确认。
          </p>
        </header>

        <a-tabs v-model:activeKey="activeTab" type="card" class="cse-mgmt-tabs">
          <a-tab-pane key="basic" tab="基础配置">
            <CseBasicConfig />
          </a-tab-pane>
          <a-tab-pane key="kek" tab="密钥管理">
            <CseKekPanel />
          </a-tab-pane>
        </a-tabs>
      </div>
    </div>
  </PageWrapper>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { PageWrapper } from '@/components/Page';
import CseBasicConfig from './CseBasicConfig.vue';
import CseKekPanel from './CseKekPanel.vue';

defineOptions({ name: 'CseKekManagement' });

const activeTab = ref<'basic' | 'kek'>('basic');
</script>

<style scoped>
.cse-mgmt-page {
  min-height: 100%;
  margin: -16px;
  padding: 24px 16px 32px;
  background: #f5f5f7;
  box-sizing: border-box;
}
.cse-mgmt-inner {
  max-width: min(1280px, 100%);
  margin: 0 auto;
}
.cse-mgmt-header {
  margin-bottom: 16px;
}
.cse-mgmt-title {
  margin: 0;
  font-size: 21px;
  font-weight: 700;
  line-height: 1.19;
  letter-spacing: 0.2px;
  color: #1d1d1f;
}
.cse-mgmt-subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  line-height: 1.43;
  letter-spacing: -0.2px;
  color: rgba(0, 0, 0, 0.48);
}
.cse-mgmt-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 16px;
}
.cse-mgmt-tabs :deep(.ant-tabs-tab) {
  padding: 8px 20px !important;
  font-size: 14px;
}
</style>
