<template>
  <div class="user-chat-container">
    <div v-if="fatalError" class="chat-fatal-error">
      <div class="fatal-title">无法进入在线客服</div>
      <div class="fatal-desc">{{ fatalErrorMessage }}</div>
    </div>
    <template v-else>
      <!-- 留言板模式（无在线客服时显示） -->
      <div v-if="showLeaveMessageBoard" class="leave-message-board">
        <div class="board-header">
          <img class="app-avatar" :src="appInfo.avatar || defaultAvatar" />
          <div class="board-title">
            <span class="app-name">{{ appInfo.name || '在线客服' }}</span>
            <span class="board-subtitle">{{ messageBoardConfig.subtitle || '客服不在线，请留言' }}</span>
          </div>
        </div>
        <div class="board-body">
          <a-form :model="leaveMessageForm" layout="vertical">
            <a-form-item label="留言内容" :rules="[{required: true, message: '请输入留言内容'}]">
              <a-textarea v-model:value="leaveMessageForm.content" placeholder="请输入您的留言" :rows="4" />
            </a-form-item>
            <a-form-item v-if="messageBoardConfig.fields?.name?.show" label="姓名"
              :rules="messageBoardConfig.fields?.name?.required ? [{required: true, message: '请输入姓名'}] : []">
              <a-input v-model:value="leaveMessageForm.name" placeholder="请输入姓名" />
            </a-form-item>
            <a-form-item v-if="messageBoardConfig.fields?.phone?.show" label="手机"
              :rules="messageBoardConfig.fields?.phone?.required ? [{required: true, message: '请输入手机号'}] : []">
              <a-input v-model:value="leaveMessageForm.phone" placeholder="请输入手机号" />
            </a-form-item>
            <a-form-item v-if="messageBoardConfig.fields?.email?.show" label="邮箱"
              :rules="messageBoardConfig.fields?.email?.required ? [{required: true, message: '请输入邮箱'}] : []">
              <a-input v-model:value="leaveMessageForm.email" placeholder="请输入邮箱" />
            </a-form-item>
            <a-form-item v-if="messageBoardConfig.fields?.qq?.show" label="QQ">
              <a-input v-model:value="leaveMessageForm.qq" placeholder="请输入QQ号" />
            </a-form-item>
            <a-form-item v-if="messageBoardConfig.fields?.wechat?.show" label="微信">
              <a-input v-model:value="leaveMessageForm.wechat" placeholder="请输入微信号" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" block :loading="submittingLeaveMessage" @click="submitLeaveMessage">
                提交留言
              </a-button>
            </a-form-item>
          </a-form>
          <div v-if="leaveMessageSubmitted" class="submit-success">
            <CheckCircleOutlined style="font-size: 32px; color: #52c41a;" />
            <p>留言已提交，客服会尽快回复您</p>
          </div>
        </div>
      </div>

      <!-- 正常聊天模式 -->
      <template v-else>
      <div class="chat-main-layout" :style="dynamicCssVars">
      <div class="chat-main-column">
      <!-- 留言回复提醒卡片 -->
      <div v-if="unreadReplies.length > 0" class="leave-message-replies">
        <div v-for="reply in unreadReplies" :key="reply.id" class="reply-card">
          <div class="reply-header">
            <span class="reply-label">留言回复</span>
            <a-button type="link" size="small" @click="dismissReply(reply.id)">关闭</a-button>
          </div>
          <div class="reply-original">您的留言：{{ reply.content || '-' }}</div>
          <div class="reply-content">客服回复：{{ reply.reply }}</div>
          <div class="reply-time">{{ reply.replyTime }}</div>
        </div>
      </div>
      <!-- 头部 -->
      <div class="chat-header" v-if="chatWindowConfig.headerVisible !== false" :style="{ background: chatWindowConfig.themeColor || '#667eea' }">
        <div class="header-info">
          <img class="app-avatar" :src="chatWindowConfig.logo ? resolveFileUrl(chatWindowConfig.logo) : (appInfo.avatar || defaultAvatar)" />
          <div class="app-info">
            <span class="app-name">{{ chatWindowConfig.pageTitle || appInfo.name || '在线客服' }}</span>
            <span class="status-text">
              <span :class="['status-dot', connectionStatus]"></span>
              {{ connectionStatusText }}
              <a-tag v-if="hasAgent && replyMode === 1" color="green" size="small" style="margin-left: 6px;">人工服务</a-tag>
              <a-tag v-else-if="replyMode === 0" color="blue" size="small" style="margin-left: 6px;">AI客服</a-tag>
            </span>
          </div>
        </div>
        <div class="header-actions">
        </div>
      </div>
      <!-- 滚动文字（跑马灯） -->
      <div v-if="chatWindowConfig.scrollText" class="scroll-text-bar"
           :style="{ background: chatWindowConfig.scrollTextBgColor || '#1890ff', color: chatWindowConfig.scrollTextColor || '#fff' }">
        <div class="scroll-text-content" :style="{ animationDuration: (chatWindowConfig.scrollDuration || 15) + 's' }">
          {{ chatWindowConfig.scrollText }}
        </div>
      </div>

    <!-- 消息区域 -->
    <div class="chat-messages" ref="messagesRef" @scroll.passive="handleMessageScroll">
      <div v-if="loading" class="loading-wrapper">
        <a-spin />
      </div>
      <template v-else>
        <div v-if="messages.length === 0" class="empty-messages">
          <MessageOutlined style="font-size: 48px; color: #bfbfbf" />
          <p>开始您的咨询吧~</p>
        </div>
        <div v-for="msg in displayMessages" :key="msg.id" 
             :class="['message-item', getMessageClass(msg)]">
          <!-- 系统消息 -->
          <div v-if="msg.senderType === 3" class="system-message">
            {{ msg.content }}
          </div>
          <!-- 用户消息 (senderType === 0 表示用户) -->
          <div v-else-if="isUserMessage(msg)" class="user-message">
            <div class="message-content">
              <div v-if="msg.content" class="message-text">{{ msg.content }}</div>
              <div
                v-if="getMediaGridData(msg).items.length"
                class="message-media-grid"
                :class="`media-grid--${Math.min(getMediaGridData(msg).total, 4)}`"
              >
                <div
                  class="media-item"
                  v-for="(item, index) in getMediaGridData(msg).items"
                  :key="`${item.url}_${index}`"
                >
                  <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview(msg, item)" />
                  <video v-else :src="getAttachmentUrl(item)" controls @click="openFilePreview(item)" />
                  <div
                    v-if="index === getMediaGridData(msg).items.length - 1 && getMediaGridData(msg).extraCount > 0"
                    class="media-more"
                    @click.stop="openMediaViewer(msg)"
                  >
                    +{{ getMediaGridData(msg).extraCount }}
                  </div>
                </div>
              </div>
              <div v-if="getFileAttachments(msg).length" class="message-file-list">
                <div
                  class="file-item"
                  v-for="(item, index) in getFileAttachments(msg)"
                  :key="`${item.url}_${index}`"
                  @click="openFilePreview(item)"
                >
                  {{ item.name || item.url }}
                </div>
              </div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
            <img class="avatar" :src="getUserAvatar(msg)" />
          </div>
          <!-- 客服/AI消息 (senderType === 1 AI, 2 客服) -->
          <div v-else class="agent-message">
            <img class="avatar" :src="getAgentAvatar(msg)" />
            <div class="message-content">
              <div class="sender-info">
                <span class="sender-name">{{ msg.senderName || '客服' }}</span>
                <a-tag v-if="msg.senderType === 1" color="blue" size="small">AI</a-tag>
              </div>
              <div v-if="msg.content" class="message-text" v-html="renderMessage(msg.content)"></div>
              <div
                v-if="getMediaGridData(msg).items.length"
                class="message-media-grid"
                :class="`media-grid--${Math.min(getMediaGridData(msg).total, 4)}`"
              >
                <div
                  class="media-item"
                  v-for="(item, index) in getMediaGridData(msg).items"
                  :key="`${item.url}_${index}`"
                >
                  <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview(msg, item)" />
                  <video v-else :src="getAttachmentUrl(item)" controls @click="openFilePreview(item)" />
                  <div
                    v-if="index === getMediaGridData(msg).items.length - 1 && getMediaGridData(msg).extraCount > 0"
                    class="media-more"
                    @click.stop="openMediaViewer(msg)"
                  >
                    +{{ getMediaGridData(msg).extraCount }}
                  </div>
                </div>
              </div>
              <div v-if="getFileAttachments(msg).length" class="message-file-list">
                <div
                  class="file-item"
                  v-for="(item, index) in getFileAttachments(msg)"
                  :key="`${item.url}_${index}`"
                  @click="openFilePreview(item)"
                >
                  {{ item.name || item.url }}
                </div>
              </div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
          </div>
        </div>
        <!-- 客服正在输入提示 -->
        <div v-if="agentTyping" class="typing-indicator">
          <img class="avatar" :src="getAgentAvatar()" />
          <div class="typing-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </template>
    </div>

    <!-- 预设问题（配置FAQ开启时隐藏AI预设问题，优先展示配置FAQ） -->
    <div v-if="presetQuestions.length > 0 && !(chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0)" class="preset-questions">
      <div class="preset-title">
        <BulbOutlined />
        <span>常见问题</span>
      </div>
      <div class="preset-list">
        <a-button 
          v-for="(question, index) in presetQuestions" 
          :key="index"
          size="small"
          @click="selectPresetQuestion(question)"
        >
          {{ question }}
        </a-button>
      </div>
    </div>

    <!-- 手机端FAQ（仅窄屏显示，PC端在右侧sidebar展示） -->
    <div v-if="chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0" class="faq-mobile-section">
      <div class="faq-mobile-header" @click="faqMobileExpanded = !faqMobileExpanded">
        <QuestionCircleOutlined />
        <span>常见问题</span>
        <span class="faq-mobile-toggle">{{ faqMobileExpanded ? '收起' : '展开' }}</span>
      </div>
      <div v-if="faqMobileExpanded" class="faq-mobile-list">
        <div v-for="(faq, idx) in chatWindowConfig.faqList" :key="idx" class="faq-mobile-item" @click="handleFaqClick(faq)">
          {{ faq.question }}
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input" v-if="!conversationClosed">
      <!-- 附件预览 -->
      <div v-if="attachmentList.length > 0" class="attachment-preview-bar">
        <div v-for="(att, idx) in attachmentList" :key="idx" class="attachment-thumb">
          <img v-if="att.type === 'image'" :src="att.previewUrl || att.url" class="att-img" />
          <div v-else class="att-file">
            <span v-if="att.type === 'video'">🎬</span>
            <span v-else>📄</span>
            <span class="att-name">{{ att.name }}</span>
          </div>
          <span class="att-remove" @click="removeAttachment(idx)">×</span>
          <div v-if="att.uploading" class="att-uploading"><a-spin size="small" /></div>
        </div>
      </div>
      <!-- 工具栏 -->
      <div class="input-toolbar" v-if="chatWindowConfig.sendEmoji || chatWindowConfig.sendImage || chatWindowConfig.sendVideo || chatWindowConfig.sendPdf">
        <SmileOutlined v-if="chatWindowConfig.sendEmoji" class="toolbar-icon" @click="showEmojiPanel = !showEmojiPanel" title="表情" />
        <PictureOutlined v-if="chatWindowConfig.sendImage" class="toolbar-icon" @click="triggerFileInput('image')" title="图片" />
        <VideoCameraOutlined v-if="chatWindowConfig.sendVideo" class="toolbar-icon" @click="triggerFileInput('video')" title="视频" />
        <FilePdfOutlined v-if="chatWindowConfig.sendPdf" class="toolbar-icon" @click="triggerFileInput('pdf')" title="PDF" />
        <input ref="imageInputRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp,image/bmp,image/svg+xml" style="display:none" @change="handleFileSelected($event, 'image')" />
        <input ref="videoInputRef" type="file" accept="video/mp4,video/webm,video/ogg,video/quicktime,video/x-msvideo,video/x-matroska,video/x-flv,video/3gpp,.mp4,.webm,.ogg,.mov,.avi,.mkv,.flv,.3gp" style="display:none" @change="handleFileSelected($event, 'video')" />
        <input ref="pdfInputRef" type="file" accept=".pdf,application/pdf" style="display:none" @change="handleFileSelected($event, 'pdf')" />
      </div>
      <!-- 表情面板 -->
      <div style="position:relative">
        <EmojiPicker :visible="showEmojiPanel" @select="appendEmoji" @close="showEmojiPanel = false" />
      </div>
      <a-textarea
        v-model:value="inputMessage"
        :placeholder="aiResponding ? 'AI正在回复中，请稍候...' : '请输入您要咨询的问题...'"
        :auto-size="{ minRows: 1, maxRows: 4 }"
        :disabled="aiResponding"
        @keydown="handleKeydown"
      />
      <a-button 
        type="primary" 
        @click="sendMessage" 
        :loading="sending || aiResponding" 
        :disabled="(!inputMessage.trim() && !attachmentList.length) || aiResponding"
      >
        <SendOutlined />
        {{ aiResponding ? 'AI回复中...' : '发送' }}
      </a-button>
    </div>
    <!-- 会话已结束时显示重新开始按钮 -->
    <div class="chat-closed" v-if="conversationClosed">
      <span>会话已结束</span>
      <a-button type="primary" @click="restartConversation">
        重新开始对话
      </a-button>
    </div>
      <a-modal v-model:open="mediaViewerVisible" :footer="null" width="820px" class="media-viewer-modal" title="媒体预览">
        <div class="media-viewer-header">
          <span>共 {{ mediaViewerList.length }} 项</span>
          <span class="media-viewer-tip">点击图片可放大，视频可播放</span>
        </div>
        <div class="media-viewer-grid">
          <div
            class="media-viewer-item"
            v-for="(item, index) in mediaViewerList"
            :key="`${item.url}_${index}`"
          >
            <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview({ extra: { attachments: mediaViewerList } }, item)" />
            <video v-else :src="getAttachmentUrl(item)" controls @click="openFilePreview(item)" />
          </div>
        </div>
      </a-modal>
      </div><!-- chat-main-column end -->
      <!-- PC右侧区域（广告+FAQ） -->
      <div v-if="chatWindowConfig.pcAdImage || (chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0)" class="chat-sidebar">
        <div v-if="chatWindowConfig.pcAdImage" class="sidebar-ad">
          <a :href="chatWindowConfig.pcAdLink || '#'" target="_blank" rel="noopener">
            <img :src="resolveFileUrl(chatWindowConfig.pcAdImage)" class="ad-sidebar-img" alt="广告" />
          </a>
        </div>
        <div v-if="chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0" class="sidebar-faq">
          <div class="sidebar-faq-title"><QuestionCircleOutlined /> 常见问题</div>
          <div class="sidebar-faq-list">
            <div v-for="(faq, idx) in chatWindowConfig.faqList" :key="idx" class="sidebar-faq-item" @click="handleFaqClick(faq)">
              {{ faq.question }}
            </div>
          </div>
        </div>
      </div>
      </div><!-- chat-main-layout end -->
      </template><!-- 正常聊天模式 end -->
    </template>
  </div>
