<template>
  <a-badge
    v-if="show"
    :count="unread"
    :overflow-count="99"
    :dot="collapseParent"
    :offset="collapseParent ? [-2, 2] : [6, 0]"
    :class="['cs-menu-badge', collapseParent ? 'cs-menu-badge--collapse' : 'cs-menu-badge--inline']"
  />
</template>

<script lang="ts">
  import { defineComponent, computed, type PropType } from 'vue';
  import { Badge as ABadge } from 'ant-design-vue';
  import { useCsStore } from '/@/store/modules/cs';
  import type { Menu } from '/@/router/types';

  const CS_PARENT_PATHS = new Set(['/cs']);

  function isTargetMenuItem(item: any): boolean {
    if (!item || !item.path) return false;
    const p = String(item.path);
    return CS_PARENT_PATHS.has(p);
  }

  export default defineComponent({
    name: 'CsMenuBadge',
    components: { ABadge },
    props: {
      item: {
        type: Object as PropType<Menu>,
        default: () => ({}),
      },
      collapseParent: {
        type: Boolean,
        default: false,
      },
    },
    setup(props) {
      const csStore = useCsStore();
      const show = computed(() => {
        if (!isTargetMenuItem(props.item)) return false;
        return (csStore.myUnreadTotal as unknown as number) > 0;
      });
      const unread = computed(() => csStore.myUnreadTotal as unknown as number);
      return { show, unread };
    },
  });
</script>

<style lang="less" scoped>
  .cs-menu-badge {
    margin-left: 6px;
    line-height: 1;

    :deep(.ant-badge-count) {
      box-shadow: none;
      font-weight: 600;
    }

    &--collapse {
      position: absolute;
      top: 6px;
      right: 8px;
      margin-left: 0;
      pointer-events: none;
    }
  }
</style>
