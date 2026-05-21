<template>
  <Layout :class="prefixCls" v-bind="lockEvents">
    <LayoutFeatures />
    <LayoutHeader fixed v-if="getShowFullHeaderRef" />
    <Layout :class="[layoutClass]">
      <LayoutSideBar v-if="getShowSidebar || getIsMobile" />
      <Layout :class="`${prefixCls}-main`">
        <LayoutMultipleHeader />
        <LayoutContent />
        <LayoutFooter />
      </Layout>
    </Layout>
    <!-- 客服后台服务：跨菜单接收 ws 消息、触发桌面通知 / title 闪烁 / 单次响铃 / 维护菜单角标 -->
    <CsBackgroundService />
  </Layout>
</template>

<script lang="ts">
  import { defineComponent, computed, unref, ref, onMounted } from 'vue';
  import { Layout } from 'ant-design-vue';
  import { createAsyncComponent } from '/@/utils/factory/createAsyncComponent';

  import LayoutHeader from './header/index.vue';
  import LayoutContent from './content/index.vue';
  import LayoutSideBar from './sider/index.vue';
  import LayoutMultipleHeader from './header/MultipleHeader.vue';
  import CsBackgroundService from '/@/views/super/airag/cs/workbench/components/CsBackgroundService.vue';

  import { useHeaderSetting } from '/@/hooks/setting/useHeaderSetting';
  import { useMenuSetting } from '/@/hooks/setting/useMenuSetting';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { useLockPage } from '/@/hooks/web/useLockPage';
  import { useAppInject } from '/@/hooks/web/useAppInject';
  import { useUserStore } from '/@/store/modules/user';
  import { useGlobSetting } from '/@/hooks/setting';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { connectWebSocket, onWebSocket } from '/@/hooks/web/useWebSocket';
  import { getToken } from '/@/utils/auth';
  import md5 from 'crypto-js/md5';

  export default defineComponent({
    name: 'DefaultLayout',
    components: {
      LayoutFeatures: createAsyncComponent(() => import('/@/layouts/default/feature/index.vue')),
      LayoutFooter: createAsyncComponent(() => import('/@/layouts/default/footer/index.vue')),
      LayoutHeader,
      LayoutContent,
      LayoutSideBar,
      LayoutMultipleHeader,
      Layout,
      CsBackgroundService,
    },
    setup() {
      const { prefixCls } = useDesign('default-layout');
      const { getIsMobile } = useAppInject();
      const { getShowFullHeaderRef } = useHeaderSetting();
      const { getShowSidebar, getIsMixSidebar, getShowMenu } = useMenuSetting();
      const userStore = useUserStore();
      const glob = useGlobSetting();
      const { createMessage } = useMessage();

      // Create a lock screen monitor
      const lockEvents = useLockPage();

      onMounted(() => {
        initGlobalWebSocket();
      });

      function initGlobalWebSocket() {
        const token = getToken();
        if (!token) return;
        const wsClientId = md5(token);
        const tabId = Math.random().toString(36).substring(2, 8);
        const userId = unref(userStore.getUserInfo)?.id;
        if (!userId) return;
        const wsKey = userId + '_' + wsClientId + '_' + tabId;
        const url = glob.domainUrl?.replace('https://', 'wss://').replace('http://', 'ws://') + '/websocket/' + wsKey;
        connectWebSocket(url);
        onWebSocket((data: any) => {
          if (data.cmd === 'kick') {
            createMessage.warning(data.msgTxt || '您的账号已在其他地方登录，当前会话已被强制下线');
            userStore.logout(true);
          }
          if (data.cmd === 'quota_kick') {
            createMessage.warning(data.msgTxt || '客服坐席已满，您已被强制下线');
            userStore.logout(true);
          }
        });
      }

      const layoutClass = computed(() => {
        let cls: string[] = ['ant-layout'];
        if (unref(getIsMixSidebar) || unref(getShowMenu)) {
          cls.push('ant-layout-has-sider');
        }
        return cls;
      });

      return {
        getShowFullHeaderRef,
        getShowSidebar,
        prefixCls,
        getIsMobile,
        getIsMixSidebar,
        layoutClass,
        lockEvents,
      };
    },
  });
</script>
<style lang="less">
  @prefix-cls: ~'@{namespace}-default-layout';

  .@{prefix-cls} {
    display: flex;
    width: 100%;
    min-height: 100%;
    background-color: @content-bg;
    flex-direction: column;

    > .ant-layout {
      min-height: 100%;
    }

    &-main {
      width: 100%;
      // 代码逻辑说明:【issues/8709】LayoutContent样式多出1px
      // margin-left: 1px;
    }
  }
</style>