</template>

<script setup lang="ts" name="UserChatPage">
import { ref, reactive, onMounted, onUnmounted, nextTick, computed, watch } from 'vue';
import MarkdownIt from 'markdown-it';
import { message } from 'ant-design-vue';
import {
  MessageOutlined, SendOutlined, BulbOutlined, CheckCircleOutlined,
  SmileOutlined, PictureOutlined, VideoCameraOutlined, FilePdfOutlined, QuestionCircleOutlined,
} from '@ant-design/icons-vue';
import { defHttp } from '/@/utils/http/axios';
import axios from 'axios';
import { useGlobSetting } from '/@/hooks/setting';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { createImgPreview } from '/@/components/Preview';
import EmojiPicker from '../components/EmojiPicker.vue';

const globSetting = useGlobSetting();
const silentRequestOptions = { successMessageMode: 'none' as const };
const visitorToken = ref('');
const sessionToken = ref('');
const sessionTokenExpiresAt = ref(0);
const lastSessionTokenKey = 'cs_session_token_last';
const rawVisitorToken = ref('');
let tokenValidateTimer: number | null = null;
const tokenRequired = ref(true); // Token验证开关，默认需要
const appKey = ref(''); // 接入密钥（免Token模式下从URL ?key= 读取）
const fatalError = ref(false);
const fatalErrorMessage = ref('token无效或已过期，请回到第三方应用重新打开');
function getQueryParam(name: string) {
  try {
    const search = window.location.search || '';
    const hash = window.location.hash || '';
    if (search) {
      const val = new URLSearchParams(search).get(name);
      if (val) return val;
    }
    const hashQueryIndex = hash.indexOf('?');
    if (hashQueryIndex > -1) {
      const hashQuery = hash.substring(hashQueryIndex + 1);
      return new URLSearchParams(hashQuery).get(name) || '';
    }
    return '';
  } catch {
    return '';
  }
}
function buildAuthHeaders(config: any) {
  if (sessionToken.value) {
    return { ...config?.headers, 'X-Visitor-Session': sessionToken.value };
  }
  if (visitorToken.value) {
    return { ...config?.headers, 'X-Visitor-Token': visitorToken.value };
  }
  // 免Token模式：传设备码 + 接入密钥
  if (!tokenRequired.value && userId.value) {
    const headers: Record<string, string> = { ...config?.headers, 'X-Device-Id': userId.value };
    if (appKey.value) {
      headers['X-App-Secret'] = appKey.value;
    }
    return headers;
  }
  return config?.headers || {};
}
function httpGet<T = any>(config: any, options: any = {}) {
  if (fatalError.value) {
    return Promise.reject(new Error('token invalid'));
  }
  return defHttp.get<T>({ ...config, headers: buildAuthHeaders(config) }, { ...silentRequestOptions, ...options });
}
function httpPost<T = any>(config: any, options: any = {}) {
  if (fatalError.value) {
    return Promise.reject(new Error('token invalid'));
  }
  return defHttp.post<T>({ ...config, headers: buildAuthHeaders(config) }, { ...silentRequestOptions, ...options });
}
function httpPut<T = any>(config: any, options: any = {}) {
  if (fatalError.value) {
    return Promise.reject(new Error('token invalid'));
  }
  return defHttp.put<T>({ ...config, headers: buildAuthHeaders(config) }, { ...silentRequestOptions, ...options });
}

// 应用信息
const appInfo = ref({
  id: '',
  name: '在线客服',
  avatar: '',
  prologue: '', // 开场白
  presetQuestion: '', // 预设问题（逗号或换行分隔）
});
const defaultAvatar = 'https://gw.alipayobjects.com/zos/rmsportal/KDpgvguMpGfqaHPjicRK.svg';
const defaultUserAvatar = 'https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png';

// ==================== 聊天窗口配置 ====================
const chatWindowConfig = reactive({
  themeColor: '#667eea',
  headerVisible: true,
  pageTitle: '',
  logo: '',
  agentBubbleBgColor: '#f5f5f5',
  agentBubbleFontColor: '#333333',
  visitorBubbleBgColor: '#667eea',
  visitorBubbleFontColor: '#ffffff',
  visitorAvatar: '',
  visitorHistory: true,
  visitorMessageConnect: false,
  sendEmoji: true,
  sendImage: true,
  sendVideo: true,
  sendPdf: true,
  maxFileSize: 10,
  visitorTimezone: 'Asia/Shanghai',
  scrollText: '',
  scrollDuration: 15,
  scrollTextColor: '#ffffff',
  scrollTextBgColor: '#1890ff',
  backgroundImage: '',
  pcAdLink: '',
  pcAdImage: '',
  faqEnabled: false,
  faqList: [] as Array<{ question: string; answer: string }>,
});

// FAQ展开状态（手机端）
const faqMobileExpanded = ref(false);

// CSS变量
const dynamicCssVars = computed(() => ({
  '--theme-color': chatWindowConfig.themeColor || '#667eea',
  '--agent-bubble-bg': chatWindowConfig.agentBubbleBgColor || '#f5f5f5',
  '--agent-bubble-color': chatWindowConfig.agentBubbleFontColor || '#333',
  '--visitor-bubble-bg': chatWindowConfig.visitorBubbleBgColor || '#667eea',
  '--visitor-bubble-color': chatWindowConfig.visitorBubbleFontColor || '#fff',
  '--scroll-text-color': chatWindowConfig.scrollTextColor || '#fff',
  '--scroll-text-bg': chatWindowConfig.scrollTextBgColor || '#1890ff',
  '--scroll-duration': (chatWindowConfig.scrollDuration || 15) + 's',
  '--chat-bg-image': chatWindowConfig.backgroundImage ? `url(${resolveFileUrl(chatWindowConfig.backgroundImage)})` : 'none',
}));

