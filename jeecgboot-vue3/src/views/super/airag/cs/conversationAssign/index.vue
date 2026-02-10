<template>
  <div class="conversation-assign-container">
    <a-card title="对话分配" :bordered="false">
      <a-spin :spinning="loading">
        <!-- 客服分配方式 -->
        <div class="config-section">
          <div class="section-title">客服分配方式：</div>
          <div class="section-body">
            <a-radio-group v-model:value="config.assignMode" @change="saveConfig">
              <div class="radio-item">
                <a-radio value="round_robin">在线客服轮流分配</a-radio>
                <div class="radio-desc">
                  <span class="desc-icon">●</span>
                  新接入的对话按客服账号创建时间先到后，轮流分配到在线的客服
                </div>
              </div>
              <div class="radio-item">
                <a-radio value="saturation">在线客服饱和度分配</a-radio>
                <div class="radio-desc">
                  <span class="desc-icon">●</span>
                  新接入的对话分配到当前饱和度最低的客服，若多个客服饱和度相同，则随机分配
                </div>
              </div>
            </a-radio-group>
          </div>
        </div>

        <a-divider />

        <!-- 继承上一次客服 -->
        <div class="config-section">
          <div class="section-title">继承上一次客服：</div>
          <div class="section-body">
            <div class="inline-setting">
              <a-switch 
                v-model:checked="config.inheritLastAgent.enabled" 
                @change="saveConfig"
              />
              <span class="setting-label">有效期：</span>
              <a-button size="small" @click="decrementInherit">-</a-button>
              <a-input-number 
                v-model:value="config.inheritLastAgent.expireMinutes" 
                :min="0" 
                :max="9999"
                size="small"
                style="width: 80px; margin: 0 4px;"
                @change="saveConfig"
              />
              <a-button size="small" @click="incrementInherit">+</a-button>
              <span class="setting-unit">分钟</span>
            </div>
            <div class="setting-tips">
              <span class="desc-icon">●</span>
              新接入的对话，优先分配给最后接待他的客服，若最后接待客服不在线、忙碌或超过接待上限，则按上面的客服分配方式进行分配
              <span class="desc-icon highlight">●</span>
              有效期大于等于0，且无上限；有效期为0分钟表示永远按照上一次接待客服进行分配；有效期 > 0 分钟，表示在对话保持有效期内优先按上一次接待客服分配
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 对话保持 -->
        <div class="config-section">
          <div class="section-title">对话保持：</div>
          <div class="section-body">
            <div class="inline-setting">
              <a-input-number 
                v-model:value="config.conversationHold.minutes" 
                :min="1" 
                :max="999"
                style="width: 120px;"
                @change="saveConfig"
              />
              <span class="setting-unit">分钟</span>
            </div>
            <div class="setting-tips">
              <span class="desc-icon">●</span>
              对话保持的时间在1-999分钟之间
            </div>
          </div>
        </div>

        <a-divider />

        <!-- 客服超时未回复，对话列表提示设置 -->
        <div class="config-section">
          <div class="section-title">客服超时未回复，对话列表提示设置</div>
          <div class="section-body">
            <div class="inline-setting">
              <span class="setting-label">功能开启：</span>
              <a-switch 
                v-model:checked="config.agentTimeoutReminder.enabled" 
                @change="saveConfig"
              />
            </div>
            <div class="inline-setting" style="margin-top: 12px;">
              <span class="setting-label">客服回复超时阈值：</span>
              <a-button size="small" @click="decrementTimeout">-</a-button>
              <a-input-number 
                v-model:value="config.agentTimeoutReminder.seconds" 
                :min="1" 
                :max="999"
                size="small"
                style="width: 80px; margin: 0 4px;"
                @change="saveConfig"
              />
              <a-button size="small" @click="incrementTimeout">+</a-button>
              <span class="setting-unit">秒</span>
            </div>
            <div class="setting-tips">
              <span class="desc-icon">●</span>
              开启"超时提示"并且阈值大于0，则会在对话列表中对未回复的消息进行"访客等待回复时长提示"。阈值范围：1-999秒，并且秒数不大于对话保持秒数
            </div>
          </div>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

const { createMessage: message } = useMessage();

const loading = ref(false);
let saveTimer: ReturnType<typeof setTimeout> | null = null;

const config = ref({
  assignMode: 'saturation',
  inheritLastAgent: {
    enabled: true,
    expireMinutes: 60,
  },
  conversationHold: {
    minutes: 10,
  },
  agentTimeoutReminder: {
    enabled: false,
    seconds: 20,
  },
});

onMounted(async () => {
  await loadConfig();
});

async function loadConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/conversation-assign' });
    const data = res?.result || res;
    if (data) {
      config.value = {
        assignMode: data.assignMode || 'saturation',
        inheritLastAgent: {
          enabled: data.inheritLastAgent?.enabled !== false,
          expireMinutes: data.inheritLastAgent?.expireMinutes ?? 60,
        },
        conversationHold: {
          minutes: data.conversationHold?.minutes ?? 10,
        },
        agentTimeoutReminder: {
          enabled: data.agentTimeoutReminder?.enabled === true,
          seconds: data.agentTimeoutReminder?.seconds ?? 20,
        },
      };
    }
  } catch (e) {
    console.error('加载对话分配配置失败', e);
  } finally {
    loading.value = false;
  }
}

function saveConfig() {
  // 防抖保存
  if (saveTimer) {
    clearTimeout(saveTimer);
  }
  saveTimer = setTimeout(async () => {
    try {
      await defHttp.put({
        url: '/cs/agent/global/conversation-assign',
        data: config.value,
      });
      message.success('保存成功');
    } catch (e) {
      console.error('保存对话分配配置失败', e);
      message.error('保存失败');
    }
  }, 500);
}

function decrementInherit() {
  if (config.value.inheritLastAgent.expireMinutes > 0) {
    config.value.inheritLastAgent.expireMinutes--;
    saveConfig();
  }
}

function incrementInherit() {
  config.value.inheritLastAgent.expireMinutes++;
  saveConfig();
}

function decrementTimeout() {
  if (config.value.agentTimeoutReminder.seconds > 1) {
    config.value.agentTimeoutReminder.seconds--;
    saveConfig();
  }
}

function incrementTimeout() {
  if (config.value.agentTimeoutReminder.seconds < 999) {
    config.value.agentTimeoutReminder.seconds++;
    saveConfig();
  }
}
</script>

<style lang="less" scoped>
.conversation-assign-container {
  padding: 16px;
  max-width: 900px;

  .config-section {
    .section-title {
      font-weight: 600;
      font-size: 14px;
      margin-bottom: 12px;
    }

    .section-body {
      padding-left: 16px;
    }

    .radio-item {
      margin-bottom: 12px;

      .radio-desc {
        padding-left: 24px;
        color: #fa8c16;
        font-size: 12px;
        margin-top: 4px;
      }
    }

    .inline-setting {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;

      .setting-label {
        font-size: 14px;
      }

      .setting-unit {
        font-size: 13px;
        color: #666;
      }
    }

    .setting-tips {
      margin-top: 8px;
      color: #fa8c16;
      font-size: 12px;
      line-height: 1.8;

      .desc-icon {
        margin-right: 4px;

        &.highlight {
          color: #fa8c16;
        }
      }
    }
  }
}
</style>
