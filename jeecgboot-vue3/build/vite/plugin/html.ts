/**
 * Plugin to minimize and use ejs template syntax in index.html.
 * https://github.com/anncwb/vite-plugin-html
 */
import type { PluginOption } from 'vite';
import { createHtmlPlugin } from 'vite-plugin-html';
import pkg from '../../../package.json';
import { GLOB_CONFIG_FILE_NAME } from '../../constant';

export function configHtmlPlugin(env: ViteEnv, isBuild: boolean, isQiankunMicro: boolean) {
  const { VITE_GLOB_APP_TITLE, VITE_PUBLIC_PATH } = env;

  const path = VITE_PUBLIC_PATH.endsWith('/') ? VITE_PUBLIC_PATH : `${VITE_PUBLIC_PATH}/`;

  const getAppConfigSrc = () => {
    return `${path || '/'}${GLOB_CONFIG_FILE_NAME}?v=${pkg.version}-${new Date().getTime()}`;
  };

  // 【JEECG作为乾坤子应用】补充静态资源前缀
  // 【Electron桌面应用】使用相对路径，避免 file:// 下根路径解析到磁盘根目录
  const {VITE_GLOB_QIANKUN_MICRO_APP_ENTRY, VITE_GLOB_RUN_PLATFORM} = env;
  const isElectron = VITE_GLOB_RUN_PLATFORM === 'electron';
  const basePublicPath = isQiankunMicro ? VITE_GLOB_QIANKUN_MICRO_APP_ENTRY : (isElectron ? '.' : '');

  const htmlPlugin: PluginOption[] = createHtmlPlugin({
    minify: isBuild,
    inject: {
      // 修改模板html的标题
      data: {
        title: VITE_GLOB_APP_TITLE,
        basePublicPath: basePublicPath,
      },
      // 将app.config.js文件注入到模板html中
      tags: isBuild
        ? [
            {
              tag: 'script',
              attrs: {
                src: getAppConfigSrc(),
              },
            },
          ]
        : [],
    },
  });
  return htmlPlugin;
}