function resolveFileUrl(url: string) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url;
  const base = (window as any)._JEECG_API_BASE_URL || import.meta.env.VITE_GLOB_DOMAIN_URL || '';
  return base + '/' + url.replace(/^\//, '');
}

async function loadChatWindowConfig() {
  try {
    const res = await defHttp.get(
      { url: '/cs/agent/global/chat-window-settings' },
      { ...silentRequestOptions, isTransformResponse: false },
    );
    const data = res?.result || res;
    let parsed: any = {};
    if (typeof data === 'string') {
      try { parsed = JSON.parse(data); } catch {}
    } else if (data && typeof data === 'object') {
      parsed = data;
    }
    Object.keys(parsed).forEach((k) => {
      if (k in chatWindowConfig) {
        (chatWindowConfig as any)[k] = parsed[k];
      }
    });
    // 设置页面标题
    if (chatWindowConfig.pageTitle) {
      document.title = chatWindowConfig.pageTitle;
    }
  } catch (e) {
    console.warn('加载聊天窗口配置失败', e);
  }
}

// ==================== 敏感词配置 ====================
const sensitiveWordsConfig = reactive({
  enabled: false,
  words: [] as string[],
});

async function loadSensitiveWords() {
  try {
    const res = await defHttp.get(
      { url: '/cs/agent/global/sensitive-words' },
      { ...silentRequestOptions, isTransformResponse: false },
    );
    const data = res?.result || res;
    let parsed: any = {};
    if (typeof data === 'string') {
      try { parsed = JSON.parse(data); } catch {}
    } else if (data && typeof data === 'object') {
      parsed = data;
    }
    sensitiveWordsConfig.enabled = !!parsed.enabled;
    sensitiveWordsConfig.words = Array.isArray(parsed.words) ? parsed.words : [];
  } catch (e) {
    console.warn('加载敏感词配置失败', e);
  }
}

function checkSensitiveWords(content: string): string | null {
  if (!sensitiveWordsConfig.enabled || !sensitiveWordsConfig.words?.length) return null;
  const lower = content.toLowerCase();
  for (const word of sensitiveWordsConfig.words) {
    if (word && lower.includes(word.toLowerCase())) return word;
  }
  return null;
}

// ==================== 消息接通模式 ====================
const messageConnectMode = ref(false);

// ==================== 表情面板 ====================
const showEmojiPanel = ref(false);
function appendEmoji(emoji: string) {
  inputMessage.value += emoji;
}

// ==================== 附件上传 ====================
const imageInputRef = ref<HTMLInputElement | null>(null);
const videoInputRef = ref<HTMLInputElement | null>(null);
const pdfInputRef = ref<HTMLInputElement | null>(null);

interface AttachmentItem {
  name: string;
  url: string;
  previewUrl?: string;
  size: number;
  type: 'image' | 'video' | 'file';
  uploading?: boolean;
}
const attachmentList = ref<AttachmentItem[]>([]);

function triggerFileInput(fileType: 'image' | 'video' | 'pdf') {
  if (fileType === 'image') imageInputRef.value?.click();
  else if (fileType === 'video') videoInputRef.value?.click();
  else if (fileType === 'pdf') pdfInputRef.value?.click();
}

// 文件上传限制（大小从聊天窗口配置动态读取，默认10MB，最大50MB）
const ALLOWED_IMAGE_EXTS = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'];
const ALLOWED_VIDEO_EXTS = ['mp4', 'webm', 'ogg', 'mov', 'avi', 'mkv', 'flv', '3gp', 'wmv'];
const ALLOWED_PDF_EXTS = ['pdf'];
const FILE_TYPE_LABEL: Record<string, string> = { image: '图片', video: '视频', pdf: 'PDF' };

function getMaxFileSize(): number {
  const mb = chatWindowConfig.maxFileSize;
  const val = (typeof mb === 'number' && mb > 0) ? Math.min(mb, 50) : 10;
  return val * 1024 * 1024;
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function getFileExt(name: string): string {
  const idx = name.lastIndexOf('.');
  return idx > -1 ? name.substring(idx + 1).toLowerCase() : '';
}

function validateFile(file: File, fileType: 'image' | 'video' | 'pdf'): string | null {
  // 校验文件大小（从聊天窗口配置动态读取）
  const maxSize = getMaxFileSize();
  if (file.size > maxSize) {
    return `文件大小 ${formatFileSize(file.size)} 超出限制，最大允许 ${formatFileSize(maxSize)}`;
  }
  // 校验文件类型
  const ext = getFileExt(file.name);
  const allowedExts = fileType === 'image' ? ALLOWED_IMAGE_EXTS : fileType === 'video' ? ALLOWED_VIDEO_EXTS : ALLOWED_PDF_EXTS;
  if (!ext || !allowedExts.includes(ext)) {
    return `不支持的${FILE_TYPE_LABEL[fileType]}格式（${ext || '未知'}），支持：${allowedExts.join(', ')}`;
  }
  return null;
}

async function handleFileSelected(e: Event, fileType: 'image' | 'video' | 'pdf') {
  const input = e.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) return;
  input.value = ''; // 重置以允许重新选择

  // 前端预校验文件大小和格式
  const validationError = validateFile(file, fileType);
  if (validationError) {
    message.warning(validationError);
    return;
  }

  const attType: 'image' | 'video' | 'file' = fileType === 'pdf' ? 'file' : fileType;
  const previewUrl = fileType === 'image' ? URL.createObjectURL(file) : undefined;
  const att: AttachmentItem = {
    name: file.name,
    url: '',
    previewUrl,
    size: file.size,
    type: attType,
    uploading: true,
  };
  attachmentList.value.push(att);
  const idx = attachmentList.value.length - 1;

  try {
    const formData = new FormData();
    formData.append('file', file);
    // 使用独立 axios 实例上传文件，避免 defHttp 全局拦截器干扰 Content-Type boundary
    const { apiUrl, urlPrefix } = globSetting;
    const uploadApiUrl = `${apiUrl}${urlPrefix || ''}/cs/message/visitor/upload`;
    const authHeaders = buildAuthHeaders({});
    const { data: res } = await axios.post(uploadApiUrl, formData, {
      headers: authHeaders,
    });
    if (!res?.success) {
      message.error(res?.message || '上传失败');
      attachmentList.value.splice(idx, 1);
      return;
    }
    const uploadedUrl = res?.message || res?.result?.url || res?.result?.message || '';
    if (!uploadedUrl) {
      message.error('上传失败：未获取到文件地址');
      attachmentList.value.splice(idx, 1);
      return;
    }
    attachmentList.value[idx].url = uploadedUrl;
    attachmentList.value[idx].uploading = false;
  } catch (err: any) {
    console.error('文件上传失败', err);
    // 提取后端返回的具体错误信息
    const serverMsg = err?.response?.data?.message;
    if (serverMsg) {
      message.error(serverMsg);
    } else if (err?.message?.includes('Network Error') || err?.message?.includes('ERR_CONNECTION')) {
      message.error('网络连接异常，请检查网络后重试');
    } else {
      message.error('文件上传失败，请稍后重试');
    }
    attachmentList.value.splice(idx, 1);
  }
}

function removeAttachment(idx: number) {
  const att = attachmentList.value[idx];
  if (att.previewUrl) URL.revokeObjectURL(att.previewUrl);
  attachmentList.value.splice(idx, 1);
}

function resolveAvatarUrl(avatar?: string) {
  if (!avatar) return '';
  return getFileAccessHttpUrl(avatar);
}

function getAgentAvatar(msg?: any) {
  const avatar = msg?.senderAvatar || appInfo.value.avatar;
  return resolveAvatarUrl(avatar) || defaultAvatar;
}

function getUserAvatar(msg?: any) {
  // 优先使用聊天窗口配置的访客头像
  if (chatWindowConfig.visitorAvatar) {
    return resolveFileUrl(chatWindowConfig.visitorAvatar);
  }
  const avatar = msg?.senderAvatar;
  return resolveAvatarUrl(avatar) || defaultUserAvatar;
}

// 预设问题列表
const presetQuestions = computed(() => {
  if (!appInfo.value.presetQuestion) return [];
  
  const rawQuestion = appInfo.value.presetQuestion;
  
  // 尝试解析JSON格式 (如: [{"key":1,"descr":"问题1"},{"key":2,"descr":"问题2"}])
  try {
    if (rawQuestion.trim().startsWith('[')) {
      const parsed = JSON.parse(rawQuestion);
      if (Array.isArray(parsed)) {
        return parsed
          .map((item: any) => item.descr || item.question || item.content || '')
          .filter((q: string) => q.length > 0);
      }
    }
  } catch {
    // 解析失败，尝试其他格式
  }
  
  // 支持逗号或换行分隔的纯文本格式
  return rawQuestion
    .split(/[,，\n]/)
    .map((q: string) => q.trim())
    .filter((q: string) => q.length > 0);
});

// 消息列表
const messages = ref<any[]>([]);
const loading = ref(false);
const sending = ref(false);
const inputMessage = ref('');
const messagesRef = ref<HTMLElement | null>(null);
const historyPageSize = 100;
const loadingHistory = ref(false);
const hasMoreHistory = ref(true);
const historyBeforeId = ref<string | null>(null);
const displayMessages = computed(() => {
  const list: any[] = [];
  let lastDateKey = '';
  for (const msg of messages.value) {
    const dateKey = getDateKey(msg?.createTime);
    if (dateKey && dateKey !== lastDateKey) {
      list.push({
        id: `date-${dateKey}`,
        senderType: 3,
        content: formatDateSeparator(msg.createTime),
        isDateSeparator: true,
      });
      lastDateKey = dateKey;
    }
    list.push(msg);
  }
  return list;
});

// WebSocket
let ws: WebSocket | null = null;
const wsConnected = ref(false);
const agentTyping = ref(false);
let wsReconnectTimer: number | null = null;
let wsManuallyClosed = false;
let wsReconnectAttempts = 0;
let wsFallbackPollTimer: number | null = null;
let lastWsMessageAt = 0;
const wsFallbackPollIntervalMs = 20000;

// AI回复中状态（用于限制用户快速发送）
const aiResponding = ref(false);
let aiResponseTimeoutTimer: number | null = null;

