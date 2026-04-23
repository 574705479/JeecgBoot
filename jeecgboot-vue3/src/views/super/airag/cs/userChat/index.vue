<template>
  <div class="user-chat-container">
    <div v-if="fatalError" class="chat-fatal-error">
      <div class="fatal-title">无法进入在线客服</div>
      <div class="fatal-desc">{{ fatalErrorMessage }}</div>
    </div>
    <template v-else>
      <!-- 留言板模式（无在线客服时显示） -->
      <div v-if="showLeaveMessageBoard" class="leave-message-board">
        <div class="board-header" :style="headerStyle">
          <div class="header-info">
            <img class="app-avatar" :src="chatWindowConfig.logo ? resolveFileUrl(chatWindowConfig.logo) : (appInfo.avatar ? resolveFileUrl(appInfo.avatar) : defaultAvatar)" @error="onImageError" />
            <div class="app-info">
              <span class="app-name">{{ chatWindowConfig.pageTitle || appInfo.name || '在线客服' }}</span>
              <span class="board-subtitle">{{ messageBoardConfig.subtitle || '客服不在线，请留言' }}</span>
            </div>
          </div>
          <div class="header-icons" v-if="chatWindowConfig.headerIcons?.length">
            <a v-for="(item, idx) in chatWindowConfig.headerIcons" :key="idx"
               :href="item.link || '#'" target="_blank" rel="noopener" class="header-icon-item">
              <img v-if="item.icon" :src="resolveFileUrl(item.icon)"
                   :class="['header-icon-img', { 'header-icon-transparent': item.transparent }]"
                   :style="{ width: (item.size || 32) + 'px', height: (item.size || 32) + 'px' }" @error="onImageError" />
              <span class="header-icon-name"
                    :style="{ fontSize: Math.max(10, Math.round((item.size || 32) * 0.35)) + 'px', maxWidth: Math.max(40, (item.size || 32) * 1.8) + 'px' }">{{ item.name }}</span>
            </a>
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
      <div class="chat-outer-layout" :style="dynamicCssVars">
      <!-- 全宽头部（在 chat-main-layout 之上） -->
      <div class="chat-header" v-if="chatWindowConfig.headerVisible !== false" :style="headerStyle">
        <LeftOutlined v-if="!chatWindowConfig.hideBackButton" class="mobile-back-btn" :style="{ color: chatWindowConfig.backButtonColor || '#fff' }" @click="goBack" />
        <div class="header-info">
          <img v-if="!chatWindowConfig.hideLogo" class="app-avatar" :src="chatWindowConfig.logo ? resolveFileUrl(chatWindowConfig.logo) : (appInfo.avatar ? resolveFileUrl(appInfo.avatar) : defaultAvatar)" @error="onImageError" />
          <div class="app-info">
            <span v-if="!chatWindowConfig.hidePageTitle" class="app-name">{{ chatWindowConfig.pageTitle || appInfo.name || '在线客服' }}</span>
            <span class="status-text">
              <template v-if="chatWindowConfig.humanAgentEnabled && !hasAgent">
                <a-tag color="cyan" size="small" style="margin-left: 6px;">智能回复</a-tag>
              </template>
              <template v-else>
                <template v-if="!chatWindowConfig.hideOnlineStatus">
                  <span :class="['status-dot', connectionStatus]"></span>
                  {{ connectionStatusText }}
                </template>
                <template v-if="!chatWindowConfig.hideAiHumanLabel">
                  <a-tag v-if="hasAgent && replyMode === 1" color="green" size="small" style="margin-left: 6px;">人工服务</a-tag>
                  <a-tag v-else-if="replyMode === 0" color="blue" size="small" style="margin-left: 6px;">AI客服</a-tag>
                </template>
              </template>
            </span>
          </div>
        </div>
        <div class="header-icons" v-if="chatWindowConfig.headerIcons?.length">
          <a v-for="(item, idx) in chatWindowConfig.headerIcons" :key="idx"
             :href="item.link || '#'" target="_blank" rel="noopener" class="header-icon-item">
            <img v-if="item.icon" :src="resolveFileUrl(item.icon)"
                 :class="['header-icon-img', { 'header-icon-transparent': item.transparent }]"
                 :style="{ width: (item.size || 32) + 'px', height: (item.size || 32) + 'px' }" @error="onImageError" />
            <span class="header-icon-name"
                  :style="{ fontSize: Math.max(10, Math.round((item.size || 32) * 0.35)) + 'px', maxWidth: Math.max(40, (item.size || 32) * 1.8) + 'px' }">{{ item.name }}</span>
          </a>
        </div>
      </div>
      <!-- WebSocket 连接状态提示条 -->
      <transition name="ws-banner">
        <div v-if="wsShowBanner" class="ws-status-banner" :class="'ws-' + wsStatus">
          <template v-if="wsStatus === 'reconnecting'">
            <LoadingOutlined spin /> 正在重连...
          </template>
          <template v-else-if="wsStatus === 'disconnected'">
            <template v-if="wsReconnectCountdown > 0">{{ wsReconnectCountdown }}秒后自动重连</template>
            <template v-else>连接已断开</template>
            <a @click="connectWebSocket()" style="margin-left:8px">立即重连</a>
          </template>
          <template v-else-if="wsStatus === 'connected'">
            <CheckCircleOutlined /> 已重新连接
          </template>
        </div>
      </transition>
      <!-- 主内容区域 -->
      <div class="chat-main-layout">
      <div class="chat-main-column">
      <!-- 滚动文字（跑马灯） -->
      <div v-if="chatWindowConfig.scrollText" class="scroll-text-bar"
           :style="{ background: chatWindowConfig.scrollTextBgColor || '#1890ff', color: chatWindowConfig.scrollTextColor || '#fff' }">
        <div class="scroll-text-content" :style="{ animationDuration: (chatWindowConfig.scrollDuration || 15) + 's' }">
          {{ chatWindowConfig.scrollText }}
        </div>
      </div>
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
          <!-- 历史会话分割线 -->
          <div v-if="msg._historySeparator" class="history-separator">
            <span class="history-separator-line"></span>
            <span class="history-separator-text">以上为历史会话记录</span>
            <span class="history-separator-line"></span>
          </div>
          <!-- 系统消息 -->
          <div v-else-if="msg.senderType === 3" class="system-message">
            {{ msg.content }}
          </div>
          <!-- 智能助手消息 (senderType === 4) -->
          <div v-else-if="msg.senderType === 4" class="agent-message smart-assistant-message">
            <img class="avatar" :src="chatWindowConfig.logo ? resolveFileUrl(chatWindowConfig.logo) : defaultAvatar" @error="onImageError" />
            <div class="message-content">
              <div class="sender-info">
                <span class="sender-name">智能助手</span>
              </div>
              <div class="message-text">
                <div v-if="msg.content" v-html="renderMessage(msg.content)" v-cse-html></div>
                <template v-if="getSmartAssistantFaqData(msg) && (getSmartAssistantFaqData(msg).faqItems?.length || getSmartAssistantFaqData(msg).showBack || getSmartAssistantFaqData(msg).showTop || getSmartAssistantFaqData(msg).showHumanAgent)">
                  <div class="sa-divider"></div>
                  <div v-if="getSmartAssistantFaqData(msg).faqItems?.length" class="sa-faq-list">
                    <a v-for="(item, idx) in getSmartAssistantFaqData(msg).faqItems" :key="idx"
                       class="sa-faq-link" href="javascript:void(0)"
                       @click="onFaqLinkClick(item, getSmartAssistantFaqData(msg))">
                      <QuestionCircleOutlined /> {{ item.question }}
                    </a>
                  </div>
                  <div class="sa-nav-links">
                    <a v-if="getSmartAssistantFaqData(msg).showTop" href="javascript:void(0)"
                       class="sa-nav-link" @click="onFaqNavigate('top', getSmartAssistantFaqData(msg))">返回第一层</a>
                    <a v-if="getSmartAssistantFaqData(msg).showBack" href="javascript:void(0)"
                       class="sa-nav-link" @click="onFaqNavigate('back', getSmartAssistantFaqData(msg))">返回上一层</a>
                    <a v-if="getSmartAssistantFaqData(msg).showHumanAgent && !hasAgent" href="javascript:void(0)"
                       class="sa-nav-link sa-human-agent" @click="requestHumanAgent()">
                      <CustomerServiceOutlined /> 人工客服
                    </a>
                  </div>
                </template>
              </div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
          </div>
          <!-- 用户消息 (senderType === 0 表示用户) -->
          <div v-else-if="isUserMessage(msg)" class="user-message">
            <div class="message-content">
              <div v-if="msg.content" class="message-text" v-html="renderUserMessage(msg.content)"></div>
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
                  <template v-if="item.type === 'image'">
                    <img :src="getAttachmentThumbUrl(item)" @error="onAttachmentImageError($event, item)" @click="openImagePreview(msg, item)" />
                    <div
                      v-if="!isAttachmentImageReady(item)"
                      class="img-skeleton-overlay"
                      :class="{ 'is-failed': isImageFailed(item) }"
                      @click.stop="isImageFailed(item) ? onAttachmentImageRetry(item) : null"
                    >
                      <ReloadOutlined v-if="isImageFailed(item)" />
                      <a-spin v-else size="small" />
                      <span v-if="isImageFailed(item)" class="overlay-text">点击重试</span>
                    </div>
                  </template>
                  <template v-else-if="item.type === 'video'">
                    <video v-if="getAttachmentUrl(item)" :src="getAttachmentUrl(item)" preload="metadata" controls @click="openFilePreview(item)" />
                    <div
                      v-else
                      class="video-skeleton"
                      :class="{ 'is-failed': isVideoFailed(item) }"
                      @click="onVideoSkeletonClick(item)"
                    >
                      <PlayCircleOutlined v-if="!isVideoFailed(item)" />
                      <ReloadOutlined v-else />
                      <span class="skeleton-text">{{ isVideoFailed(item) ? '加载失败，点击重试' : '视频加载中...' }}</span>
                    </div>
                  </template>
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
                <template v-for="(item, index) in getFileAttachments(msg)">
                  <template v-if="isAudioAttachment(item)">
                    <audio
                      v-if="getAttachmentUrl(item)"
                      :key="`audio_${item.url}_${index}`"
                      :src="getAttachmentUrl(item)"
                      controls
                      preload="metadata"
                      style="max-width: 100%; margin-top: 4px;"
                    />
                    <div
                      v-else
                      :key="`audio_skeleton_${item.url}_${index}`"
                      class="audio-skeleton"
                      :class="{ 'is-failed': isAudioFailed(item) }"
                      @click="onAudioSkeletonClick(item)"
                    >
                      <CustomerServiceOutlined />
                      <span>{{ isAudioFailed(item) ? '音频加载失败，点击重试' : '音频加载中…' }}</span>
                    </div>
                  </template>
                  <FileChip
                    v-else
                    :key="`chip_${item.url}_${index}`"
                    :name="item.name || item.url"
                    :size="item.size"
                    :type="item.type"
                    :url="item.url"
                    :uploading="false"
                    @click="openFilePreview(item)"
                  />
                </template>
              </div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
            <img class="avatar" :src="getUserAvatar(msg)" @error="onImageError" />
          </div>
          <!-- 客服/AI消息 (senderType === 1 AI, 2 客服) -->
          <div v-else class="agent-message">
            <img class="avatar" :src="getAgentAvatar(msg)" @error="onImageError" />
            <div class="message-content">
              <div class="sender-info">
                <span class="sender-name">{{ msg.senderName || (isAiMessage(msg) ? 'AI客服' : '客服') }}</span>
                <a-tag v-if="isAiMessage(msg)" color="blue" size="small">AI</a-tag>
              </div>
              <div v-if="msg.content" class="message-text" v-html="msg.isStreaming ? renderStreamingText(msg.content) : renderMessage(msg.content)" v-cse-html></div>
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
                  <template v-if="item.type === 'image'">
                    <img :src="getAttachmentThumbUrl(item)" @error="onAttachmentImageError($event, item)" @click="openImagePreview(msg, item)" />
                    <div
                      v-if="!isAttachmentImageReady(item)"
                      class="img-skeleton-overlay"
                      :class="{ 'is-failed': isImageFailed(item) }"
                      @click.stop="isImageFailed(item) ? onAttachmentImageRetry(item) : null"
                    >
                      <ReloadOutlined v-if="isImageFailed(item)" />
                      <a-spin v-else size="small" />
                      <span v-if="isImageFailed(item)" class="overlay-text">点击重试</span>
                    </div>
                  </template>
                  <template v-else-if="item.type === 'video'">
                    <video v-if="getAttachmentUrl(item)" :src="getAttachmentUrl(item)" preload="metadata" controls @click="openFilePreview(item)" />
                    <div
                      v-else
                      class="video-skeleton"
                      :class="{ 'is-failed': isVideoFailed(item) }"
                      @click="onVideoSkeletonClick(item)"
                    >
                      <PlayCircleOutlined v-if="!isVideoFailed(item)" />
                      <ReloadOutlined v-else />
                      <span class="skeleton-text">{{ isVideoFailed(item) ? '加载失败，点击重试' : '视频加载中...' }}</span>
                    </div>
                  </template>
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
                <template v-for="(item, index) in getFileAttachments(msg)">
                  <template v-if="isAudioAttachment(item)">
                    <audio
                      v-if="getAttachmentUrl(item)"
                      :key="`audio_${item.url}_${index}`"
                      :src="getAttachmentUrl(item)"
                      controls
                      preload="metadata"
                      style="max-width: 100%; margin-top: 4px;"
                    />
                    <div
                      v-else
                      :key="`audio_skeleton_${item.url}_${index}`"
                      class="audio-skeleton"
                      :class="{ 'is-failed': isAudioFailed(item) }"
                      @click="onAudioSkeletonClick(item)"
                    >
                      <CustomerServiceOutlined />
                      <span>{{ isAudioFailed(item) ? '音频加载失败，点击重试' : '音频加载中…' }}</span>
                    </div>
                  </template>
                  <FileChip
                    v-else
                    :key="`chip_${item.url}_${index}`"
                    :name="item.name || item.url"
                    :size="item.size"
                    :type="item.type"
                    :url="item.url"
                    :uploading="false"
                    @click="openFilePreview(item)"
                  />
                </template>
              </div>
              <div class="message-time">{{ formatTime(msg.createTime) }}</div>
            </div>
          </div>
        </div>
        
        <!-- 客服正在输入提示 -->
        <div v-if="agentTyping" class="typing-indicator">
          <img class="avatar" :src="getAgentAvatar()" @error="onImageError" />
          <div class="typing-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </template>
    </div>

    

    <!-- 预设问题（配置FAQ开启时隐藏AI预设问题，优先展示配置FAQ） -->
    <div v-if="presetQuestions.length > 0 && !(chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0) && !conversationClosed" class="preset-questions">
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

    <!-- 输入区域 -->
    <div class="chat-input" v-if="!conversationClosed">
      <!-- 附件预览 -->
      <div v-if="attachmentList.length > 0" class="attachment-preview-bar">
        <div
          v-for="(att, idx) in attachmentList"
          :key="idx"
          class="attachment-thumb"
          :class="{ 'is-chip': att.type !== 'image' && att.type !== 'video' }"
        >
          <img v-if="att.type === 'image'" :src="resolveAttachmentThumb(att)" class="att-img" />
          <div v-else-if="att.type === 'video'" class="att-file">
            <span>🎬</span>
            <span class="att-name">{{ att.name }}</span>
          </div>
          <FileChip
            v-else
            :name="att.name"
            :size="att.size"
            :type="att.type"
            :url="att.url"
            :uploading="att.uploading"
            :progress="att.progress"
            :downloadable="false"
            click-action="none"
          />
          <span class="att-remove" @click="removeAttachment(idx)">×</span>
          <div v-if="att.uploading && att.type !== 'file' && att.type !== 'audio'" class="att-uploading">
            <a-spin size="small" />
            <span v-if="att.progress != null" class="att-progress-text">{{ att.progress }}%</span>
          </div>
        </div>
      </div>
      <!-- 表情面板 -->
      <div style="position:relative">
        <EmojiPicker :visible="showEmojiPanel" @select="appendEmoji" @close="showEmojiPanel = false" />
      </div>
      <!-- 有图片/视频/文件/FAQ功能时，工具栏显示在输入框上方 -->
      <div class="input-toolbar" v-if="chatWindowConfig.sendImage || chatWindowConfig.sendVideo || chatWindowConfig.sendPdf || (chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0)">
        <SmileOutlined v-if="chatWindowConfig.sendEmoji" class="toolbar-icon" @click="showEmojiPanel = !showEmojiPanel" title="表情" />
        <PictureOutlined v-if="chatWindowConfig.sendImage" class="toolbar-icon" @click="triggerFileInput('image')" title="图片" />
        <VideoCameraOutlined v-if="chatWindowConfig.sendVideo" class="toolbar-icon" @click="triggerFileInput('video')" title="视频" />
        <FilePdfOutlined v-if="chatWindowConfig.sendPdf" class="toolbar-icon" @click="triggerFileInput('pdf')" title="PDF" />
        <QuestionCircleOutlined v-if="chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0" class="toolbar-icon" @click="onFaqNavigate('top', { parentPath: [] })" title="常见问题" />
        <input ref="imageInputRef" type="file" accept="image/jpeg,image/png,image/gif,image/webp,image/bmp,image/svg+xml" style="display:none" @change="handleFileSelected($event, 'image')" />
        <input ref="videoInputRef" type="file" accept="video/mp4,video/webm,video/ogg,video/quicktime,video/x-msvideo,video/x-matroska,video/x-flv,video/3gpp,.mp4,.webm,.ogg,.mov,.avi,.mkv,.flv,.3gp" style="display:none" @change="handleFileSelected($event, 'video')" />
        <input ref="pdfInputRef" type="file" accept=".pdf,application/pdf" style="display:none" @change="handleFileSelected($event, 'pdf')" />
      </div>
      <!-- 输入行：[表情(仅emoji模式)] + 文本框 + 发送/终止图标 -->
      <div class="input-row">
        <SmileOutlined v-if="chatWindowConfig.sendEmoji && !chatWindowConfig.sendImage && !chatWindowConfig.sendVideo && !chatWindowConfig.sendPdf && !(chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0)" class="toolbar-icon inline-emoji-icon" @click="showEmojiPanel = !showEmojiPanel" title="表情" />
        <span class="sound-toggle-btn" @click="toggleSound" :title="soundEnabled ? '关闭提示音' : '开启提示音'">
          <SoundOutlined v-if="soundEnabled" />
          <span v-else class="sound-muted-icon"><SoundOutlined /><span class="mute-line"></span></span>
        </span>
        <a-textarea
          v-model:value="inputMessage"
          :placeholder="aiResponding ? 'AI正在回复，可随时终止...' : '请输入您要咨询的问题...'"
          :auto-size="{ minRows: 1, maxRows: 4 }"
          @keydown="handleKeydown"
        />
        <PauseCircleOutlined v-if="aiResponding" class="stop-icon-btn" @click="stopAiReply()" title="终止" />
        <SendOutlined v-else-if="inputMessage.trim() || attachmentList.length" class="send-icon-btn" @click="sendMessage" title="发送" />
      </div>
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
            <template v-if="item.type === 'image'">
              <img
                :src="getAttachmentUrl(item)"
                @error="onAttachmentImageError($event, item)"
                @click="openImagePreview({ extra: { attachments: mediaViewerList } }, item)"
              />
              <div
                v-if="isImageFailed(item)"
                class="img-skeleton-overlay is-failed"
                @click.stop="onAttachmentImageRetry(item)"
              >
                <ReloadOutlined />
                <span class="overlay-text">点击重试</span>
              </div>
            </template>
            <template v-else-if="item.type === 'video'">
              <video v-if="getAttachmentUrl(item)" :src="getAttachmentUrl(item)" controls @click="openFilePreview(item)" />
              <div
                v-else
                class="video-skeleton"
                :class="{ 'is-failed': isVideoFailed(item) }"
                @click="onVideoSkeletonClick(item)"
              >
                <PlayCircleOutlined v-if="!isVideoFailed(item)" />
                <ReloadOutlined v-else />
                <span class="skeleton-text">{{ isVideoFailed(item) ? '加载失败，点击重试' : '视频加载中...' }}</span>
              </div>
            </template>
          </div>
        </div>
      </a-modal>
      </div><!-- chat-main-column end -->
      <!-- PC右侧区域（广告+FAQ） -->
      <div v-if="chatWindowConfig.pcAdImage || (chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0)" class="chat-sidebar" :style="{ width: (chatWindowConfig.rightSidebarWidth || 200) + 'px' }">
        <div v-if="chatWindowConfig.pcAdImage" class="sidebar-ad">
          <a :href="chatWindowConfig.pcAdLink || '#'" target="_blank" rel="noopener">
            <img :src="resolveFileUrl(chatWindowConfig.pcAdImage)" class="ad-sidebar-img" alt="广告" @error="onImageError" />
          </a>
        </div>
        <div v-if="chatWindowConfig.faqEnabled && chatWindowConfig.faqList?.length > 0" :class="['sidebar-faq', { 'sidebar-faq-disabled': conversationClosed }]">
          <div class="sidebar-faq-title"><QuestionCircleOutlined /> 常见问题</div>
          <div class="sidebar-faq-list" :class="{ 'sidebar-faq-list-scrollable': faqPcShowAll && chatWindowConfig.faqList.length > FAQ_PC_DEFAULT_COUNT }">
            <div v-for="(faq, idx) in (faqPcShowAll ? chatWindowConfig.faqList : chatWindowConfig.faqList.slice(0, FAQ_PC_DEFAULT_COUNT))" :key="idx" class="sidebar-faq-item" @click="onFaqLinkClick({ index: idx, question: faq.question }, { parentPath: [] })">
              {{ faq.question }}
            </div>
          </div>
          <div v-if="!faqPcShowAll && chatWindowConfig.faqList.length > FAQ_PC_DEFAULT_COUNT" class="sidebar-faq-more" @click="faqPcShowAll = true">
            查看全部 ({{ chatWindowConfig.faqList.length }}条)
          </div>
          <div v-if="faqPcShowAll && chatWindowConfig.faqList.length > FAQ_PC_DEFAULT_COUNT" class="sidebar-faq-more" @click="faqPcShowAll = false">
            收起
          </div>
        </div>
      </div>
      </div><!-- chat-main-layout end -->
      </div><!-- chat-outer-layout end -->
      </template><!-- 正常聊天模式 end -->
    </template>

    <!-- 人工客服自定义字段填写弹窗 -->
    <a-modal
      v-model:open="showHumanAgentModal"
      title="转接人工客服"
      :width="400"
      :maskClosable="false"
      okText="提交"
      cancelText="取消"
      :confirmLoading="humanAgentSubmitting"
      @ok="submitHumanAgent"
    >
      <a-form layout="vertical" style="margin-top: 16px; padding: 0 8px">
        <a-form-item
          v-for="(field, fIdx) in chatWindowConfig.humanAgentFields"
          :key="fIdx"
          :label="field.label"
          :required="field.required"
        >
          <a-input
            v-model:value="humanAgentForm[field.label]"
            :placeholder="'请输入' + field.label"
            :type="field.type === 'email' ? 'email' : field.type === 'phone' ? 'tel' : 'text'"
          />
        </a-form-item>
        <div v-if="!chatWindowConfig.humanAgentFields?.length" style="color:#999; text-align:center; padding: 24px 0;">
          点击提交即可转接人工客服
        </div>
      </a-form>
    </a-modal>

    <!-- 满意度评价弹窗 -->
    <div v-if="showSatisfactionModal" class="satisfaction-overlay" @click.self="showSatisfactionModal = false">
      <div class="satisfaction-modal">
        <div class="satisfaction-header">
          <span>服务评价</span>
          <span class="satisfaction-close" @click="showSatisfactionModal = false">&times;</span>
        </div>
        <div class="satisfaction-body">
          <div class="satisfaction-tip">请对本次服务进行评价</div>
          <div class="satisfaction-stars">
            <span
              v-for="star in 5"
              :key="star"
              class="star-item"
              :class="{ active: star <= satisfactionRating }"
              @click="satisfactionRating = star"
            >★</span>
          </div>
          <div class="satisfaction-labels">
            <span v-if="satisfactionRating === 1">非常不满意</span>
            <span v-else-if="satisfactionRating === 2">不满意</span>
            <span v-else-if="satisfactionRating === 3">一般</span>
            <span v-else-if="satisfactionRating === 4">满意</span>
            <span v-else-if="satisfactionRating === 5">非常满意</span>
            <span v-else>请点击星星评分</span>
          </div>
          <textarea
            v-model="satisfactionComment"
            class="satisfaction-textarea"
            placeholder="请输入您的评价（选填）"
            rows="3"
          ></textarea>
        </div>
        <div class="satisfaction-footer">
          <button class="satisfaction-btn-submit" @click="submitSatisfaction" :disabled="satisfactionSubmitting || satisfactionRating === 0">
            {{ satisfactionSubmitting ? '提交中...' : '提交评价' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="UserChatPage">
import { ref, reactive, onMounted, onUnmounted, nextTick, computed, watch } from 'vue';
import MarkdownIt from 'markdown-it';
import DOMPurify from 'dompurify';
import { message } from 'ant-design-vue';
import {
  MessageOutlined, SendOutlined, BulbOutlined, CheckCircleOutlined,
  SmileOutlined, PictureOutlined, VideoCameraOutlined, FilePdfOutlined, QuestionCircleOutlined,
  PauseCircleOutlined, LeftOutlined, CustomerServiceOutlined, LoadingOutlined,
  SoundOutlined, PlayCircleOutlined, ReloadOutlined,
} from '@ant-design/icons-vue';
import { defHttp } from '/@/utils/http/axios';
import axios from 'axios';
import { useGlobSetting } from '/@/hooks/setting';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { createImgPreview } from '/@/components/Preview';
import EmojiPicker from '../components/EmojiPicker.vue';
import { computeFileMd5 } from '../utils/fileHash';
import { encryptTransport, decryptTransport, decryptMessage, decryptStorage } from '../utils/csEncrypt';
import { playCsNotificationSound } from '../utils/csNotificationSound';
import { withImageCache, withImageCacheAsync, preloadImages, onImageError, getCachedChatWindowConfig, setCachedChatWindowConfig } from '../utils/csImageCache';
import {
  withMediaCache,
  releaseAllMedia,
  withImageThumbCache,
  isImageReady,
  retryMedia,
  getMediaFailureState,
  retryImage,
  getImageFailureState,
} from '/@/utils/file/imageCache';
import { compressImage } from '/@/utils/file/compressImage';
import FileChip from '../components/FileChip.vue';
import { isCseUrl } from '/@/utils/cse/cseUrl';
import { resolveBrandPublicUrl } from '/@/utils/brand';
// Phase 3.2e：把访客 sessionToken 同步到 cseAuthContext，
// 让公共 CSE 解密管线（cseDecrypt.ts / imageCache）也能在访客模式下正确派生 IKM 与注入 X-Visitor-Session
import { setVisitorCredential, clearVisitorCredential, setDeviceCredential, clearDeviceCredential } from '/@/utils/cse/cseAuthContext';
import { clearDekCache } from '/@/utils/cse/cseDecrypt';
import { vCseHtml } from '../utils/cseHtmlImg';
import { getBrandSetting, DEFAULT_BRAND } from '/@/settings/brandSetting';

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
const preferredAgentId = ref(''); // 指定客服ID（可选，URL ?agentId= 读取）
const fatalError = ref(false);
const fatalErrorMessage = ref('token无效或已过期，请回到第三方应用重新打开');
function goBack() {
  window.history.back();
}

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
function decryptApiResponse(rawData: any): any {
  if (typeof rawData !== 'string') return rawData;
  const decrypted = decryptTransport(rawData);
  if (typeof decrypted === 'string') {
    try { return JSON.parse(decrypted); } catch { return decrypted; }
  }
  return decrypted;
}

// 应用信息
const appInfo = ref({
  id: '',
  name: '在线客服',
  avatar: '',
  prologue: '', // 开场白
  presetQuestion: '', // 预设问题（逗号或换行分隔）
});
// 从已有品牌缓存同步初始化 brandLogoUrl，避免首帧显示默认 logo 后跳变
const _cachedBrand = getBrandSetting();
const brandLogoUrl = ref(
  _cachedBrand.logoUrl && _cachedBrand.logoUrl !== DEFAULT_BRAND.logoUrl ? _cachedBrand.logoUrl : '',
);
const defaultAvatar = computed(() => {
  if (brandLogoUrl.value) return resolveFileUrl(brandLogoUrl.value);
  return '/logo.svg';
});
const defaultUserAvatar = '/default-user-avatar.png';

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
  headerBgImage: '',
  headerIcons: [] as Array<{ icon: string; name: string; link: string; size: number; transparent: boolean }>,
  rightSidebarWidth: 200,
  pcAdLink: '',
  pcAdImage: '',
  faqEnabled: false,
  faqList: [] as Array<any>,
  hidePageTitle: false,
  hideOnlineStatus: false,
  hideAiHumanLabel: false,
  hideLogo: false,
  hideBackButton: false,
  backButtonColor: '#ffffff',
  headerBgImageMode: 'cover' as string,
  headerBgPosition: 'center' as string,
  mobileHeaderBgImage: '',
  mobileHeaderBgImageMode: 'cover' as string,
  mobileHeaderBgPosition: 'center' as string,
  humanAgentEnabled: false,
  humanAgentFields: [] as Array<{ label: string; type: string; required: boolean }>,
  messageBoardEnabled: true,
  faqLinkColor: '#e8453c',
  faqNavColor: '#1890ff',
  faqHeaderText: '',
});

