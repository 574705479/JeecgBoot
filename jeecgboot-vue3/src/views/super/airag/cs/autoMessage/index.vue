<template>
  <div class="auto-msg-page">
    <a-spin :spinning="loading">
      <!-- 顶栏：标题 + 操作 -->
      <div class="page-header">
        <div class="header-left">
          <h2 class="page-title">自动消息</h2>
          <span class="page-desc">访客接入客服后自动发送的欢迎消息，支持多语言配置</span>
        </div>
        <a-button type="primary" :loading="saving" @click="handleSave">
          保存
        </a-button>
      </div>

      <!-- 默认语言 + 语言 Tab -->
      <div class="main-body">
        <div class="lang-bar">
          <div class="lang-tabs">
            <div
              v-for="(langConfig, langKey) in config.languages"
              :key="langKey"
              :class="['lang-tab', { active: activeTab === langKey }]"
              @click="activeTab = String(langKey)"
            >
              {{ langConfig.label }}
              <span v-if="langKey === config.defaultLang" class="default-badge">默认</span>
              <span v-if="langConfig.messages.length > 0" class="msg-count">{{ langConfig.messages.length }}</span>
            </div>
          </div>
          <div class="lang-default">
            <span class="default-label">默认语言</span>
            <a-select v-model:value="config.defaultLang" size="small" style="width: 130px;">
              <a-select-option v-for="(lc, lk) in config.languages" :key="lk" :value="lk">
                {{ lc.label }}
              </a-select-option>
            </a-select>
          </div>
        </div>

        <!-- 消息编辑区 -->
        <div class="editor-area">
          <div
            v-for="(langConfig, langKey) in config.languages"
            :key="langKey"
            v-show="activeTab === langKey"
            class="lang-content"
          >
            <!-- 消息列表 -->
            <div v-if="langConfig.messages.length > 0" class="msg-list">
              <div
                v-for="(msg, index) in langConfig.messages"
                :key="`${langKey}_${index}_${langConfig.messages.length}`"
                class="msg-card"
              >
                <div class="msg-card-head">
                  <div class="msg-index">
                    <span class="index-num">{{ index + 1 }}</span>
                    <span class="index-label">{{ index === 0 ? '首条欢迎语' : `第 ${index + 1} 条消息` }}</span>
                  </div>
                  <a-popconfirm title="确认删除?" @confirm="removeMessage(String(langKey), index)" placement="topRight">
                    <a class="msg-delete" title="删除">
                      <DeleteOutlined />
                    </a>
                  </a-popconfirm>
                </div>
                <div class="msg-editor-box">
                  <Tinymce
                    v-model:modelValue="msg.content"
                    :height="220"
                    :showImageUpload="true"
                    :toolbar="editorToolbar"
                    :plugins="editorPlugins"
                    :menubar="''"
                    :autoFocus="false"
                  />
                </div>
              </div>
            </div>

            <!-- 空态 -->
            <div v-else class="empty-state">
              <div class="empty-icon">
                <MessageOutlined />
              </div>
              <p>暂无自动消息</p>
              <span>添加消息后，访客接入客服时将自动发送</span>
            </div>

            <!-- 添加按钮 -->
            <div class="add-msg-btn" @click="addMessage(String(langKey))">
              <PlusOutlined />
              <span>添加消息</span>
            </div>
          </div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { DeleteOutlined, PlusOutlined, MessageOutlined } from '@ant-design/icons-vue';
import { Tinymce } from '/@/components/Tinymce/index';
import { defHttp } from '/@/utils/http/axios';
import { useGlobSetting } from '/@/hooks/setting';

const globSetting = useGlobSetting();

function normalizeImgUrls(html: string): string {
  if (!html) return html;
  try {
    const origin = new URL(globSetting.domainUrl).origin;
    return html.replace(
      /(<img[^>]*?\ssrc=["'])(\/[^"']+)(["'])/gi,
      (_match, pre, path, suf) => `${pre}${origin}${path}${suf}`
    );
  } catch { return html; }
}

const loading = ref(false);
const saving = ref(false);
const activeTab = ref('zh-CN');

interface AutoMessageItem {
  content: string;
}

interface LangConfig {
  label: string;
  messages: AutoMessageItem[];
}

interface AutoMessagesConfig {
  defaultLang: string;
  languages: Record<string, LangConfig>;
}

const config = reactive<AutoMessagesConfig>({
  defaultLang: 'zh-CN',
  languages: {
    'zh-CN': { label: '中文简体', messages: [] },
    'zh-TW': { label: '中文繁體', messages: [] },
    'en': { label: 'English', messages: [] },
  },
});

// Tinymce 编辑器配置
const editorPlugins = 'lists image link media fullscreen';
const editorToolbar =
  'bold italic underline strikethrough | bullist numlist | subscript superscript | alignleft aligncenter alignright | fontsize styles | forecolor backcolor | blockquote hr | removeformat | link image media';

