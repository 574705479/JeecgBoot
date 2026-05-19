<template>
  <a-drawer
    v-model:open="drawerOpen"
    title="客服设置"
    placement="right"
    :width="360"
  >
    <div class="settings-content">
      <!-- 回复建议应用 -->
      <div class="setting-item">
        <div class="setting-label">
          <ThunderboltOutlined />
          <span>回复建议应用</span>
        </div>
        <div class="setting-desc">AI辅助模式下，为客服生成回复建议</div>
        <a-select
          v-model:value="selectedAppIdModel"
          placeholder="选择AI应用"
          style="width: 100%;"
          allowClear
          @change="onSelectedAppChange"
        >
          <a-select-option v-for="app in settings.aiAppList.value" :key="app.id" :value="app.id">
            {{ app.name }}
          </a-select-option>
        </a-select>
      </div>

      <a-divider />

      <!-- AI自动回复开关（全局配置） -->
      <div class="setting-item">
        <div class="setting-label">
          <RobotOutlined />
          <span>AI自动回复</span>
          <a-tag color="orange" size="small">全局</a-tag>
        </div>
        <div class="setting-desc">开启后，访客进入会话将先由AI自动回复；关闭后，访客默认接入在线客服人工回复</div>
        <a-switch
          v-model:checked="aiEnabledModel"
          checked-children="开启"
          un-checked-children="关闭"
          @change="onAiEnabledChange"
        />
      </div>

      <div class="setting-item" v-if="settings.aiEnabled.value">
        <div class="setting-label">
          <RobotOutlined />
          <span>使用AI开场白</span>
          <a-tag color="orange" size="small">全局</a-tag>
        </div>
        <div class="setting-desc">开启时使用AI应用中的开场白作为欢迎语；关闭则使用自动消息内容作为欢迎语</div>
        <a-switch
          v-model:checked="aiPrologueEnabledModel"
          checked-children="开启"
          un-checked-children="关闭"
          @change="onAiPrologueEnabledChange"
        />
      </div>

      <!-- 访客AI应用（全局配置，仅AI开启时显示） -->
      <div class="setting-item" v-if="settings.aiEnabled.value">
        <div class="setting-label">
          <RobotOutlined />
          <span>访客AI应用</span>
          <a-tag color="orange" size="small">全局</a-tag>
        </div>
        <div class="setting-desc">AI自动回复模式下，自动回复访客消息</div>
        <a-alert
          message="此设置为全局配置，修改后将影响所有客服的访客AI回复"
          type="warning"
          show-icon
          style="margin-bottom: 12px; font-size: 12px;"
        />
        <a-select
          v-model:value="visitorAppIdModel"
          placeholder="选择AI应用"
          style="width: 100%;"
          allowClear
          @change="onVisitorAppChange"
        >
          <a-select-option v-for="app in settings.aiAppList.value" :key="app.id" :value="app.id">
            {{ app.name }}
          </a-select-option>
        </a-select>
      </div>

      <a-divider />

      <!-- 新消息提示音 -->
      <div class="setting-item">
        <div class="setting-label">
          <span>新消息提示音</span>
        </div>
        <div class="setting-desc">收到新消息、新会话、转接会话时播放提示音</div>
        <a-switch
          v-model:checked="soundEnabledModel"
          checked-children="开启"
          un-checked-children="关闭"
          @change="onSoundEnabledChange"
        />
        <template v-if="settings.soundEnabled.value">
          <div class="setting-desc" style="margin-top: 12px">提示音音量（100% 与访客端默认一致；高音量可能失真）</div>
          <a-slider
            v-model:value="soundVolumePercentModel"
            :min="0"
            :max="200"
            :step="5"
            :marks="soundVolumeSliderMarks"
            :tooltip="soundVolumeTooltip"
          />

          <a-divider style="margin: 12px 0; font-size: 12px;">持续响铃</a-divider>

          <div class="sub-row sub-row--col">
            <div class="sub-text">
              <div class="sub-title">持续响铃模式</div>
              <div class="sub-tip">开启后，每隔 N 秒响铃一次，直到满足下方「停止条件」</div>
            </div>
            <a-radio-group
              v-model:value="continuousRingModeModel"
              button-style="solid"
              size="small"
              @change="onContinuousRingModeChangeProxy"
            >
              <a-radio-button value="off">关闭</a-radio-button>
              <a-radio-button value="on_blur">仅失焦/被遮挡时</a-radio-button>
              <a-radio-button value="always">始终响</a-radio-button>
            </a-radio-group>
          </div>

          <template v-if="settings.continuousRingActive.value">
            <div class="sub-row sub-row--col">
              <div class="sub-text">
                <div class="sub-title">停止条件</div>
                <div class="sub-tip">
                  <strong>回复任一即停</strong>：客服回复任意一个会话后立即停止响铃<br/>
                  <strong>全部回复才停</strong>：所有未回复访客都处理完才停止响铃
                </div>
              </div>
              <a-radio-group
                v-model:value="ringStopConditionModel"
                button-style="solid"
                size="small"
                @change="onRingStopConditionChangeProxy"
              >
                <a-radio-button value="any_one">回复任一即停</a-radio-button>
                <a-radio-button value="all_visitors">全部回复才停</a-radio-button>
              </a-radio-group>
            </div>

            <div class="sub-row">
              <div class="sub-text"><div class="sub-title">响铃间隔</div></div>
              <a-select
                v-model:value="ringIntervalSecondsModel"
                style="width: 110px;"
                size="small"
                @change="onRingIntervalChangeProxy"
              >
                <a-select-option :value="3">3 秒</a-select-option>
                <a-select-option :value="5">5 秒</a-select-option>
                <a-select-option :value="10">10 秒</a-select-option>
                <a-select-option :value="15">15 秒</a-select-option>
              </a-select>
            </div>

            <a-button
              v-if="!settings.isRingPaused.value"
              block
              size="small"
              style="margin-top: 8px;"
              @click="onPauseRingClick"
            >
              暂停响铃 5 分钟
            </a-button>
            <a-alert
              v-else
              type="info"
              show-icon
              closable
              style="margin-top: 8px;"
              :message="`已暂停响铃，剩余 ${formatPauseRemain(settings.pauseRemainSeconds.value)}`"
              @close="onResumeRingClick"
            />
          </template>
        </template>
      </div>

      <a-divider />

      <!-- 外观主题 -->
      <div class="setting-item">
        <div class="setting-label">
          <BgColorsOutlined />
          <span>外观主题</span>
        </div>
        <div class="setting-desc">选择预设主题或自定义配色</div>
        <div class="theme-presets">
          <div
            v-for="(preset, key) in theme.THEME_PRESETS"
            :key="key"
            class="theme-preset-item"
            :class="{ active: theme.currentThemeKey.value === key }"
            @click="theme.selectTheme(String(key))"
          >
            <div class="theme-color" :style="{ background: `linear-gradient(135deg, ${preset.brandStart}, ${preset.brandEnd})` }"></div>
            <span>{{ preset.name }}</span>
          </div>
          <div
            class="theme-preset-item"
            :class="{ active: theme.currentThemeKey.value === 'custom' }"
            @click="theme.selectTheme('custom')"
          >
            <div class="theme-color custom-color">
              <SettingOutlined />
            </div>
            <span>自定义</span>
          </div>
        </div>
        <div class="custom-colors" v-if="theme.currentThemeKey.value === 'custom'">
          <div class="color-row">
            <label>主色调起始</label>
            <input type="color" :value="theme.customTheme.value.brandStart" @input="theme.onColorInput('brandStart', $event)" />
          </div>
          <div class="color-row">
            <label>主色调结束</label>
            <input type="color" :value="theme.customTheme.value.brandEnd" @input="theme.onColorInput('brandEnd', $event)" />
          </div>
          <div class="color-row">
            <label>页面背景</label>
            <input type="color" :value="theme.customTheme.value.bgPage" @input="theme.onColorInput('bgPage', $event)" />
          </div>
          <div class="color-row">
            <label>聊天背景</label>
            <input type="color" :value="theme.customTheme.value.bgChat" @input="theme.onColorInput('bgChat', $event)" />
          </div>
          <div class="color-row">
            <label>客服气泡</label>
            <input type="color" :value="theme.customTheme.value.bubbleAgent" @input="theme.onColorInput('bubbleAgent', $event)" />
          </div>
          <div class="color-row">
            <label>用户气泡</label>
            <input type="color" :value="theme.customTheme.value.bubbleUser" @input="theme.onColorInput('bubbleUser', $event)" />
          </div>
          <div class="color-row">
            <label>AI气泡</label>
            <input type="color" :value="theme.customTheme.value.bubbleAi" @input="theme.onColorInput('bubbleAi', $event)" />
          </div>
          <div class="color-row">
            <label>助手气泡</label>
            <input type="color" :value="theme.customTheme.value.bubbleAssistant" @input="theme.onColorInput('bubbleAssistant', $event)" />
          </div>
          <a-divider style="margin: 8px 0;">面板与文字</a-divider>
          <div class="color-row">
            <label>面板背景</label>
            <input type="color" :value="theme.customTheme.value.bgSurface" @input="theme.onColorInput('bgSurface', $event)" />
          </div>
          <div class="color-row">
            <label>卡片背景</label>
            <input type="color" :value="theme.customTheme.value.bgCard" @input="theme.onColorInput('bgCard', $event)" />
          </div>
          <div class="color-row">
            <label>输入框背景</label>
            <input type="color" :value="theme.customTheme.value.bgInput" @input="theme.onColorInput('bgInput', $event)" />
          </div>
          <div class="color-row">
            <label>代码块背景</label>
            <input type="color" :value="theme.customTheme.value.bgCode" @input="theme.onColorInput('bgCode', $event)" />
          </div>
          <div class="color-row">
            <label>主文字色</label>
            <input type="color" :value="theme.customTheme.value.textPrimary" @input="theme.onColorInput('textPrimary', $event)" />
          </div>
          <div class="color-row">
            <label>次文字色</label>
            <input type="color" :value="theme.customTheme.value.textSecondary" @input="theme.onColorInput('textSecondary', $event)" />
          </div>
          <div class="color-row">
            <label>弱文字色</label>
            <input type="color" :value="theme.customTheme.value.textMuted" @input="theme.onColorInput('textMuted', $event)" />
          </div>
          <div class="color-row">
            <label>边框色</label>
            <input type="color" :value="theme.customTheme.value.border" @input="theme.onColorInput('border', $event)" />
          </div>
          <div class="color-row">
            <label>标题栏起始</label>
            <input type="color" :value="theme.customTheme.value.barStart" @input="theme.onColorInput('barStart', $event)" />
          </div>
          <div class="color-row">
            <label>标题栏结束</label>
            <input type="color" :value="theme.customTheme.value.barEnd" @input="theme.onColorInput('barEnd', $event)" />
          </div>
          <div class="color-row">
            <label>品牌区文字</label>
            <input type="color" :value="theme.customTheme.value.brandText" @input="theme.onColorInput('brandText', $event)" />
          </div>
        </div>
      </div>

    </div>
  </a-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  ThunderboltOutlined, RobotOutlined,
  BgColorsOutlined, SettingOutlined,
} from '@ant-design/icons-vue';
import { useCsWorkbenchContext, type ContinuousRingMode, type RingStopCondition } from '../context';
import { useCsWorkbenchTheme } from '../composables/useCsWorkbenchTheme';

