import type { MainAppProps } from "#/main";
import 'uno.css';
import '/@/design/index.less';
import 'ant-design-vue/dist/reset.css';
// 注册图标
import 'virtual:svg-icons-register';

import App from './App.vue';
import { createApp } from 'vue';
import { initAppConfigStore } from '/@/logics/initAppConfig';
import { setupErrorHandle } from '/@/logics/error-handle';
import { router, createRouter, setupRouter } from '/@/router';
import { setupRouterGuard } from '/@/router/guard';
import { setupStore } from '/@/store';
import { setupGlobDirectives } from '/@/directives';
import { setupI18n } from '/@/locales/setupI18n';
import { setupElectron } from "@/electron";
import { registerGlobComp } from '/@/components/registerGlobComp';
import { registerThirdComp } from '/@/settings/registerThirdComp';
import { registerSuper } from '/@/views/super/registerSuper';
import { useSso } from '/@/hooks/web/useSso';
import { checkIsQiankunMicro } from "/@/qiankun/micro";
import { autoUseQiankunMicro } from "/@/qiankun/micro/qiankunMicro";
import { useAppStoreWithOut } from "@/store/modules/app";
import { useUserStoreWithOut } from '/@/store/modules/user';
import { loadBrandConfig } from '/@/utils/brand';
// Phase 4.3 (T1)：客服模块的 initImageCache 历史上是 utils/file/imageCache 的转发壳，
// 与下行的 initCseImageCache 在 main.ts 看起来像被「双重调用」。
// 现已重命名为 initCsAvatarCache（语义化客服头像缓存初始化），二者逻辑等价。
// 这里只调用一次全局 initCseImageCache 即可，cleanupImageCache 仍保留 no-op 兼容性导入。
import { cleanupImageCache } from '/@/views/super/airag/cs/utils/csImageCache';
import { initImageCache as initCseImageCache, clearImageCache as clearCseImageCache } from '/@/utils/file/imageCache';
import { cseSelfTest } from '/@/utils/cse/cseDecrypt';
import { useGlobSetting } from '/@/hooks/setting';
import { ElectronEnum } from '/@/enums/jeecgEnum';
import { defHttp } from '/@/utils/http/axios';
import { refreshCache } from '/@/views/system/dict/dict.api';

// 注册online模块lib
import { registerPackages } from '/@/utils/monorepo/registerPackages';

// 程序入口
async function main() {
  if (checkIsQiankunMicro()) {
    // 【JEECG作为乾坤子应用】以乾坤子应用模式启动
    // await autoUseQiankunMicro(bootstrap)
    await autoUseQiankunMicro(bootstrap)
  } else {
    // 获取参数
    const props = getMainAppProps();
    // 普通启动
    await bootstrap(props)
  }
}

main();