function stopAiResponding(reason?: string) {
  if (aiResponseTimeoutTimer) {
    clearTimeout(aiResponseTimeoutTimer);
    aiResponseTimeoutTimer = null;
  }
  if (aiResponding.value) {
    aiResponding.value = false;
  }
  if (reason) {
    messages.value.push({
      id: Date.now().toString(),
      content: reason,
      senderType: 3,
      createTime: new Date().toISOString(),
    });
    scrollToBottom();
  }
}

// 流式AI消息临时存储 (messageId -> 累积内容)
const streamingMessages = ref<Map<string, string>>(new Map());

// 用户信息
const userId = ref('');
const userName = ref('访客');
const conversationId = ref('');
const conversationClosed = ref(false);  // 会话是否已结束
const replyMode = ref(0);  // 回复模式: 0=AI自动, 1=手动
const hasAgent = ref(false);  // 是否有客服接入

// 留言板相关
const showLeaveMessageBoard = ref(false);  // 是否显示留言板
const messageBoardConfig = ref<any>({ subtitle: '客服不在线，请留言', fields: {} });
const leaveMessageForm = ref<any>({ content: '', name: '', phone: '', email: '', qq: '', wechat: '' });
const submittingLeaveMessage = ref(false);
const leaveMessageSubmitted = ref(false);
const unreadReplies = ref<any[]>([]);  // 未读留言回复列表

const handleVisibilityChange = () => {
  if (document.hidden) return;
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    connectWebSocket();
  }
};

const handleNetworkOnline = () => {
  connectWebSocket();
};

// 连接状态
const connectionStatus = computed(() => {
  if (!wsConnected.value) return 'offline';
  return 'online';
});

const connectionStatusText = computed(() => {
  if (!wsConnected.value) return '连接中...';
  return '在线';
});

// 初始化
onMounted(async () => {
  // 首先查询是否需要Token验证
  try {
    const tokenRes = await defHttp.get(
      { url: '/airag/cs/visitor/token/required' },
      { ...silentRequestOptions, isTransformResponse: false },
    );
    if (tokenRes?.success && tokenRes.result === false) {
      tokenRequired.value = false;
    }
  } catch {
    // 查询失败默认需要Token
  }

  // 读取接入密钥参数
  const keyFromUrl = getQueryParam('key');
  if (keyFromUrl) {
    appKey.value = keyFromUrl;
  }

  const sessionFromUrl = getQueryParam('sessionToken');
  if (sessionFromUrl) {
    sessionToken.value = sessionFromUrl;
    sessionTokenExpiresAt.value = 0;
  }
  const tokenFromUrl = getQueryParam('token') || getQueryParam('visitorToken');
  if (tokenFromUrl) {
    visitorToken.value = tokenFromUrl;
    rawVisitorToken.value = tokenFromUrl;
  }
  await checkIpBlocked();
  if (fatalError.value) {
    return;
  }
  // 生成或获取用户ID
  initUserId();

  if (!tokenRequired.value) {
    // ========= 免Token模式：跳过Token验证流程 =========
    // 校验接入密钥（密钥无效直接阻断页面）
    await checkAppKey();
    if (fatalError.value) {
      return;
    }
    // 检查访客是否被拉黑（通过设备码）
    await checkUserBlocked();
    if (fatalError.value) {
      return;
    }
  } else {
    // ========= Token模式：原有流程 =========
    loadSessionToken();
    if (!canProceedWithToken()) {
      blockForInvalidToken();
      return;
    }
    await ensureSessionToken();
    if (fatalError.value) {
      return;
    }
    const sessionValid = await validateSessionToken();
    if (sessionValid) {
      rawVisitorToken.value = '';
      startTokenValidateTimer();
      await checkUserBlocked();
      if (fatalError.value) {
        return;
      }
    } else if (visitorToken.value) {
      await checkUserBlocked();
      if (fatalError.value) {
        return;
      }
      await validateShortTokenIfProvided();
      if (fatalError.value) {
        return;
      }
      startTokenValidateTimer();
    } else {
      blockForInvalidToken();
      return;
    }
    if (!canProceedWithToken()) {
      blockForInvalidToken();
      return;
    }
  }

  // 加载聊天窗口配置和敏感词配置
  await Promise.all([loadChatWindowConfig(), loadSensitiveWords()]);

  // 加载访客AI应用信息（头像/开场白/预设问题）
  await loadVisitorAppInfo();

  // 获取或创建会话（如果是消息接通模式，延迟到发送第一条消息时）
  if (chatWindowConfig.visitorMessageConnect) {
    // 消息接通模式：先不创建正式会话，等用户发送第一条消息
    messageConnectMode.value = true;
  } else {
    await initConversation();
  }

  // 如果是留言板模式，不需要后续操作
  if (showLeaveMessageBoard.value) {
    return;
  }

  // 加载未读留言回复
  await loadUnreadReplies();

  // 加载历史消息（根据 visitorHistory 开关决定）
  if (chatWindowConfig.visitorHistory !== false) {
    await loadMessages();
  }

  // 连接WebSocket
  connectWebSocket();
  startFallbackPoll();
  document.addEventListener('visibilitychange', handleVisibilityChange);
  window.addEventListener('online', handleNetworkOnline);

  // 滚动到底部
  scrollToBottom();
});

onUnmounted(() => {
  disconnectWebSocket();
  stopFallbackPoll();
  stopTokenValidateTimer();
  window.removeEventListener('online', handleNetworkOnline);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
});

// 初始化用户ID
function initUserId() {
  const queryUserId = getQueryParam('externalUserId') || getQueryParam('uid') || getQueryParam('userId');
  const queryUserName = getQueryParam('userName');
  const querySource = getQueryParam('source') || getQueryParam('appKey');

  if (!tokenRequired.value) {
    // 免Token模式：设备码作为userId
    const deviceId = generateDeviceId();
    userId.value = deviceId;
    // 允许URL中可选传userName
    if (queryUserName) {
      userName.value = queryUserName;
    }
    return;
  }

  // Token模式：原有逻辑
  if (queryUserId) {
    userId.value = querySource ? `${querySource}:${queryUserId}` : queryUserId;
    if (queryUserName) {
      userName.value = queryUserName;
    }
    return;
  }

  // 从localStorage获取或生成新的用户ID
  let storedUserId = localStorage.getItem('cs_user_id');
  if (!storedUserId) {
    storedUserId = 'user_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    localStorage.setItem('cs_user_id', storedUserId);
  }
  userId.value = storedUserId;

  // 获取用户名
  const storedUserName = localStorage.getItem('cs_user_name');
  if (storedUserName) {
    userName.value = storedUserName;
  }
}

async function checkIpBlocked() {
  try {
    const res = await defHttp.get({ url: '/airag/cs/visitor/blacklist/ip/check-current' }, silentRequestOptions);
    if (res?.result?.blacklisted || res?.blacklisted) {
      fatalError.value = true;
      fatalErrorMessage.value = '访问已被禁止，请联系管理员';
    }
  } catch {
    // 忽略检测失败，继续后续流程
  }
}

/** 免Token模式下校验接入密钥，无效则直接阻断页面 */
async function checkAppKey() {
  try {
    const headers: Record<string, string> = {};
    if (appKey.value) {
      headers['X-App-Secret'] = appKey.value;
    }
    const res = await defHttp.get(
      { url: '/airag/cs/visitor/validate-key', headers },
      { ...silentRequestOptions, isTransformResponse: false, errorMessageMode: 'none' },
    );
    if (!res?.success) {
      fatalError.value = true;
      fatalErrorMessage.value = '接入密钥无效，无法访问客服';
    }
  } catch {
    fatalError.value = true;
    fatalErrorMessage.value = '接入密钥无效，无法访问客服';
  }
}

async function checkUserBlocked() {
  if (!visitorToken.value && !sessionToken.value) {
    return;
  }
  try {
    const headers: Record<string, string> = {};
    if (sessionToken.value) {
      headers['X-Visitor-Session'] = sessionToken.value;
    } else if (visitorToken.value) {
      headers['X-Visitor-Token'] = visitorToken.value;
    }
    const res = await defHttp.get({
      url: '/airag/cs/visitor/blacklist/check-self',
      headers,
    }, { ...silentRequestOptions, errorMessageMode: 'none' });
    if (res?.result?.blacklisted || res?.blacklisted) {
      fatalError.value = true;
      fatalErrorMessage.value = '访问已被禁止，请联系管理员';
    }
  } catch {
    console.warn('[UserChat] 拉黑自检失败');
  }
}

function getSessionTokenKey() {
  if (userId.value) {
    return `cs_session_token_${userId.value}`;
  }
  return 'cs_session_token';
}

function loadSessionToken() {
  try {
    const raw = localStorage.getItem(getSessionTokenKey());
    if (raw) {
      const data = JSON.parse(raw);
      if (data?.token) {
        sessionToken.value = data.token;
        sessionTokenExpiresAt.value = data.expireAt || 0;
        return;
      }
    }
    const lastRaw = localStorage.getItem(lastSessionTokenKey);
    if (lastRaw) {
      const last = JSON.parse(lastRaw);
      const lastUserId = last?.userId || '';
      if (!lastUserId || lastUserId === userId.value) {
        sessionToken.value = last?.token || '';
        sessionTokenExpiresAt.value = last?.expireAt || 0;
      }
    }
  } catch {
    // ignore
  }
}

function canProceedWithToken() {
  const now = Date.now();
  const hasSession = sessionToken.value && (!sessionTokenExpiresAt.value || sessionTokenExpiresAt.value >= now);
  const hasShort = !!visitorToken.value;
  if (!hasSession && !hasShort) {
    return false;
  }
  if (sessionToken.value && sessionTokenExpiresAt.value && sessionTokenExpiresAt.value < now) {
    sessionToken.value = '';
    sessionTokenExpiresAt.value = 0;
    return !!visitorToken.value;
  }
  return true;
}

function blockForInvalidToken(messageText?: string) {
  fatalError.value = true;
  fatalErrorMessage.value = messageText || 'token无效或已过期，请回到第三方应用重新打开';
  disconnectWebSocket();
  stopFallbackPoll();
}

function saveSessionToken(token: string, expireAt: number) {
  sessionToken.value = token;
  sessionTokenExpiresAt.value = expireAt || 0;
  try {
    localStorage.setItem(getSessionTokenKey(), JSON.stringify({ token, expireAt }));
    localStorage.setItem(lastSessionTokenKey, JSON.stringify({ token, expireAt, userId: userId.value || '' }));
  } catch {
    // ignore
  }
}

