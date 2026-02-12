<template>
  <div class="chat-window-settings">
    <div class="settings-layout">
      <!-- 左侧配置表单 -->
      <div class="settings-form">
        <a-card title="聊天窗口设置" :bordered="false">
          <a-form layout="vertical">
            <!-- 外观 -->
            <a-divider orientation="left">外观</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="主题色（头部背景）">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.themeColor" class="color-input" />
                    <a-input v-model:value="config.themeColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="页面标题">
                  <a-input v-model:value="config.pageTitle" placeholder="在线客服" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="Logo">
                  <a-space>
                    <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'logo')">
                      <a-button size="small">上传Logo</a-button>
                    </a-upload>
                    <a-input v-model:value="config.logo" placeholder="图片URL" size="small" style="width:180px" />
                  </a-space>
                  <div v-if="config.logo" class="preview-thumb">
                    <img :src="resolveUrl(config.logo)" alt="logo" />
                    <DeleteOutlined class="thumb-delete" @click="config.logo = ''" title="删除" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="背景图片（聊天区域）">
                  <a-space>
                    <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'backgroundImage')">
                      <a-button size="small">上传背景</a-button>
                    </a-upload>
                    <a-input v-model:value="config.backgroundImage" placeholder="图片URL" size="small" style="width:180px" />
                  </a-space>
                  <div v-if="config.backgroundImage" class="preview-thumb preview-thumb--wide">
                    <img :src="resolveUrl(config.backgroundImage)" alt="bg" />
                    <DeleteOutlined class="thumb-delete" @click="config.backgroundImage = ''" title="删除" />
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 头部 -->
            <a-divider orientation="left">头部</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="显示头部">
                  <a-switch v-model:checked="config.headerVisible" />
                  <span class="switch-hint">关闭后访客端不显示头部</span>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="头部背景图">
                  <a-space>
                    <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'headerBgImage')">
                      <a-button size="small">上传背景图</a-button>
                    </a-upload>
                    <a-input v-model:value="config.headerBgImage" placeholder="图片URL" size="small" style="width:160px" />
                  </a-space>
                  <div class="upload-hint">上传后头部将使用背景图替代纯色</div>
                  <div v-if="config.headerBgImage" class="preview-thumb preview-thumb--wide">
                    <img :src="resolveUrl(config.headerBgImage)" alt="header bg" />
                    <DeleteOutlined class="thumb-delete" @click="config.headerBgImage = ''" title="删除" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="24">
                <a-form-item label="滚动文字（跑马灯）">
                  <a-input v-model:value="config.scrollText" placeholder="填入后头部下方展示滚动文字" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="滚动时长（秒）">
                  <a-input-number v-model:value="config.scrollDuration" :min="10" :max="120" placeholder="15" style="width:100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="滚动文字颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.scrollTextColor" class="color-input" />
                    <a-input v-model:value="config.scrollTextColor" size="small" style="width:100px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="滚动文字背景">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.scrollTextBgColor" class="color-input" />
                    <a-input v-model:value="config.scrollTextBgColor" size="small" style="width:100px" />
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 头部图标（PC端显示在头部右侧） -->
            <a-divider orientation="left">头部图标（PC端）</a-divider>
            <div class="upload-hint" style="margin-bottom:12px">PC端头部右侧显示的图标链接，点击可跳转。最多配置5个。</div>
            <div class="header-icons-section">
              <div v-if="config.headerIcons.length === 0" class="header-icons-empty">
                <span style="color:#bbb">暂无图标，点击下方按钮添加</span>
              </div>
              <div v-for="(item, idx) in config.headerIcons" :key="idx" class="header-icon-row">
                <div class="header-icon-row-num">{{ idx + 1 }}</div>
                <div class="header-icon-row-fields">
                  <div class="header-icon-field">
                    <span class="header-icon-label">图标</span>
                    <a-space>
                      <a-upload :showUploadList="false" :customRequest="(info) => handleIconUpload(info, idx)">
                        <a-button size="small">上传</a-button>
                      </a-upload>
                      <a-input v-model:value="item.icon" placeholder="图标URL" size="small" style="width:140px" />
                    </a-space>
                    <div v-if="item.icon" class="preview-thumb" style="margin-top:4px">
                      <img :src="resolveUrl(item.icon)" alt="icon" />
                      <DeleteOutlined class="thumb-delete" @click="item.icon = ''" title="删除" />
                    </div>
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">昵称</span>
                    <a-input v-model:value="item.name" placeholder="图标名称" size="small" style="width:120px" />
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">链接</span>
                    <a-input v-model:value="item.link" placeholder="点击跳转URL" size="small" style="width:200px" />
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">尺寸</span>
                    <a-input-number v-model:value="item.size" :min="16" :max="80" placeholder="32" size="small" style="width:80px" />
                    <span style="color:#999;font-size:12px;margin-left:2px">px</span>
                  </div>
                  <div class="header-icon-field">
                    <span class="header-icon-label">透明</span>
                    <a-switch v-model:checked="item.transparent" size="small" />
                    <span style="color:#999;font-size:11px;margin-left:4px">去掉边框背景</span>
                  </div>
                </div>
                <a-button type="text" size="small" danger @click="removeHeaderIcon(idx)"><DeleteOutlined /></a-button>
              </div>
              <a-button type="dashed" size="small" :disabled="config.headerIcons.length >= 5" @click="addHeaderIcon" style="margin-top:8px">
                <PlusOutlined /> 添加图标
              </a-button>
              <span v-if="config.headerIcons.length >= 5" class="faq-limit-hint" style="margin-left:8px">已达上限</span>
            </div>

            <!-- 客服气泡 -->
            <a-divider orientation="left">客服气泡</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="背景色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.agentBubbleBgColor" class="color-input" />
                    <a-input v-model:value="config.agentBubbleBgColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="字体颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.agentBubbleFontColor" class="color-input" />
                    <a-input v-model:value="config.agentBubbleFontColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 访客气泡 -->
            <a-divider orientation="left">访客气泡</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="背景色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.visitorBubbleBgColor" class="color-input" />
                    <a-input v-model:value="config.visitorBubbleBgColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="字体颜色">
                  <div class="color-picker-row">
                    <input type="color" v-model="config.visitorBubbleFontColor" class="color-input" />
                    <a-input v-model:value="config.visitorBubbleFontColor" size="small" style="width:120px" />
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="访客头像">
                  <a-space>
                    <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'visitorAvatar')">
                      <a-button size="small">上传头像</a-button>
                    </a-upload>
                    <a-input v-model:value="config.visitorAvatar" placeholder="图片URL" size="small" style="width:160px" />
                  </a-space>
                  <div class="upload-hint">建议尺寸 80×80 px</div>
                  <div v-if="config.visitorAvatar" class="preview-thumb">
                    <img :src="resolveUrl(config.visitorAvatar)" alt="avatar" />
                    <DeleteOutlined class="thumb-delete" @click="config.visitorAvatar = ''" title="删除" />
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 功能开关 -->
            <a-divider orientation="left">功能开关</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="访客历史记录">
                  <a-switch v-model:checked="config.visitorHistory" />
                  <span class="switch-hint">开启后访客可查看历史消息</span>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="访客消息接通">
                  <a-switch v-model:checked="config.visitorMessageConnect" />
                  <span class="switch-hint">开启后发消息才接通，否则自动接入</span>
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送表情">
                  <a-switch v-model:checked="config.sendEmoji" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送图片">
                  <a-switch v-model:checked="config.sendImage" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送视频">
                  <a-switch v-model:checked="config.sendVideo" />
                </a-form-item>
              </a-col>
              <a-col :span="6">
                <a-form-item label="发送PDF">
                  <a-switch v-model:checked="config.sendPdf" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="文件大小限制">
                  <div style="display:flex;align-items:center;gap:8px">
                    <a-input-number v-model:value="config.maxFileSize" :min="1" :max="50" :step="1" style="width:120px" />
                    <span style="color:#999">MB（最大50MB）</span>
                  </div>
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="访客端时区">
                  <a-select v-model:value="config.visitorTimezone" style="width:100%">
                    <a-select-option value="Asia/Shanghai">北京时间</a-select-option>
                    <a-select-option value="auto">自动跟随访客时区</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- PC广告位 -->
            <a-divider orientation="left">PC右侧广告位</a-divider>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="广告链接">
                  <a-input v-model:value="config.pcAdLink" placeholder="点击广告图片跳转的链接" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="广告图片">
                  <a-space>
                    <a-upload :showUploadList="false" :customRequest="(info) => handleUpload(info, 'pcAdImage')">
                      <a-button size="small">上传广告图</a-button>
                    </a-upload>
                    <a-input v-model:value="config.pcAdImage" placeholder="图片URL" size="small" style="width:160px" />
                  </a-space>
                  <div class="upload-hint">建议尺寸 200×540 px</div>
                  <div v-if="config.pcAdImage" class="preview-thumb preview-thumb--wide">
                    <img :src="resolveUrl(config.pcAdImage)" alt="ad" />
                    <DeleteOutlined class="thumb-delete" @click="config.pcAdImage = ''" title="删除" />
                  </div>
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 常见问题 -->
            <a-divider orientation="left">常见问题</a-divider>
            <a-row :gutter="16">
              <a-col :span="24">
                <a-form-item label="启用常见问题">
                  <a-switch v-model:checked="config.faqEnabled" />
                  <span class="switch-hint">开启后将替代 AI 预设问题，访客端优先展示此处配置的常见问题，点击后直接返回预设答案（不触发 AI 回复）</span>
                </a-form-item>
              </a-col>
              <a-col :span="24" v-if="config.faqEnabled">
                <div class="faq-section">
                  <div class="faq-header">
                    <a-button type="primary" size="small" :disabled="config.faqList.length >= 10" @click="openFaqEditor(-1)">
                      <PlusOutlined /> 添加问题
                    </a-button>
                    <span class="faq-count">{{ config.faqList.length }} / 10 条</span>
                    <span v-if="config.faqList.length >= 10" class="faq-limit-hint">已达上限，最多添加10条常见问题</span>
                  </div>
                  <div v-if="config.faqList.length === 0" class="faq-empty">
                    <QuestionCircleOutlined style="font-size:28px;color:#d9d9d9;margin-bottom:8px" />
                    <div>暂无常见问题</div>
                    <div style="font-size:12px;color:#bbb;margin-top:4px">点击上方"添加问题"按钮开始配置</div>
                  </div>
                  <div v-for="(faq, idx) in config.faqList" :key="idx" class="faq-item" @click="openFaqEditor(idx)">
                    <div class="faq-item-num">{{ idx + 1 }}</div>
                    <div class="faq-item-body">
                      <div class="faq-item-question">{{ faq.question }}</div>
                      <div class="faq-item-answer">{{ stripHtml(faq.answer) }}</div>
                    </div>
                    <div class="faq-item-actions" @click.stop>
                      <a-tooltip title="编辑">
                        <a-button type="text" size="small" @click="openFaqEditor(idx)"><EditOutlined /></a-button>
                      </a-tooltip>
                      <a-tooltip title="删除">
                        <a-button type="text" size="small" danger @click="removeFaq(idx)"><DeleteOutlined /></a-button>
                      </a-tooltip>
                    </div>
                  </div>
                </div>
              </a-col>
            </a-row>

            <a-button type="primary" @click="handleSave" :loading="saving" style="margin-top:16px">保存设置</a-button>
          </a-form>
        </a-card>
      </div>

      <!-- 右侧预览区 -->
      <div class="settings-preview">
        <a-card :bordered="false" :bodyStyle="{ padding: '12px' }">
          <a-tabs v-model:activeKey="previewTab" centered>
            <a-tab-pane key="pc" tab="电脑端预览" />
            <a-tab-pane key="mobile" tab="手机端预览" />
          </a-tabs>
          <div class="preview-container" :class="previewTab">
            <div class="preview-wrapper" :style="previewWrapperStyle">
              <!-- 全宽头部（独立于 preview-chat，横跨整个 preview-wrapper） -->
              <div v-if="config.headerVisible" class="p-header-full" :style="previewHeaderStyle">
                <div class="p-header-content">
                  <img v-if="config.logo" :src="resolveUrl(config.logo)" class="p-logo" alt="logo" />
                  <span class="p-title">{{ config.pageTitle || '在线客服' }}</span>
                </div>
                <div v-if="previewTab === 'pc' && config.headerIcons?.length" class="p-header-icons">
                  <a v-for="(item, idx) in config.headerIcons" :key="idx" class="p-header-icon-item" :href="item.link || '#'" target="_blank" rel="noopener">
                    <img v-if="item.icon" :src="resolveUrl(item.icon)"
                         :class="['p-header-icon-img', { 'p-header-icon-transparent': item.transparent }]"
                         :style="{ width: (item.size || 32) + 'px', height: (item.size || 32) + 'px' }" />
                    <span v-else class="p-header-icon-placeholder">📎</span>
                    <span class="p-header-icon-name"
                          :style="{ fontSize: Math.max(9, Math.round((item.size || 32) * 0.3)) + 'px', maxWidth: Math.max(36, (item.size || 32) * 1.6) + 'px' }">{{ item.name || '图标' }}</span>
                  </a>
                </div>
              </div>
              <!-- 内容区域（聊天 + 侧边栏水平排列） -->
              <div class="preview-body">
                <!-- 聊天窗口 -->
                <div class="preview-chat" :style="previewChatStyle">
                  <!-- 滚动文字 -->
                  <div v-if="config.scrollText" class="p-scroll-bar"
                       :style="{ background: config.scrollTextBgColor || '#1890ff', color: config.scrollTextColor || '#fff' }">
                    <div class="p-scroll-text" :style="{ animationDuration: (config.scrollDuration || 15) + 's' }">
                      {{ config.scrollText }}
                    </div>
                  </div>
                  <!-- 消息区域 -->
                  <div class="p-messages" :style="msgAreaStyle">
                    <!-- 客服消息 -->
                    <div class="p-msg p-msg-agent">
                      <div class="p-avatar">🤖</div>
                      <div class="p-bubble" :style="{ background: config.agentBubbleBgColor || '#f5f5f5', color: config.agentBubbleFontColor || '#333' }">
                        你好，有什么可以帮助您的？
                      </div>
                    </div>
                    <!-- 访客消息 -->
                    <div class="p-msg p-msg-visitor">
                      <div class="p-bubble" :style="{ background: config.visitorBubbleBgColor || '#667eea', color: config.visitorBubbleFontColor || '#fff' }">
                        我想咨询一下产品信息
                      </div>
                      <div class="p-avatar">
                        <img v-if="config.visitorAvatar" :src="resolveUrl(config.visitorAvatar)" class="p-avatar-img" />
                        <span v-else>👤</span>
                      </div>
                    </div>
                    <!-- 客服回复 -->
                    <div class="p-msg p-msg-agent">
                      <div class="p-avatar">🤖</div>
                      <div class="p-bubble" :style="{ background: config.agentBubbleBgColor || '#f5f5f5', color: config.agentBubbleFontColor || '#333' }">
                        好的，请问您想了解哪款产品？
                      </div>
                    </div>
                  </div>
                  <!-- 手机端FAQ入口（输入框上方） -->
                  <div v-if="previewTab === 'mobile' && config.faqEnabled && config.faqList.length > 0" class="p-faq-mobile">
                    <div class="p-faq-mobile-title"><QuestionCircleOutlined /> 常见问题</div>
                    <div class="p-faq-mobile-list">
                      <div v-for="(faq, idx) in config.faqList" :key="idx" class="p-faq-mobile-item">{{ faq.question }}</div>
                    </div>
                  </div>
                  <!-- 输入区 -->
                  <div class="p-input-area">
                    <div class="p-toolbar">
                      <span v-if="config.sendEmoji" class="p-tool-icon">😊</span>
                      <span v-if="config.sendImage" class="p-tool-icon">🖼</span>
                      <span v-if="config.sendVideo" class="p-tool-icon">🎬</span>
                      <span v-if="config.sendPdf" class="p-tool-icon">📄</span>
                    </div>
                    <div class="p-input-box">
                      <span class="p-input-placeholder">输入消息...</span>
                      <span class="p-send-btn" :style="{ background: config.themeColor || '#667eea' }">发送</span>
                    </div>
                  </div>
                </div>
                <!-- PC右侧区域（广告+FAQ） -->
                <div v-if="previewTab === 'pc' && hasSidebar" class="preview-sidebar">
                  <div v-if="config.pcAdImage" class="preview-ad">
                    <a :href="config.pcAdLink || '#'" target="_blank" rel="noopener">
                      <img :src="resolveUrl(config.pcAdImage)" class="preview-ad-img" alt="ad" />
                    </a>
                  </div>
                  <div v-if="config.faqEnabled && config.faqList.length > 0" class="preview-faq">
                    <div class="preview-faq-title"><QuestionCircleOutlined /> 常见问题</div>
                    <div class="preview-faq-list">
                      <div v-for="(faq, idx) in config.faqList" :key="idx" class="preview-faq-item">{{ faq.question }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </a-card>
      </div>
    </div>

    <!-- FAQ编辑弹窗 -->
    <a-modal
      v-model:open="faqEditorVisible"
      :title="faqEditIndex >= 0 ? '编辑常见问题' : '添加常见问题'"
      :width="780"
      :maskClosable="false"
      :bodyStyle="{ padding: '20px 24px' }"
      okText="保存"
      cancelText="取消"
      @ok="saveFaqItem"
      @cancel="faqEditorVisible = false"
    >
      <a-form layout="vertical" style="margin-top:8px">
        <a-form-item label="问题" :required="true" style="margin-bottom:16px">
          <a-input v-model:value="faqEditForm.question" placeholder="请输入访客常见问题，例如：你们的产品有哪些？" :maxlength="100" showCount size="large" />
        </a-form-item>
        <a-form-item style="margin-bottom:0">
          <template #label>
            <span>答案 <span style="color:#999;font-weight:normal;font-size:12px">（支持富文本格式，可插入图片、链接等）</span></span>
          </template>
          <Tinymce
            v-model:modelValue="faqEditForm.answer"
            :height="300"
            :showImageUpload="true"
            :toolbar="faqEditorToolbar"
            :plugins="faqEditorPlugins"
            :menubar="''"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts" name="ChatWindowSettingsPage">
import { computed, onMounted, reactive, ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { uploadImg } from '/@/api/sys/upload';
import { DeleteOutlined, EditOutlined, PlusOutlined, QuestionCircleOutlined } from '@ant-design/icons-vue';
import { Tinymce } from '/@/components/Tinymce/index';

defineOptions({ name: 'ChatWindowSettingsPage' });

const { createMessage } = useMessage();
const saving = ref(false);
const previewTab = ref('pc');

interface FaqItem {
  question: string;
  answer: string;
}

const config = reactive({
  themeColor: '#667eea',
  headerVisible: true,
  pageTitle: '在线客服',
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
  headerBgImage: '',
  headerIcons: [] as Array<{ icon: string; name: string; link: string; size: number; transparent: boolean }>,
  faqEnabled: false,
  faqList: [] as FaqItem[],
});

// FAQ编辑器状态
const faqEditorVisible = ref(false);
const faqEditIndex = ref(-1); // -1=新增
const faqEditForm = reactive<FaqItem>({ question: '', answer: '' });
const faqEditorPlugins = 'lists image link media fullscreen paste';
const faqEditorToolbar = 'bold italic underline strikethrough | bullist numlist | alignleft aligncenter alignright | fontsize forecolor backcolor | link image media | removeformat';

// 判断富文本内容是否实质为空（去掉HTML标签后检查）
function isHtmlEmpty(html: string): boolean {
  if (!html) return true;
  const text = html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
  return text.length === 0;
}

// 去除HTML标签，用于列表摘要显示
function stripHtml(html: string): string {
  if (!html) return '';
  return html.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, ' ').trim();
}

function openFaqEditor(idx: number) {
  faqEditIndex.value = idx;
  if (idx >= 0 && config.faqList[idx]) {
    faqEditForm.question = config.faqList[idx].question;
    faqEditForm.answer = config.faqList[idx].answer;
  } else {
    faqEditForm.question = '';
    faqEditForm.answer = '';
  }
  faqEditorVisible.value = true;
}

function saveFaqItem() {
  if (!faqEditForm.question.trim()) {
    createMessage.warning('请输入问题');
    return;
  }
  if (isHtmlEmpty(faqEditForm.answer)) {
    createMessage.warning('请输入答案');
    return;
  }
  const item: FaqItem = { question: faqEditForm.question.trim(), answer: faqEditForm.answer };
  if (faqEditIndex.value >= 0) {
    config.faqList[faqEditIndex.value] = item;
  } else {
    if (config.faqList.length >= 10) {
      createMessage.warning('最多添加10条常见问题');
      return;
    }
    config.faqList.push(item);
  }
  faqEditorVisible.value = false;
}

function removeFaq(idx: number) {
  config.faqList.splice(idx, 1);
}

// ==================== 头部图标 ====================
function addHeaderIcon() {
  if (config.headerIcons.length >= 5) {
    createMessage.warning('最多添加5个头部图标');
    return;
  }
  config.headerIcons.push({ icon: '', name: '', link: '', size: 32, transparent: false });
}

function removeHeaderIcon(idx: number) {
  config.headerIcons.splice(idx, 1);
}

async function handleIconUpload(info: any, index: number) {
  const file = info?.file;
  if (!file) return;
  try {
    const res: any = await uploadImg({ file }, () => {});
    const data = res?.result || res;
    const url = data?.url || data?.fileUrl || data?.path || data?.message;
    if (!url) {
      createMessage.error('上传失败：未获取到文件地址');
      return;
    }
    if (config.headerIcons[index]) {
      config.headerIcons[index].icon = url;
    }
    createMessage.success('上传成功');
  } catch (e) {
    createMessage.error('上传失败');
  }
}

// 是否有右侧内容
const hasSidebar = computed(() => {
  return !!config.pcAdImage || (config.faqEnabled && config.faqList.length > 0);
});

function resolveUrl(url: string) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url;
  const base = (window as any)._JEECG_API_BASE_URL || import.meta.env.VITE_GLOB_DOMAIN_URL || '';
  return base + '/' + url.replace(/^\//, '');
}

async function handleUpload(info: any, field: string) {
  const file = info?.file;
  if (!file) return;
  try {
    const res: any = await uploadImg({ file }, () => {});
    const data = res?.result || res;
    const url = data?.url || data?.fileUrl || data?.path || data?.message;
    if (!url) {
      createMessage.error('上传失败：未获取到文件地址');
      return;
    }
    (config as any)[field] = url;
    createMessage.success('上传成功');
  } catch (e) {
    createMessage.error('上传失败');
  }
}

async function fetchConfig() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/chat-window-settings' }, { isTransformResponse: false });
    const data = res?.result || res;
    let parsed: any = null;
    if (data && typeof data === 'string') {
      try { parsed = JSON.parse(data); } catch {}
    } else if (data && typeof data === 'object') {
      parsed = data;
    }
    if (parsed) {
      Object.keys(parsed).forEach((k) => {
        if (k in config) {
          (config as any)[k] = parsed[k];
        }
      });
      // 确保 faqList 和 headerIcons 是数组
      if (!Array.isArray(config.faqList)) {
        config.faqList = [];
      }
      if (!Array.isArray(config.headerIcons)) {
        config.headerIcons = [];
      }
    }
  } catch (e) {
    console.error('获取聊天窗口设置失败', e);
  }
}

