<template>
  <div class="widget-preview">
    <div class="tip">右下角挂件预览，点击按钮打开</div>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted } from 'vue';

const params = new URLSearchParams(window.location.search);
const baseUrl = params.get('baseUrl') || window.location.origin;
const token = params.get('token') || '';
const externalUserId = params.get('externalUserId') || 'U1001';
const userName = params.get('userName') || 'Tom';
const source = params.get('source') || 'partnerA';

let widget: any = null;
let scriptEl: HTMLScriptElement | null = null;

function initWidget() {
  const w = window as any;
  if (!w.JeecgCsWidget || widget) {
    return;
  }
  widget = w.JeecgCsWidget.init({
    baseUrl,
    externalUserId,
    userName,
    source,
    token,
    getToken: () => Promise.resolve(token),
  });
}

onMounted(() => {
  const w = window as any;
  if (w.JeecgCsWidget) {
    initWidget();
    return;
  }
  scriptEl = document.createElement('script');
  scriptEl.src = '/cs-widget.js';
  scriptEl.onload = () => initWidget();
  document.body.appendChild(scriptEl);
});

onBeforeUnmount(() => {
  if (widget?.destroy) {
    widget.destroy();
  }
  if (scriptEl && scriptEl.parentNode) {
    scriptEl.parentNode.removeChild(scriptEl);
  }
});
</script>

<style scoped>
.widget-preview {
  min-height: 100vh;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.tip {
  color: #8c8c8c;
  font-size: 14px;
}
</style>
