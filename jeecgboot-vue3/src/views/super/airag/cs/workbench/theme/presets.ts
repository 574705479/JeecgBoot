/**
 * 客服工作台皮肤主题预设
 *
 * 主题字段在多处使用：
 *   - 父级 index.vue 应用 themeVars 到根元素
 *   - CsWorkbenchSettingsDrawer 用于切换 / 自定义
 *   - 通过 useCsWorkbenchTheme composable 共享单例状态
 */

export interface ThemeConfig {
  name: string;
  brandStart: string;
  brandEnd: string;
  brandRgb: string;
  bgPage: string;
  bgChat: string;
  bubbleAgent: string;
  bubbleAgentEnd: string;
  bubbleUser: string;
  bubbleAi: string;
  bubbleAiEnd: string;
  bubbleAssistant: string;
  bubbleAssistantEnd: string;
  bgSurface: string;
  bgCard: string;
  bgInput: string;
  bgCode: string;
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  border: string;
  brandText: string;
  barStart: string;
  barEnd: string;
}

const LIGHT_COMMON = {
  bgSurface: '#ffffff', bgCard: '#fafbfc', bgInput: '#f5f6fa',
  bgCode: '#f0f0f0', textPrimary: '#333333', textSecondary: '#666666',
  textMuted: '#999999', border: '#eeeeee', brandText: '#ffffff',
};

export const THEME_PRESETS: Record<string, ThemeConfig> = {
  blue: {
    name: '经典蓝',
    brandStart: '#4096ff', brandEnd: '#1677ff', brandRgb: '64, 150, 255',
    barStart: '#4096ff', barEnd: '#1677ff',
    bgPage: '#f5f6fa', bgChat: '#f7f8fc',
    bubbleAgent: '#dbeafe', bubbleAgentEnd: '#e0f0ff',
    bubbleUser: '#ffffff',
    bubbleAi: '#e8f4ff', bubbleAiEnd: '#dbeafe',
    bubbleAssistant: '#e6fffb', bubbleAssistantEnd: '#b5f5ec',
    ...LIGHT_COMMON,
  },
  green: {
    name: '清新绿',
    brandStart: '#52c41a', brandEnd: '#389e0d', brandRgb: '82, 196, 26',
    barStart: '#52c41a', barEnd: '#389e0d',
    bgPage: '#f6faf3', bgChat: '#f8fcf6',
    bubbleAgent: '#d9f7be', bubbleAgentEnd: '#e6ffe0',
    bubbleUser: '#ffffff',
    bubbleAi: '#e6ffe0', bubbleAiEnd: '#d9f7be',
    bubbleAssistant: '#e6fffb', bubbleAssistantEnd: '#b5f5ec',
    ...LIGHT_COMMON,
  },
  orange: {
    name: '活力橙',
    brandStart: '#fa8c16', brandEnd: '#d46b08', brandRgb: '250, 140, 22',
    barStart: '#fa8c16', barEnd: '#d46b08',
    bgPage: '#fffbf5', bgChat: '#fefcf8',
    bubbleAgent: '#fff7e6', bubbleAgentEnd: '#ffe7ba',
    bubbleUser: '#ffffff',
    bubbleAi: '#fff1d6', bubbleAiEnd: '#ffe7ba',
    bubbleAssistant: '#e6fffb', bubbleAssistantEnd: '#b5f5ec',
    ...LIGHT_COMMON,
  },
  cyan: {
    name: '科技青',
    brandStart: '#13c2c2', brandEnd: '#08979c', brandRgb: '19, 194, 194',
    barStart: '#13c2c2', barEnd: '#08979c',
    bgPage: '#f5fafa', bgChat: '#f0f9f9',
    bubbleAgent: '#b5f5ec', bubbleAgentEnd: '#d6fff8',
    bubbleUser: '#ffffff',
    bubbleAi: '#d6fff8', bubbleAiEnd: '#b5f5ec',
    bubbleAssistant: '#e6fffb', bubbleAssistantEnd: '#87e8de',
    ...LIGHT_COMMON,
  },
  rose: {
    name: '玫瑰粉',
    brandStart: '#eb2f96', brandEnd: '#c41d7f', brandRgb: '235, 47, 150',
    barStart: '#eb2f96', barEnd: '#c41d7f',
    bgPage: '#fff5f8', bgChat: '#fef7fa',
    bubbleAgent: '#ffd6e7', bubbleAgentEnd: '#ffecf3',
    bubbleUser: '#ffffff',
    bubbleAi: '#ffecf3', bubbleAiEnd: '#ffd6e7',
    bubbleAssistant: '#e6fffb', bubbleAssistantEnd: '#b5f5ec',
    ...LIGHT_COMMON,
  },
  pureWhite: {
    name: '纯白极简',
    brandStart: '#4096ff', brandEnd: '#1677ff', brandRgb: '64, 150, 255',
    barStart: '#ffffff', barEnd: '#f5f5f5',
    brandText: '#000000',
    bgPage: '#f7f7f7', bgChat: '#fafafa',
    bgSurface: '#ffffff', bgCard: '#f9f9f9', bgInput: '#f3f3f3',
    bgCode: '#ededed',
    bubbleAgent: '#dbeafe', bubbleAgentEnd: '#e0f0ff',
    bubbleUser: '#ffffff',
    bubbleAi: '#e8f4ff', bubbleAiEnd: '#dbeafe',
    bubbleAssistant: '#e6fffb', bubbleAssistantEnd: '#b5f5ec',
    textPrimary: '#1a1a1a', textSecondary: '#595959', textMuted: '#a6a6a6',
    border: '#e8e8e8',
  },
};