async function handleSave() {
  saving.value = true;
  try {
    await defHttp.put({
      url: '/cs/agent/global/chat-window-settings',
      data: { ...config },
    }, { isTransformResponse: false });
    createMessage.success('保存成功');
  } catch (e) {
    createMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

const previewWrapperStyle = computed(() => {
  if (previewTab.value === 'pc') {
    const needSidebar = hasSidebar.value;
    return { width: needSidebar ? '680px' : '480px', height: '600px' };
  }
  return { width: '375px', height: '667px' };
});

const previewChatStyle = computed(() => ({
  flex: '1',
  minWidth: '0',
}));

const previewHeaderStyle = computed(() => {
  const s: any = { background: config.themeColor || '#667eea' };
  if (config.headerBgImage) {
    s.backgroundImage = `url(${resolveUrl(config.headerBgImage)})`;
    s.backgroundSize = 'cover';
    s.backgroundPosition = 'center';
  }
  return s;
});

const msgAreaStyle = computed(() => {
  const s: any = {};
  if (config.backgroundImage) {
    s.backgroundImage = `url(${resolveUrl(config.backgroundImage)})`;
    s.backgroundSize = 'cover';
    s.backgroundPosition = 'center';
  }
  return s;
});

onMounted(() => {
  fetchConfig();
});
</script>

<style scoped>
.chat-window-settings {
  padding: 16px;
}
.settings-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.settings-form {
  flex: 1;
  min-width: 0;
  max-width: 640px;
}
.settings-preview {
  flex: 0 0 auto;
  position: sticky;
  top: 16px;
}
.color-picker-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.color-input {
  width: 36px;
  height: 32px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  padding: 2px;
  cursor: pointer;
  background: transparent;
}
.switch-hint {
  margin-left: 8px;
  color: #999;
  font-size: 12px;
}
.upload-hint {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

/* 图片预览缩略图 + 删除按钮 */
.preview-thumb {
  margin-top: 8px;
  width: 60px;
  height: 60px;
  border: 1px solid #eee;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}
.preview-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-thumb--wide {
  width: 120px;
  height: 60px;
}
.thumb-delete {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border-radius: 50%;
  padding: 3px;
  font-size: 12px;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.preview-thumb:hover .thumb-delete {
  opacity: 1;
}

/* 头部图标配置区 */
.header-icons-section {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}
.header-icons-empty {
  text-align: center;
  padding: 16px 0;
}
.header-icon-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
}
.header-icon-row:hover {
  border-color: #d9d9d9;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.header-icon-row-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #667eea;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
  margin-top: 4px;
}
.header-icon-row-fields {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.header-icon-field {
  display: flex;
  align-items: flex-start;
  gap: 6px;
}
.header-icon-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  line-height: 32px;
}

/* FAQ配置区 */
.faq-section {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
  background: #fafafa;
}
.faq-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.faq-count {
  color: #666;
  font-size: 13px;
}
.faq-limit-hint {
  color: #ff4d4f;
  font-size: 12px;
}
.faq-empty {
  text-align: center;
  color: #bbb;
  padding: 32px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.faq-item {
  display: flex;
  align-items: center;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.faq-item:hover {
  border-color: #d9d9d9;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.faq-item:last-child {
  margin-bottom: 0;
}
.faq-item-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #667eea;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
  margin-right: 12px;
}
.faq-item-body {
  flex: 1;
  min-width: 0;
}
.faq-item-question {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}
.faq-item-answer {
  font-size: 12px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 380px;
}
.faq-item-actions {
  flex-shrink: 0;
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s;
}
.faq-item:hover .faq-item-actions {
  opacity: 1;
}

/* ========== 预览区 ========== */
.preview-container {
  display: flex;
  justify-content: center;
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
  min-height: 650px;
}
.preview-wrapper {
  display: flex;
  flex-direction: column;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  transition: all 0.3s;
}
.preview-body {
  display: flex;
  flex: 1;
  min-height: 0;
}
.preview-chat {
  display: flex;
  flex-direction: column;
  background: #fff;
  overflow: hidden;
}
/* 全宽头部 */
.p-header-full {
  padding: 12px 16px;
  color: #fff;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.p-header-content {
  display: flex;
  align-items: center;
  gap: 8px;
}
.p-header-icons {
  display: flex;
  align-items: center;
  gap: 10px;
}
.p-header-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-decoration: none;
  color: #fff;
  gap: 2px;
}
.p-header-icon-img {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid rgba(255,255,255,0.3);
}
.p-header-icon-img.p-header-icon-transparent {
  border: none;
  border-radius: 0;
  background: transparent;
}
.p-header-icon-placeholder {
  font-size: 18px;
}
.p-header-icon-name {
  font-size: 10px;
  opacity: 0.9;
  max-width: 48px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.p-logo {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  object-fit: cover;
}
.p-title {
  font-size: 15px;
  font-weight: 500;
}
.p-scroll-bar {
  overflow: hidden;
  white-space: nowrap;
  padding: 4px 0;
  font-size: 12px;
  flex-shrink: 0;
}
.p-scroll-text {
  display: inline-block;
  animation: marquee 15s linear infinite;
  padding-left: 100%;
}
@keyframes marquee {
  0% { transform: translateX(0); }
  100% { transform: translateX(-100%); }
}
.p-messages {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fff;
}
.p-msg {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.p-msg-visitor {
  justify-content: flex-end;
}
.p-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
  overflow: hidden;
}
.p-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.p-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  max-width: 70%;
  line-height: 1.5;
  word-break: break-word;
}
.p-input-area {
  border-top: 1px solid #f0f0f0;
  padding: 8px 12px;
  flex-shrink: 0;
  background: #fff;
}
.p-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 16px;
}
.p-tool-icon {
  cursor: default;
  opacity: 0.7;
}
.p-input-box {
  display: flex;
  align-items: center;
  background: #f5f5f5;
  border-radius: 20px;
  padding: 6px 12px;
}
.p-input-placeholder {
  flex: 1;
  color: #bbb;
  font-size: 13px;
}
.p-send-btn {
  color: #fff;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: 12px;
  cursor: default;
}

/* 手机端FAQ预览（输入框上方） */
.p-faq-mobile {
  border-top: 1px solid #f0f0f0;
  padding: 8px 12px;
  background: #fafafa;
  flex-shrink: 0;
}
.p-faq-mobile-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.p-faq-mobile-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.p-faq-mobile-item {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 14px;
  padding: 4px 10px;
  font-size: 11px;
  color: #333;
  cursor: pointer;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* PC右侧区域 */
.preview-sidebar {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-left: 1px solid #f0f0f0;
  overflow: hidden;
}
.preview-ad {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
}
.preview-ad-img {
  width: 100%;
  object-fit: cover;
  display: block;
}
.preview-faq {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px;
  border-top: 1px solid #f0f0f0;
}
.preview-faq-title {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}
.preview-faq-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.preview-faq-item {
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
}
.preview-faq-item:hover {
  background: #e6f7ff;
  border-color: #91d5ff;
}
</style>
