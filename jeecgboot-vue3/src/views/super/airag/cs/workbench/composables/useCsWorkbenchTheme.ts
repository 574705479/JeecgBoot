/**
 * 客服工作台皮肤主题 composable（模块级单例）
 *
 * 多次调用返回同一份响应式状态，保证父级 index.vue 与
 * CsWorkbenchSettingsDrawer 共享主题，无需走 provide / inject。
 */
import { ref, computed } from 'vue';
import { THEME_PRESETS, type ThemeConfig } from '../theme/presets';

const THEME_STORAGE_KEY = 'cs-workbench-theme';

const currentThemeKey = ref('pureWhite');
const customTheme = ref<ThemeConfig>({ ...THEME_PRESETS.pureWhite });
let initialized = false;

function hexToRgb(hex: string): string {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `${r}, ${g}, ${b}`;
}

function saveThemeToStorage() {
  localStorage.setItem(THEME_STORAGE_KEY, JSON.stringify({
    key: currentThemeKey.value,
    custom: customTheme.value,
  }));
}

function loadThemeFromStorage() {
  try {
    const raw = localStorage.getItem(THEME_STORAGE_KEY);
    if (raw) {
      const data = JSON.parse(raw);
      currentThemeKey.value = data.key || 'pureWhite';
      if (data.custom) {
        const merged = { ...THEME_PRESETS.pureWhite, ...data.custom };
        if (!merged.barStart) { merged.barStart = merged.brandStart; }
        if (!merged.barEnd) { merged.barEnd = merged.brandEnd; }
        customTheme.value = merged;
      }
    }
  } catch { /* ignore */ }
}

function selectTheme(key: string) {
  currentThemeKey.value = key;
  if (key !== 'custom') {
    customTheme.value = { ...THEME_PRESETS[key] };
  }
  saveThemeToStorage();
}

function updateCustomColor(field: string, value: string) {
  (customTheme.value as any)[field] = value;
  if (field === 'brandStart') {
    customTheme.value.brandRgb = hexToRgb(value);
  }
  saveThemeToStorage();
}

function onColorInput(field: string, e: Event) {
  const el = e.target as HTMLInputElement;
  if (el) updateCustomColor(field, el.value);
}

const activeTheme = computed<ThemeConfig>(() => {
  if (currentThemeKey.value === 'custom') return customTheme.value;
  return THEME_PRESETS[currentThemeKey.value] || THEME_PRESETS.pureWhite;
});

const themeVars = computed<Record<string, string>>(() => {
  const t = activeTheme.value;
  return {
    '--cs-brand-start': t.brandStart,
    '--cs-brand-end': t.brandEnd,
    '--cs-brand-rgb': t.brandRgb,
    '--cs-bg-page': t.bgPage,
    '--cs-bg-chat': t.bgChat,
    '--cs-bubble-agent': t.bubbleAgent,
    '--cs-bubble-agent-end': t.bubbleAgentEnd,
    '--cs-bubble-user': t.bubbleUser,
    '--cs-bubble-ai': t.bubbleAi,
    '--cs-bubble-ai-end': t.bubbleAiEnd,
    '--cs-bubble-assistant': t.bubbleAssistant,
    '--cs-bubble-assistant-end': t.bubbleAssistantEnd,
    '--cs-bg-surface': t.bgSurface,
    '--cs-bg-card': t.bgCard,
    '--cs-bg-input': t.bgInput,
    '--cs-bg-code': t.bgCode,
    '--cs-text-primary': t.textPrimary,
    '--cs-text-secondary': t.textSecondary,
    '--cs-text-muted': t.textMuted,
    '--cs-border': t.border,
    '--cs-brand-text': t.brandText,
    '--cs-bar-start': t.barStart || t.brandStart,
    '--cs-bar-end': t.barEnd || t.brandEnd,
  };
});

export function useCsWorkbenchTheme() {
  if (!initialized) {
    loadThemeFromStorage();
    initialized = true;
  }
  return {
    currentThemeKey,
    customTheme,
    activeTheme,
    themeVars,
    THEME_PRESETS,
    selectTheme,
    onColorInput,
  };
}

export type { ThemeConfig };
