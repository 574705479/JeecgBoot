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
  // 不在 App.vue.onMounted 立刻 remove visitor-loading：
  //   1. ChatMain 还在 await bootstrap → messages 是空 → skeleton 消失会出现一段灰底空白，造成 CLS。
  //   2. 由 ChatMain 在第一批消息渲染后通过 window.__visitorReady() 主动淡出，平滑接管。
  //
  // 兜底：即使 ChatMain 没能调到（极端异常），8 秒后强制移除，避免 skeleton 一直挡在用户面前。
  (window as any).__visitorReady = () => {
    const el = document.getElementById('visitor-loading');
    if (!el) return;
    el.style.transition = 'opacity 180ms ease-out';
    el.style.opacity = '0';
    el.style.pointerEvents = 'none';
    setTimeout(() => {
      try { el.remove(); } catch {}
    }, 220);
  };
  setTimeout(() => {
    try { (window as any).__visitorReady?.(); } catch {}
  }, 8000);
});
</script>

<style>
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
}
</style>
