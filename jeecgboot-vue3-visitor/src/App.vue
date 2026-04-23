<template>
  <ConfigProvider :locale="zhCN" :theme="antdTheme">
    <RouterView />
  </ConfigProvider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { ConfigProvider } from 'ant-design-vue';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';
import { initImageCache } from '/@/utils/file/imageCache';
import { loadBrandConfig } from '/@/utils/brand';

dayjs.locale('zh-cn');

const antdTheme = {
  token: {
    colorPrimary: '#1677ff',
  },
};

onMounted(() => {
  // 初始化图片缓存（异步，不阻塞首屏）
  initImageCache().catch(() => {});
  // 拉品牌（失败不影响业务，使用默认 logo/title）
  loadBrandConfig().catch(() => {});
  // 移除 index.html 里的占位 loading
  const el = document.getElementById('visitor-loading');
  if (el) el.remove();
});
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
