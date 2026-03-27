import type {BrowserWindowConstructorOptions} from 'electron';
import {app, BrowserWindow, dialog} from 'electron';
import path from 'path';
import {_PATHS} from '../paths';
import {$env, isDev} from '../env';
import {createTray} from './tray';
import {isQuitting} from '../state';

// 获取公共窗口选项（webPreferences 深合并，避免外部 partition 等配置覆盖 preload）
export function getBrowserWindowOptions(options?: BrowserWindowConstructorOptions): BrowserWindowConstructorOptions {
  const { webPreferences: extraWebPrefs, ...restOptions } = options || {};
  return {
    width: 1200,
    height: 800,
    frame: false,
    webPreferences: {
      preload: path.join(_PATHS.preloadRoot, 'index.js'),
      nodeIntegration: false,
      contextIsolation: true,
      backgroundThrottling: false,
      webSecurity: !isDev,
      ...extraWebPrefs,
    },
    icon: isDev ? _PATHS.appIcon : void 0,
    ...restOptions,
  }
}

// 创建窗口
export function createBrowserWindow(options?: BrowserWindowConstructorOptions) {
  const win = new BrowserWindow(getBrowserWindowOptions(options));
  if (process.platform === 'darwin') {
    if (app.dock) {
      app.dock.setIcon(path.join(_PATHS.electronRoot, './icons/mac/dock.png').replace(/[\\/]dist[\\/]/, '/'));
    }
  }

  // 子窗口只继承 partition，不继承父窗口的宽高标题
  const partitionOption = options?.webPreferences?.partition
    ? { webPreferences: { partition: options.webPreferences.partition } }
    : undefined;

  win.webContents.setWindowOpenHandler(() => {
    return {
      action: 'allow',
      overrideBrowserWindowOptions: getBrowserWindowOptions(partitionOption),
    }
  });

  // 窗口最大化状态变化时通知渲染进程
  win.on('maximize', () => {
    win.webContents.send('window-maximized-change', true);
  });
  win.on('unmaximize', () => {
    win.webContents.send('window-maximized-change', false);
  });

  // 当 beforeunload 阻止窗口关闭时触发
  win.webContents.on('will-prevent-unload', () => {
    const choice = dialog.showMessageBoxSync(win, {
      type: 'question',
      title: '确认关闭吗？',
      message: '系统可能不会保存您所做的更改。',
      buttons: ['关闭', '取消'],
      defaultId: 1,
      cancelId: 1,
      noLink: true,
    });
    // 用户选择了关闭，直接销毁窗口
    if (choice === 0) {
      win.destroy();
    }
  });

  return win;
}

// 创建主窗口、系统托盘（每个窗口独立托盘 + close-to-hide）
export function createMainWindow(partition?: string) {
  const extraOpts = partition ? { webPreferences: { partition } } : undefined;
  const win = createIndexWindow(extraOpts);

  createTray(win);

  win.on('close', (event) => {
    if (!isQuitting) {
      event.preventDefault();
      win.hide();
    }
  });

  win.on('focus', () => {
    if (process.platform === 'win32') {
      win.flashFrame(false);
    } else if (process.platform === 'darwin' && app.dock) {
      app.dock.setBadge('');
    }
  });

  return win;
}

// 创建索引窗口
export function createIndexWindow(extraOptions?: BrowserWindowConstructorOptions) {
  const win = createBrowserWindow({
    width: 1600,
    height: 1000,
    title: $env.VITE_GLOB_APP_TITLE!,
    ...extraOptions,
  });

  // F12 / Ctrl+Shift+I 打开 DevTools
  win.webContents.on('before-input-event', (event, input) => {
    const isToggleDevTools =
      input.key === 'F12' ||
      (input.control && input.shift && input.key.toLowerCase() === 'i');
    if (isToggleDevTools) {
      win.webContents.toggleDevTools();
      event.preventDefault();
    }
  });

  if (isDev) {
    let serverUrl = $env.VITE_DEV_SERVER_URL! as string;
    // 【JHHB-936】由于wps预览不能使用localhost访问，所以把localhost替换为127.0.0.1
    serverUrl = serverUrl.replace('localhost', '127.0.0.1');
    win.loadURL(serverUrl)
    // 开发环境下，自动打开调试工具
    // win.webContents.openDevTools()
  } else {
    win.loadFile(path.join(_PATHS.publicRoot, 'index.html'));
  }

  return win;
}
