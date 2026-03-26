import { app, BrowserWindow, Menu, dialog } from 'electron';
import { isDev, $env } from './env';
import { createMainWindow, createIndexWindow } from './utils/window';
import { getAppInfo } from './utils';
import * as LicenseStore from './license/LicenseStore';
import { fetchDomains, resolveBestDomain } from './license/DomainResolver';
import type { DomainConfig } from './license/DomainResolver';
import './ipc';

// 隐藏所有菜单
Menu.setApplicationMenu(null);

let mainWindow: BrowserWindow | null = null;

declare global {
  var __DOMAIN_CONFIG__: DomainConfig | null;
  var __NEEDS_ACTIVATION__: boolean;
}
global.__DOMAIN_CONFIG__ = null;
global.__NEEDS_ACTIVATION__ = false;

function main() {
  mainWindow = createMainWindow();
  mainWindow.on('focus', () => {
    if (process.platform === 'win32') {
      mainWindow!.flashFrame(false);
    }
  });
  return mainWindow;
}

async function initDomainConfig(): Promise<void> {
  const licenseUrl = ($env as Record<string, string>).VITE_GLOB_LICENSE_URL;
  if (!licenseUrl) {
    console.warn('[Main] VITE_GLOB_LICENSE_URL not configured, skip domain resolution');
    global.__NEEDS_ACTIVATION__ = true;
    return;
  }

  const stored = LicenseStore.load();
  if (!stored?.licenseKey) {
    global.__NEEDS_ACTIVATION__ = true;
    return;
  }

  try {
    const resp = await fetchDomains(licenseUrl, stored.licenseKey);
    if (resp.code !== 200 || !resp.data?.domains) {
      const errorMsg = resp.message || '获取域名失败';
      if (resp.code === 40002 || resp.code === 40003 || resp.code === 40004) {
        dialog.showErrorBox('授权异常', errorMsg);
        LicenseStore.clear();
        global.__NEEDS_ACTIVATION__ = true;
        return;
      }
      throw new Error(errorMsg);
    }

    const config = await resolveBestDomain(resp.data.domains);
    if (!config) {
      if (stored.resolvedDomain) {
        console.warn('[Main] All domains unavailable, using cached fallback');
        global.__DOMAIN_CONFIG__ = {
          apiUrl: stored.resolvedDomain,
          domainUrl: stored.resolvedDomainUrl || stored.resolvedDomain + '/jeecgboot',
        };
        return;
      }
      const { response } = await dialog.showMessageBox({
        type: 'error',
        title: '域名不可用',
        message: '所有业务域名均无法访问，请检查网络连接',
        buttons: ['重试', '退出'],
        defaultId: 0,
      });
      if (response === 0) {
        return initDomainConfig();
      }
      app.exit(1);
      return;
    }

    stored.resolvedDomain = config.apiUrl;
    stored.resolvedDomainUrl = config.domainUrl;
    stored.lastVerifyTime = Date.now();
    LicenseStore.save(stored);
    global.__DOMAIN_CONFIG__ = config;
  } catch (err) {
    console.error('[Main] Domain resolution error:', err);
    if (stored.resolvedDomain) {
      console.warn('[Main] License server unreachable, using cached domain');
      global.__DOMAIN_CONFIG__ = {
        apiUrl: stored.resolvedDomain,
        domainUrl: stored.resolvedDomainUrl || stored.resolvedDomain + '/jeecgboot',
      };
      return;
    }
    const { response } = await dialog.showMessageBox({
      type: 'error',
      title: '授权服务器不可用',
      message: '无法连接授权服务器，且没有缓存的域名配置',
      buttons: ['重试', '退出'],
      defaultId: 0,
    });
    if (response === 0) {
      return initDomainConfig();
    }
    app.exit(1);
  }
}

// 非开发环境，只允许一个实例运行
if (!isDev) {
  const gotTheLock = app.requestSingleInstanceLock();

  if (gotTheLock) {
    app.on('second-instance', () => {
      createIndexWindow();
    });
  } else {
    app.exit(0);
  }
}

// 生命周期管理
app.whenReady().then(async () => {
  const $appInfo = getAppInfo();
  if ($appInfo?.productName && $appInfo?.appId) {
    app.setName($appInfo.productName);
    app.setAppUserModelId($appInfo.appId);
  }

  await initDomainConfig();
  main();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      main();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