// 从 localStorage 同步恢复上次配置，消除首帧默认值 → 真实值跳变
const _cachedCwc = getCachedChatWindowConfig();
if (_cachedCwc) {
  Object.keys(_cachedCwc).forEach((k) => {
    if (k in chatWindowConfig) {
      (chatWindowConfig as any)[k] = _cachedCwc[k];
    }
  });
}

// FAQ展开状态
const faqPcShowAll = ref(false);
const FAQ_PC_DEFAULT_COUNT = 8;

// FAQ层级导航状态
// 人工客服弹窗
const showHumanAgentModal = ref(false);
const humanAgentForm = reactive<Record<string, string>>({});
const humanAgentSubmitting = ref(false);

// 移动端检测
const isMobile = ref(window.innerWidth <= 800);
function onResizeCheck() { isMobile.value = window.innerWidth <= 800; }

function applyHeaderBgStyle(s: any, bgImage: string, bgMode: string, bgPosition: string) {
  if (!bgImage) return;
  s.backgroundImage = `url(${resolveFileUrl(bgImage)})`;
  const pos = bgPosition || 'center';
  const mode = bgMode || 'cover';
  switch (mode) {
    case 'contain':
      s.backgroundSize = 'contain';
      s.backgroundRepeat = 'no-repeat';
      s.backgroundPosition = pos;
      break;
    case 'stretch':
      s.backgroundSize = '100% 100%';
      s.backgroundPosition = pos;
      break;
    case 'repeat':
      s.backgroundSize = 'auto';
      s.backgroundRepeat = 'repeat';
      s.backgroundPosition = pos;
      break;
    case 'center':
      s.backgroundSize = 'auto';
      s.backgroundPosition = pos;
      s.backgroundRepeat = 'no-repeat';
      break;
    default:
      s.backgroundSize = 'cover';
      s.backgroundPosition = pos;
  }
}

