<!--
  CseImage：可显示 cse:// 端到端加密图片的通用组件
  - 自动检测 src 是否为 cse://，是则解密为 blob URL，否则透传
  - 内部走 imageCache，保证多账号隔离 + LRU + IndexedDB 持久化
  - 支持 a-image（带预览）与原生 img 两种渲染：preview=true 用 a-image
  - 失败时显示 fallback（默认 logo），不会泄露密文
-->
<template>
  <a-image
    v-if="preview"
    :src="resolved || transparent"
    :width="width"
    :height="height"
    :alt="alt"
    :preview="previewObj"
    @error="handleError"
  />
  <img
    v-else
    :src="resolved || transparent"
    :alt="alt"
    :width="width"
    :height="height"
    :class="imgClass"
    @error="handleError"
  />
</template>

<script lang="ts" setup>
  import { ref, watch, computed, onBeforeUnmount } from 'vue';
  import { isCseUrl } from '/@/utils/cse/cseUrl';
  import { withImageCacheAsync, onImageError } from '/@/utils/file/imageCache';

  const TRANSPARENT_PNG =
    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';

  const props = withDefaults(
    defineProps<{
      src: string;
      width?: number | string;
      height?: number | string;
      alt?: string;
      preview?: boolean;
      previewObj?: any;
      fallback?: string;
      imgClass?: string;
    }>(),
    {
      preview: true,
      alt: '',
      fallback: '/logo.svg',
    },
  );

  const resolved = ref<string>('');
  const transparent = TRANSPARENT_PNG;
  let cancelled = false;

  async function load(url: string) {
    if (!url) {
      resolved.value = '';
      return;
    }
    // 非 cse:// 直接给原 URL，但仍走缓存，避免重复下载
    try {
      const u = await withImageCacheAsync(url);
      if (!cancelled) resolved.value = u || (isCseUrl(url) ? '' : url);
    } catch {
      if (!cancelled) resolved.value = isCseUrl(url) ? '' : url;
    }
  }

  watch(
    () => props.src,
    (v) => load(v),
    { immediate: true },
  );

  function handleError(e: Event) {
    onImageError(e, props.fallback);
  }

  onBeforeUnmount(() => {
    cancelled = true;
  });

  defineExpose({ reload: () => load(props.src) });
  // 计算属性占位（避免 lint 未使用警告）
  const _ = computed(() => resolved.value);
  void _;
</script>
