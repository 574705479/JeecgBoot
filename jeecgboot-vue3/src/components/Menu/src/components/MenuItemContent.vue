<template>
  <span :class="`${prefixCls}- flex items-center cs-menu-item-content`">
    <Icon v-if="getIcon" :icon="getIcon" :size="18" :class="`${prefixCls}-wrapper__icon mr-2`" />
    {{ getI18nName }}
    <a-badge
      v-if="showBadge"
      :count="badgeCount"
      :overflow-count="99"
      :dot="badgeAsDot"
      class="cs-menu-badge"
    />
  </span>
</template>
<script lang="ts">
  import { computed, defineComponent } from 'vue';

  import Icon from '/@/components/Icon/index';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { contentProps } from '../props';
  import { Badge } from 'ant-design-vue';
  import { useCsStore } from '/@/store/modules/cs';
  const { t } = useI18n();

  function hasWorkbenchPath(item: any): boolean {
    if (!item) return false;
    const path = item.path || '';
    if (typeof path === 'string' && path.includes('/cs/workbench')) return true;
    const children = item.children;
    if (Array.isArray(children) && children.length > 0) {
      return children.some((child: any) => hasWorkbenchPath(child));
    }
    return false;
  }

  function isLeafWorkbench(item: any): boolean {
    const path = item?.path || '';
    return typeof path === 'string' && path.includes('/cs/workbench');
  }

  export default defineComponent({
    name: 'MenuItemContent',
    components: {
      Icon,
      [Badge.name as string]: Badge,
    },
    props: contentProps,
    setup(props) {
      const { prefixCls } = useDesign('basic-menu-item-content');
      const getI18nName = computed(() => t(props.item?.name));
      const getIcon = computed(() => props.item?.icon);

      const csStore = useCsStore();
      const showBadge = computed(() => hasWorkbenchPath(props.item) && csStore.myUnreadTotal > 0);
      const badgeCount = computed(() => (showBadge.value ? csStore.myUnreadTotal : 0));
      // 叶子节点（确切定位到 /cs/workbench）显示数字徽标，
      // 父级 SubMenu（折叠态只展示父项）显示红点，避免数字撑开图标
      const badgeAsDot = computed(() => !isLeafWorkbench(props.item));

      return {
        prefixCls,
        getI18nName,
        getIcon,
        showBadge,
        badgeCount,
        badgeAsDot,
      };
    },
  });
</script>
<style lang="less" scoped>
  .cs-menu-item-content {
    position: relative;
  }
  .cs-menu-badge {
    margin-left: 8px;
    line-height: 1;
    :deep(.ant-badge-count) {
      box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.4);
    }
  }
</style>