// 头部样式（支持背景图 + 手机端独立背景图）
const headerStyle = computed(() => {
  const s: any = { background: chatWindowConfig.themeColor || '#667eea' };
  if (isMobile.value && chatWindowConfig.mobileHeaderBgImage) {
    applyHeaderBgStyle(s, chatWindowConfig.mobileHeaderBgImage, chatWindowConfig.mobileHeaderBgImageMode, chatWindowConfig.mobileHeaderBgPosition);
  } else {
    applyHeaderBgStyle(s, chatWindowConfig.headerBgImage, chatWindowConfig.headerBgImageMode, chatWindowConfig.headerBgPosition);
  }
  return s;
});

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
  '--faq-link-color': chatWindowConfig.faqLinkColor || '#e8453c',
  '--faq-nav-color': chatWindowConfig.faqNavColor || '#1890ff',
}));

function resolveFileUrl(url: string) {
  if (!url) return '';
  // cse:// 走匿名代理 /cs/brand/file/{fid}：未登录访客（嵌入端 / H5 / iframe）也能加载，
  // 而 withImageCache 走 /sys/secure/file/{fid}/key 解密链路需要 token，访客侧会破图
  if (isCseUrl(url)) return resolveBrandPublicUrl(url);
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return withImageCache(url);
  return withImageCache(getFileAccessHttpUrl(url));
}

async function loadChatWindowConfig() {
  try {
    const res = await defHttp.get(
      { url: '/cs/agent/global/chat-window-settings' },
      { ...silentRequestOptions, isTransformResponse: false },
    );
    const rawData = res?.result || res;
    const data = typeof rawData === 'string' ? decryptTransport(rawData) : rawData;
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
    // 确保数组字段
    if (!Array.isArray(chatWindowConfig.headerIcons)) {
      chatWindowConfig.headerIcons = [];
    }
    if (!Array.isArray(chatWindowConfig.faqList)) {
      chatWindowConfig.faqList = [];
    }
    // 旧数据兼容
    chatWindowConfig.faqList.forEach((faq: any) => {
      if (!Array.isArray(faq.keywords)) {
        faq.keywords = [];
      }
      if (!Array.isArray(faq.children)) {
        faq.children = [];
      }
    });
    if (!Array.isArray(chatWindowConfig.humanAgentFields)) {
      chatWindowConfig.humanAgentFields = [];
    }
    if (!chatWindowConfig.headerBgImageMode) {
      chatWindowConfig.headerBgImageMode = 'cover';
    }
    // visitorMessageConnect 旧值兼容
    if (parsed.visitorMessageConnect === true && !parsed.humanAgentEnabled) {
      chatWindowConfig.humanAgentEnabled = true;
    }
    // 设置页面标题
    if (chatWindowConfig.pageTitle) {
      document.title = chatWindowConfig.pageTitle;
    }
    // 预热配置中的图片到缓存
    preloadImages([
      chatWindowConfig.logo ? resolveFileUrl(chatWindowConfig.logo) : undefined,
      chatWindowConfig.visitorAvatar ? resolveFileUrl(chatWindowConfig.visitorAvatar) : undefined,
      chatWindowConfig.pcAdImage ? resolveFileUrl(chatWindowConfig.pcAdImage) : undefined,
      chatWindowConfig.backgroundImage ? resolveFileUrl(chatWindowConfig.backgroundImage) : undefined,
      chatWindowConfig.headerBgImage ? resolveFileUrl(chatWindowConfig.headerBgImage) : undefined,
      ...(chatWindowConfig.headerIcons || []).map((i: any) => i.icon ? resolveFileUrl(i.icon) : undefined),
    ]);
    // 缓存后处理过的配置，下次打开页面可同步恢复
    setCachedChatWindowConfig({ ...chatWindowConfig });
  } catch (e) {
    console.warn('加载聊天窗口配置失败', e);
  }
}

async function loadBrandLogo() {
  try {
    const res = await defHttp.get(
      { url: '/cs/brand/get' },
      { ...silentRequestOptions, isTransformResponse: false },
    );
    const data = decryptApiResponse(res?.result || res);
    if (data?.logoUrl) {
      brandLogoUrl.value = data.logoUrl;
      preloadImages([resolveFileUrl(data.logoUrl)]);
    }
  } catch {}
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
    const rawData = res?.result || res;
    const data = typeof rawData === 'string' ? decryptTransport(rawData) : rawData;
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
  type: 'image' | 'video' | 'file' | 'audio';
  uploading?: boolean;
  /** 上传进度 0~100 */
  progress?: number;
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
  const originalFile = input.files?.[0];
  if (!originalFile) return;
  input.value = '';

  const validationError = validateFile(originalFile, fileType);
  if (validationError) {
    message.warning(validationError);
    return;
  }

  // R6: 客户端图片压缩（仅图片；GIF/HEIC/HEIF 自动跳过；失败 fallback 原文件）
  let file: File = originalFile;
  if (fileType === 'image') {
    try {
      file = await compressImage(originalFile);
    } catch {
      file = originalFile;
    }
  }

  const attType: 'image' | 'video' | 'file' = fileType === 'pdf' ? 'file' : fileType;
  const previewUrl = fileType === 'image' ? URL.createObjectURL(file) : undefined;
  // Vue3 响应式陷阱：直接保存 push 进去的原始对象引用 → 写它属性不经 Proxy → 不触发响应式。
  // 用 Symbol __uid 标记占位条，所有写操作都走 findCurrent() 拿 Proxy 元素。
  // Symbol 属性不会被 JSON.stringify 序列化，不会泄漏到 sendMessage 的 extra。
  const uid = Symbol('attUid');
  const att: any = {
    __uid: uid,
    name: originalFile.name,
    url: '',
    previewUrl,
    size: file.size,
    type: attType,
    uploading: true,
    progress: 0,
  };
  attachmentList.value.push(att);
  const findCurrent = () => attachmentList.value.find((a: any) => a.__uid === uid);
  const removeAtt = () => {
    const i = attachmentList.value.findIndex((a: any) => a.__uid === uid);
    if (i > -1) {
      const cur: any = attachmentList.value[i];
      const pv = cur?.previewUrl;
      attachmentList.value.splice(i, 1);
      if (pv) {
        try { URL.revokeObjectURL(pv); } catch {}
      }
    }
  };

  const hideLoading = file.size > 5 * 1024 * 1024
    ? message.loading('正在校验文件...', 0)
    : null;

  try {
    const md5 = await computeFileMd5(file);
    hideLoading?.();

    const { apiUrl, urlPrefix } = globSetting;
    const authHeaders = buildAuthHeaders({});

    // 秒传检测
    const checkHashUrl = `${apiUrl}${urlPrefix || ''}/cs/message/visitor/checkHash`;
    const checkRes = await axios.post(checkHashUrl, null, {
      params: { md5, fileSize: file.size },
      headers: authHeaders,
    });

    if (checkRes.data?.result?.exists) {
      message.success('文件秒传成功');
      const cur: any = findCurrent();
      if (cur) {
        cur.url = checkRes.data.result.url;
        cur.uploading = false;
        cur.progress = 100;
      }
      return;
    }

    // 正常上传，FormData 追加 md5；
    // R6: 显式声明 X-No-Strip-Metadata=1（客户端 Canvas 已剥离 EXIF），让后端跳过二次重写
    // R7: onUploadProgress 实时回写进度，附件预览条显示百分比
    const formData = new FormData();
    formData.append('file', file);
    formData.append('md5', md5);
    const uploadApiUrl = `${apiUrl}${urlPrefix || ''}/cs/message/visitor/upload`;
    const headers: Record<string, string> = { ...authHeaders };
    if (file !== originalFile) {
      headers['X-No-Strip-Metadata'] = '1';
    }
    const { data: res } = await axios.post(uploadApiUrl, formData, {
      headers,
      onUploadProgress: (e: any) => {
        try {
          const total = e.total || (e.lengthComputable ? e.loaded : 0);
          if (total > 0) {
            const percent = Math.min(100, Math.round((e.loaded / total) * 100));
            const cur: any = findCurrent();
            if (cur) cur.progress = percent;
          }
        } catch {}
      },
    });
    if (!res?.success) {
      message.error(res?.message || '上传失败');
      removeAtt();
      return;
    }
    const uploadedUrl = res?.message || res?.result?.url || res?.result?.message || '';
    if (!uploadedUrl) {
      message.error('上传失败：未获取到文件地址');
      removeAtt();
      return;
    }
    const cur: any = findCurrent();
    if (cur) {
      cur.url = uploadedUrl;
      cur.uploading = false;
      cur.progress = 100;
    }
  } catch (err: any) {
    hideLoading?.();
    console.error('文件上传失败', err);
    const serverMsg = err?.response?.data?.message;
    if (serverMsg) {
      message.error(serverMsg);
    } else if (err?.message?.includes('Network Error') || err?.message?.includes('ERR_CONNECTION')) {
      message.error('网络连接异常，请检查网络后重试');
    } else {
      message.error('文件上传失败，请稍后重试');
    }
    removeAtt();
  }
}

