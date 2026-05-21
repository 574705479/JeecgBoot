<template>
  <a-badge
    v-if="show"
    dot
    :offset="[0, 0]"
    class="cs-mix-sider-badge"
  />
</template>

<script lang="ts">
  import { defineComponent, computed, type PropType } from 'vue';
  import { Badge as ABadge } from 'ant-design-vue';
  import { useCsStore } from '/@/store/modules/cs';
  import type { Menu } from '/@/router/types';

  /**
   * MIX_SIDEBAR 左栏一级图标徽标：
   *   仅当 item.path === '/cs' 且 csStore.myUnreadTotal > 0 时显示一个红点。
   *   绝对定位到父 <li>（MixSider 的 .@{namespace}-layout-mix-sider-module__item）
   *   的右上角，避开 SimpleMenuTag 已有的位置。
   *
   * 仅服务于 MIX_SIDEBAR；顶栏 BasicMenu / 整树 SimpleMenu 由各自原有徽标组件覆盖。
   */
  const CS_PARENT_PATH = '/cs';

  export default defineComponent({
    name: 'CsMixSiderBadge',
    components: { ABadge },
    props: {
      item: {
        type: Object as PropType<Menu>,
        default: () => ({}),
      },
    },
    setup(props) {
      const csStore = useCsStore();
      const show = computed(() => {
        const path = props.item?.path;
        if (path !== CS_PARENT_PATH) return false;
        return (csStore.myUnreadTotal as unknown as number) > 0;
      });
      return { show };
    },
  });
</script>

<style lang="less" scoped>
  .cs-mix-sider-badge {
    position: absolute;
    top: 14px;
    right: 22px;
    z-index: 1;
    line-height: 1;
    pointer-events: none;

    :deep(.ant-badge-dot) {
      width: 8px;
      height: 8px;
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.4);
    }
  }
</style>