async function ensureSessionToken() {
  if (sessionToken.value) {
    if (sessionTokenExpiresAt.value && sessionTokenExpiresAt.value < Date.now()) {
      sessionToken.value = '';
      sessionTokenExpiresAt.value = 0;
    } else {
      return;
    }
  }
  if (!visitorToken.value) return;
  try {
    const res = await defHttp.post({
      url: '/airag/cs/visitor/session/exchange',
      data: { token: visitorToken.value },
      headers: { 'X-Visitor-Token': visitorToken.value }
    }, { ...silentRequestOptions });
    const payload = res?.result || res;
    const success = res?.success !== false && (res?.code === undefined || res?.code === 200);
    if (success && payload?.token) {
      saveSessionToken(payload.token, payload.expireAt || 0);
      return;
    }
    visitorToken.value = '';
    blockForInvalidToken(res?.message || payload?.message);
  } catch (e: any) {
    visitorToken.value = '';
    blockForInvalidToken(e?.message);
  }
}

async function validateShortTokenIfProvided() {
  if (!rawVisitorToken.value) {
    return;
  }
  try {
    const res = await defHttp.get({
      url: '/airag/cs/visitor/token/validate',
      params: { token: rawVisitorToken.value },
    }, { ...silentRequestOptions, errorMessageMode: 'none' });
    const payload = res?.result || res;
    const success = res?.success !== false && (res?.code === undefined || res?.code === 200);
    if (success && payload?.token) {
      return;
    }
    visitorToken.value = '';
    rawVisitorToken.value = '';
    console.warn('[UserChat] token校验失败', res);
    blockForInvalidToken('当前访问已失效，请返回第三方页面重新打开');
  } catch (e: any) {
    visitorToken.value = '';
    rawVisitorToken.value = '';
    console.warn('[UserChat] token校验失败', e);
    blockForInvalidToken('当前访问已失效，请返回第三方页面重新打开');
  }
}

function startTokenValidateTimer() {
  stopTokenValidateTimer();
  if (!rawVisitorToken.value) {
    return;
  }
  tokenValidateTimer = window.setInterval(async () => {
    if (fatalError.value) {
      stopTokenValidateTimer();
      return;
    }
    await validateShortTokenIfProvided();
  }, 60000);
}

function stopTokenValidateTimer() {
  if (tokenValidateTimer) {
    clearInterval(tokenValidateTimer);
    tokenValidateTimer = null;
  }
}

async function validateSessionToken() {
  if (!sessionToken.value) {
    return false;
  }
  try {
    const res = await defHttp.get({
      url: '/airag/cs/visitor/session/validate',
      headers: { 'X-Visitor-Session': sessionToken.value },
    }, { ...silentRequestOptions });
    const payload = res?.result || res;
    const success = res?.success !== false && (res?.code === undefined || res?.code === 200);
    if (success && payload?.token) {
      return true;
    }
    sessionToken.value = '';
    sessionTokenExpiresAt.value = 0;
    return false;
  } catch {
    sessionToken.value = '';
    sessionTokenExpiresAt.value = 0;
    return false;
  }
}


// 选择预设问题
function selectPresetQuestion(question: string) {
  inputMessage.value = question;
  // 直接发送
  sendMessage();
}

// FAQ点击：一次API调用完成 发送问题（不触发AI） + 返回预设答案
async function handleFaqClick(faq: { question: string; answer: string }) {
  if (!faq.question || !faq.answer) return;
  if (!conversationId.value) return;
  try {
    await httpPost({
      url: '/cs/message/faq/answer',
      data: {
        conversationId: conversationId.value,
        question: faq.question,
        answer: faq.answer,
      },
    });
    // 问题和答案都通过WebSocket推送回来，无需手动添加
  } catch (err) {
    console.error('FAQ回复失败', err);
  }
}

// 生成持久化设备码（UUID + 浏览器指纹哈希后缀）
const CS_DEVICE_ID_KEY = 'cs_device_id';
function generateDeviceId(): string {
  let deviceId = localStorage.getItem(CS_DEVICE_ID_KEY);
  if (deviceId) return deviceId;
  try {
    // 生成 UUID v4
    const uuid = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
          const r = (Math.random() * 16) | 0;
          return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16);
        });
    // 浏览器指纹哈希后缀
    const fingerprint = [
      navigator.platform,
      screen.width,
      screen.height,
      screen.colorDepth,
      navigator.language,
      navigator.hardwareConcurrency,
    ].join('|');
    let hash = 0;
    for (let i = 0; i < fingerprint.length; i++) {
      hash = ((hash << 5) - hash) + fingerprint.charCodeAt(i);
      hash |= 0;
    }
    deviceId = `${uuid}_${Math.abs(hash).toString(16)}`;
  } catch {
    deviceId = 'dev_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
  }
  localStorage.setItem(CS_DEVICE_ID_KEY, deviceId);
  return deviceId;
}

// 初始化会话
async function initConversation() {
  try {
    // 从localStorage获取已有的会话ID（现在不需要appId）
    const storedConvId = localStorage.getItem(`cs_conversation_${userId.value}`);
    if (storedConvId) {
      // 检查会话是否已结束
      try {
        const convRes = await httpGet({ url: `/cs/conversation/${storedConvId}` });
        if (convRes && convRes.status === 2) {
          // 会话已结束，清除存储并创建新会话
          localStorage.removeItem(`cs_conversation_${userId.value}`);
        } else if (convRes && convRes.status === 0 && !convRes.ownerAgentId) {
          // 旧会话未分配（上次无客服在线）→ 清除并重新创建，重新尝试分配客服
          localStorage.removeItem(`cs_conversation_${userId.value}`);
        } else {
          conversationId.value = storedConvId;
          if (convRes?.replyMode !== undefined) {
            replyMode.value = convRes.replyMode;
          }
          if (convRes?.ownerAgentId) {
            hasAgent.value = true;
          }
          return;
        }
      } catch {
        // 获取会话失败，创建新会话
        localStorage.removeItem(`cs_conversation_${userId.value}`);
      }
    }

    // 创建新会话（后端会自动分配客服），附带设备指纹
    const res = await httpPost({
      url: '/cs/conversation/create',
      data: {
        userId: userId.value,
        userName: userName.value,
        deviceId: generateDeviceId(),
        lang: navigator.language || navigator.userLanguage || 'en',
      },
    });
    if (res) {
      const conv = res.result || res;
      if (conv.id) {
        conversationId.value = conv.id;
        localStorage.setItem(`cs_conversation_${userId.value}`, conv.id);
        
        // 检查是否有客服分配
        if (conv.status === 0 && !conv.ownerAgentId) {
          // 无在线客服 → 显示留言板
          await loadMessageBoardConfig();
          showLeaveMessageBoard.value = true;
          return;
        }
        
        // 有客服分配
        if (conv.replyMode !== undefined) {
          replyMode.value = conv.replyMode;
        }
        if (conv.ownerAgentId) {
          hasAgent.value = true;
        }
      }
    }
  } catch (e) {
    console.error('初始化会话失败', e);
    // 使用临时会话ID
    conversationId.value = `temp_${userId.value}_${Date.now()}`;
  }
}

// 加载访客AI应用信息
async function loadVisitorAppInfo() {
  try {
    // 先检查AI开关状态
    const aiRes = await httpGet({ url: '/cs/agent/global/ai-enabled' });
    const aiData = aiRes?.result || aiRes;
    const aiEnabled = aiData?.enabled !== false;

    if (!aiEnabled) {
      // AI关闭时不加载AI应用信息
      return;
    }

    const res = await httpGet({ url: '/cs/agent/global/visitor-app' });
    const appId = res?.appId || res?.result?.appId;
    if (!appId) return;

    const appRes = await httpGet({ url: '/airag/app/queryById', params: { id: appId } });
    const app = appRes?.result || appRes;
    if (!app) return;

    appInfo.value = {
      id: app.id || '',
      name: app.name || '在线客服',
      avatar: app.avatar || '',
      prologue: app.prologue || '',
      presetQuestion: app.presetQuestion || '',
    };
  } catch (e) {
    console.warn('[UserChat] 加载访客AI应用信息失败', e);
  }
}

// 加载留言板配置
async function loadMessageBoardConfig() {
  try {
    const res = await httpGet({ url: '/cs/agent/global/message-board' });
    const data = res?.result || res;
    if (data) {
      messageBoardConfig.value = data;
    }
  } catch (e) {
    console.warn('[UserChat] 加载留言板配置失败', e);
  }
}

// 提交留言
async function submitLeaveMessage() {
  const form = leaveMessageForm.value;
  if (!form.content?.trim()) {
    message.warning('请输入留言内容');
    return;
  }
  // 检查必填字段
  const fields = messageBoardConfig.value.fields || {};
  for (const [key, cfg] of Object.entries(fields) as [string, any][]) {
    if (cfg.show && cfg.required && !form[key]?.trim()) {
      const labels: Record<string, string> = { name: '姓名', phone: '手机', email: '邮箱', qq: 'QQ', wechat: '微信', image: '图片' };
      message.warning(`请填写${labels[key] || key}`);
      return;
    }
  }

  submittingLeaveMessage.value = true;
  try {
    await httpPost({
      url: '/cs/leaveMessage/submit',
      data: {
        userId: userId.value,
        content: form.content,
        name: form.name,
        phone: form.phone,
        email: form.email,
        qq: form.qq,
        wechat: form.wechat,
      },
    });
    leaveMessageSubmitted.value = true;
    message.success('留言已提交');
  } catch (e) {
    console.error('[UserChat] 提交留言失败', e);
    message.error('提交失败，请稍后重试');
  } finally {
    submittingLeaveMessage.value = false;
  }
}

// 加载未读留言回复
async function loadUnreadReplies() {
  if (!userId.value) return;
  try {
    const res = await httpGet({ url: '/cs/leaveMessage/byUser', params: { userId: userId.value } });
    const data = res?.result || res;
    if (Array.isArray(data) && data.length > 0) {
      unreadReplies.value = data;
    }
  } catch (e) {
    console.warn('[UserChat] 加载留言回复失败', e);
  }
}

// 关闭留言回复提醒
function dismissReply(replyId: string) {
  unreadReplies.value = unreadReplies.value.filter(r => r.id !== replyId);
  // 如果所有回复都已关闭，标记为已读
  if (unreadReplies.value.length === 0 && userId.value) {
    httpPut({ url: '/cs/leaveMessage/markRead', data: { userId: userId.value } }).catch(() => {});
  }
}