defineOptions({ name: 'CsWorkbenchSettingsDrawer' });

const ctx = useCsWorkbenchContext();
const theme = useCsWorkbenchTheme();
const settings = ctx.settings;

const drawerOpen = computed({
  get: () => ctx.showSettingsDrawer.value,
  set: (v: boolean) => { ctx.showSettingsDrawer.value = v; },
});

const selectedAppIdModel = computed({
  get: () => settings.selectedAppId.value,
  set: (v: string | undefined) => { settings.selectedAppId.value = v; },
});
const visitorAppIdModel = computed({
  get: () => settings.visitorAppId.value,
  set: (v: string | undefined) => { settings.visitorAppId.value = v; },
});
const aiEnabledModel = computed({
  get: () => settings.aiEnabled.value,
  set: (v: boolean) => { settings.aiEnabled.value = v; },
});
const aiPrologueEnabledModel = computed({
  get: () => settings.aiPrologueEnabled.value,
  set: (v: boolean) => { settings.aiPrologueEnabled.value = v; },
});
const soundEnabledModel = computed({
  get: () => settings.soundEnabled.value,
  set: (v: boolean) => { settings.soundEnabled.value = v; },
});
const soundVolumePercentModel = computed({
  get: () => settings.soundVolumePercent.value,
  set: (v: number) => { settings.soundVolumePercent.value = v; },
});

