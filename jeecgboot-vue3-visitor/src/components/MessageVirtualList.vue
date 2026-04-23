<script setup lang="ts">
/**
 * Phase 3 Sprint 2：基于 @tanstack/vue-virtual 的消息虚拟滚动包装组件。
 *
 * <p>当前 ChatMain 的消息列表已通过 CSS `content-visibility: auto` 实现浏览器原生虚拟化，
 * 满足 200 条以内首屏 < 50ms 的目标。本组件作为"消息超长场景（>500 条）"的进阶方案，
 * 通过 JS 计算可视区，更精细控制 DOM 节点数量。</p>
 *
 * <h3>使用方式</h3>
 * <pre>
 * &lt;MessageVirtualList :items="displayMessages" :scroll-element="messagesRef" :estimate-size="80"&gt;
 *   &lt;template #default="{ item, index }"&gt;
 *     &lt;div class="message-item" :class="getMessageClass(item)"&gt;
 *       &lt;!-- 自定义渲染 --&gt;
 *     &lt;/div&gt;
 *   &lt;/template&gt;
 * &lt;/MessageVirtualList&gt;
 * </pre>
 *
 * <h3>触发时机建议</h3>
 * 配合阈值开关使用：仅当 messages.length >= 100 时启用，避免少量消息的通信开销。
 * 阈值以下的场景，CSS content-visibility 已足够。
 */
import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue';
import { useVirtualizer } from '@tanstack/vue-virtual';

const props = withDefaults(defineProps<{
  /** 数据数组 */
  items: any[];
  /** 滚动容器（外部父元素），如 chat-messages div */
  scrollElement: HTMLElement | null;
  /** 估算每项高度，用于初次布局；后续按 measureElement 精确测量 */
  estimateSize?: number;
  /** 上下预渲染条数，避免快速滚动时白屏 */
  overscan?: number;
  /** 取唯一 key 函数 */
  getKey?: (item: any, index: number) => string | number;
}>(), {
  estimateSize: 80,
  overscan: 5,
  getKey: (item: any, index: number) => item?.id ?? index,
});

const scrollElRef = ref<HTMLElement | null>(props.scrollElement);

watch(
  () => props.scrollElement,
  (val) => { scrollElRef.value = val; },
  { immediate: true }
);

const virtualizer = useVirtualizer(
  computed(() => ({
    count: props.items.length,
    getScrollElement: () => scrollElRef.value,
    estimateSize: () => props.estimateSize,
    overscan: props.overscan,
    getItemKey: (i: number) => props.getKey(props.items[i], i),
  }))
);

const virtualRows = computed(() => virtualizer.value.getVirtualItems());
const totalSize = computed(() => virtualizer.value.getTotalSize());

/**
 * 暴露 measureElement / scrollToIndex 给父组件，用于流式消息高度变化、
 * "滚动到底部"等场景。
 */
defineExpose({
  measureElement: (el: HTMLElement) => virtualizer.value.measureElement(el),
  scrollToIndex: (
    index: number,
    options?: { align?: 'auto' | 'start' | 'center' | 'end'; smoothScroll?: boolean }
  ) => virtualizer.value.scrollToIndex(index, options),
  scrollToOffset: (offset: number) => virtualizer.value.scrollToOffset(offset),
});
</script>

<template>
  <div class="msg-virtual-list" :style="{ height: `${totalSize}px`, position: 'relative', width: '100%' }">
    <div
      v-for="vRow in virtualRows"
      :key="vRow.key"
      :data-index="vRow.index"
      :ref="(el) => el && virtualizer.measureElement(el as HTMLElement)"
      :style="{
        position: 'absolute',
        top: 0,
        left: 0,
        width: '100%',
        transform: `translateY(${vRow.start}px)`,
      }"
    >
      <slot :item="items[vRow.index]" :index="vRow.index" />
    </div>
  </div>
</template>

<style scoped>
.msg-virtual-list {
  width: 100%;
}
</style>
