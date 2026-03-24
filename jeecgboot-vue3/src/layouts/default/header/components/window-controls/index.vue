<template>
  <div class="window-controls">
    <div class="window-controls__btn" @click="handleMinimize" :title="t('layout.header.windowMinimize')">
      <svg width="16" height="2" viewBox="0 0 16 2">
        <rect width="16" height="2" rx="0.5" fill="currentColor" />
      </svg>
    </div>
    <div class="window-controls__btn" @click="handleMaximize" :title="t('layout.header.windowMaximize')">
      <svg v-if="!isMaximized" width="14" height="14" viewBox="0 0 14 14">
        <rect x="1" y="1" width="12" height="12" rx="0" fill="none" stroke="currentColor" stroke-width="1.4" />
      </svg>
      <svg v-else width="14" height="14" viewBox="0 0 14 14">
        <rect x="3" y="0.5" width="10" height="10" rx="0" fill="none" stroke="currentColor" stroke-width="1.4" />
        <rect x="0.5" y="3" width="10" height="10" rx="0" fill="none" stroke="currentColor" stroke-width="1.4" />
      </svg>
    </div>
    <div class="window-controls__btn window-controls__btn--close" @click="handleClose" :title="t('layout.header.windowClose')">
      <svg width="14" height="14" viewBox="0 0 14 14">
        <line x1="1" y1="1" x2="13" y2="13" stroke="currentColor" stroke-width="1.6" />
        <line x1="13" y1="1" x2="1" y2="13" stroke="currentColor" stroke-width="1.6" />
      </svg>
    </div>
  </div>
</template>

<script lang="ts">
  import { defineComponent, ref, onMounted, onUnmounted } from 'vue';
  import { ElectronEnum } from '/@/enums/jeecgEnum';
  import { useI18n } from '/@/hooks/web/useI18n';

  export default defineComponent({
    name: 'WindowControls',
    setup() {
      const { t } = useI18n();
      const isMaximized = ref(false);
      const api = window[ElectronEnum.ELECTRON_API];

      function handleMinimize() {
        api?.windowMinimize?.();
      }
      function handleMaximize() {
        api?.windowMaximize?.();
      }
      function handleClose() {
        api?.windowClose?.();
      }

      onMounted(() => {
        api?.onMaximizedChange?.((val: boolean) => {
          isMaximized.value = val;
        });
      });

      return { t, isMaximized, handleMinimize, handleMaximize, handleClose };
    },
  });
</script>

<style lang="less" scoped>
  .window-controls {
    display: flex;
    align-items: center;
    height: 100%;
    -webkit-app-region: no-drag;

    &__btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 52px;
      min-height: 40px;
      height: 100%;
      cursor: pointer;
      transition: background-color 0.15s;
      color: inherit;

      &:hover {
        background-color: rgba(0, 0, 0, 0.08);
      }

      &--close:hover {
        background-color: #e81123;
        color: #fff;
      }
    }
  }
</style>