const continuousRingModeModel = computed({
  get: () => settings.continuousRingMode.value,
  set: (v: ContinuousRingMode) => { settings.continuousRingMode.value = v; },
});
const ringStopConditionModel = computed({
  get: () => settings.ringStopCondition.value,
  set: (v: RingStopCondition) => { settings.ringStopCondition.value = v; },
});
const ringIntervalSecondsModel = computed({
  get: () => settings.ringIntervalSeconds.value,
  set: (v: number) => { settings.ringIntervalSeconds.value = v; },
});

const soundVolumeSliderMarks: Record<number, string> = { 0: '0%', 100: '100%', 200: '200%' };
const soundVolumeTooltip = { formatter: (v?: number) => (v != null ? `${v}%` : '') };

function onSelectedAppChange(v: any) {
  settings.onAppChange(v as string | undefined);
}
function onVisitorAppChange(v: any) {
  settings.onVisitorAppChange(v as string | undefined);
}
function onAiEnabledChange(v: any) {
  settings.onAiEnabledChange(Boolean(v));
}
function onAiPrologueEnabledChange(v: any) {
  settings.onAiPrologueEnabledChange(Boolean(v));
}
function onSoundEnabledChange(v: any) {
  settings.onSoundEnabledChange(Boolean(v));
}