function removeAttachment(idx: number) {
  const att = attachmentList.value[idx];
  if (att.previewUrl) URL.revokeObjectURL(att.previewUrl);
  attachmentList.value.splice(idx, 1);
}

function resolveAvatarUrl(avatar?: string) {
  if (!avatar) return '';
  return withImageCache(getFileAccessHttpUrl(avatar));
}

function getAgentAvatar(msg?: any) {
  if (msg && isAiMessage(msg)) {
    if (chatWindowConfig.logo) return resolveFileUrl(chatWindowConfig.logo);
    return defaultAvatar.value;
  }
  const avatar = msg?.senderAvatar || currentAgentAvatar.value || appInfo.value.avatar;
  if (avatar) return resolveAvatarUrl(avatar);
  if (chatWindowConfig.logo) return resolveFileUrl(chatWindowConfig.logo);
  return defaultAvatar.value;
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

// 跨会话历史记录
const historyConvIds = ref<string[]>([]); // 历史会话ID列表（已结束，倒序）
const historyConvIndex = ref(0); // 当前加载到第几个历史会话
const hasMoreHistoryConv = ref(false); // 是否还有更多历史会话
const historyConvLoaded = ref(false); // 是否已加载历史会话ID列表
const displayMessages = computed(() => {
  const list: any[] = [];
  let lastDateKey = '';
  for (const msg of messages.value) {
    if (msg.status === 3) continue;
    if (msg._historySeparator) {
      lastDateKey = '';
      list.push(msg);
      continue;
    }
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

// 提示音
const SOUND_STORAGE_KEY = 'cs_user_chat_sound_enabled';
const soundEnabled = ref(localStorage.getItem(SOUND_STORAGE_KEY) !== 'false');
let audioCtx: AudioContext | null = null;
let lastSoundTime = 0;
const SOUND_THROTTLE_MS = 1500;

function toggleSound() {
  soundEnabled.value = !soundEnabled.value;
  localStorage.setItem(SOUND_STORAGE_KEY, String(soundEnabled.value));
  ensureAudioCtx();
}

function ensureAudioCtx() {
  if (!audioCtx) audioCtx = new AudioContext();
  if (audioCtx.state === 'suspended') audioCtx.resume();
}

function playNotificationSound() {
  if (!soundEnabled.value) return;
  const now = Date.now();
  if (now - lastSoundTime < SOUND_THROTTLE_MS) return;
  lastSoundTime = now;
  try {
    ensureAudioCtx();
    playCsNotificationSound(audioCtx!, 1);
  } catch { /* 忽略音频播放异常 */ }
}

// WebSocket
let ws: WebSocket | null = null;
const wsConnected = ref(false);
const agentTyping = ref(false);
let typingTimer: number | null = null;
let wsReconnectTimer: number | null = null;
let wsManuallyClosed = false;
let wsReconnectAttempts = 0;
let wsFallbackPollTimer: number | null = null;
let lastWsMessageAt = 0;
let lastMessageLoadAt = 0;
const wsFallbackPollIntervalMs = 20000;
const wsStatus = ref<'connected' | 'reconnecting' | 'disconnected'>('disconnected');
const wsShowBanner = ref(false);
const wsReconnectCountdown = ref(0);
let hasConnectedOnce = false;
let wsConnectedBannerTimer: number | null = null;
let wsCountdownTimer: number | null = null;

// AI回复中状态（用于限制用户快速发送）
const aiResponding = ref(false);
let aiResponseTimeoutTimer: number | null = null;
let aiTimedOutMessageId: string | null = null;
const AI_TOKEN_TIMEOUT_MS = 30000;

function resetAiTokenTimeout() {
  if (aiResponseTimeoutTimer) {
    clearTimeout(aiResponseTimeoutTimer);
  }
  aiResponseTimeoutTimer = window.setTimeout(() => {
    if (aiResponding.value) {
      stopAiReply('AI回复超时，请稍后重试');
    }
  }, AI_TOKEN_TIMEOUT_MS);
}

function stopAiResponding(reason?: string) {
  if (aiResponseTimeoutTimer) {
    clearTimeout(aiResponseTimeoutTimer);
    aiResponseTimeoutTimer = null;
  }
  if (aiResponding.value) {
    aiResponding.value = false;
  }
  if (reason) {
    // 记录超时的 messageId，用于过滤后续到达的 token
    for (const [msgId] of streamingMessages.value) {
      aiTimedOutMessageId = msgId;
    }
    messages.value.push({
      id: Date.now().toString(),
      content: reason,
      senderType: 3,
      createTime: new Date().toISOString(),
    });
    scrollToBottom();
  }
}

/** 用户主动终止AI回复 / 超时终止 */
function stopAiReply(reason?: string) {
  // 1. 通知后端停止推送
  if (ws && ws.readyState === WebSocket.OPEN && conversationId.value) {
    ws.send(JSON.stringify({
      type: 'stop_ai',
      conversationId: conversationId.value,
    }));
  }

  // 2. 将所有正在流式输出的消息标记为完成
  for (const [msgId] of streamingMessages.value) {
    const msg = messages.value.find(m => m.id === msgId);
    if (msg) {
      msg.isStreaming = false;
    }
  }
  streamingMessages.value.clear();

  // 3. 重置状态
  stopAiResponding(reason);
  agentTyping.value = false;
}

// 流式AI消息临时存储 (messageId -> 累积内容)
const streamingMessages = ref<Map<string, string>>(new Map());

// RAF 批处理缓冲区
const pendingTokens = new Map<string, { tokens: string[]; conversationId: string }>();
let tokenRafId: number | null = null;
let scrollRafId: number | null = null;

// 用户信息
const userId = ref('');
const userName = ref('访客');
const visitorSource = ref('');
const conversationId = ref('');
const conversationClosed = ref(false);  // 会话是否已结束
const replyMode = ref(0);  // 回复模式: 0=AI自动, 1=手动
const hasAgent = ref(false);  // 是否有客服接入
const currentAgentAvatar = ref(''); // 当前接待客服头像（用于无消息场景）

// ==================== 满意度评价 ====================
const showSatisfactionModal = ref(false);
const satisfactionRating = ref(0);
const satisfactionComment = ref('');
const satisfactionSubmitting = ref(false);
const satisfactionConversationId = ref('');

async function submitSatisfaction() {
  if (satisfactionRating.value === 0) return;
  satisfactionSubmitting.value = true;
  try {
    const cid = satisfactionConversationId.value || conversationId.value;
    await defHttp.post(
      {
        url: `/cs/conversation/${cid}/rate`,
        headers: buildAuthHeaders({}),
        data: {
          satisfaction: satisfactionRating.value,
          comment: encryptTransport(satisfactionComment.value),
        },
      },
      { ...silentRequestOptions, isTransformResponse: false },
    );
    showSatisfactionModal.value = false;
    satisfactionRating.value = 0;
    satisfactionComment.value = '';
    messages.value.push({
      id: Date.now().toString(),
      content: '感谢您的评价！',
      senderType: 3,
      createTime: new Date().toISOString(),
    });
    scrollToBottom();
  } catch (e: any) {
    console.error('[UserChat] 提交评价失败', e);
    message.error(e?.message || '提交评价失败，请重试');
  } finally {
    satisfactionSubmitting.value = false;
  }
}

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
  if (conversationId.value && !conversationId.value.startsWith('temp_') && Date.now() - lastWsMessageAt > 30000) {
    loadMessages();
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
  window.addEventListener('resize', onResizeCheck);
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

  // 读取指定客服ID参数
  const agentIdFromUrl = getQueryParam('agentId');
  if (agentIdFromUrl) {
    preferredAgentId.value = agentIdFromUrl;
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
    // Phase 3.2f：免 Token 模式下让 cse 解密链路也能鉴权。
    // cseDecrypt 走的是独立 cseAuthContext 通道，不会自动带 buildAuthHeaders 的 X-Device-Id+X-App-Secret，
    // 这里显式注入，使 /sys/secure/file/{fid} 请求带访客头并被后端 SecureFileController 第 4 优先级识别。
    if (userId.value && appKey.value) {
      setDeviceCredential(userId.value, appKey.value);
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

  // 加载聊天窗口配置、敏感词配置和品牌Logo
  await Promise.all([loadChatWindowConfig(), loadSensitiveWords(), loadBrandLogo()]);

  // 加载访客AI应用信息（头像/开场白/预设问题）
  await loadVisitorAppInfo();

  // 前置检测：无客服在线时根据留言板开关决定行为
  const hasStoredConv = !!localStorage.getItem(`cs_conversation_${userId.value}`);
  if (!hasStoredConv) {
    const agentOnline = await checkAgentOnline();
    if (!agentOnline) {
      if (chatWindowConfig.messageBoardEnabled !== false) {
        await loadMessageBoardConfig();
        showLeaveMessageBoard.value = true;
        return;
      }
    }
  }

  // 获取或创建会话
  // humanAgentEnabled 模式下始终立即创建会话（后端不分配客服）
  // visitorMessageConnect 已废弃，但保留兼容（映射到 humanAgentEnabled）
  if (chatWindowConfig.visitorMessageConnect && !chatWindowConfig.humanAgentEnabled) {
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

  // 始终加载当前会话消息（当前会话不属于"历史记录"）
  await loadMessages();

  // 仅在 visitorHistory 开启时加载历史（已关闭）会话列表
  if (chatWindowConfig.visitorHistory !== false) {
    await loadHistoryConvIds();
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
  releaseAllMedia(); // 释放所有视频 blob URL
  disconnectWebSocket();
  stopFallbackPoll();
  stopTokenValidateTimer();
  stopWsCountdown();
  if (wsConnectedBannerTimer) { clearTimeout(wsConnectedBannerTimer); wsConnectedBannerTimer = null; }
  if (typingTimer) { clearTimeout(typingTimer); typingTimer = null; }
  window.removeEventListener('resize', onResizeCheck);
  if (tokenRafId) { cancelAnimationFrame(tokenRafId); tokenRafId = null; }
  if (scrollRafId) { cancelAnimationFrame(scrollRafId); scrollRafId = null; }
  window.removeEventListener('online', handleNetworkOnline);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  if (audioCtx) { audioCtx.close(); audioCtx = null; }
  // Phase 3.2e：离开访客页时清空 cseAuthContext，避免下一次进入「登录态页面」时还残留访客凭证
  clearVisitorCredential();
  clearDeviceCredential();
  try { clearDekCache(); } catch {}
});

// 初始化用户ID
function initUserId() {
  const queryUserId = getQueryParam('externalUserId') || getQueryParam('uid') || getQueryParam('userId');
  const queryUserName = getQueryParam('userName');
  const querySource = getQueryParam('source') || getQueryParam('appKey');

  // 记录访客来源：URL参数 source + document.referrer 域名
  let referrerHost = '';
  if (document.referrer) {
    try { referrerHost = new URL(document.referrer).hostname; } catch {}
  }
  if (querySource && referrerHost) {
    visitorSource.value = `${querySource}(${referrerHost})`;
  } else if (querySource) {
    visitorSource.value = querySource;
  } else if (referrerHost) {
    visitorSource.value = referrerHost;
  }

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
  const headers: Record<string, string> = {};
  if (sessionToken.value) {
    headers['X-Visitor-Session'] = sessionToken.value;
  } else if (visitorToken.value) {
    headers['X-Visitor-Token'] = visitorToken.value;
  } else if (!tokenRequired.value && userId.value) {
    headers['X-Device-Id'] = userId.value;
    if (appKey.value) {
      headers['X-App-Secret'] = appKey.value;
    }
  } else {
    return;
  }
  try {
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

// Phase 3.2e：兜底监听 —— sessionToken/visitorToken 任一变化都同步到 cseAuthContext，
// 避免 loadSessionToken / 直接赋值等绕过 saveSessionToken 的路径漏同步。
watch([sessionToken, rawVisitorToken], ([sess, raw]) => {
  if (sess) {
    setVisitorCredential(sess, raw || undefined);
  } else {
    clearVisitorCredential();
  }
}, { immediate: true });

function blockForInvalidToken(messageText?: string) {
  fatalError.value = true;
  fatalErrorMessage.value = messageText || 'token无效或已过期，请回到第三方应用重新打开';
  disconnectWebSocket();
  stopFallbackPoll();
  // Phase 3.2e：失效时一并清空访客凭证与 DEK 缓存，避免后续 CSE 解密读取过期 token 报 AuthError
  clearVisitorCredential();
  clearDeviceCredential();
  try { clearDekCache(); } catch {}
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
  // Phase 3.2e：每次 session token 刷新都同步到 cseAuthContext，并清掉旧 DEK 缓存
  if (token) {
    setVisitorCredential(token, rawVisitorToken.value || undefined);
    try { clearDekCache(); } catch {}
  } else {
    clearVisitorCredential();
    try { clearDekCache(); } catch {}
  }
}

async function ensureSessionToken() {
  // 如果URL带了新的visitor token，强制重新exchange（旧session token可能已失效）
  if (sessionToken.value && !rawVisitorToken.value) {
    if (sessionTokenExpiresAt.value && sessionTokenExpiresAt.value < Date.now()) {
      sessionToken.value = '';
      sessionTokenExpiresAt.value = 0;
    } else {
      return;
    }
  }
  // 有新visitor token时，清除旧session token，重新exchange
  if (rawVisitorToken.value && sessionToken.value) {
    sessionToken.value = '';
    sessionTokenExpiresAt.value = 0;
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
  if (conversationClosed.value) return;
  inputMessage.value = question;
  // 直接发送
  sendMessage();
}


function requestHumanAgent() {
  if (conversationClosed.value) return;
  if (chatWindowConfig.humanAgentFields?.length) {
    showHumanAgentModal.value = true;
  } else {
    submitHumanAgent();
  }
}

// 提交人工客服转接请求
async function submitHumanAgent() {
  if (conversationClosed.value) { showHumanAgentModal.value = false; return; }
  const phoneReg = /^1[3-9]\d{9}$/;
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  for (const field of chatWindowConfig.humanAgentFields) {
    const val = humanAgentForm[field.label]?.trim() || '';
    if (field.required && !val) {
      message.warning(`请填写${field.label}`);
      return;
    }
    if (val && field.type === 'phone' && !phoneReg.test(val)) {
      message.warning(`${field.label}格式不正确，请输入11位手机号`);
      return;
    }
    if (val && field.type === 'email' && !emailReg.test(val)) {
      message.warning(`${field.label}格式不正确，请输入有效的邮箱地址`);
      return;
    }
  }
  humanAgentSubmitting.value = true;
  try {
    const customFields: Record<string, string> = {};
    for (const field of chatWindowConfig.humanAgentFields) {
      if (humanAgentForm[field.label]) {
        customFields[field.label] = humanAgentForm[field.label];
      }
    }
    const res = await httpPost({
      url: `/cs/conversation/${conversationId.value}/request-human-agent`,
      data: { customFields },
    });
    if (res?.success === false || res?.code === 500) {
      const errMsg = res?.message || '';
      if (errMsg.includes('暂无客服在线')) {
        showHumanAgentModal.value = false;
        if (chatWindowConfig.messageBoardEnabled !== false) {
          await loadMessageBoardConfig();
          showLeaveMessageBoard.value = true;
        } else {
          message.warning('客服不在线，请稍后再试');
        }
        return;
      }
      message.error(errMsg || '请求失败，请稍后再试');
      return;
    }
    showHumanAgentModal.value = false;
    message.success('已为您转接人工客服');
  } catch (err: any) {
    const errMsg = err?.response?.data?.message || err?.data?.message || err?.message || '';
    if (errMsg.includes('暂无客服在线')) {
      showHumanAgentModal.value = false;
      if (chatWindowConfig.messageBoardEnabled !== false) {
        await loadMessageBoardConfig();
        showLeaveMessageBoard.value = true;
      } else {
        message.warning('客服不在线，请稍后再试');
      }
      return;
    }
    message.error(errMsg || (chatWindowConfig.messageBoardEnabled !== false ? '暂无客服在线' : '客服不在线，请稍后再试'));
  } finally {
    humanAgentSubmitting.value = false;
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

async function checkAgentOnline(): Promise<boolean> {
  try {
    const res = await httpGet({ url: '/cs/agent/global/online-status' });
    const data = decryptApiResponse(res);
    return data?.online === true;
  } catch {
    return true;
  }
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
          if (convRes.humanAgentMode === 1) {
            // humanAgent模式: 未分配是正常状态，复用会话
            conversationId.value = storedConvId;
            if (convRes.replyMode !== undefined) {
              replyMode.value = convRes.replyMode;
            }
            return;
          }
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

    // 二次检查：创建新会话前确认客服在线（humanAgentEnabled模式跳过，后端不分配客服）
    if (!chatWindowConfig.humanAgentEnabled) {
      const agentOnline = await checkAgentOnline();
      if (!agentOnline) {
        if (chatWindowConfig.messageBoardEnabled !== false) {
          await loadMessageBoardConfig();
          showLeaveMessageBoard.value = true;
          return;
        }
      }
    }

    // 创建新会话（后端会自动分配客服），附带设备指纹
    const createData: any = {
      userId: userId.value,
      userName: userName.value,
      source: visitorSource.value || undefined,
      deviceId: generateDeviceId(),
      lang: navigator.language || navigator.userLanguage || 'en',
      landingPage: window.location.href,
      referrerPage: document.referrer || undefined,
    };
    if (preferredAgentId.value) {
      createData.agentId = preferredAgentId.value;
    }
    const res = await httpPost({
      url: '/cs/conversation/create',
      data: createData,
    });
    if (res) {
      const conv = res.result || res;
      if (conv.id) {
        conversationId.value = conv.id;
        localStorage.setItem(`cs_conversation_${userId.value}`, conv.id);
        
        // 检查是否有客服分配
        if (conv.status === 0 && !conv.ownerAgentId && conv.humanAgentMode !== 1) {
          if (chatWindowConfig.messageBoardEnabled !== false) {
            await loadMessageBoardConfig();
            showLeaveMessageBoard.value = true;
            return;
          }
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
    const aiData = decryptApiResponse(aiRes);
    const aiEnabled = aiData?.enabled !== false;

    if (!aiEnabled) {
      return;
    }

    const res = await httpGet({ url: '/cs/agent/global/visitor-app' });
    const visitorAppData = decryptApiResponse(res);
    const appId = visitorAppData?.appId;
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
    const data = decryptApiResponse(res);
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
        content: encryptTransport(form.content),
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
      unreadReplies.value = data.map((m: any) => ({
        ...m,
        content: decryptMessage(m.content),
        reply: decryptMessage(m.reply),
      }));
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
    const rawList = Array.isArray(res) ? res : (res?.result || res?.records || []);
    const list = rawList.map((m: any) => ({ ...m, content: decryptMessage(m.content) }));
    if (list) {
      // 防御：服务端返回空但当前已有服务端消息时，跳过替换（可能是后端临时故障）
      if (list.length === 0) {
        const hasServerMessages = messages.value.some(
          (m: any) => !String(m.id).startsWith('local_') && !m.isStreaming && m.senderType !== 3
        );
        if (hasServerMessages) {
          console.warn('[UserChat] 服务端返回空消息列表，跳过替换以保护已有消息');
          return;
        }
      }
      // 智能合并：保留 local_ 消息和正在流式输出的消息（基于id去重）
      const serverIds = new Set(list.map((m: any) => m.id));
      const localAndStreaming = messages.value.filter((m: any) => {
        if (String(m.id).startsWith('local_')) {
          if (serverIds.has(m.id)) return false;
          return !list.some((s: any) =>
            s.senderId === m.senderId &&
            s.id === m.id
          );
        }
        if (m.isStreaming) return true;
        if (m.senderType === 3) {
          return !list.some((s: any) => Number(s.senderType) === 3 && s.id === m.id);
        }
        return false;
      });
      messages.value = [...list, ...localAndStreaming];
      historyBeforeId.value = list[0]?.id || null;
      hasMoreHistory.value = list.length >= historyPageSize;
      lastMessageLoadAt = Date.now();
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
    const rawOlder = Array.isArray(res) ? res : (res?.result || res?.records || []);
    const olderMessages = rawOlder.map((m: any) => ({ ...m, content: decryptMessage(m.content) }));
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

// 加载访客历史会话ID列表
async function loadHistoryConvIds() {
  if (historyConvLoaded.value || !userId.value) return;
  historyConvLoaded.value = true;
  try {
    const res = await httpGet({
      url: '/cs/conversation/visitor-history',
      params: { userId: userId.value, excludeId: conversationId.value },
    });
    const decrypted = decryptApiResponse(res);
    const ids = Array.isArray(decrypted) ? decrypted : (decrypted?.result || []);
    historyConvIds.value = ids;
    historyConvIndex.value = 0;
    hasMoreHistoryConv.value = ids.length > 0;
  } catch {
    hasMoreHistoryConv.value = false;
  }
}

// 加载上一个历史会话的全部消息，插入到消息列表最前面
async function loadHistoryConvMessages() {
  if (loadingHistory.value || !hasMoreHistoryConv.value) return;
  if (historyConvIndex.value >= historyConvIds.value.length) {
    hasMoreHistoryConv.value = false;
    return;
  }

  const el = messagesRef.value;
  const prevScrollHeight = el?.scrollHeight || 0;
  const prevScrollTop = el?.scrollTop || 0;

  loadingHistory.value = true;
  try {
    const histConvId = historyConvIds.value[historyConvIndex.value];
    const res = await httpGet({
      url: '/cs/message/list',
      params: { conversationId: histConvId, limit: 200 },
    });
    const rawHist = Array.isArray(res) ? res : (res?.result || res?.records || []);
    const histMsgs = rawHist.map((m: any) => ({ ...m, content: decryptMessage(m.content) }));
    if (histMsgs.length > 0) {
      // 添加分割线标记
      const separator = {
        id: `__history_sep_${histConvId}`,
        content: '',
        senderType: -1, // 特殊类型，用于渲染分割线
        createTime: histMsgs[histMsgs.length - 1]?.createTime || '',
        _historySeparator: true,
        _historyConvId: histConvId,
      };
      messages.value = [...histMsgs, separator, ...messages.value];
    }
    historyConvIndex.value++;
    hasMoreHistoryConv.value = historyConvIndex.value < historyConvIds.value.length;

    nextTick(() => {
      const nextEl = messagesRef.value;
      if (!nextEl) return;
      const nextScrollHeight = nextEl.scrollHeight;
      nextEl.scrollTop = nextScrollHeight - prevScrollHeight + prevScrollTop;
    });
  } catch {
    // 忽略
  } finally {
    loadingHistory.value = false;
  }
}

function handleMessageScroll(event?: Event) {
  const el = (event?.target as HTMLElement) || messagesRef.value;
  if (!el) return;
  if (loadingHistory.value) return;
  if (el.scrollTop <= 20) {
    if (hasMoreHistory.value) {
      loadMoreMessages();
    } else if (chatWindowConfig.visitorHistory !== false && hasMoreHistoryConv.value) {
      loadHistoryConvMessages();
    }
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
    base = globSetting.apiUrl || window.location.origin;
  }
  let parsed: URL;
  try {
    parsed = new URL(base);
  } catch {
    parsed = new URL(globSetting.apiUrl || window.location.origin);
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
  stopWsCountdown();
  if (hasConnectedOnce) {
    wsStatus.value = 'reconnecting';
    wsShowBanner.value = true;
  }

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
  // Token 模式下 userId 来自 token，与浏览器 deviceId 是两个值；这里始终把浏览器 deviceId
  // 拼到 WS URL，让后端握手期能稳定捕获，避免 cs_conversation 出现「未知访客」。
  if (!authParams.includes('deviceId=')) {
    try {
      const browserDeviceId = generateDeviceId();
      if (browserDeviceId) {
        authParams += `&deviceId=${encodeURIComponent(browserDeviceId)}`;
      }
    } catch (e) {
      // 忽略生成失败，握手期仍可走原逻辑
    }
  }
  const wsUrl = `${wsBase}/ws/cs/user?userId=${userId.value}&conversationId=${conversationId.value}${authParams}`;

  console.log('[UserChat] 连接WebSocket:', wsUrl);

  ws = new WebSocket(wsUrl);

  ws.onopen = async () => {
    console.log('[UserChat] WebSocket已连接');
    wsConnected.value = true;
    const isReconnect = hasConnectedOnce;
    wsReconnectAttempts = 0;
    lastWsMessageAt = Date.now();
    startHeartbeat();
    if (isReconnect) {
      wsStatus.value = 'connected';
      if (wsConnectedBannerTimer) clearTimeout(wsConnectedBannerTimer);
      wsConnectedBannerTimer = window.setTimeout(() => {
        if (wsStatus.value === 'connected') {
          wsShowBanner.value = false;
        }
        wsConnectedBannerTimer = null;
      }, 1500);
      await loadMessages();
    }
    hasConnectedOnce = true;
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
  wsShowBanner.value = false;
  stopWsCountdown();
  if (wsConnectedBannerTimer) { clearTimeout(wsConnectedBannerTimer); wsConnectedBannerTimer = null; }
}

function stopWsCountdown() {
  if (wsCountdownTimer) { clearInterval(wsCountdownTimer); wsCountdownTimer = null; }
  wsReconnectCountdown.value = 0;
}

function scheduleWsReconnect() {
  if (wsManuallyClosed) return;
  if (wsReconnectTimer) return;
  const jitter = Math.floor(Math.random() * 1000);
  const delay = Math.min(30000, 1000 * Math.pow(2, wsReconnectAttempts)) + jitter;
  wsReconnectAttempts += 1;
  if (hasConnectedOnce) {
    wsStatus.value = 'disconnected';
    wsShowBanner.value = true;
    if (wsCountdownTimer) { clearInterval(wsCountdownTimer); wsCountdownTimer = null; }
    wsReconnectCountdown.value = Math.ceil(delay / 1000);
    wsCountdownTimer = window.setInterval(() => {
      wsReconnectCountdown.value -= 1;
      if (wsReconnectCountdown.value <= 0) {
        if (wsCountdownTimer) { clearInterval(wsCountdownTimer); wsCountdownTimer = null; }
      }
    }, 1000);
  }
  wsReconnectTimer = window.setTimeout(() => {
    wsReconnectTimer = null;
    stopWsCountdown();
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
    if (conversationClosed.value) return;
    if (loading.value) return;
    if (ws && ws.readyState === WebSocket.OPEN) {
      return;
    }
    try {
      await loadMessages();
    } catch {
      // ignore
    }
  }, wsFallbackPollIntervalMs);
}

// 心跳 + 超时检测
let heartbeatTimer: any = null;
const HEARTBEAT_INTERVAL = 30000;
const HEARTBEAT_TIMEOUT = 90000;

function startHeartbeat() {
  stopHeartbeat();
  heartbeatTimer = setInterval(() => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      // 检测是否超时（超过3个心跳周期没收到任何消息）
      if (lastWsMessageAt && Date.now() - lastWsMessageAt > HEARTBEAT_TIMEOUT) {
        console.warn('[UserChat] 心跳超时，主动重连');
        ws.close();
        return;
      }
      ws.send(JSON.stringify({ type: 'ping' }));
    }
  }, HEARTBEAT_INTERVAL);
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
      if (msgSenderType === 1 && data.senderAvatar) {
        currentAgentAvatar.value = data.senderAvatar;
      }
      const newMsg = {
        id: data.messageId || Date.now().toString(),
        conversationId: data.conversationId,
        content: decryptMessage(data.content),
        msgType: data.msgType,
        extra: data.extra,
        senderType: msgSenderType,
        senderId: data.senderId,
        senderName: data.senderName,
        senderAvatar: data.senderAvatar,
        isAiGenerated: data.isAiGenerated,
        createTime: data.timestamp || new Date().toISOString(),
      };
      // 避免重复添加
      if (!messages.value.find(m => m.id === newMsg.id)) {
        messages.value.push(newMsg);
        scrollToBottom();
        if (msgSenderType !== 0) {
          playNotificationSound();
        }
      }
      // 收到非用户消息（AI/客服）后，解除等待状态
      // senderType: 0=用户, 1=AI, 2=客服, 3=系统
      if (msgSenderType !== 0) {
        console.log('[UserChat] 收到AI/客服回复，解除等待状态, senderType:', msgSenderType);
        stopAiResponding();
      }
      break;

    case 'system':
      // 系统消息
      messages.value.push({
        id: Date.now().toString(),
        content: decryptTransport(data.content),
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'typing':
      // 客服正在输入
      agentTyping.value = true;
      if (typingTimer) clearTimeout(typingTimer);
      typingTimer = window.setTimeout(() => {
        agentTyping.value = false;
        typingTimer = null;
      }, 3000);
      break;

    case 'ai_typing':
      // AI正在输入状态
      if (data.extra?.isTyping) {
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
      if (data.extra?.agentAvatar) {
        currentAgentAvatar.value = data.extra.agentAvatar;
      } else if (data.senderAvatar) {
        currentAgentAvatar.value = data.senderAvatar;
      }
      if (data.extra?.replyMode !== undefined) {
        replyMode.value = data.extra.replyMode;
      } else {
        replyMode.value = 1; // 默认切换为手动模式
      }
      // ★ 切换为手动模式后，解除AI回复中状态
      stopAiResponding();
      playNotificationSound();
      messages.value.push({
        id: Date.now().toString(),
        content: decryptTransport(data.content) || `客服 ${data.extra?.agentName || data.senderName || ''} 已为您服务`,
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'agent_changed':
      // 会话转接后更新当前客服信息
      hasAgent.value = true;
      if (data.extra?.agentAvatar) {
        currentAgentAvatar.value = data.extra.agentAvatar;
      } else if (data.senderAvatar) {
        currentAgentAvatar.value = data.senderAvatar;
      }
      playNotificationSound();
      messages.value.push({
        id: Date.now().toString(),
        content: decryptTransport(data.content) || `客服 ${data.extra?.agentName || data.senderName || ''} 继续为您服务`,
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

    case 'message_recall': {
      const recallMsgId = data.messageId;
      if (recallMsgId) {
        const idx = messages.value.findIndex((m) => m.id === recallMsgId);
        if (idx !== -1) {
          messages.value[idx].status = 3;
        }
      }
      break;
    }

    case 'conversation_closed':
      // 会话已结束
      conversationClosed.value = true;
      showHumanAgentModal.value = false;
      stopFallbackPoll();
      messages.value.push({
        id: Date.now().toString(),
        content: decryptTransport(data.content) || '会话已结束，感谢您的咨询',
        senderType: 3,
        createTime: new Date().toISOString(),
      });
      scrollToBottom();
      break;

    case 'satisfaction_survey':
      // 客服推送满意度评价
      console.log('[UserChat] 收到满意度评价推送');
      showSatisfactionModal.value = true;
      satisfactionConversationId.value = data.extra?.conversationId || data.conversationId || conversationId.value;
      break;

    case 'pong':
      break;

    case 'sensitive_word_blocked':
      message.warning(decryptTransport(data.content) || '消息包含敏感内容，请修改后重试');
      sending.value = false;
      break;

    case 'visitor_blocked':
      fatalError.value = true;
      fatalErrorMessage.value = '访问已被禁止，请联系管理员';
      disconnectWebSocket();
      stopFallbackPoll();
      break;

    default:
      console.log('[UserChat] 未处理的消息类型:', data.type);
  }
}

// 发送消息
async function sendMessage() {
  ensureAudioCtx();
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

  // 消息接通模式：第一次发送时才创建正式会话
  if (messageConnectMode.value && !conversationId.value) {
    try {
      await initConversation();
      if (showLeaveMessageBoard.value) {
        return;
      }
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

  sending.value = true;
  
  const isAiMode = replyMode.value === 0;
  if (isAiMode) {
    aiResponding.value = true;
    aiTimedOutMessageId = null;
    resetAiTokenTimeout();
  }
  
  try {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'message',
        conversationId: conversationId.value,
        content: encryptTransport(content),
        userName: userName.value,
        msgType,
        extra,
      }));
    } else {
      await httpPost({
        url: '/cs/message/send',
        data: {
          conversationId: conversationId.value,
          content: encryptTransport(content),
          senderId: userId.value,
          senderName: userName.value,
          senderType: 'user',
          msgType,
          extra,
        },
      });
    }
    // 发送成功后才展示消息气泡并清空输入
    messages.value.push(localMsg);
    inputMessage.value = '';
    attachmentList.value.forEach(a => { if (a.previewUrl) URL.revokeObjectURL(a.previewUrl); });
    attachmentList.value = [];
    await nextTick();
    scrollToBottom();
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
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault();
    // AI回复中时不发送消息，允许用户预输入
    if (aiResponding.value) return;
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
    // 释放上一会话产生的视频 blob URL，避免新会话期间内存堆积
    releaseAllMedia();
    // 创建新会话
    await initConversation();

    // 加载历史消息并追加开场白（若需要）
    await loadMessages();

    // 重新连接WebSocket
    connectWebSocket();
    startFallbackPoll();
    
    console.log('[UserChat] 已开始新对话');
  } catch (e) {
    console.error('[UserChat] 重新开始对话失败', e);
    message.error('重新开始失败，请刷新页面');
  }
}



// 处理AI流式token（RAF 批处理，每帧最多刷新一次 DOM）
function handleAiStreamToken(data: any) {
  const messageId = data.messageId;
  const token = decryptTransport(data.content);

  if (!messageId || !token) return;

  // 超时后忽略后续到达的 token
  if (!aiResponding.value || messageId === aiTimedOutMessageId) return;

  // 首个 token 到达时播放提示音
  if (!streamingMessages.value.has(messageId) && !pendingTokens.has(messageId)) {
    playNotificationSound();
  }

  // AI 开始输出内容后，立即关闭 typing 指示器
  if (agentTyping.value) {
    agentTyping.value = false;
  }

  // 将 token 放入缓冲区
  let entry = pendingTokens.get(messageId);
  if (!entry) {
    entry = { tokens: [], conversationId: data.conversationId, senderName: data.senderName };
    pendingTokens.set(messageId, entry);
  }
  entry.tokens.push(token);

  // 用 RAF 合并刷新
  if (!tokenRafId) {
    tokenRafId = requestAnimationFrame(flushPendingTokens);
  }
}

function flushPendingTokens() {
  tokenRafId = null;
  for (const [messageId, entry] of pendingTokens) {
    const batch = entry.tokens.join('');
    const currentContent = streamingMessages.value.get(messageId) || '';
    const newContent = currentContent + batch;
    streamingMessages.value.set(messageId, newContent);

    let existingMsg = messages.value.find(m => m.id === messageId);
    if (!existingMsg) {
      existingMsg = {
        id: messageId,
        conversationId: entry.conversationId,
        content: newContent,
        senderType: 1,
        senderId: 'ai',
        senderName: entry.senderName || '智能客服',
        createTime: new Date().toISOString(),
        isStreaming: true,
      };
      messages.value.push(existingMsg);
    } else {
      existingMsg.content = newContent;
    }
  }
  pendingTokens.clear();
  resetAiTokenTimeout();
  scrollToBottom();
}

// 处理AI流式消息完成
function handleAiStreamComplete(data: any) {
  const messageId = data.messageId;
  const fullContent = decryptMessage(data.content);
  
  // 清理缓冲区，防止残留 RAF 刷入脏数据
  pendingTokens.delete(messageId);
  streamingMessages.value.delete(messageId);
  
  // 更新消息为最终内容（RAF 未 flush 时 complete 先到则创建）
  let existingMsg = messages.value.find(m => m.id === messageId);
  if (!existingMsg) {
    existingMsg = {
      id: messageId,
      conversationId: data.conversationId,
      content: fullContent,
      senderType: 1,
      senderId: 'ai',
      senderName: data.senderName || '智能客服',
      createTime: new Date().toISOString(),
      isStreaming: false,
    };
    messages.value.push(existingMsg);
  } else {
    existingMsg.content = fullContent;
    existingMsg.isStreaming = false;
  }
  
  // 解除AI回复中状态
  stopAiResponding();
  agentTyping.value = false;
  
  scrollToBottom();
}

// 滚动到底部（RAF 防抖，同帧多次调用只执行一次）
function scrollToBottom() {
  if (scrollRafId) return;
  scrollRafId = requestAnimationFrame(() => {
    scrollRafId = null;
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

function getTimezoneOption(): string | undefined {
  return chatWindowConfig.visitorTimezone === 'Asia/Shanghai' ? 'Asia/Shanghai' : undefined;
}

function getDateKey(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  const tz = getTimezoneOption();
  // 使用配置的时区获取年月日，避免浏览器本地时区导致日期错误
  const parts = date.toLocaleDateString('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: tz });
  return parts; // 格式: YYYY-MM-DD
}

function formatDateSeparator(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  const tz = getTimezoneOption();
  const dateKey = getDateKey(date);
  const todayKey = getDateKey(new Date());
  if (dateKey === todayKey) return '今天';
  const yesterday = new Date();
  yesterday.setDate(yesterday.getDate() - 1);
  if (dateKey === getDateKey(yesterday)) return '昨天';
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: tz });
}

// 格式化时间（根据聊天窗口配置的时区，带年月日）
function formatTime(time: string | Date) {
  if (!time) return '';
  const date = new Date(time);
  if (Number.isNaN(date.getTime())) return '';
  const tz = getTimezoneOption();
  const datePart = date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: tz });
  const timePart = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', timeZone: tz });
  return `${datePart} ${timePart}`;
}

// senderType: 0-用户, 1-AI, 2-客服, 3-系统
function isUserMessage(msg: any): boolean {
  if (msg.senderType === 0) return true;
  if (msg.senderId === userId.value) return true;
  return false;
}

function isAiMessage(msg: any): boolean {
  const st = Number(msg?.senderType);
  return st === 1 || msg?.isAiGenerated || (st === 2 && !msg?.senderId);
}

function getMessageClass(msg: any) {
  if (msg.senderType === 3) return 'is-system';
  if (msg.senderType === 4) return 'is-smart-assistant';
  if (isUserMessage(msg)) return 'is-user';
  return isAiMessage(msg) ? 'is-ai' : 'is-agent';
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

function getSmartAssistantFaqData(msg: any) {
  const extra = parseExtra(msg?.extra);
  if (!extra?.faqType) return null;
  return extra;
}

async function onFaqLinkClick(item: any, faqData: any) {
  if (conversationClosed.value) return;
  try {
    await httpPost({
      url: '/cs/message/faq/interact',
      data: {
        conversationId: conversationId.value,
        action: 'click',
        faqIndex: item.index,
        parentPath: faqData.parentPath ?? [],
      },
    });
  } catch (e) {
    console.error('[UserChat] FAQ点击交互失败', e);
  }
}

async function onFaqNavigate(action: string, faqData: any) {
  if (conversationClosed.value) return;
  try {
    await httpPost({
      url: '/cs/message/faq/interact',
      data: {
        conversationId: conversationId.value,
        action,
        parentPath: faqData.parentPath ?? [],
      },
    });
  } catch (e) {
    console.error('[UserChat] FAQ导航交互失败', e);
  }
}

function getMessageAttachments(msg: any): any[] {
  const extra = parseExtra(msg?.extra);
  return extra?.attachments || [];
}

function getMediaAttachments(msg: any): any[] {
  return getMessageAttachments(msg).filter(item => item.type === 'image' || item.type === 'video');
}

function getFileAttachments(msg: any): any[] {
  // 非图片/视频统一走 file 列表（含 audio、pdf、未知类型），由 FileChip / audio 标签分别渲染
  return getMessageAttachments(msg).filter(item => item.type !== 'image' && item.type !== 'video');
}

function isAudioAttachment(att: any): boolean {
  const t = String(att?.type || '').toLowerCase();
  if (t === 'audio') return true;
  const name = String(att?.name || att?.url || '').toLowerCase();
  return /\.(mp3|m4a|wav|ogg|opus|aac|flac)$/.test(name);
}

/**
 * 列表 / 气泡缩略图：图片走 ?thumb=1 通道（withImageThumbCache），
 * 解密失败自动回退原图；非图片复用 getAttachmentUrl 行为。
 */
function getAttachmentThumbUrl(attachment: any): string {
  if (!attachment) return '';
  const type = String(attachment?.type || '').toLowerCase();
  if (type === 'image') {
    if (attachment.previewUrl) return attachment.previewUrl;
    const url = attachment?.url;
    if (!url) return '';
    return withImageThumbCache(getFileAccessHttpUrl(url));
  }
  return getAttachmentUrl(attachment);
}

/**
 * <img @error> 兜底：缩略图 (WEBP) 在老浏览器或 thumb 失败时，
 * 回退到 withImageCache(原图) 重试一次，避免破图。
 */
function onAttachmentImageError(e: Event, attachment: any) {
  const img = e?.target as HTMLImageElement | null;
  if (!img || !attachment?.url) return;
  const fallback = withImageCache(getFileAccessHttpUrl(attachment.url));
  if (fallback && img.src !== fallback) {
    img.src = fallback;
  }
}

/** 模板骨架屏判断：是否已经有可显示的图片源（本地预览或解密完成）。 */
function isAttachmentImageReady(attachment: any): boolean {
  if (!attachment) return false;
  if (attachment.previewUrl) return true;
  if (!attachment.url) return false;
  return isImageReady(getFileAccessHttpUrl(attachment.url));
}

// ─── 【retry-storm-fix】cse:// 媒体/图片失败重试统一入口（与 workbench 同构）─────
function _resolvedUrlOf(attachment: any): string {
  const url = attachment?.url;
  if (!url) return '';
  return getFileAccessHttpUrl(url);
}
function isVideoFailed(attachment: any): boolean {
  const u = _resolvedUrlOf(attachment);
  if (!u || !isCseUrl(u)) return false;
  return getMediaFailureState(u).failed;
}
function isAudioFailed(attachment: any): boolean {
  return isVideoFailed(attachment);
}
function isImageFailed(attachment: any): boolean {
  const u = _resolvedUrlOf(attachment);
  if (!u || !isCseUrl(u)) return false;
  return getImageFailureState(u).failed;
}
function onVideoSkeletonClick(attachment: any) {
  const u = _resolvedUrlOf(attachment);
  if (u && isCseUrl(u) && getMediaFailureState(u).failed) {
    retryMedia(u);
    return;
  }
  openFilePreview(attachment);
}
function onAudioSkeletonClick(attachment: any) {
  const u = _resolvedUrlOf(attachment);
  if (u && isCseUrl(u) && getMediaFailureState(u).failed) {
    retryMedia(u);
  }
}
function onAttachmentImageRetry(attachment: any) {
  const u = _resolvedUrlOf(attachment);
  if (u && isCseUrl(u)) {
    retryImage(u);
  }
}

function getMediaGridData(msg: any) {
  const media = getMediaAttachments(msg);
  const maxItems = 4;
  const items = media.slice(0, maxItems);
  const extraCount = Math.max(0, media.length - maxItems);
  return { items, extraCount, total: media.length };
}

function getAttachmentUrl(attachment: any) {
  const url = attachment?.url;
  if (!url) return '';
  const type = String(attachment?.type || '').toLowerCase();
  const resolved = getFileAccessHttpUrl(url);
  // 图片走 withImageCache（同步占位 + reactive 触发刷新）
  if (type === 'image') {
    return withImageCache(resolved);
  }
  // 视频走 withMediaCache（独立通道：cse:// 解密为 blob URL，模板需 v-if 保护）
  if (type === 'video') {
    if (isCseUrl(resolved)) {
      const mime = String(attachment?.mime || attachment?.contentType || 'video/mp4');
      return withMediaCache(resolved, mime);
    }
    return resolved;
  }
  // 音频：cse:// 同样需走 blob 通道
  if (type === 'audio' || isAudioAttachment(attachment)) {
    if (isCseUrl(resolved)) {
      const mime = String(attachment?.mime || attachment?.contentType || 'audio/mpeg');
      return withMediaCache(resolved, mime);
    }
    return resolved;
  }
  // 【S-P0-8】未知/file/audio 等类型：cse:// 不能直接交给 <img>/<video>/window.open，兜底返回 ''；
  // 业务侧用 v-if + 下载入口（downloadCse）处理。
  if (isCseUrl(resolved)) return '';
  return resolved;
}

/**
 * 输入区附件缩略图：优先用本地 blob 预览（previewUrl，上传期间立即可见），
 * 若 previewUrl 已被释放则回退到 att.url（后端返回的 cse:// 或 http(s) URL），
 * 并通过 withImageCache 解密 / 缓存。
 */
function resolveAttachmentThumb(att: any): string {
  if (!att) return '';
  if (att.previewUrl) return att.previewUrl;
  if (!att.url) return '';
  return withImageCache(getFileAccessHttpUrl(att.url));
}

function openFilePreview(item: any) {
  const url = getAttachmentUrl(item);
  if (url) {
    window.open(url, '_blank');
  }
}

async function openImagePreview(msg: any, item: any) {
  const images = getMessageAttachments(msg).filter(att => att.type === 'image');
  if (!images.length) return;
  const targetKey = String(item?.url || '');
  const resolved = await Promise.all(
    images.map(async (att) => {
      const u = att?.url;
      const blobUrl = u ? await withImageCacheAsync(getFileAccessHttpUrl(u)) : '';
      return { cseUrl: String(u || ''), blobUrl };
    }),
  );
  const usable = resolved.filter(r => r.blobUrl && !r.blobUrl.startsWith('data:'));
  if (!usable.length) return;
  const idx = Math.max(0, usable.findIndex(r => r.cseUrl === targetKey));
  createImgPreview({
    imageList: usable.map(r => r.blobUrl),
    index: Math.min(idx, usable.length - 1),
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

// Unicode emoji 正则（覆盖绝大部分 emoji 字符范围）
const emojiRegex = /([\u{1F300}-\u{1F9FF}\u{2600}-\u{27BF}\u{FE00}-\u{FE0F}\u{200D}\u{20E3}\u{E0020}-\u{E007F}\u{1FA00}-\u{1FA6F}\u{1FA70}-\u{1FAFF}\u{2702}-\u{27B0}\u{1F1E0}-\u{1F1FF}]+)/gu;

/** 纯文本 URL 自动识别：在 HTML 转义前检测 URL，转为可点击的 <a> 标签 */
function linkifyPlainText(text: string): string {
  const urlPattern = /(https?:\/\/[^\s<>]*[^\s<>.,;:!?。，；：！？)\]】]|www\.[^\s<>]*[^\s<>.,;:!?。，；：！？)\]】])/gi;
  let lastIndex = 0;
  let result = '';
  const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  let match: RegExpExecArray | null;
  urlPattern.lastIndex = 0;
  while ((match = urlPattern.exec(text)) !== null) {
    result += esc(text.slice(lastIndex, match.index));
    const url = match[0];
    const href = url.startsWith('www.') ? 'https://' + url : url;
    result += `<a class="auto-link" href="${esc(href)}" target="_blank" rel="noopener noreferrer">${esc(url)}</a>`;
    lastIndex = match.index + url.length;
  }
  result += esc(text.slice(lastIndex));
  return result.replace(/\n/g, '<br>');
}

/** 渲染用户消息（纯文本，表情字符用 span.emoji 包裹以避免被 color 覆盖） */
function renderUserMessage(content: string) {
  if (!content) return '';
  const escaped = linkifyPlainText(content);
  return escaped.replace(emojiRegex, '<span class="emoji">$1</span>');
}

// 将 <img src="/path/..."> 中的路径型 URL 补全为绝对 URL（兼容 Electron 环境）
function normalizeImgUrls(html: string): string {
  try {
    const origin = new URL(globSetting.domainUrl).origin;
    return html.replace(
      /(<img[^>]*?\ssrc=["'])(\/[^"']+)(["'])/gi,
      (_match, pre, path, suf) => `${pre}${origin}${path}${suf}`
    );
  } catch { return html; }
}

// 流式消息轻量渲染（跳过 markdown 解析，完成后由 renderMessage 接管）
function renderStreamingText(content: string) {
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>');
}

// 默认 ALLOWED_URI_REGEXP 拒绝 cse:// → 富文本里的加密图 src 会被剥离；
// 这里在白名单中加入 cse:，由 v-cse-html 指令负责异步解密为 blob: URL
const CSE_ALLOWED_URI_REGEXP =
  /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|sms|cid|xmpp|cse):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i;

function sanitizeHtml(html: string): string {
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['iframe'],
    ADD_ATTR: ['target', 'allowfullscreen', 'frameborder'],
    ALLOW_DATA_ATTR: false,
    ALLOWED_URI_REGEXP: CSE_ALLOWED_URI_REGEXP,
  });
}

// 渲染消息内容（支持富文本HTML、Markdown、纯文本）
function renderMessage(content: string) {
  if (!content) return '';
  content = content.replace(/#\s*\{\s*domainURL\s*\}/g, globSetting.domainUrl);
  content = normalizeImgUrls(content);
  // 1. 检测是否为完整HTML（TinyMCE富文本，如FAQ答案）— sanitize 后返回
  const isRichHtml = /^\s*<(?:p|div|ul|ol|h[1-6]|table|blockquote)\b/i.test(content.trim());
  if (isRichHtml) {
    return sanitizeHtml(content);
  }
  // 2. Markdown 检测
  const hasMarkdown = /!\[[^\]]*]\([^)]*\)|\*\*[^*]+\*\*|```|^\s*#/m.test(content);
  if (hasMarkdown) {
    return sanitizeHtml(md.render(content));
  }
  // 3. 检测内联HTML（如 <a>、<img>、<br> 等）
  const hasInlineHtml = /<([a-z][\s\S]*?)>/i.test(content);
  if (hasInlineHtml) {
    return sanitizeHtml(md.render(content));
  }
  // 4. 纯文本：转义并保留换行，自动识别超链接
  return linkifyPlainText(content);
}

// 监听新增消息，自动滚动（浅监听 length 变化，不再 deep watch 避免流式 content 修改触发）
watch(() => messages.value.length, () => {
  if (loadingHistory.value) return;
  if (!isMessagesAtBottom()) return;
  scrollToBottom();
});
</script>

<style lang="less" scoped>
.user-chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  height: 100dvh;
  width: 100%;
  max-width: 100vw;
  background: #f5f5f5;
  overflow-x: hidden;
}

.chat-outer-layout {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  overflow: hidden;
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
  overflow: hidden;
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
}
.ad-sidebar-img {
  width: 100%;
  object-fit: cover;
  cursor: pointer;
  display: block;
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
.sidebar-faq-list-scrollable {
  max-height: 400px;
  overflow-y: auto;
}
.sidebar-faq-more {
  text-align: center;
  font-size: 12px;
  color: #1890ff;
  cursor: pointer;
  padding: 6px 0 2px;
  user-select: none;
}
.sidebar-faq-more:hover {
  color: #40a9ff;
}
.sidebar-faq-disabled {
  opacity: 0.5;
  pointer-events: none;
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
    justify-content: space-between;
    padding: 12px 16px;
    background: #667eea;
    background-size: cover;
    background-position: center;
    color: #fff;
    min-height: 56px;
    
    .header-info {
      display: flex;
      align-items: center;
      gap: 10px;
      flex: 1;
      min-width: 0;
    }
    
    .app-avatar {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      object-fit: cover;
      flex-shrink: 0;
    }
    
    .app-info {
      display: flex;
      flex-direction: column;
      min-width: 0;
    }
    
    .app-name {
      font-size: 15px;
      font-weight: 600;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .board-subtitle {
      font-size: 12px;
      opacity: 0.85;
      margin-top: 2px;
    }
    
    .header-icons {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;
    }
    
    .header-icon-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-decoration: none;
      color: #fff;
    }
    
    .header-icon-img {
      object-fit: contain;
    }
    
    .header-icon-transparent {
      background: transparent;
    }
    
    .header-icon-name {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: center;
    }
  }
  
  .board-body {
    flex: 1;
    padding: 24px 20px;
    padding-bottom: calc(24px + env(safe-area-inset-bottom, 0px));
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

.ws-status-banner {
  padding: 4px 12px;
  font-size: 12px;
  text-align: center;
  overflow: hidden;
  a { cursor: pointer; text-decoration: underline; }
}
.ws-reconnecting { background: #fff7e6; color: #d46b08; }
.ws-disconnected { background: #fff2f0; color: #cf1322; }
.ws-connected { background: #f6ffed; color: #389e0d; }
.ws-banner-enter-active, .ws-banner-leave-active { transition: all 0.3s ease; }
.ws-banner-enter-from, .ws-banner-leave-to { max-height: 0; padding-top: 0; padding-bottom: 0; opacity: 0; }
.ws-banner-enter-to, .ws-banner-leave-from { max-height: 30px; opacity: 1; }

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--theme-color, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;

  .mobile-back-btn {
    display: none;
  }

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

  .header-icons {
    display: flex;
    align-items: center;
    gap: 16px;
    flex-shrink: 0;
  }

  .header-icon-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    text-decoration: none;
    color: #fff;
    gap: 4px;
    transition: opacity 0.2s;

    &:hover {
      opacity: 0.85;
    }
  }

  .header-icon-img {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    object-fit: cover;
    border: 1.5px solid rgba(255, 255, 255, 0.4);

    &.header-icon-transparent {
      border: none;
      border-radius: 0;
      background: transparent;
    }
  }

  .header-icon-name {
    font-size: 11px;
    opacity: 0.9;
    max-width: 56px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: center;
  }
}

.chat-messages {
  flex: 1;
  padding: 16px 5px;
  overflow-y: auto;
  overflow-x: hidden;
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

.history-separator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  width: 100%;
}
.history-separator-line {
  flex: 1;
  height: 1px;
  background: #e0e0e0;
}
.history-separator-text {
  font-size: 11px;
  color: #bbb;
  white-space: nowrap;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;

  .message-content {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    max-width: 70%;
    min-width: 0;
    overflow: hidden;
  }

  .message-text {
    max-width: 100%;
    padding: 10px 14px;
    background: var(--visitor-bubble-bg, linear-gradient(135deg, #667eea 0%, #764ba2 100%));
    color: var(--visitor-bubble-color, #fff);
    border-radius: 20px 20px 4px 20px;
    font-size: 14px;
    line-height: 1.6;
    overflow-wrap: anywhere;

    :deep(a) {
      color: inherit;
      text-decoration: underline;
      overflow-wrap: anywhere;
      word-break: break-all;
    }

    :deep(.emoji) {
      color: initial;
      font-family: 'Apple Color Emoji', 'Segoe UI Emoji', 'Noto Color Emoji', 'Twemoji Mozilla', sans-serif;
    }
  }

  .message-time {
    text-align: right;
    font-size: 11px;
    color: #999;
    margin-top: 4px;
  }

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
  }
}

.agent-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;

  .avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    object-fit: cover;
  }

  .message-content {
    width: fit-content;
    max-width: 70%;
    min-width: 0;
    overflow: hidden;
  }

  .sender-info {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 3px;
  }

  .sender-name {
    font-size: 12px;
    color: #666;
  }

  .message-text {
    display: block;
    width: fit-content;
    max-width: 100%;
    padding: 10px 14px;
    background: var(--agent-bubble-bg, #f5f5f5);
    color: var(--agent-bubble-color, #333);
    border-radius: 20px 20px 20px 4px;
    font-size: 14px;
    line-height: 1.6;
    overflow-wrap: anywhere;
    overflow-x: hidden;

    :deep(a) {
      overflow-wrap: anywhere;
      word-break: break-all;
    }

    :deep(.auto-link) {
      color: #1890ff;
      text-decoration: underline;
    }

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
      max-width: 100%;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      word-break: break-all;
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
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      word-break: break-all;
    }

    :deep(table) {
      max-width: 100%;
      overflow-x: auto;
      display: block;
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
  max-width: 600px;

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

    .img-skeleton,
    .video-skeleton {
      width: 100%;
      height: 100%;
      min-height: 80px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #efefef 0%, #f7f7f7 100%);
      color: #aaa;
      font-size: 22px;
      gap: 4px;
      cursor: pointer;

      .skeleton-text {
        font-size: 12px;
      }

      // 【retry-storm-fix】视频骨架失败态
      &.is-failed {
        background: rgba(255, 77, 79, 0.06);
        color: #cf1322;
        border: 1px dashed rgba(255, 77, 79, 0.5);
        .anticon { color: #cf1322; }
      }
    }

    .img-skeleton-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 4px;
      background: linear-gradient(135deg, #efefef 0%, #f7f7f7 100%);
      color: #aaa;
      font-size: 22px;
      pointer-events: auto;
      cursor: progress;

      // 【retry-storm-fix】图片骨架失败态
      &.is-failed {
        background: rgba(255, 77, 79, 0.06);
        color: #cf1322;
        cursor: pointer;
        .overlay-text {
          font-size: 12px;
          line-height: 1;
        }
      }
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
    aspect-ratio: auto;
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

  // 【retry-storm-fix】音频骨架屏（裸 <audio> v-if 拒绝渲染时占位）
  .audio-skeleton {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    margin-top: 4px;
    background: linear-gradient(135deg, #efefef 0%, #f7f7f7 100%);
    color: #888;
    border-radius: 8px;
    font-size: 13px;
    cursor: pointer;
    .anticon { font-size: 18px; }

    &.is-failed {
      background: rgba(255, 77, 79, 0.06);
      color: #cf1322;
      border: 1px dashed rgba(255, 77, 79, 0.5);
      .anticon { color: #cf1322; }
    }
  }
}

.media-viewer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 10px;
}

.media-viewer-item {
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  border: 1px solid #f0f0f0;
  aspect-ratio: 16 / 9;
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

/* 智能助手消息 FAQ 交互 */
.sa-divider {
  border-top: 1px solid #e8e8e8;
  margin: 10px 0;
}
.sa-faq-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sa-faq-link {
  color: var(--faq-link-color, #e8453c);
  cursor: pointer;
  font-size: 13px;
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 4px;
}
.sa-faq-link:hover {
  text-decoration: underline;
}
.sa-nav-links {
  display: flex;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}
.sa-nav-link {
  color: var(--faq-nav-color, #1890ff);
  font-size: 12px;
  cursor: pointer;
}
.sa-human-agent {
  color: var(--faq-nav-color, #1890ff);
  font-weight: 500;
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
  padding: 8px 12px 10px;
  padding-bottom: calc(10px + env(safe-area-inset-bottom, 0px));
  background: #fff;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;

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
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(255,255,255,0.7);
      gap: 2px;

      .att-progress-text {
        font-size: 10px;
        color: #555;
      }
    }
  }

  // 当承载 FileChip 时让 attachment-thumb 自适应宽度
  .attachment-thumb.is-chip {
    width: auto;
    height: auto;
    border: none;
    border-radius: 0;
    overflow: visible;

    .att-remove {
      top: -6px;
      right: -6px;
      border-radius: 9px;
    }
  }

  .input-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    font-size: 18px;
    color: #999;
    padding: 2px 4px 6px;
  }

  .toolbar-icon {
    cursor: pointer;
    transition: color 0.2s;
    &:hover {
      color: var(--theme-color, #667eea);
    }
  }

  .input-row {
    display: flex;
    align-items: flex-end;
    gap: 6px;
  }

  .inline-emoji-icon {
    font-size: 20px;
    flex-shrink: 0;
    padding-bottom: 6px;
  }

  .sound-toggle-btn {
    cursor: pointer;
    color: #999;
    font-size: 20px;
    display: inline-flex;
    align-items: center;
    flex-shrink: 0;
    padding-bottom: 9px;
    transition: color 0.2s;
    &:hover {
      color: var(--theme-color, #667eea);
    }
  }

  .sound-muted-icon {
    position: relative;
    display: inline-flex;
    opacity: 0.5;
  }

  .mute-line {
    position: absolute;
    width: 2px;
    height: 120%;
    background: currentColor;
    transform: rotate(45deg);
    top: -10%;
    left: 50%;
  }

  :deep(.ant-input) {
    flex: 1;
    border-radius: 18px;
    padding: 8px 14px;
    resize: none;
    border-color: #e0e0e0;
    font-size: 14px;
    min-height: 36px;

    &:focus {
      border-color: var(--theme-color, #667eea);
      box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.15);
    }
  }

  .send-icon-btn {
    font-size: 22px;
    color: var(--theme-color, #667eea);
    cursor: pointer;
    flex-shrink: 0;
    padding-bottom: 6px;
    transition: color 0.2s, transform 0.2s;
    &:hover {
      color: var(--theme-color, #764ba2);
      transform: scale(1.15);
    }
  }

  .stop-icon-btn {
    font-size: 22px;
    color: #ff4d4f;
    cursor: pointer;
    flex-shrink: 0;
    padding-bottom: 6px;
    transition: color 0.2s, transform 0.2s;
    &:hover {
      color: #cf1322;
      transform: scale(1.15);
    }
  }
}

.chat-fatal-error {
  min-height: 100vh;
  min-height: 100dvh;
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

/* ==================== 满意度评价弹窗 ==================== */
.satisfaction-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.satisfaction-modal {
  background: #fff;
  border-radius: 12px;
  width: 340px;
  max-width: 90vw;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

.satisfaction-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}

.satisfaction-close {
  cursor: pointer;
  font-size: 20px;
  color: #999;
  line-height: 1;
  
  &:hover {
    color: #333;
  }
}

.satisfaction-body {
  padding: 20px;
  text-align: center;
}

.satisfaction-tip {
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}

.satisfaction-stars {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-bottom: 8px;
}

.star-item {
  font-size: 32px;
  cursor: pointer;
  color: #d9d9d9;
  transition: color 0.2s, transform 0.15s;
  
  &:hover {
    transform: scale(1.15);
  }
  
  &.active {
    color: #faad14;
  }
}

.satisfaction-labels {
  font-size: 13px;
  color: #999;
  margin-bottom: 14px;
  height: 20px;
}

.satisfaction-textarea {
  width: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  resize: none;
  outline: none;
  transition: border-color 0.2s;
  
  &:focus {
    border-color: #667eea;
  }
  
  &::placeholder {
    color: #bfbfbf;
  }
}

.satisfaction-footer {
  padding: 12px 20px 20px;
  text-align: center;
}

.satisfaction-btn-submit {
  width: 100%;
  height: 40px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
  
  &:hover {
    opacity: 0.9;
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

@media (max-width: 800px) {
  .chat-sidebar {
    display: none;
  }

  /* 头部 */
  .chat-header {
    padding: 12px 14px;
    .mobile-back-btn {
      display: block;
      font-size: 20px;
      cursor: pointer;
      flex-shrink: 0;
      margin-right: 8px;
    }
    .header-info {
      gap: 10px;
    }
    .header-icons {
      display: none;
    }
    .app-avatar {
      width: 40px;
      height: 40px;
    }
    .app-name {
      font-size: 17px;
    }
    .status-tag {
      font-size: 13px;
      padding: 0 8px;
      height: 22px;
      line-height: 22px;
    }
  }

  /* 消息区域 */
  .chat-messages {
    padding: 10px 8px;
  }
  .message-item {
    margin-bottom: 10px;
  }

  /* 气泡通用 */
  .user-message,
  .agent-message {
    gap: 10px;
    .avatar {
      width: 40px;
      height: 40px;
    }
    .message-content {
      max-width: 80%;
    }
    .message-text {
      padding: 12px 16px;
      font-size: 14px;
      line-height: 1.6;
    }
    .message-time {
      font-size: 12px;
      margin-top: 3px;
    }
  }
  .user-message .message-text {
    border-radius: 18px 18px 4px 18px;
  }
  .agent-message .message-text {
    border-radius: 18px 18px 18px 4px;
    :deep(p) {
      margin: 0 0 6px;
    }
    :deep(ul), :deep(ol) {
      padding-left: 16px;
      margin: 6px 0;
    }
    :deep(li) {
      margin-bottom: 3px;
    }
  }
  .agent-message .sender-name {
    font-size: 13px;
  }
  .agent-message .sender-info {
    margin-bottom: 2px;
  }

  /* 系统消息 */
  .system-message {
    font-size: 13px;
    padding: 5px 14px;
  }

  /* 媒体网格 */
  .message-media-grid {
    max-width: 100%;
  }

  /* 预设问题 */
  .preset-questions {
    padding: 8px 10px;
    .preset-title {
      margin-bottom: 6px;
      font-size: 14px;
    }
    .preset-list {
      gap: 6px;
      :deep(.ant-btn) {
        font-size: 14px;
        padding: 4px 10px;
        height: auto;
        line-height: 1.4;
      }
    }
  }

  /* 输入区域 */
  .chat-input {
    padding: 6px 8px 8px;
    padding-bottom: calc(8px + env(safe-area-inset-bottom, 0px));
    .input-toolbar {
      gap: 12px;
      font-size: 20px;
      padding: 2px 4px 4px;
    }
    :deep(.ant-input) {
      padding: 8px 14px;
      font-size: 14px;
      min-height: 38px;
      border-radius: 18px;
    }
    .send-icon-btn, .stop-icon-btn {
      font-size: 24px;
      padding-bottom: 6px;
    }
    .inline-emoji-icon {
      font-size: 22px;
      padding-bottom: 6px;
    }
    .sound-toggle-btn {
      font-size: 22px;
    }
  }

  /* 正在输入指示器 */
  .typing-indicator {
    gap: 6px;
    margin-bottom: 8px;
    .typing-dots {
      padding: 6px 10px;
      border-radius: 14px;
    }
  }

  /* 会话结束 */
  .chat-closed {
    padding: 12px 10px;
    gap: 8px;
    font-size: 13px;
  }

}
</style>

<style lang="less">
html, body {
  overflow: hidden;
  margin: 0;
  padding: 0;
  max-width: 100vw;
  height: 100vh;
  height: 100dvh;
}
</style>
