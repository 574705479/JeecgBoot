import path from 'path';
import { Tray, Menu, app, dialog, nativeImage, BrowserWindow, ipcMain } from 'electron';
import type { IpcMainEvent } from 'electron';
import {_PATHS} from '../paths';
import {isDev} from '../env';

const TrayIcons = {
  normal: nativeImage.createFromPath(
    path.join(_PATHS.electronRoot, process.platform === 'darwin' ? './icons/mac/tray-icon.png' : './icons/mac/tray-icon@2x.png').replace(/[\\/]dist[\\/]/, '/')
  ),
  empty: nativeImage.createEmpty(),
};

// ============ 窗口-托盘映射 ============

interface TrayEntry {
  tray: Tray;
  startBlink: () => void;
  stopBlink: () => void;
  username: string;
}

const windowTrayMap = new Map<number, TrayEntry>();

export function getWindowUsername(winId: number): string {
  return windowTrayMap.get(winId)?.username || '';
}

// ============ IPC 注册（全局一次） ============

let ipcRegistered = false;

function registerTrayIPC() {
  if (ipcRegistered) return;
  ipcRegistered = true;

  ipcMain.on('tray-flash', (event: IpcMainEvent) => {
    if (process.platform !== 'win32') return;
    const win = BrowserWindow.fromWebContents(event.sender);
    if (win) windowTrayMap.get(win.id)?.startBlink();
  });

  ipcMain.on('tray-flash-stop', (event: IpcMainEvent) => {
    if (process.platform !== 'win32') return;
    const win = BrowserWindow.fromWebContents(event.sender);
    if (win) windowTrayMap.get(win.id)?.stopBlink();
  });

  ipcMain.on('tray-set-user', (event: IpcMainEvent, username: string) => {
    const win = BrowserWindow.fromWebContents(event.sender);
    if (!win) return;
    const entry = windowTrayMap.get(win.id);
    const baseName = app.getName();
    const displayName = username ? `${baseName} - ${username}` : baseName;
    if (entry) {
      entry.username = username;
      entry.tray.setToolTip(displayName + (isDev ? ' (开发环境)' : ''));
    }
    win.setTitle(displayName);
  });
}

// ============ 闪烁控制器 ============

function createBlinkController(tray: Tray) {
  let isBlinking = false;
  let blinkTimer: NodeJS.Timeout | null = null;

  function startBlink() {
    isBlinking = true;
    tray.setImage(TrayIcons.empty);
    blinkTimer = setTimeout(() => {
      tray.setImage(TrayIcons.normal);
      setTimeout(() => {
        if (isBlinking) {
          startBlink();
        }
      }, 500);
    }, 500);
  }

  function stopBlink() {
    isBlinking = false;
    if (blinkTimer) {
      clearTimeout(blinkTimer);
      blinkTimer = null;
    }
    if (!tray.isDestroyed()) {
      tray.setImage(TrayIcons.normal);
    }
  }

  return { startBlink, stopBlink, isBlinking: () => isBlinking };
}

// ============ 创建托盘 ============

export function createTray(win: BrowserWindow) {
  registerTrayIPC();

  const tray = new Tray(TrayIcons.normal);
  const blinkCtrl = createBlinkController(tray);

  windowTrayMap.set(win.id, { tray, ...blinkCtrl, username: '' });

  tray.setToolTip(app.getName() + (isDev ? ' (开发环境)' : ''));

  tray.on('click', () => {
    win.show();
    win.focus();
  });

  tray.on('right-click', () => {
    const menu = buildTrayMenu(win, blinkCtrl);
    tray.popUpContextMenu(menu);
  });

  win.on('focus', () => blinkCtrl.stopBlink());

  win.on('closed', () => {
    windowTrayMap.delete(win.id);
    if (!tray.isDestroyed()) tray.destroy();
  });
}

// ============ 托盘菜单 ============

const MenuIcon = {
  exit: nativeImage
    .createFromDataURL(
      'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXBIWXMAAA7EAAAOxAGVKw4bAAACJ0lEQVR4nH1TzWvUQBRP7fpxsWqVXsSLiAevRWhhN28msRJo981kay4WRBCF/QdEFJpbaUHw4kFBQTwUKX4gKh48KPiBBcGLJ1F0uzPZ7ibWXf0DIjObielS+mDIm/fxm9/85sWyBixN06E0CIaV3wB2XhC8puOWNZSG4Y7B+k2mi7Kl9l2n9rHnzvbWJoLRYn7r5jTViQjwzM8ynlC+AFyVgN2NU8G+Rnn6QETx3FfP223A/jeHfWqCsAUJ7Hlryh9Te0nYqiDsz9rE6VHVIABvNwEf/ADYk4OsZPeVFbwiCHtcZBVR9k4CJhJmDuUxwEVJ8H4fINOkC9Vjbeq/UTR1IgPturX3f93Z35+B7ddxgJL6dih/skF9zE9KCJ//5bDLpii1+npIuzolKTubC5gBxzarJo6vWWjrUP+etFlF+ds9lRFOXalN+NPEmxvRDS3KH34v8+PFIgNmTh0EahH+InGCwzoQEbYcuTMnlR8aYbaxGHFvRNiznssP6sA65UsxrdU1+hYnFhlpAGAkdvzlPLFu88mY8pcrVjCsxcqGapC2eYW249/tUH4xS4QaVQLeigi/YWJqPl4DlNRSrAwzSaoXIspeWUYrI9qXINglgT1qAt5JPG+kkNN5BSAJuyoJfhAVdmST4PlPBFASNs6rIgnspqC8HlF+SQAuRQTfKpYiEy6fwuIdP42P71T+t0l/TBKcE8AXm4DXBfB6w50+apgUhf4HZ5j+Z5+zNTAAAAAASUVORK5CYII='
    )
    .resize({ width: 16, height: 16 }),
};

function buildTrayMenu(win: BrowserWindow, blinkCtrl: ReturnType<typeof createBlinkController>) {
  const { startBlink, stopBlink } = blinkCtrl;
  const isBlinking = blinkCtrl.isBlinking();

  return Menu.buildFromTemplate([
    ...(isDev
      ? [
          {
            label: '开发工具',
            submenu: [
              {
                label: '以下菜单仅显示在开发环境',
                sublabel: '当前为开发环境',
                enabled: false,
              },
              { type: 'separator' },
              {
                label: '切换 DevTools',
                click: () => win.webContents.toggleDevTools(),
              },
              {
                label: `托盘图标${isBlinking ? '停止' : '开始'}闪烁`,
                sublabel: '模拟新消息提醒',
                click: () => (isBlinking ? stopBlink() : startBlink()),
              },
            ],
          },
          { type: 'separator' },
        ]
      : ([] as any)),
    {
      label: '显示窗口',
      icon: TrayIcons.normal.resize({ width: 16, height: 16 }),
      click: () => {
        win.show();
        win.focus();
      },
    },
    { type: 'separator' },
    {
      label: '关闭窗口',
      icon: MenuIcon.exit,
      click: () => {
        if (!win.isVisible()) win.show();
        const choice = dialog.showMessageBoxSync(win, {
          type: 'question',
          title: '提示',
          message: '确定要关闭此窗口吗？',
          buttons: ['关闭', '取消'],
          defaultId: 1,
          cancelId: 1,
          noLink: true,
        });
        if (choice === 0) {
          win.removeAllListeners('close');
          win.destroy();
        }
      },
    },
  ]);
}