function onContinuousRingModeChangeProxy(e: any) {
  const v = (e?.target?.value ?? e) as ContinuousRingMode;
  settings.onContinuousRingModeChange(v);
}
function onRingStopConditionChangeProxy(e: any) {
  const v = (e?.target?.value ?? e) as RingStopCondition;
  settings.onRingStopConditionChange(v);
}
function onRingIntervalChangeProxy(v: any) {
  settings.onRingIntervalChange(Number(v));
}
function onPauseRingClick() {
  settings.onPauseRing(5);
}
function onResumeRingClick() {
  settings.onResumeRing();
}

function formatPauseRemain(sec: number): string {
  const s = Math.max(0, Math.floor(sec));
  const mm = Math.floor(s / 60);
  const ss = s % 60;
  return `${mm}:${String(ss).padStart(2, '0')}`;
}
</script>

<style lang="less" scoped>
.settings-content {
  .setting-item {
    margin-bottom: 16px;
    background: var(--cs-bg-card);
    border-radius: 10px;
    padding: 16px;

    .setting-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 8px;
      color: var(--cs-text-primary);
      :deep(.anticon) { color: var(--cs-brand-start); }
    }

    .setting-desc {
      font-size: 12px;
      color: var(--cs-text-muted);
      margin-bottom: 12px;
    }
  }

  .sub-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-top: 12px;

    &.sub-row--col {
      flex-direction: column;
      align-items: stretch;
      gap: 8px;
    }

    .sub-text {
      flex: 1;
      min-width: 0;
    }

    .sub-title {
      font-size: 13px;
      color: var(--cs-text-primary);
      font-weight: 500;
    }

    .sub-tip {
      font-size: 12px;
      color: var(--cs-text-muted);
      line-height: 1.6;
      margin-top: 2px;

      strong {
        color: var(--cs-text-secondary);
        font-weight: 600;
      }
    }
  }

  .theme-presets {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
    margin-top: 8px;
  }

  .theme-preset-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    padding: 8px;
    border-radius: 8px;
    border: 2px solid transparent;
    transition: all 0.2s;

    &:hover { background: var(--cs-bg-input); }
    &.active { border-color: var(--cs-brand-start, #4096ff); background: rgba(var(--cs-brand-rgb, 64, 150, 255), 0.06); }

    .theme-color {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      box-shadow: 0 2px 6px rgba(0,0,0,0.15);

      &.custom-color {
        background: conic-gradient(#ff4d4f, #fa8c16, #fadb14, #52c41a, #13c2c2, #1677ff, #722ed1, #ff4d4f);
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        font-size: 16px;
      }
    }

    span { font-size: 12px; color: var(--cs-text-secondary); }
  }

  .custom-colors {
    margin-top: 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .color-row {
    display: flex;
    align-items: center;
    justify-content: space-between;

    label { font-size: 13px; color: var(--cs-text-secondary); }

    input[type="color"] {
      width: 36px;
      height: 28px;
      border: 1px solid var(--cs-border);
      border-radius: 6px;
      padding: 2px;
      cursor: pointer;
      background: none;
    }
  }
}
</style>
