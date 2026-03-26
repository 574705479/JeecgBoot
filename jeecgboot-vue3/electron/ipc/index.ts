import { Tray, ipcMain, BrowserWindow, app, Notification } from 'electron';
import type { NotificationConstructorOptions, IpcMainInvokeEvent } from 'electron';
import { openInBrowser } from '../utils';
import { omit } from 'lodash-es';
import { $env } from '../env';
import * as LicenseStore from '../license/LicenseStore';
import { fetchDomains, resolveBestDomain } from '../license/DomainResolver';

ipcMain.on('open-in-browser', (event: IpcMainInvokeEvent, url: string) => openInBrowser(url));

// 窗口控制
ipcMain.on('window-minimize', () => {
  const win = BrowserWindow.getAllWindows()[0];
  if (win) win.minimize();
});
ipcMain.on('window-maximize', () => {
  const win = BrowserWindow.getAllWindows()[0];
  if (!win) return;
  win.isMaximized() ? win.unmaximize() : win.maximize();
});
ipcMain.on('window-close', () => {
  const win = BrowserWindow.getAllWindows()[0];
  if (win) win.close();
});
// 处理任务栏闪烁
ipcMain.on('notify-flash', (event: IpcMainInvokeEvent, count: number = 0) => {
  const win = BrowserWindow.getAllWindows()[0];
  if (!win) return;
  if (win.isFocused()) return;
  if (process.platform === 'win32') {
    // windows
    win.flashFrame(true);
  } else if (process.platform === 'darwin') {
    // Mac
    if (app.dock) {
      app.dock.bounce('informational');
      // 设置角标(未读消息)
      if (count > 0) {
        app.dock.setBadge(count.toString());
      } else {
        app.dock.setBadge('');
      }
    }
  }
});
// 通知 (点击通知打开指定页面)
ipcMain.on('notify-with-path', (event: IpcMainInvokeEvent, options: NotificationConstructorOptions & { path: string }) => {
  const win = BrowserWindow.getAllWindows()[0];
  if (!win) return;
  if (win.isFocused()) return;
  const notification = new Notification({
    ...omit(options, 'path'),
  });
  notification.on('click', () => {
    if (win.isMinimized()) win.restore();
    win.show();
    win.focus();
    win.webContents.send('navigate-to', options.path);
  });
  notification.show();
});

// ==================== License / Domain IPC ====================

ipcMain.on('license:get-domain-config', (event) => {
  event.returnValue = global.__DOMAIN_CONFIG__;
});

ipcMain.on('license:needs-activation', (event) => {
  event.returnValue = global.__NEEDS_ACTIVATION__;
});

ipcMain.on('license:get-stored-key', (event) => {
  const data = LicenseStore.load();
  event.returnValue = data?.licenseKey || null;
});

ipcMain.handle('license:activate', async (_event, licenseKey: string) => {
  const licenseUrl = ($env as Record<string, string>).VITE_GLOB_LICENSE_URL;
  if (!licenseUrl) {
    return { error: '未配置授权服务器地址' };
  }

  try {
    const resp = await fetchDomains(licenseUrl, licenseKey);
    if (resp.code !== 200 || !resp.data?.domains) {
      return { error: resp.message || '获取域名失败' };
    }

    const config = await resolveBestDomain(resp.data.domains);
    if (!config) {
      return { error: '所有业务域名均无法访问' };
    }

    const storeData: LicenseStore.LicenseData = {
      licenseKey,
      resolvedDomain: config.apiUrl,
      resolvedDomainUrl: config.domainUrl,
      lastVerifyTime: Date.now(),
    };
    LicenseStore.save(storeData);

    global.__DOMAIN_CONFIG__ = config;
    global.__NEEDS_ACTIVATION__ = false;

    return { success: true, config };
  } catch (err: any) {
    return { error: err?.message || '激活失败' };
  }
});

ipcMain.handle('license:clear', async () => {
  LicenseStore.clear();
  global.__DOMAIN_CONFIG__ = null;
  global.__NEEDS_ACTIVATION__ = true;
  return { success: true };
});