// 加载历史消息
async function loadMessages() {
  if (!conversationId.value || conversationId.value.startsWith('temp_')) return;

  loading.value = true;
  try {
    const res = await httpGet({
      url: '/cs/message/list',
      params: {
        conversationId: conversationId.value,
        limit: historyPageSize,
      },
    });
    const list = Array.isArray(res) ? res : (res?.result || res?.records || []);
    if (list) {
      messages.value = list;
      historyBeforeId.value = list[0]?.id || null;
      hasMoreHistory.value = list.length >= historyPageSize;
    }
  } catch {
    // 忽略错误
  } finally {
    loading.value = false;
  }
}

async function loadMoreMessages() {
  if (!conversationId.value || conversationId.value.startsWith('temp_')) return;
  if (loadingHistory.value || !hasMoreHistory.value) return;
  const beforeId = historyBeforeId.value;
  if (!beforeId) {
    hasMoreHistory.value = false;
    return;
  }

  const el = messagesRef.value;
  const prevScrollHeight = el?.scrollHeight || 0;
  const prevScrollTop = el?.scrollTop || 0;

  loadingHistory.value = true;
  try {
    const res = await httpGet({
      url: `/cs/message/${conversationId.value}/page`,
      params: { beforeId, limit: historyPageSize },
    });
    const olderMessages = Array.isArray(res) ? res : (res?.result || res?.records || []);
    if (!olderMessages.length) {
      hasMoreHistory.value = false;
      return;
    }
    const existingIds = new Set(messages.value.map(m => m.id));
    const filtered = olderMessages.filter((m: any) => !existingIds.has(m.id));
    if (!filtered.length) {
      hasMoreHistory.value = false;
      return;
    }
    messages.value = [...filtered, ...messages.value];
    historyBeforeId.value = filtered[0]?.id || historyBeforeId.value;
    if (olderMessages.length < historyPageSize) {
      hasMoreHistory.value = false;
    }
    nextTick(() => {
      const nextEl = messagesRef.value;
      if (!nextEl) return;
      const nextScrollHeight = nextEl.scrollHeight;
      nextEl.scrollTop = nextScrollHeight - prevScrollHeight + prevScrollTop;
    });
  } catch {
    // 忽略错误
  } finally {
    loadingHistory.value = false;
  }
}

function handleMessageScroll(event?: Event) {
  const el = (event?.target as HTMLElement) || messagesRef.value;
  if (!el) return;
  // 如果访客历史关闭，不加载更多
  if (chatWindowConfig.visitorHistory === false) return;
  if (loadingHistory.value || !hasMoreHistory.value) return;
  if (el.scrollTop <= 20) {
    loadMoreMessages();
  }
}

// 连接WebSocket
function getWsBaseUrl() {
  const { apiUrl, domainUrl, urlPrefix } = globSetting;
  let base = /^https?:\/\//.test(apiUrl) ? apiUrl : '';
  if (!base && /^https?:\/\//.test(domainUrl)) {
    base = domainUrl;
  }
  if (!base) {
    base = window.location.origin;
  }
  let parsed: URL;
  try {
    parsed = new URL(base);
  } catch {
    parsed = new URL(window.location.origin);
  }
  const wsProtocol = parsed.protocol === 'https:' ? 'wss:' : 'ws:';
  let prefix = urlPrefix || parsed.pathname || '';
  if (prefix && !prefix.startsWith('/')) {
    prefix = `/${prefix}`;
  }
  prefix = prefix.replace(/\/$/, '');
  return `${wsProtocol}//${parsed.host}${prefix}`;
}

function connectWebSocket() {
  if (!conversationId.value || !userId.value) {
    console.warn('缺少conversationId或userId，无法连接WebSocket');
    return;
  }
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  wsManuallyClosed = false;

  const wsBase = getWsBaseUrl();
  let authParams = '';
  if (sessionToken.value) {
    authParams = `&sessionToken=${encodeURIComponent(sessionToken.value)}`;
  } else if (visitorToken.value) {
    authParams = `&visitorToken=${encodeURIComponent(visitorToken.value)}`;
  } else if (!tokenRequired.value) {
    // 免Token模式：传递设备码 + 接入密钥
    authParams = `&deviceId=${encodeURIComponent(userId.value)}`;
    if (appKey.value) {
      authParams += `&key=${encodeURIComponent(appKey.value)}`;
    }
  }
  const wsUrl = `${wsBase}/ws/cs/user?userId=${userId.value}&conversationId=${conversationId.value}${authParams}`;

  console.log('[UserChat] 连接WebSocket:', wsUrl);

  ws = new WebSocket(wsUrl);

  ws.onopen = () => {
    console.log('[UserChat] WebSocket已连接');
    wsConnected.value = true;
    wsReconnectAttempts = 0;
    lastWsMessageAt = Date.now();
    startHeartbeat();
  };

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      lastWsMessageAt = Date.now();
      handleWsMessage(data);
    } catch (e) {
      console.error('[UserChat] 解析WebSocket消息失败', e);
    }
  };

  ws.onclose = (event) => {
    console.log('[UserChat] WebSocket已断开:', event.code, event.reason);
    wsConnected.value = false;
    stopHeartbeat();
    if (aiResponding.value && replyMode.value === 0) {
      stopAiResponding('网络中断，AI回复可能未完成，请稍后重试');
    }
    ws = null;
    // 自动重连
    if (!wsManuallyClosed) {
      scheduleWsReconnect();
    }
  };

  ws.onerror = (error) => {
    console.error('[UserChat] WebSocket错误:', error);
    if (aiResponding.value && replyMode.value === 0) {
      stopAiResponding('网络异常，AI回复可能未完成，请稍后重试');
    }
    try {
      ws?.close();
    } catch {
      // ignore
    }
  };
}

// 断开WebSocket
function disconnectWebSocket() {
  stopHeartbeat();
  wsManuallyClosed = true;
  stopFallbackPoll();
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  if (ws) {
    ws.close();
    ws = null;
  }
  wsConnected.value = false;
  stopAiResponding();
}

function scheduleWsReconnect() {
  if (wsManuallyClosed) return;
  if (wsReconnectTimer) return;
  const jitter = Math.floor(Math.random() * 1000);
  const delay = Math.min(30000, 1000 * Math.pow(2, wsReconnectAttempts)) + jitter;
  wsReconnectAttempts += 1;
  wsReconnectTimer = window.setTimeout(() => {
    wsReconnectTimer = null;
    connectWebSocket();
  }, delay);
}

function stopFallbackPoll() {
  if (wsFallbackPollTimer) {
    clearInterval(wsFallbackPollTimer);
    wsFallbackPollTimer = null;
  }
}

function startFallbackPoll() {
  stopFallbackPoll();
  wsFallbackPollTimer = window.setInterval(async () => {
    if (document.hidden) return;
    if (!conversationId.value) return;
    if (loading.value) return;
    if (ws && ws.readyState === WebSocket.OPEN && lastWsMessageAt) {
      const now = Date.now();
      if (now - lastWsMessageAt < wsFallbackPollIntervalMs) {
        return;
      }
    }
    try {
      await loadMessages();
    } catch {
      // ignore
    }
  }, wsFallbackPollIntervalMs);
}

// 心跳
let heartbeatTimer: any = null;
function startHeartbeat() {
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'ping' }));
    }
  }, 30000);
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer);
    heartbeatTimer = null;
  }
}