async function loadConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/auto-messages' }, { successMessageMode: 'none' });
    const data = res || {};
    if (data.defaultLang) {
      config.defaultLang = data.defaultLang;
    }
    if (data.languages) {
      for (const key of Object.keys(data.languages)) {
        if (config.languages[key]) {
          config.languages[key].label = data.languages[key].label || config.languages[key].label;
          config.languages[key].messages = (data.languages[key].messages || []).map((m: any) => ({
            content: normalizeImgUrls(m.content || ''),
          }));
        } else {
          config.languages[key] = {
            label: data.languages[key].label || key,
            messages: (data.languages[key].messages || []).map((m: any) => ({
              content: normalizeImgUrls(m.content || ''),
            })),
          };
        }
      }
    }
  } catch (e) {
    console.error('加载自动消息配置失败', e);
  } finally {
    loading.value = false;
  }
}

function addMessage(langKey: string) {
  if (config.languages[langKey]) {
    config.languages[langKey].messages.push({ content: '' });
  }
}

function removeMessage(langKey: string, index: number) {
  if (!config.languages[langKey]) return;
  config.languages[langKey].messages.splice(index, 1);
}

async function handleSave() {
  saving.value = true;
  try {
    await defHttp.put(
      { url: '/cs/agent/global/auto-messages', data: { ...config } },
      { successMessageMode: 'none' }
    );
    message.success('保存成功');
  } catch (e) {
    message.error('保存失败');
    console.error('保存自动消息配置失败', e);
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  loadConfig();
});
</script>

<style lang="less" scoped>
.auto-msg-page {
  height: 100%;
  padding: 20px 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 120px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;

  .header-left {
    .page-title {
      margin: 0 0 4px;
      font-size: 20px;
      font-weight: 600;
      color: #1a1a1a;
    }
    .page-desc {
      font-size: 13px;
      color: #8c8c8c;
    }
  }
}

.main-body {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.lang-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafbfc;

  .lang-tabs {
    display: flex;

    .lang-tab {
      padding: 14px 20px;
      font-size: 14px;
      color: #666;
      cursor: pointer;
      position: relative;
      transition: color 0.2s;
      display: flex;
      align-items: center;
      gap: 6px;
      user-select: none;

      &:hover {
        color: #333;
      }

      &.active {
        color: #1677ff;
        font-weight: 500;

        &::after {
          content: '';
          position: absolute;
          bottom: 0;
          left: 12px;
          right: 12px;
          height: 2px;
          background: #1677ff;
          border-radius: 1px;
        }
      }

      .default-badge {
        font-size: 10px;
        padding: 1px 5px;
        border-radius: 3px;
        background: #e6f4ff;
        color: #1677ff;
        line-height: 1.4;
      }

      .msg-count {
        font-size: 11px;
        min-width: 18px;
        height: 18px;
        line-height: 18px;
        text-align: center;
        border-radius: 9px;
        background: #f0f0f0;
        color: #666;
      }

      &.active .msg-count {
        background: #e6f4ff;
        color: #1677ff;
      }
    }
  }

  .lang-default {
    display: flex;
    align-items: center;
    gap: 8px;

    .default-label {
      font-size: 13px;
      color: #8c8c8c;
      white-space: nowrap;
    }
  }
}

.editor-area {
  padding: 20px;
  min-height: 400px;
}

.lang-content {
  .msg-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .msg-card {
    border: 1px solid #eee;
    border-radius: 8px;
    overflow: hidden;
    transition: border-color 0.2s, box-shadow 0.2s;

    &:hover {
      border-color: #d9d9d9;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    }

    .msg-card-head {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 16px;
      background: #fafbfc;
      border-bottom: 1px solid #f5f5f5;

      .msg-index {
        display: flex;
        align-items: center;
        gap: 8px;

        .index-num {
          width: 22px;
          height: 22px;
          line-height: 22px;
          text-align: center;
          border-radius: 6px;
          background: #1677ff;
          color: #fff;
          font-size: 12px;
          font-weight: 600;
        }
        .index-label {
          font-size: 13px;
          color: #555;
        }
      }

      .msg-delete {
        color: #bbb;
        font-size: 14px;
        cursor: pointer;
        padding: 4px 6px;
        border-radius: 4px;
        transition: all 0.2s;

        &:hover {
          color: #ff4d4f;
          background: #fff1f0;
        }
      }
    }

    .msg-editor-box {
      padding: 0;
    }
  }

  .empty-state {
    text-align: center;
    padding: 48px 20px;
    color: #bbb;

    .empty-icon {
      font-size: 40px;
      margin-bottom: 12px;
      color: #d9d9d9;
    }

    p {
      margin: 0 0 4px;
      font-size: 15px;
      color: #999;
    }

    span {
      font-size: 13px;
      color: #bbb;
    }
  }

  .add-msg-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    margin-top: 16px;
    padding: 10px;
    border: 1px dashed #d9d9d9;
    border-radius: 8px;
    color: #8c8c8c;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
    user-select: none;

    &:hover {
      color: #1677ff;
      border-color: #1677ff;
      background: #f6f9ff;
    }
  }
}
</style>