async function bootstrap(props?: MainAppProps) {
  // 创建应用实例
  const app = createApp(App);
  // 【QQYUN-6329】
  window['JAppRootInstance'] = app;

  // 创建路由
  createRouter();

  // 配置存储
  setupStore(app);

  // 读取品牌配置 + 初始化图片缓存（并行，互不阻塞）
  // Phase 4.3 (T1)：只需调用一次全站 IDB 头像缓存初始化（含客服模块）
  await Promise.all([loadBrandConfig(), initCseImageCache()]);
  window.addEventListener('beforeunload', () => {
    cleanupImageCache();
    clearCseImageCache();
  });
  // CSE 启动自检：异步触发，结果在 app.mount 后通过 toast 反馈（见下方 mount 流程）
  // 这里仅启动 Promise，避免阻塞启动；console 兜底保留方便排查
  const cseSelfTestPromise = cseSelfTest().then((ok) => {
    if (!ok) console.warn('[CSE] SubtleCrypto + noble fallback 自检失败，加密图片可能无法解密');
    return ok;
  });

  // 配置参数
  setupProps(props);

  // 多语言配置,异步情况:语言文件可以从服务器端获得
  await setupI18n(app);

  // 初始化内部系统配置
  initAppConfigStore();

  // 注册外部模块路由(注册online模块lib)
  registerPackages(app);

  // 注册全局组件
  registerGlobComp(app);

  //CAS单点登录
  await useSso().ssoLogin();

  // 注册super应用路由
  await registerSuper(app);
  
  // 配置路由
  setupRouter(app);

  // 路由保护（必须紧跟 setupRouter，在任何 await 之前注册守卫，
  // 否则初始导航会在守卫注册前完成，导致 404）
  setupRouterGuard(router);

  // Electron: 自动向 JeecgBoot 后端激活（幂等，避免用户二次输入授权码）
  const glob = useGlobSetting();
  if (glob.isElectronPlatform && glob.apiUrl) {
    const storedKey = (window as any)[ElectronEnum.ELECTRON_API]?.getStoredLicenseKey?.();
    if (storedKey) {
      try {
        await Promise.race([
          defHttp.post(
            { url: '/license/activate', params: { licenseKey: storedKey } },
            { errorMessageMode: 'none' }
          ),
          new Promise((_, reject) => setTimeout(() => reject(new Error('timeout')), 5000)),
        ]);
      } catch {
        // 已激活、超时或失败均不阻塞启动；901 会由路由守卫引导至激活页
      }
    }
  }

  // 注册全局指令
  setupGlobDirectives(app);

  // 配置全局错误处理
  setupErrorHandle(app);

  // 注册第三方组件
  await registerThirdComp(app);

  // 配置electron
  setupElectron(app)

  // 当路由准备好时再执行挂载( https://next.router.vuejs.org/api/#isready)
  await router.isReady();

  // 挂载应用
  app.mount(getMountContainer(props), true);

  // Phase 4.4 (m1)：cseSelfTest 失败时友好 toast 提醒。
  // 必须等到 app.mount + nextTick 之后，message 组件才能正常渲染；
  // 失败也只是降级（noble fallback），不强制阻塞用户。
  cseSelfTestPromise.then(async (ok) => {
    if (ok) return;
    try {
      const { nextTick } = await import('vue');
      await nextTick();
      const { message } = await import('ant-design-vue');
      message.warning('图片加密自检失败，部分加密图片可能无法显示，请尝试更换浏览器或刷新页面');
    } catch (e) {
      console.warn('[CSE] selftest toast 渲染失败:', e);
    }
  });

  // Electron: app 完全就绪后同步托盘昵称 + 刷新缓存
  if (glob.isElectronPlatform && glob.apiUrl) {
    const cachedUser = useUserStoreWithOut().getUserInfo;
    if (cachedUser?.realname) {
      (window as any)[ElectronEnum.ELECTRON_API]?.setTrayUser?.(cachedUser.realname || '');
    }
    refreshCache().catch(() => {});
  }

  console.log(" vue3 app 加载完成！")

  return app
}

// 获取应用挂载容器
function getMountContainer(props?: MainAppProps) {
  const id = '#app';
  if (!props?.container?.querySelector) {
    return id;
  }
  return props.container.querySelector(id) ?? id;
}

// 获取主应用参数
function getMainAppProps(): MainAppProps {
  // 从 queryString 中获取
  const searchParams = new URLSearchParams(window.location.search);
  // 隐藏侧边栏（菜单）
  let hideSider = searchParams.get('hideSider') === 'true';
  // 隐藏顶部
  let hideHeader = searchParams.get('hideHeader') === 'true';
  // 隐藏 多Tab 切换
  let hideMultiTabs = searchParams.get('hideMultiTabs') === 'true';

  return {
    hideSider,
    hideHeader,
    hideMultiTabs
  }
}

// 配置主应用参数
function setupProps(props?: MainAppProps) {
  if (!props) {
    return
  }
  const appStore = useAppStoreWithOut();
  appStore.setMainAppProps(props);
}