// 处理WebSocket消息
function handleWsMessage(data: any) {
  console.log('[UserChat] 收到消息:', data);

  switch (data.type) {
    case 'connected':
      console.log('[UserChat] 连接成功确认');
      // 获取会话的replyMode
      if (data.extra) {
        replyMode.value = data.extra.replyMode ?? 0;
        hasAgent.value = data.extra.hasAgent ?? false;
      }
      break;

    case 'message':
      // 收到新消息（来自客服或AI）
      const msgSenderType = Number(data.senderType);
      const newMsg = {
        id: data.messageId || Date.now().toString(),
        conversationId: data.conversationId,
        content: data.content,
        msgType: data.msgType,
        extra: data.extra,
        senderType: msgSenderType,
        senderId: data.senderId,
        senderName: data.senderName,
        senderAvatar: data.senderAvatar,
        createTime: data.timestamp || new Date().toISOString(),
      };
      // 避免重复添加
      if (!messages.value.find(m => m.id === newMsg.id)) {
        messages.value.push(newMsg);
        scrollToBottom();
      }
      // 收到非用户消息（AI/客服）后，解除等待状态
      // senderType: 0=用户, 1=客服, 2=AI, 3=系统
      if (msgSenderType !== 0) {
        console.log('[UserChat] 收到AI/客服回复，解除等待状态, senderType:', msgSenderType);
        stopAiResponding();
      }
      break;

    case 'system':
      // 系统消息
      messages.value.push({
        id: Date.now().toString(),
        content: data.content,
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'typing':
      // 客服正在输入
      agentTyping.value = true;
      setTimeout(() => {
        agentTyping.value = false;
      }, 3000);
      break;

    case 'ai_typing':
      // AI正在输入状态
      if (data.data?.isTyping) {
        agentTyping.value = true;
      } else {
        agentTyping.value = false;
      }
      break;

    case 'ai_stream':
      // AI流式消息 - 逐步显示
      handleAiStreamToken(data);
      break;

    case 'ai_stream_complete':
      // AI流式消息完成
      handleAiStreamComplete(data);
      break;

    case 'agent_connected':
      // 客服已接入，自动切换为手动模式
      hasAgent.value = true;
      if (data.extra?.replyMode !== undefined) {
        replyMode.value = data.extra.replyMode;
      } else {
        replyMode.value = 1; // 默认切换为手动模式
      }
      // ★ 切换为手动模式后，解除AI回复中状态
      stopAiResponding();
      messages.value.push({
        id: Date.now().toString(),
        content: data.content || `客服 ${data.extra?.agentName || data.senderName || ''} 已为您服务`,
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'mode_changed':
      // 模式切换通知
      if (data.extra?.replyMode !== undefined) {
        replyMode.value = data.extra.replyMode;
      }
      // ★ 切换为手动模式后，解除AI回复中状态
      if (replyMode.value === 1) {
        stopAiResponding();
      }
      console.log('[UserChat] 回复模式已切换为', replyMode.value === 1 ? '人工服务' : 'AI自动回复');
      break;

    case 'conversation_closed':
      // 会话已结束
      conversationClosed.value = true;
      messages.value.push({
        id: Date.now().toString(),
        content: data.content || '会话已结束，感谢您的咨询',
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'pong':
      // 心跳响应
      break;

    default:
      console.log('[UserChat] 未处理的消息类型:', data.type);
  }
}

// 发送消息
async function sendMessage() {
  const content = inputMessage.value.trim();
  const attachments = attachmentList.value.filter(a => !a.uploading && a.url);
  if (!content && !attachments.length) return;

  // 敏感词前端校验
  if (content) {
    const hitWord = checkSensitiveWords(content);
    if (hitWord) {
      message.warning('消息包含敏感内容，请修改后重试');
      return;
    }
  }

  // 附件是否还在上传中
  if (attachmentList.value.some(a => a.uploading)) {
    message.warning('文件正在上传中，请稍候');
    return;
  }

  // AI自动模式下，AI回复中时不允许发送新消息
  if (replyMode.value === 0 && aiResponding.value) {
    message.warning('请等待AI回复完成');
    return;
  }

  // 消息接通模式：第一次发送时才创建正式会话
  if (messageConnectMode.value && !conversationId.value) {
    try {
      await initConversation();
      messageConnectMode.value = false;
      if (!conversationId.value) {
        message.error('会话创建失败');
        return;
      }
      // 连接WebSocket
      connectWebSocket();
      startFallbackPoll();
    } catch (e) {
      message.error('会话初始化失败');
      return;
    }
  }

  if (!conversationId.value) {
    message.error('会话未初始化');
    return;
  }

  const msgType = attachments.length > 0 ? 5 : 0;
  const extra = attachments.length > 0 ? JSON.stringify({ attachments: attachments.map(a => ({ name: a.name, url: a.url, size: a.size, type: a.type })) }) : undefined;

  // 先添加到本地显示
  const localMsg: any = {
    id: 'local_' + Date.now(),
    conversationId: conversationId.value,
    content: content,
    senderType: 0,
    senderId: userId.value,
    senderName: userName.value,
    createTime: new Date().toISOString(),
    msgType,
    extra: extra ? JSON.parse(extra) : undefined,
  };
  messages.value.push(localMsg);
  
  // 清空输入框和附件列表
  inputMessage.value = '';
  attachmentList.value.forEach(a => { if (a.previewUrl) URL.revokeObjectURL(a.previewUrl); });
  attachmentList.value = [];
  await nextTick();
  
  scrollToBottom();

  sending.value = true;
  
  const isAiMode = replyMode.value === 0;
  if (isAiMode) {
    aiResponding.value = true;
  }
  
  if (aiResponseTimeoutTimer) {
    clearTimeout(aiResponseTimeoutTimer);
  }
  aiResponseTimeoutTimer = window.setTimeout(() => {
    if (isAiMode) {
      stopAiResponding('AI回复超时，请稍后重试');
    }
  }, 60000);
  
  try {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'message',
        conversationId: conversationId.value,
        content: content,
        userName: userName.value,
        msgType,
        extra,
      }));
    } else {
      await httpPost({
        url: '/cs/message/send',
        data: {
          conversationId: conversationId.value,
          content: content,
          senderId: userId.value,
          senderName: userName.value,
          senderType: 'user',
          msgType,
          extra,
        },
      });
    }
  } catch (e) {
    console.error('发送消息失败', e);
    message.error('发送失败，请重试');
    if (isAiMode) {
      stopAiResponding();
    }
  } finally {
    sending.value = false;
  }
}

// 处理键盘事件
function handleKeydown(e: KeyboardEvent) {
  // 只在按下 Enter 且不带 Shift 时发送
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    sendMessage();
  }
  // Shift+Enter 允许默认换行行为
}

// 重新开始对话
async function restartConversation() {
  try {
    // 断开当前WebSocket
    disconnectWebSocket();
    
    // 清除localStorage中的会话ID，以便创建新会话
    localStorage.removeItem(`cs_conversation_${userId.value}`);
    
    // 清空消息和状态
    messages.value = [];
    conversationClosed.value = false;
    conversationId.value = '';
    // 创建新会话
    await initConversation();

    // 加载历史消息并追加开场白（若需要）
    await loadMessages();

    // 重新连接WebSocket
    connectWebSocket();
    
    console.log('[UserChat] 已开始新对话');
  } catch (e) {
    console.error('[UserChat] 重新开始对话失败', e);
    message.error('重新开始失败，请刷新页面');
  }
}



// 处理AI流式token
function handleAiStreamToken(data: any) {
  const messageId = data.messageId;
  const token = data.content;
  
  if (!messageId || !token) return;
  
  // 累积token
  const currentContent = streamingMessages.value.get(messageId) || '';
  const newContent = currentContent + token;
  streamingMessages.value.set(messageId, newContent);
  
  // 查找或创建消息
  let existingMsg = messages.value.find(m => m.id === messageId);
  if (!existingMsg) {
    // 创建新的流式消息
    existingMsg = {
      id: messageId,
      conversationId: data.conversationId,
      content: newContent,
      senderType: 1, // AI消息
      senderId: 'ai',
      senderName: '智能客服',
      createTime: new Date().toISOString(),
      isStreaming: true, // 标记为流式消息
    };
    messages.value.push(existingMsg);
  } else {
    // 更新现有消息内容
    existingMsg.content = newContent;
  }
  
  scrollToBottom();
}

// 处理AI流式消息完成
function handleAiStreamComplete(data: any) {
  const messageId = data.messageId;
  const fullContent = data.content;
  
  // 清除流式缓存
  streamingMessages.value.delete(messageId);
  
  // 更新消息为最终内容
  const existingMsg = messages.value.find(m => m.id === messageId);
  if (existingMsg) {
    existingMsg.content = fullContent;
    existingMsg.isStreaming = false;
  }
  
  // 解除AI回复中状态
  stopAiResponding();
  agentTyping.value = false;
  
  scrollToBottom();
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}

function isMessagesAtBottom() {
  const el = messagesRef.value;
  if (!el) return true;
  const threshold = 40;
  return el.scrollHeight - (el.scrollTop + el.clientHeight) <= threshold;
}

function getDateKey(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatDateSeparator(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  const today = new Date();
  const dateKey = getDateKey(date);
  const todayKey = getDateKey(today);
  if (dateKey === todayKey) return '今天';
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  if (dateKey === getDateKey(yesterday)) return '昨天';
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
}

// 格式化时间（根据聊天窗口配置的时区）
function formatTime(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (chatWindowConfig.visitorTimezone === 'Asia/Shanghai') {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Shanghai' });
  }
  // 自动跟随访客时区
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

// 判断是否是用户消息
// senderType: 0-用户, 1-AI, 2-客服, 3-系统
function isUserMessage(msg: any): boolean {
  // 用户消息 senderType === 0
  // 或者 senderId 等于当前用户ID
  if (msg.senderType === 0) return true;
  if (msg.senderId === userId.value) return true;
  return false;
}

// 获取消息样式类
function getMessageClass(msg: any) {
  if (msg.senderType === 3) return 'is-system';
  if (isUserMessage(msg)) return 'is-user';
  return msg.senderType === 1 ? 'is-ai' : 'is-agent';
}

function parseExtra(extra: any) {
  if (!extra) return null;
  if (typeof extra === 'string') {
    try {
      return JSON.parse(extra);
    } catch {
      return null;
    }
  }
  return extra;
}

function getMessageAttachments(msg: any): any[] {
  const extra = parseExtra(msg?.extra);
  return extra?.attachments || [];
}

function getMediaAttachments(msg: any): any[] {
  return getMessageAttachments(msg).filter(item => item.type === 'image' || item.type === 'video');
}

function getFileAttachments(msg: any): any[] {
  return getMessageAttachments(msg).filter(item => item.type === 'file');
}

function getMediaGridData(msg: any) {
  const media = getMediaAttachments(msg);
  const maxItems = 4;
  const items = media.slice(0, maxItems);
  const extraCount = Math.max(0, media.length - maxItems);
  return { items, extraCount, total: media.length };
}

function getAttachmentUrl(attachment: any) {
  return getFileAccessHttpUrl(attachment?.url);
}

function openFilePreview(item: any) {
  const url = getAttachmentUrl(item);
  if (url) {
    window.open(url, '_blank');
  }
}

function openImagePreview(msg: any, item: any) {
  const images = getMessageAttachments(msg).filter(att => att.type === 'image');
  const imageList = images.map(att => getAttachmentUrl(att));
  if (!imageList.length) return;
  const targetUrl = getAttachmentUrl(item);
  const index = imageList.findIndex(url => url === targetUrl);
  createImgPreview({
    imageList,
    index: index >= 0 ? index : 0,
    defaultWidth: 700,
    rememberState: true,
  });
}

const mediaViewerVisible = ref(false);
const mediaViewerList = ref<any[]>([]);
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
});

function openMediaViewer(msg: any) {
  mediaViewerList.value = getMediaAttachments(msg);
  mediaViewerVisible.value = true;
}

// 渲染消息内容（支持富文本HTML、Markdown、纯文本）
function renderMessage(content: string) {
  if (!content) return '';
  // 1. 检测是否为完整HTML（TinyMCE富文本，如FAQ答案）— 直接返回，不经markdown-it二次处理
  const isRichHtml = /^\s*<(?:p|div|ul|ol|h[1-6]|table|blockquote)\b/i.test(content.trim());
  if (isRichHtml) {
    return content;
  }
  // 2. Markdown 检测
  const hasMarkdown = /!\[[^\]]*]\([^)]*\)|\*\*[^*]+\*\*|```|^\s*#/m.test(content);
  if (hasMarkdown) {
    return md.render(content);
  }
  // 3. 检测内联HTML（如 <a>、<img>、<br> 等）
  const hasInlineHtml = /<([a-z][\s\S]*?)>/i.test(content);
  if (hasInlineHtml) {
    return md.render(content);
  }
  // 4. 纯文本：转义并保留换行
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>');
}

// 监听消息变化，自动滚动
watch(messages, () => {
  if (loadingHistory.value) return;
  if (!isMessagesAtBottom()) return;
  scrollToBottom();
}, { deep: true });
</script>

<style lang="less" scoped>
.user-chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 1100px;
  margin: 0 auto;
  background: #f5f5f5;
}

.chat-main-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.chat-main-column {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  min-height: 0;
}

/* PC右侧区域（广告+FAQ） */
.chat-sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  border-left: 1px solid #f0f0f0;
  overflow: hidden;
}
.sidebar-ad {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 8px;
}
.ad-sidebar-img {
  width: 100%;
  max-height: 300px;
  object-fit: contain;
  border-radius: 4px;
  cursor: pointer;
}
.sidebar-faq {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
  border-top: 1px solid #f0f0f0;
}
.sidebar-faq-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}
.sidebar-faq-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sidebar-faq-item {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 12px;
  color: #333;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: all 0.2s;
}
.sidebar-faq-item:hover {
  background: #e6f7ff;
  border-color: #91d5ff;
}

/* 手机端FAQ */
.faq-mobile-section {
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  flex-shrink: 0;
  display: none;
}
.faq-mobile-header {
  padding: 8px 16px;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  user-select: none;
}
.faq-mobile-toggle {
  margin-left: auto;
  color: #1890ff;
  font-size: 12px;
}
.faq-mobile-list {
  padding: 0 12px 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.faq-mobile-item {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 14px;
  padding: 4px 12px;
  font-size: 12px;
  color: #333;
  cursor: pointer;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.faq-mobile-item:active {
  background: #e6f7ff;
  border-color: #91d5ff;
}

@media (max-width: 800px) {
  .chat-sidebar {
    display: none;
  }
  .faq-mobile-section {
    display: block;
  }
}

/* 滚动文字跑马灯 */
.scroll-text-bar {
  overflow: hidden;
  white-space: nowrap;
  padding: 5px 0;
  font-size: 13px;
  flex-shrink: 0;
}
.scroll-text-content {
  display: inline-block;
  padding-left: 100%;
  animation: marquee var(--scroll-duration, 15s) linear infinite;
}
@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}

/* 留言板样式 */
.leave-message-board {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  
  .board-header {
    display: flex;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    
    .app-avatar {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      margin-right: 12px;
      object-fit: cover;
    }
    
    .board-title {
      display: flex;
      flex-direction: column;
      
      .app-name {
        font-size: 16px;
        font-weight: 600;
      }
      
      .board-subtitle {
        font-size: 13px;
        opacity: 0.9;
        margin-top: 2px;
      }
    }
  }
  
  .board-body {
    flex: 1;
    padding: 24px 20px;
    overflow-y: auto;
    
    .submit-success {
      text-align: center;
      padding: 40px 0;
      
      p {
        margin-top: 12px;
        color: #52c41a;
        font-size: 15px;
      }
    }
  }
}

/* 留言回复卡片 */
.leave-message-replies {
  padding: 8px 12px;
  background: #fffbe6;
  border-bottom: 1px solid #ffe58f;
  
  .reply-card {
    background: #fff;
    border-radius: 8px;
    padding: 10px 12px;
    margin-bottom: 6px;
    border: 1px solid #ffe58f;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .reply-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
      
      .reply-label {
        font-weight: 600;
        color: #fa8c16;
        font-size: 13px;
      }
    }
    
    .reply-original {
      font-size: 12px;
      color: #999;
      margin-bottom: 4px;
    }
    
    .reply-content {
      font-size: 13px;
      color: #333;
    }
    
    .reply-time {
      font-size: 11px;
      color: #bbb;
      margin-top: 4px;
    }
  }
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--theme-color, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;

  .header-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .app-avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.3);
    object-fit: cover;
  }

  .app-info {
    display: flex;
    flex-direction: column;
  }

  .app-name {
    font-size: 18px;
    font-weight: 600;
  }

  .status-text {
    font-size: 12px;
    opacity: 0.9;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    
    &.online {
      background: #52c41a;
      box-shadow: 0 0 4px #52c41a;
    }
    
    &.offline {
      background: #faad14;
    }
  }

  .header-actions {
    font-size: 18px;
    cursor: pointer;
    opacity: 0.8;
    
    &:hover {
      opacity: 1;
    }
  }
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #fff;
  background-image: var(--chat-bg-image, none);
  background-size: cover;
  background-position: center;

  .loading-wrapper, .empty-messages {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: #999;
    
    p {
      margin-top: 16px;
      font-size: 14px;
    }
  }
}

.message-item {
  margin-bottom: 16px;

  &.is-system {
    display: flex;
    justify-content: center;
  }
}

.system-message {
  padding: 6px 16px;
  background: #f0f0f0;
  border-radius: 16px;
  font-size: 12px;
  color: #666;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 12px;

  .message-content {
    max-width: 70%;
  }

  .message-text {
    padding: 12px 16px;
    background: var(--visitor-bubble-bg, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
    color: var(--visitor-bubble-color, #fff);
    border-radius: 20px 20px 4px 20px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;
  }

  .message-time {
    text-align: right;
    font-size: 11px;
    color: #999;
    margin-top: 4px;
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
  }
}

.agent-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 12px;

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
  }

  .message-content {
    max-width: 70%;
  }

  .sender-info {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 4px;
  }

  .sender-name {
    font-size: 12px;
    color: #666;
  }

  .message-text {
    padding: 12px 16px;
    background: var(--agent-bubble-bg, #f5f5f5);
    color: var(--agent-bubble-color, #333);
    border-radius: 20px 20px 20px 4px;
    font-size: 14px;
    line-height: 1.6;
    word-break: break-word;

    :deep(p) {
      margin: 0 0 8px;
      &:last-child {
        margin-bottom: 0;
      }
    }

    :deep(pre) {
      background: #f0f0f0;
      padding: 8px 12px;
      border-radius: 8px;
      overflow-x: auto;
    }

    :deep(img) {
      max-width: 100%;
      height: auto;
      border-radius: 8px;
      display: block;
      margin: 4px 0;
    }

    :deep(code) {
      background: #e8e8e8;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
    }

    :deep(ul), :deep(ol) {
      padding-left: 20px;
      margin: 8px 0;
    }
  }

  .message-time {
    font-size: 11px;
    color: #999;
    margin-top: 4px;
  }
}

.message-media-grid {
  margin-top: 6px;
  display: grid;
  gap: 4px;

  .media-item {
    border-radius: 6px;
    overflow: hidden;
    background: #f5f5f5;
    position: relative;
    img,
    video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .media-more {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.55);
      color: #fff;
      font-size: 16px;
      font-weight: 600;
    }
  }
}

.media-grid--1 {
  grid-template-columns: 1fr;
  .media-item {
    aspect-ratio: 3 / 2;
  }
}

.media-grid--2 {
  grid-template-columns: repeat(2, 1fr);
  .media-item {
    aspect-ratio: 1 / 1;
  }
}

.media-grid--3 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  .media-item {
    aspect-ratio: 1 / 1;
  }
  .media-item:nth-child(1) {
    grid-row: span 2;
  }
}

.media-grid--4 {
  grid-template-columns: repeat(2, 1fr);
  .media-item {
    aspect-ratio: 1 / 1;
  }
}


.message-file-list {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .file-item {
    padding: 6px 8px;
    background: #f7f7f7;
    border-radius: 6px;
    font-size: 12px;
    cursor: pointer;
  }
}

.media-viewer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
}

.media-viewer-item {
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  border: 1px solid #f0f0f0;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  img,
  video {
    width: 100%;
    height: 100%;
    object-fit: cover;
    cursor: pointer;
  }
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  }
}

.media-viewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #666;
  font-size: 13px;
}

.media-viewer-tip {
  color: #999;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
  }

  .typing-dots {
    display: flex;
    gap: 4px;
    padding: 12px 16px;
    background: #f5f5f5;
    border-radius: 20px;

    span {
      width: 8px;
      height: 8px;
      background: #999;
      border-radius: 50%;
      animation: typing 1.4s infinite ease-in-out;

      &:nth-child(1) { animation-delay: 0s; }
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.preset-questions {
  padding: 12px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;

  .preset-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 12px;
    color: #666;
    margin-bottom: 10px;

    .anticon {
      color: #faad14;
    }
  }

  .preset-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    :deep(.ant-btn) {
      background: #fff;
      border: 1px solid #e8e8e8;
      border-radius: 16px;
      font-size: 12px;
      color: #666;
      transition: all 0.3s;

      &:hover {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        border-color: transparent;
      }
    }
  }
}

.chat-input {
  display: flex;
  flex-direction: column;
  padding: 8px 16px 12px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;

  .input-toolbar {
    display: flex;
    gap: 14px;
    padding: 4px 4px 6px;
    font-size: 18px;
    color: #666;
  }

  .toolbar-icon {
    cursor: pointer;
    transition: color 0.2s;
    &:hover {
      color: var(--theme-color, #667eea);
    }
  }

  .attachment-preview-bar {
    display: flex;
    gap: 8px;
    padding: 6px 0;
    overflow-x: auto;
  }

  .attachment-thumb {
    position: relative;
    width: 64px;
    height: 64px;
    border-radius: 6px;
    overflow: hidden;
    border: 1px solid #eee;
    flex-shrink: 0;

    .att-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .att-file {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      font-size: 22px;
      background: #f9f9f9;

      .att-name {
        font-size: 9px;
        color: #999;
        max-width: 56px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .att-remove {
      position: absolute;
      top: 0;
      right: 0;
      width: 18px;
      height: 18px;
      background: rgba(0,0,0,0.5);
      color: #fff;
      font-size: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      border-radius: 0 6px 0 6px;
    }

    .att-uploading {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(255,255,255,0.7);
    }
  }

  :deep(.ant-input) {
    flex: 1;
    border-radius: 20px;
    padding: 10px 16px;
    resize: none;
    border-color: #d9d9d9;
    
    &:focus {
      border-color: var(--theme-color, #667eea);
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
    }
  }

  :deep(.ant-btn) {
    border-radius: 20px;
    height: 40px;
    padding: 0 24px;
    background: var(--theme-color, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
    border: none;
    margin-top: 8px;
    align-self: flex-end;
    
    &:hover {
      opacity: 0.9;
    }
    
    &:disabled {
      background: #d9d9d9;
    }
  }
}

.chat-fatal-error {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
  color: #1f1f1f;
  text-align: center;
  padding: 24px;
}

.chat-fatal-error .fatal-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.chat-fatal-error .fatal-desc {
  font-size: 14px;
  color: #8c8c8c;
}

.chat-closed {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 20px;
  background: #fafafa;
  border-top: 1px solid #f0f0f0;
  
  span {
    color: #999;
    font-size: 14px;
  }
  
  :deep(.ant-btn) {
    border-radius: 20px;
    height: 40px;
    padding: 0 32px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border: none;
  }
}
</style>
