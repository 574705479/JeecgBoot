<template>
  <div class="cs-workbench" :style="[themeVars, workbenchLayoutStyle]">
    <!-- 左侧会话列表 -->
    <div class="sidebar">
      <CsAgentBar />
      <CsWsStatusBanner />
      
      <!-- 设置抽屉 -->
      <CsWorkbenchSettingsDrawer />

      <!-- 搜索框 -->
      <div class="search-bar">
        <a-input
          v-model:value="searchKeyword"
          placeholder="搜索会话..."
          allowClear
          size="small"
        >
          <template #prefix><SearchOutlined /></template>
        </a-input>
      </div>

      <!-- 筛选标签 -->
      <div class="filter-tabs">
        <div 
          class="filter-tab" 
          :class="{ active: filter === 'mine' }"
          @click="filter = 'mine'"
        >
          我的 <span class="count">{{ myCount }}</span>
        </div>
        <!-- 待接入标签已隐藏：会话创建时后端自动分配客服 -->
        <!--
        <div 
          class="filter-tab" 
          :class="{ active: filter === 'unassigned' }"
          @click="filter = 'unassigned'"
        >
          待接入 <span class="count">{{ unassignedCount }}</span>
        </div>
        -->
        <!-- 已结束标签已隐藏：已结束会话可在"会话记录"页面查看 -->
        <!--
        <div 
          class="filter-tab" 
          :class="{ active: filter === 'closed' }"
          @click="filter = 'closed'"
        >
          已结束 <span class="count">{{ closedCount }}</span>
        </div>
        -->
        <!-- 全部标签暂时隐藏，避免界面拥挤 -->
        <!-- <div 
          class="filter-tab" 
          :class="{ active: filter === 'all' }"
          @click="filter = 'all'"
        >
          全部
        </div> -->
        <!-- 同事会话：查看所有客服的进行中会话 -->
        <div 
          class="filter-tab supervisor-tab" 
          :class="{ active: filter === 'monitor' }"
          @click="filter = 'monitor'"
        >
          <TeamOutlined /> 同事会话 <span class="count" v-if="colleagueCount > 0">{{ colleagueCount }}</span>
        </div>
      </div>

      <!-- 会话列表：普通模式（我的/已结束等） -->
      <div class="conversation-list" v-if="filter !== 'monitor'">
        <div
          v-for="conv in displayConversations"
          :key="conv.id"
          class="conversation-item"
          :class="{ 
            active: currentConversation?.id === conv.id,
            unread: conv.unreadCount > 0,
            closed: conv.status === 2
          }"
          @click="selectConversation(conv)"
        >
          <div class="conv-avatar">
            <a-badge :status="(conv.userOnline ?? (currentConversation?.id === conv.id ? userOnline : false)) ? 'success' : 'default'" dot>
              <a-avatar :size="42" class="visitor-avatar">{{ getDisplayName(conv).charAt(0) }}</a-avatar>
            </a-badge>
            <StarFilled v-if="conv.visitorStar === 1" class="conv-star-badge" />
          </div>
          <div class="conv-content">
            <div class="conv-header">
              <span class="conv-name">{{ getDisplayName(conv) }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-preview">{{ stripHtmlTags(conv.lastMessage) || '暂无消息' }}</div>
            <!-- 显示当前对话客服 - 从消息列表中获取最后一个发消息的客服 -->
            <div class="conv-agent" v-if="conv.lastTalkingAgent && conv.status === 1">
              <span class="agent-label">对话中:</span>
              <span class="agent-name">{{ conv.lastTalkingAgent }}</span>
            </div>
            <div class="conv-waiting" v-if="agentTimeoutConfig.enabled && visitorWaitingSeconds[conv.id]">
              <span class="waiting-icon">⏱</span>
              <span class="waiting-text">等待回复 {{ formatWaitingTime(visitorWaitingSeconds[conv.id]) }}</span>
            </div>
            <div class="conv-custom-tags" v-if="parseConvCustomFields(conv, 'showInConvList').length">
              <a-tag v-for="cf in parseConvCustomFields(conv, 'showInConvList')" :key="cf.label" color="red" size="small"
                style="color: #cf1322; background: #fff1f0; border-color: #ffa39e;">
                {{ cf.label }}: {{ cf.value }}
              </a-tag>
            </div>
          </div>
          <div class="conv-badge" v-if="conv.unreadCount > 0">
            {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
          </div>
          <!-- 操作按钮组 -->
          <div class="conv-actions">
            <a-dropdown v-if="conv.status === 2" :trigger="['click']" @click.stop>
              <a-button size="small" type="text">
                <MoreOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="delete" @click="deleteConversation(conv.id)">
                    <DeleteOutlined /> 删除会话
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </div>
        
        <div class="empty-state" v-if="displayConversations.length === 0">
          <InboxOutlined class="empty-icon" />
          <p>{{ searchKeyword ? '无匹配会话' : '暂无会话' }}</p>
        </div>
      </div>

      <!-- 会话列表：监控模式（按客服分组） -->
      <div class="conversation-list monitor-agent-list" v-else>
        <div v-if="monitorGroups.length === 0" class="empty-state">
          <TeamOutlined class="empty-icon" />
          <p>暂无客服数据</p>
        </div>
        <div 
          v-for="group in monitorGroups" 
          :key="group.agent.id" 
          class="monitor-agent-group"
          :class="{ expanded: group.expanded }"
        >
          <!-- 客服行：点击展开/折叠 -->
          <div class="monitor-agent-header" @click="toggleAgentExpand(group.agent.id)">
            <span class="monitor-expand-icon">
              <CaretDownOutlined v-if="group.expanded" />
              <CaretRightOutlined v-else />
            </span>
            <a-avatar
              :size="30"
              class="monitor-agent-avatar"
              :src="getAgentItemAvatarUrl(group.agent)"
            >
              {{ (group.agent.nickname || '客').charAt(0) }}
            </a-avatar>
            <span class="monitor-agent-name">{{ group.agent.nickname || '未知客服' }}</span>
            <span v-if="group.agent.id !== '__unassigned__'" class="monitor-agent-status" :class="'status-' + group.agent.status">
              {{ getAgentStatusText(group.agent.status) }}
            </span>
            <span class="monitor-agent-sessions">
              <template v-if="group.agent.id !== '__unassigned__'">{{ group.conversations.filter(c => c.userOnline).length }}/</template>{{ group.conversations.length }}
            </span>
          </div>
          <!-- 展开的对话列表 -->
          <div class="monitor-agent-conversations" v-show="group.expanded">
            <div v-if="group.conversations.length === 0" class="monitor-no-conv">
              暂无对话
            </div>
            <div
              v-for="conv in group.conversations"
              :key="conv.id"
              class="conversation-item monitor-conv-item"
              :class="{ 
                active: currentConversation?.id === conv.id,
                unread: conv.unreadCount > 0
              }"
              @click="selectConversation(conv)"
            >
              <div class="conv-avatar">
                <a-badge :status="(conv.userOnline ?? (currentConversation?.id === conv.id ? userOnline : false)) ? 'success' : 'default'" dot>
                  <a-avatar :size="36" class="visitor-avatar">{{ getDisplayName(conv).charAt(0) }}</a-avatar>
                </a-badge>
                <StarFilled v-if="conv.visitorStar === 1" class="conv-star-badge" />
              </div>
              <div class="conv-content">
                <div class="conv-header">
                  <span class="conv-name">{{ getDisplayName(conv) }}</span>
                  <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
                </div>
                <div class="conv-preview">{{ stripHtmlTags(conv.lastMessage) || '暂无消息' }}</div>
                <div class="conv-waiting" v-if="agentTimeoutConfig.enabled && visitorWaitingSeconds[conv.id]">
                  <span class="waiting-icon">⏱</span>
                  <span class="waiting-text">等待回复 {{ formatWaitingTime(visitorWaitingSeconds[conv.id]) }}</span>
                </div>
                <div class="conv-custom-tags" v-if="parseConvCustomFields(conv, 'showInConvList').length">
                  <a-tag v-for="cf in parseConvCustomFields(conv, 'showInConvList')" :key="cf.label" color="red" size="small"
                    style="color: #cf1322; background: #fff1f0; border-color: #ffa39e;">
                    {{ cf.label }}: {{ cf.value }}
                  </a-tag>
                </div>
              </div>
              <div class="conv-badge" v-if="conv.unreadCount > 0">
                {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 中间聊天区域 -->
    <div class="chat-area" v-if="currentConversation">
      <CsChatHeader />

      <!-- 消息容器 -->
      <div class="chat-body">
        <div class="messages-container" ref="messagesRef" @scroll.passive="handleMessageScroll">
          <div v-if="loadingHistory" class="loading-history">
            <a-spin size="small" />
            <span>加载历史消息...</span>
          </div>
          <div v-for="msg in displayMessages" :key="msg._clientKey ?? msg.id" class="message-wrapper" :class="getMessageClass(msg)">
            <!-- 日期分隔符 -->
            <div v-if="msg.isDateSeparator" class="date-separator">
              <span>{{ msg.content }}</span>
            </div>
            <!-- 系统消息 -->
            <div v-else-if="msg.senderType === 3" class="system-msg">
              <span>{{ msg.content }}</span>
            </div>
            <!-- 用户/访客消息 (显示在左边) -->
            <template v-else-if="msg.senderType === 0">
              <a-avatar :size="messageAvatarSize" class="msg-avatar">
                {{ getVisitorDisplayName(msg).charAt(0) }}
              </a-avatar>
              <div class="msg-body">
                <div class="msg-info">
                  <span class="sender-name">{{ getVisitorDisplayName(msg) }}</span>
                </div>
                <div class="msg-bubble user-bubble">
                  <div v-if="msg.content" class="msg-text" v-html="renderMessage(msg.content)" v-cse-html></div>
                  <div
                    v-if="getMediaGridData(msg).items.length"
                    class="msg-media-grid"
                    :class="`media-grid--${Math.min(getMediaGridData(msg).total, 4)}`"
                  >
                    <div
                      class="media-item"
                      v-for="(item, index) in getMediaGridData(msg).items"
                      :key="`${item.url}_${index}`"
                    >
                      <template v-if="item.type === 'image'">
                        <img
                          :src="getAttachmentThumbUrl(item)"
                          @click="openImagePreview(msg, item)"
                          @error="onAttachmentImageError($event, item)"
                        />
                        <div
                          v-if="!isAttachmentImageReady(item)"
                          class="media-skeleton-overlay"
                          :class="{ 'is-failed': isImageFailed(item) }"
                          @click.stop="isImageFailed(item) ? onAttachmentImageRetry(item) : null"
                        >
                          <ReloadOutlined v-if="isImageFailed(item)" />
                          <LoadingOutlined v-else spin />
                          <span v-if="isImageFailed(item)" class="overlay-text">点击重试</span>
                        </div>
                      </template>
                      <template v-else-if="item.type === 'video'">
                        <video
                          v-if="getAttachmentUrl(item)"
                          :src="getAttachmentUrl(item)"
                          controls
                          playsinline
                          preload="metadata"
                        />
                        <div
                          v-else
                          class="video-skeleton"
                          :class="{ 'is-failed': isVideoFailed(item) }"
                          @click="onVideoSkeletonClick(item)"
                        >
                          <PlayCircleOutlined />
                          <span>{{ isVideoFailed(item) ? '加载失败，点击重试' : '视频加载中…' }}</span>
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
                  <div v-if="getFileAttachments(msg).length" class="msg-file-list">
                    <template v-for="(item, index) in getFileAttachments(msg)">
                      <template v-if="item.type === 'audio'">
                        <audio
                          v-if="getAttachmentUrl(item)"
                          :key="`audio_${item.url}_${index}`"
                          :src="getAttachmentUrl(item)"
                          controls
                          preload="metadata"
                          class="msg-audio"
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
                        :key="`file_${item.url}_${index}`"
                        :name="item.name || item.url"
                        :size="item.size"
                        :type="item.type"
                        :url="getFileAccessHttpUrl(item.url)"
                      />
                    </template>
                  </div>
                </div>
                <div class="msg-meta">
                  {{ formatMessageTime(msg.createTime) }}
                  <!-- 手动触发回复建议 -->
                  <a-button 
                    type="link" 
                    size="small" 
                    class="ai-assist-btn"
                    :loading="aiSuggestionLoading"
                    @click="requestAiSuggestion(msg.content)"
                    title="获取回复建议"
                  >
                    <RobotOutlined /> 回复建议
                  </a-button>
                </div>
              </div>
            </template>
            <!-- 客服/AI/智能助手消息 (显示在右边) -->
            <template v-else>
              <div class="msg-body">
                <div class="msg-info">
                  <span class="sender-name">{{ isSmartAssistant(msg) ? (msg.senderName || '智能助手') : (msg.actualSenderName || msg.senderName) }}</span>
                  <a-tag v-if="isSmartAssistant(msg)" color="cyan" size="small">助手</a-tag>
                  <a-tag v-else-if="isAiMessage(msg)" color="purple" size="small">AI</a-tag>
                  <a-avatar v-if="isSmartAssistant(msg)" :size="messageAvatarSize" class="msg-avatar-inline" style="background: #13c2c2">助</a-avatar>
                  <a-avatar v-else :size="messageAvatarSize" class="msg-avatar-inline" :src="getMessageAvatarUrl(msg)">
                    {{ (msg.actualSenderName || msg.senderName)?.charAt(0) || (isAiMessage(msg) ? 'AI' : '客') }}
                  </a-avatar>
                </div>
                <div class="msg-bubble agent-bubble" :class="{ 'ai-bubble': isAiMessage(msg), 'assistant-bubble': isSmartAssistant(msg) }">
                  <div v-if="msg.content" class="msg-text" v-html="msg.isStreaming ? renderStreamingText(msg.content) : renderMessage(msg.content)" v-cse-html></div>
                  <div
                    v-if="getMediaGridData(msg).items.length"
                    class="msg-media-grid"
                    :class="`media-grid--${Math.min(getMediaGridData(msg).total, 4)}`"
                  >
                    <div
                      class="media-item"
                      v-for="(item, index) in getMediaGridData(msg).items"
                      :key="`${item.url}_${index}`"
                    >
                      <template v-if="item.type === 'image'">
                        <img
                          :src="getAttachmentThumbUrl(item)"
                          @click="openImagePreview(msg, item)"
                          @error="onAttachmentImageError($event, item)"
                        />
                        <div
                          v-if="!isAttachmentImageReady(item)"
                          class="media-skeleton-overlay"
                          :class="{ 'is-failed': isImageFailed(item) }"
                          @click.stop="isImageFailed(item) ? onAttachmentImageRetry(item) : null"
                        >
                          <ReloadOutlined v-if="isImageFailed(item)" />
                          <LoadingOutlined v-else spin />
                          <span v-if="isImageFailed(item)" class="overlay-text">点击重试</span>
                        </div>
                      </template>
                      <template v-else-if="item.type === 'video'">
                        <video
                          v-if="getAttachmentUrl(item)"
                          :src="getAttachmentUrl(item)"
                          controls
                          playsinline
                          preload="metadata"
                        />
                        <div
                          v-else
                          class="video-skeleton"
                          :class="{ 'is-failed': isVideoFailed(item) }"
                          @click="onVideoSkeletonClick(item)"
                        >
                          <PlayCircleOutlined />
                          <span>{{ isVideoFailed(item) ? '加载失败，点击重试' : '视频加载中…' }}</span>
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
                  <div v-if="getFileAttachments(msg).length" class="msg-file-list">
                    <template v-for="(item, index) in getFileAttachments(msg)">
                      <template v-if="item.type === 'audio'">
                        <audio
                          v-if="getAttachmentUrl(item)"
                          :key="`audio_${item.url}_${index}`"
                          :src="getAttachmentUrl(item)"
                          controls
                          preload="metadata"
                          class="msg-audio"
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
                        :key="`file_${item.url}_${index}`"
                        :name="item.name || item.url"
                        :size="item.size"
                        :type="item.type"
                        :url="getFileAccessHttpUrl(item.url)"
                      />
                    </template>
                  </div>
                </div>
                <div class="msg-meta">
                  {{ formatMessageTime(msg.createTime) }}
                  <a-popconfirm v-if="isAiMessage(msg) || msg.senderId === agentId" title="确定撤回这条消息？" ok-text="确定" cancel-text="取消" @confirm="recallMessage(msg)">
                    <UndoOutlined class="recall-btn" title="撤回" />
                  </a-popconfirm>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- 回到底部按钮 -->
        <transition name="fade-up">
          <div class="scroll-to-bottom" v-show="showScrollToBottom" @click="scrollToBottomClick">
            <DownOutlined />
          </div>
        </transition>

        <!-- 回复建议展示区 -->
        <div class="ai-suggestion" v-if="aiSuggestion">
          <div class="suggestion-label">
            <RobotOutlined /> 回复建议
            <a-tag v-if="aiSuggestionLoading" color="processing" size="small">生成中...</a-tag>
          </div>
          <div class="suggestion-text" v-html="renderMarkdown(aiSuggestion)" v-cse-html></div>
          <div class="suggestion-btns">
            <a-button type="primary" size="small" @click="useSuggestion(true)" :disabled="aiSuggestionLoading">直接发送</a-button>
            <a-button size="small" @click="useSuggestion(false)" :disabled="aiSuggestionLoading">填入编辑</a-button>
            <a-button size="small" type="text" @click="dismissSuggestion()">忽略</a-button>
          </div>
        </div>
      </div>

      <!-- 输入区域 - 固定底部 -->
      <div class="chat-input-area" v-if="currentConversation.status !== 2">
        <div class="input-toolbar">
          <a-tooltip title="表情">
            <SmileOutlined class="toolbar-icon" @click="toggleEmojiPanel" />
          </a-tooltip>
          <a-upload
            :showUploadList="false"
            :multiple="true"
            :accept="ALLOWED_UPLOAD_EXTS.join(',')"
            :beforeUpload="beforeUploadAttachment"
            :customRequest="handleCustomUpload"
          >
            <a-tooltip title="上传附件">
              <PaperClipOutlined class="toolbar-icon" />
            </a-tooltip>
          </a-upload>
          <a-tooltip title="快捷回复">
            <ThunderboltOutlined class="toolbar-icon" @click="toggleQuickReply" />
          </a-tooltip>
        </div>
        <div style="position:relative">
          <EmojiPicker :visible="showEmojiPanel" @select="appendEmoji" @close="showEmojiPanel = false" />
        </div>
        <div class="quick-reply-panel" v-if="showQuickReply">
          <div class="quick-reply-header">
            <a-input
              v-model:value="quickReplyKeyword"
              allowClear
              placeholder="搜索快捷回复"
              size="small"
            />
            <a-button size="small" type="link" @click="loadQuickReplies(true)">刷新</a-button>
          </div>
          <a-spin :spinning="quickReplyLoading" size="small">
            <div v-if="filteredQuickReplies.length" class="quick-reply-list">
              <div
                v-for="item in filteredQuickReplies"
                :key="item.id"
                class="quick-reply-item"
                @click="applyQuickReply(item)"
              >
                <div class="quick-reply-title">
                  <span class="quick-reply-title-left">
                    <span>{{ item.title || '无标题' }}</span>
                    <a-tag size="small">{{ item.scope === 'public' ? '公共' : '我的' }}</a-tag>
                    <a-tag v-if="item.shortcutKey" size="small" color="blue">{{ item.shortcutKey }}</a-tag>
                  </span>
                  <span class="quick-reply-title-right">
                    <a-tag v-if="item.msgType === 1" size="small" color="orange">图片</a-tag>
                    <a-tag v-else-if="item.msgType === 2" size="small" color="cyan">文件</a-tag>
                    <a-tag v-else-if="item.msgType === 5" size="small" color="purple">富文本</a-tag>
                  </span>
                </div>
                <a-popover v-if="item.msgType === 1" placement="topLeft" trigger="hover" :overlayStyle="{ maxWidth: '400px' }">
                  <template #content>
                    <img :src="withImageCache(getFileAccessHttpUrl(item.content))" style="max-width: 360px; max-height: 300px; border-radius: 6px; display: block" @error="onImageError" />
                  </template>
                  <div class="quick-reply-content">
                    <img :src="withImageThumbCache(getFileAccessHttpUrl(item.content))" class="quick-reply-img" @error="onImageError" />
                  </div>
                </a-popover>
                <a-popover v-else-if="item.msgType === 5" placement="topLeft" trigger="hover" :overlayStyle="{ maxWidth: '450px' }">
                  <template #content>
                    <div class="quick-reply-preview-richtext" v-html="sanitizeHtml(item.content || '')" v-cse-html></div>
                  </template>
                  <div class="quick-reply-content quick-reply-richtext" v-html="sanitizeHtml(item.content || '')" v-cse-html></div>
                </a-popover>
                <div class="quick-reply-content" v-else-if="item.msgType === 2">
                  <PaperClipOutlined style="margin-right: 4px" />{{ item.content?.split('/').pop() || '文件' }}
                </div>
                <div class="quick-reply-content" v-else>{{ item.content }}</div>
              </div>
            </div>
            <a-empty v-else description="暂无快捷回复" />
          </a-spin>
        </div>
        <div class="attachment-preview" v-if="attachmentList.length">
          <div
            class="attachment-item"
            :class="{ 'is-uploading': item.uploading }"
            v-for="(item, index) in attachmentList"
            :key="`${item.url || item.previewUrl || ''}_${index}`"
          >
            <template v-if="item.type === 'image'">
              <img :src="getAttachmentThumbUrl(item)" @click="!item.uploading && openImagePreviewFromList(attachmentList, item)" @error="onAttachmentImageError($event, item)" />
              <div v-if="item.uploading" class="upload-mask">
                <LoadingOutlined spin />
                <span class="upload-percent">{{ item.progress || 0 }}%</span>
              </div>
              <div
                v-else-if="!item.uploading && isImageFailed(item)"
                class="upload-mask is-failed"
                @click.stop="onAttachmentImageRetry(item)"
                title="点击重试"
              >
                <ReloadOutlined />
                <span class="upload-percent">加载失败</span>
              </div>
            </template>
            <template v-else-if="item.type === 'video'">
              <video v-if="getAttachmentUrl(item)" :src="getAttachmentUrl(item)" @click="!item.uploading && openVideoPreview(item)" />
              <div
                v-else
                class="video-skeleton small"
                :class="{ 'is-failed': isVideoFailed(item) }"
                @click.stop="!item.uploading && onVideoSkeletonClick(item)"
              >
                <PlayCircleOutlined v-if="!isVideoFailed(item)" />
                <ReloadOutlined v-else />
              </div>
              <div v-if="item.uploading" class="upload-mask">
                <LoadingOutlined spin />
                <span class="upload-percent">{{ item.progress || 0 }}%</span>
              </div>
            </template>
            <FileChip
              v-else
              :name="item.name"
              :size="item.size"
              :type="item.type"
              :url="item.url ? getFileAccessHttpUrl(item.url) : ''"
              :uploading="item.uploading"
              :progress="item.progress"
              :downloadable="false"
              click-action="none"
            />
            <CloseOutlined class="remove-attachment" @click="removeAttachment(index)" />
          </div>
        </div>
        <div class="input-wrapper">
          <!--
            原生 textarea + 完全脱离响应式的 input 路径：
              - 不挂 v-model / :value，避免模板 render fn 把 inputMessage 当依赖。
                v-model 形态下，inputMessage 每次变更都触发整个 7610 行模板 render fn 重跑
                （含 100+ 条消息 v-for 的 vnode 生成），实测每个 keystroke 增加 ~300ms，
                长按时主线程被打满，setTimeout / rAF 被饿死，表现为「停下来很久才显示字符」。
              - @input 走 onTextareaInput，把字符存到非响应式 _textBuffer，浏览器立即把字符
                画到 textarea；rAF 节流写 inputMessage / hasInputText，render fn 60Hz 上限。
              - 高度计算交给浏览器原生 CSS field-sizing: content（Chrome 123+），零 JS 成本。
              - emoji / 快捷回复 / sendMessage 清空 / AI 建议替换 等外部写入统一走 setInputText，
                同时把 textarea.value、_textBuffer、inputMessage、hasInputText 一致刷新。
          -->
          <textarea
            ref="inputRef"
            :placeholder="inputPlaceholder"
            rows="1"
            class="ant-input cs-fast-textarea"
            @input="onTextareaInput"
            @keydown="handleInputKeydown"
            @compositionstart="handleInputCompositionStart"
            @compositionend="handleInputCompositionEnd"
            @paste="handlePasteUpload"
          />
        </div>
        <div class="input-footer">
          <span class="input-hint">Enter 发送 / Ctrl+Enter 或 Shift+Enter 换行</span>
          <a-button type="primary" class="send-btn" @click="sendMessage" :disabled="!canSendMessage">
            发送
          </a-button>
        </div>
      </div>
      <div class="chat-ended" v-else>
        <CheckCircleOutlined class="ended-icon" />
        <span>会话已结束</span>
      </div>
      <CsMediaPreviewModals
        :get-thumb-url="getAttachmentThumbUrl"
        :get-url="getAttachmentUrl"
        :on-image-error="onAttachmentImageError"
        :on-image-click="openImagePreviewFromList"
      />
    </div>

    <!-- 空状态 -->
    <CsChatEmptyState v-else />

    <!-- 右侧详情面板 -->
    <div class="detail-panel" v-if="currentConversation && showDetailPanel">
      <div class="panel-header">
        <span>访客信息</span>
        <a-button type="text" size="small" @click="showDetailPanel = false">
          <CloseOutlined />
        </a-button>
      </div>
      
      <div class="panel-body">
        <!-- 基本信息 -->
        <div class="info-section">
          <div class="section-title">基本信息</div>
          <div class="info-item">
            <label>访客ID</label>
            <span class="info-value">{{ currentConversation.userId || '-' }}</span>
          </div>
          <div class="info-item editable" @click="editField('nickname')">
            <label>备注昵称</label>
            <span class="info-value">
              {{ visitorInfo.nickname || '点击添加' }}
              <EditOutlined class="edit-icon" />
            </span>
          </div>
          <div class="info-item editable" @click="editField('realName')">
            <label>真实姓名</label>
            <span class="info-value">
              {{ visitorInfo.realName || '点击添加' }}
              <EditOutlined class="edit-icon" />
            </span>
          </div>
          <div class="info-item editable" @click="editField('phone')">
            <label>手机号</label>
            <span class="info-value">
              {{ visitorInfo.phone || '点击添加' }}
              <EditOutlined class="edit-icon" />
            </span>
          </div>
          <div class="info-item">
            <label>客户等级</label>
            <a-rate v-model:value="visitorInfo.level" :count="3" @change="updateVisitorLevel" />
          </div>
          <div class="info-item">
            <label>星标</label>
            <StarFilled 
              v-if="visitorInfo.star === 1" 
              class="star-btn active" 
              @click="toggleStar" 
            />
            <StarOutlined v-else class="star-btn" @click="toggleStar" />
          </div>
        </div>

        <div v-if="parsedCustomFieldsForVisitorInfo.length" class="info-section">
          <div class="section-title">转人工填写信息</div>
          <div v-for="cf in parsedCustomFieldsForVisitorInfo" :key="cf.label" class="info-item">
            <label style="color: #ff4d4f;">{{ cf.label }}</label>
            <span class="info-value" style="color: #ff4d4f; font-weight: 500;">{{ cf.value }}</span>
          </div>
        </div>

        <!-- 访问信息 -->
        <div class="info-section">
          <div class="section-title">访问信息</div>
          <div class="info-item">
            <label>IP地址</label>
            <span class="info-value">{{ currentConversation.userIp || '-' }}</span>
          </div>
          <div class="info-item">
            <label>IP归属地</label>
            <span class="info-value">
              <EnvironmentOutlined style="margin-right: 4px; color: #1890ff;" />
              {{ formatGeoLocation(currentConversation) }}
            </span>
          </div>
          <div class="info-item">
            <label>操作系统</label>
            <span class="info-value device-info-icon">
              <span v-html="getOsIcon(currentConversation.userOs)" style="margin-right: 4px; display: inline-flex; vertical-align: middle;"></span>
              {{ currentConversation.userOs || '-' }}
              <span v-if="currentConversation.userOsVersion" class="version-text">{{ currentConversation.userOsVersion }}</span>
            </span>
          </div>
          <div class="info-item">
            <label>浏览器</label>
            <span class="info-value device-info-icon">
              <span v-html="getBrowserIcon(currentConversation.userBrowser)" style="margin-right: 4px; display: inline-flex; vertical-align: middle;"></span>
              {{ currentConversation.userBrowser || '-' }}
              <span v-if="currentConversation.userBrowserVersion" class="version-text">{{ formatBrowserVersion(currentConversation.userBrowserVersion) }}</span>
            </span>
          </div>
          <div class="info-item" v-if="currentConversation.userDeviceId">
            <label>设备码</label>
            <span class="info-value" style="font-family: monospace; font-size: 12px;">{{ currentConversation.userDeviceId }}</span>
          </div>
          <div class="info-item">
            <label>浏览器语言</label>
            <span class="info-value">
              <GlobalOutlined style="margin-right: 4px; color: #1890ff;" />
              {{ currentConversation.userLang || '-' }}
            </span>
          </div>
          <div class="info-item">
            <label>来源</label>
            <span class="info-value">{{ currentConversation.source || '直接访问' }}</span>
          </div>
          <div class="info-item">
            <label>首次访问</label>
            <span class="info-value">{{ formatDateTime(visitorInfo.firstVisitTime) }}</span>
          </div>
          <div class="info-item">
            <label>访问次数</label>
            <span class="info-value">{{ visitorInfo.visitCount || 1 }} 次</span>
          </div>
        </div>

        <!-- 会话信息 -->
        <div class="info-section">
          <div class="section-title">会话信息</div>
          <div class="info-item">
            <label>会话状态</label>
            <a-tag :color="getStatusColor(currentConversation.status)">
              {{ getStatusName(currentConversation.status) }}
            </a-tag>
          </div>
          <div class="info-item">
            <label>回复模式</label>
            <a-tag :color="getModeColor(currentReplyMode)">
              {{ getModeName(currentReplyMode) }}
            </a-tag>
          </div>
          <div class="info-item">
            <label>创建时间</label>
            <span class="info-value">{{ formatDateTime(currentConversation.createTime) }}</span>
          </div>
          <div class="info-item">
            <label>消息数</label>
            <span class="info-value">{{ currentConversation.messageCount || 0 }} 条</span>
          </div>
        </div>

        <!-- 拉黑控制 -->
        <div class="info-section">
          <div class="section-title">拉黑控制</div>
          <div class="info-item">
            <label>用户ID</label>
            <span class="info-value">
              <a-switch
                :checked="userIdBlacklisted"
                :loading="blacklistLoading"
                :disabled="!currentConversation.userId"
                checked-children="已拉黑"
                @change="(checked) => checked ? openBanModal('user') : removeBlacklist('user')"
              />
            </span>
          </div>
          <div class="info-item">
            <label>IP</label>
            <span class="info-value">
              <a-switch
                :checked="ipBlacklisted"
                :loading="blacklistLoading"
                :disabled="!currentConversation.userIp || isColleagueReadonly"
                checked-children="已拉黑"
                @change="(checked) => checked ? openBanModal('ip') : removeBlacklist('ip')"
              />
            </span>
          </div>
        </div>

        <!-- 拉黑原因弹窗 -->
        <CsBlacklistModal
          v-model:open="banModalVisible"
          :type="banModalType"
          :default-ip="currentConversation?.userIp || ''"
          :visitor-label="currentConversation?.userName || currentConversation?.userId || ''"
          @confirm="onBlacklistConfirm"
        />

        <!-- 备注 -->
        <div class="info-section">
          <div class="section-title">
            备注
            <a-button type="link" size="small" @click="editField('notes')">编辑</a-button>
          </div>
          <div class="notes-content">
            {{ visitorInfo.notes || '暂无备注，点击编辑添加...' }}
          </div>
        </div>

        <!-- 标签 -->
        <div class="info-section">
          <div class="section-title">标签</div>
          <div class="tags-wrapper">
            <a-tag 
              v-for="tag in visitorTags" 
              :key="tag" 
              closable 
              @close="removeTag(tag)"
            >
              {{ tag }}
            </a-tag>
            <a-input
              v-if="showTagInput"
              ref="tagInputRef"
              v-model:value="newTag"
              size="small"
              style="width: 80px"
              @blur="addTag"
              @keyup.enter="addTag"
            />
            <a-tag v-else class="add-tag" @click="showTagInput = true">
              <PlusOutlined /> 添加
            </a-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 移交弹窗 -->
    <CsTransferConversationModal
      v-model:open="showTransferModal"
      :loading="transferLoading"
      :agents="availableAgents"
      @transfer="doTransfer"
    />

    <!-- 编辑字段弹窗 -->
    <CsVisitorFieldEditModal
      v-model:open="showEditModal"
      v-model:value="editValue"
      :field="editingField"
      @save="saveEditField"
    />

  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'CsWorkbench' });
import { ref, computed, unref, onMounted, onUnmounted, onActivated, onDeactivated, watch, nextTick, provide } from 'vue';
import { CS_WORKBENCH_CONTEXT_KEY, type CsWorkbenchContext, type CsWorkbenchSettings } from './context';
import { useCsWorkbenchTheme } from './composables/useCsWorkbenchTheme';
import CsAgentBar from './components/CsAgentBar.vue';
import CsWsStatusBanner from './components/CsWsStatusBanner.vue';
import CsChatHeader from './components/CsChatHeader.vue';
import CsChatEmptyState from './components/CsChatEmptyState.vue';
import CsWorkbenchSettingsDrawer from './components/CsWorkbenchSettingsDrawer.vue';
import CsTransferConversationModal from './components/CsTransferConversationModal.vue';
import CsBlacklistModal from './components/CsBlacklistModal.vue';
import CsVisitorFieldEditModal from './components/CsVisitorFieldEditModal.vue';
import { usePageContext } from '/@/hooks/component/usePageContext';
import { message } from 'ant-design-vue';
import { 
  StarFilled, StarOutlined,
  CloseOutlined, EditOutlined, PlusOutlined, InboxOutlined,
  SmileOutlined, ThunderboltOutlined, RobotOutlined,
  MoreOutlined, DeleteOutlined, PaperClipOutlined, EnvironmentOutlined, GlobalOutlined,
  TeamOutlined, CaretRightOutlined, CaretDownOutlined, DownOutlined, SearchOutlined,
  CheckCircleOutlined, UndoOutlined,
  LoadingOutlined, PlayCircleOutlined,
  CustomerServiceOutlined, ReloadOutlined
} from '@ant-design/icons-vue';
import { useRoute, useRouter } from 'vue-router';
import { defHttp } from '/@/utils/http/axios';
import { useGlobSetting } from '/@/hooks/setting';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { getBrandSetting } from '/@/settings/brandSetting';
import { resolveBrandPublicUrl } from '/@/utils/brand';
import { createImgPreview } from '/@/components/Preview';
import { getToken } from '/@/utils/auth';
import EmojiPicker from '../components/EmojiPicker.vue';
import { computeFileMd5 } from '../utils/fileHash';
import { encryptTransport, decryptTransport, decryptMessage } from '../utils/csEncrypt';
import { playCsNotificationSound, CS_NOTIFY_MAX_GAIN } from '../utils/csNotificationSound';
import { withImageCache, withImageCacheAsync, preloadImages, onImageError, getCachedChatWindowConfig, setCachedChatWindowConfig } from '../utils/csImageCache';
import {
  withMediaCache,
  releaseAllMedia,
  warmupAvatars,
  withImageThumbCache,
  isImageReady,
  retryMedia,
  getMediaFailureState,
  retryImage,
  getImageFailureState,
} from '/@/utils/file/imageCache';
import { isCseUrl } from '/@/utils/cse/cseUrl';
import { compressImage } from '/@/utils/file/compressImage';
import FileChip from '../components/FileChip.vue';
import { vCseHtml } from '../utils/cseHtmlImg';
import { storeToRefs } from 'pinia';
import { useUserStoreWithOut } from '/@/store/modules/user';
import { useCsStore } from '/@/store/modules/cs';
import {
  stripHtmlTags,
  buildMessagePreview,
  renderStreamingText,
  sanitizeHtml,
  renderMessage,
  renderMarkdown,
} from './render/csMessageRender';
import { useCsMessageMedia } from './composables/useCsMessageMedia';
import CsMediaPreviewModals from './components/CsMediaPreviewModals.vue';

const userStore = useUserStoreWithOut();
const csStore = useCsStore();
// 持续响铃 / 访客等待相关状态（state owner = csStore）：
//   - state ref 用 storeToRefs 拿到 Ref，传给 csWorkbenchSettings 满足 Ref<...> 契约 + v-model 绑定。
//   - computed 用本地 computed wrapper（storeToRefs 对 setup-store computed 返回联合类型，
//     无法直接赋给 context.ts 中 ComputedRef<boolean>）。
const {
  continuousRingMode: csContinuousRingMode,
  ringStopCondition: csRingStopCondition,
  ringIntervalSeconds: csRingIntervalSeconds,
  pauseRemainSeconds: csPauseRemainSeconds,
} = storeToRefs(csStore);
const csContinuousRingActive = computed(() => csStore.continuousRingActive);
const csIsRingPaused = computed(() => csStore.isRingPaused);
const silentRequestOptions = { successMessageMode: 'none' as const };
const globSetting = useGlobSetting();
const route = useRoute();
const router = useRouter();

// ==================== 皮肤主题系统 ====================
// 主题预设、状态、持久化逻辑已迁出至：
//   ./theme/presets.ts
//   ./composables/useCsWorkbenchTheme.ts
// 设置抽屉内的主题选择 UI 在 components/CsWorkbenchSettingsDrawer.vue
const { themeVars } = useCsWorkbenchTheme();

/** 与布局顶栏/多标签占位一致的可视高度（关标签等会自动变），无注入时走样式表 fallback */
const { contentHeight } = usePageContext();
const workbenchLayoutStyle = computed(() => {
  const h = contentHeight != null ? unref(contentHeight) : null;
  if (h == null || h <= 0) {
    return {};
  }
  const px = `${h}px`;
  return {
    height: px,
    maxHeight: px,
  };
});

function httpGet<T = any>(config: any, options: any = {}) {
  return defHttp.get<T>(config, { ...silentRequestOptions, ...options });
}
function httpPost<T = any>(config: any, options: any = {}) {
  return defHttp.post<T>(config, { ...silentRequestOptions, ...options });
}
function httpPut<T = any>(config: any, options: any = {}) {
  return defHttp.put<T>(config, { ...silentRequestOptions, ...options });
}
function httpDelete<T = any>(config: any, options: any = {}) {
  return defHttp.delete<T>(config, { ...silentRequestOptions, ...options });
}
function decryptApiResponse(rawData: any): any {
  if (typeof rawData !== 'string') return rawData;
  const decrypted = decryptTransport(rawData);
  if (typeof decrypted === 'string') {
    try { return JSON.parse(decrypted); } catch { return decrypted; }
  }
  return decrypted;
}

// 聊天窗口配置 Logo（优先级高于品牌配置 Logo）
// 从 localStorage 同步恢复，避免首帧显示默认 logo 后跳变
const chatWindowLogo = ref(getCachedChatWindowConfig()?.logo || '');
const chatWindowSettings = ref<any>({});

// 客服信息
const agentId = ref('');
const agentName = ref('');
const agentAvatar = ref('');
const agentStatus = ref(0);
const isOnline = ref(false);
const agentRole = ref(0); // 0-普通客服, 1-管理者
const isSupervisor = computed(() => agentRole.value === 1);
/**
 * 越权防护：非管理者客服仅能操作自己拥有的会话。
 *  - 管理者(role=1)始终可写；
 *  - 无 currentConversation 时返回 false，不影响默认 UI；
 *  - 当前会话 ownerAgentId 与本人 agentId 不一致时只读，
 *    覆盖 monitor tab 残留、tab 切换、转接前后等所有场景。
 */
const isColleagueReadonly = computed(() => {
  if (isSupervisor.value) return false;
  const conv = currentConversation.value;
  if (!conv) return false;
  const owner = conv.ownerAgentId;
  return !!owner && owner !== agentId.value;
});
const keepConnectionOnDeactivate = true;

// AI应用选择
const selectedAppId = ref<string | undefined>(undefined);  // 回复建议应用
const visitorAppId = ref<string | undefined>(undefined);   // 访客AI应用
const aiAppList = ref<any[]>([]);
const showSettingsDrawer = ref(false);
const SOUND_STORAGE_KEY = 'cs_workbench_sound_enabled';
const SOUND_VOLUME_STORAGE_KEY = 'cs_workbench_sound_volume';
function readSoundVolumePercent(): number {
  const raw = localStorage.getItem(SOUND_VOLUME_STORAGE_KEY);
  const n = raw != null ? parseInt(raw, 10) : NaN;
  if (!Number.isFinite(n)) return 100;
  return Math.max(0, Math.min(200, n));
}
// 声音偏好统一在 csStore 维护（同时持久化到 localStorage），
// 工作台保留同名 ref/setter 以保持现有 UI 双向绑定语义
const soundEnabled = computed({
  get: () => csStore.soundEnabled,
  set: (v: boolean) => {
    csStore.setSoundEnabled(v);
    try {
      localStorage.setItem(SOUND_STORAGE_KEY, String(v));
    } catch (_) {
      /* ignore */
    }
  },
});
const soundVolumePercent = computed({
  get: () => csStore.soundVolumePercent,
  set: (v: number) => {
    const n = Math.max(0, Math.min(200, v | 0));
    csStore.setSoundVolume(n);
    try {
      localStorage.setItem(SOUND_VOLUME_STORAGE_KEY, String(n));
    } catch (_) {
      /* ignore */
    }
  },
});
// 兼容历史：从工作台旧 key 读取一次同步到 csStore（若 csStore 已有 cs.* key 则跳过）
(function syncLegacySoundPrefs() {
  try {
    const legacyEnabled = localStorage.getItem(SOUND_STORAGE_KEY);
    if (legacyEnabled !== null) {
      csStore.setSoundEnabled(legacyEnabled !== 'false');
    }
    const legacyVolume = localStorage.getItem(SOUND_VOLUME_STORAGE_KEY);
    if (legacyVolume !== null) {
      const n = parseInt(legacyVolume, 10);
      if (Number.isFinite(n)) csStore.setSoundVolume(Math.max(0, Math.min(200, n)));
    }
  } catch (_) {
    /* ignore */
  }
})();
const soundVolumeSliderMarks: Record<number, string> = { 0: '0%', 100: '100%', 200: '200%' };
const soundVolumeTooltip = { formatter: (v?: number) => (v != null ? `${v}%` : '') };
function onSoundEnabledChange(val: boolean) {
  try {
    localStorage.setItem(SOUND_STORAGE_KEY, String(val));
  } catch (_) {
    /* ignore */
  }
}
const aiEnabled = ref(true);  // AI自动回复开关
const aiPrologueEnabled = ref(true); // AI开场白开关

// 客服超时未回复配置
const agentTimeoutConfig = ref({ enabled: false, seconds: 20 });
// 访客等待回复时长（conversationId -> 等待秒数），每秒刷新（UI 私有状态）
// 数据源已切到 csStore.visitorLastMsgTime（background 接管访客等待全局状态）。
const visitorWaitingSeconds = ref<Record<string, number>>({});
const timeoutNotifiedSet = new Set<string>();
let waitingTimerHandle: ReturnType<typeof setInterval> | null = null;

// 音频上下文（声明上移至此，便于持续响铃 composable 在初始化时引用）
let audioCtx: AudioContext | null = null;
let lastSoundTime = 0;
const SOUND_THROTTLE_MS = 1500;

// 会话列表
const filter = ref('mine');
const conversations = ref<any[]>([]);
const currentConversation = ref<any>(null);
const currentReplyMode = ref(0);

// 会话搜索
const searchKeyword = ref('');
const displayConversations = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return conversations.value;
  return conversations.value.filter(conv => {
    const name = (conv.visitorNickname || conv.userName || conv.userId || '').toLowerCase();
    const msg = (conv.lastMessage || '').toLowerCase();
    return name.includes(kw) || msg.includes(kw);
  });
});

// 回到底部按钮
const showScrollToBottom = ref(false);

// 统计
// 统计数据（从后端获取）
const statsData = ref({ myCount: 0, unassignedCount: 0, closedCount: 0, colleagueCount: 0 });
const myCount = computed(() => statsData.value.myCount);
const _unassignedCount = computed(() => statsData.value.unassignedCount); // 待接入标签已隐藏
const _closedCount = computed(() => statsData.value.closedCount); // 已结束标签已隐藏
// 「同事会话」徽标：来自后端 stats.colleagueCount（其他客服正在处理且未关闭，不含自己/未分配），
// 不依赖当前 tab，始终展示
const colleagueCount = computed(() => statsData.value.colleagueCount);

// ============ 监控模式：按客服分组 ============
const monitorAgentList = ref<any[]>([]); // 监控模式下的所有客服列表
const expandedAgents = ref<Set<string>>(new Set()); // 展开的客服ID集合

// 监控模式下按客服分组的对话列表
const monitorGroups = computed(() => {
  if (filter.value !== 'monitor') return [];
  
  // 按 ownerAgentId 分组对话
  const convByAgent = new Map<string, any[]>();
  for (const conv of conversations.value) {
    const aid = conv.ownerAgentId || '__unassigned__';
    if (!convByAgent.has(aid)) {
      convByAgent.set(aid, []);
    }
    convByAgent.get(aid)!.push(conv);
  }
  
  // 构建分组列表：有对话的客服 + 无对话的客服都要展示
  const groups: any[] = [];
  const agentMap = new Map<string, any>();
  for (const agent of monitorAgentList.value) {
    agentMap.set(agent.id, agent);
  }
  
  // 先添加有对话的客服（排除自己，按在线状态排序：在线 > 隐身 > 离线）
  for (const agent of monitorAgentList.value) {
    // 监控模式不显示自己
    if (agent.id === agentId.value) {
      convByAgent.delete(agent.id);
      continue;
    }
    const convs = convByAgent.get(agent.id) || [];
    groups.push({
      agent,
      conversations: convs,
      expanded: expandedAgents.value.has(agent.id),
    });
    convByAgent.delete(agent.id);
  }
  
  // 处理有对话但不在客服列表中的情况（如已删除客服的残留对话）
  if (convByAgent.has('__unassigned__')) {
    groups.push({
      agent: { id: '__unassigned__', nickname: '未分配', status: -1, currentSessions: 0, maxSessions: 0 },
      conversations: convByAgent.get('__unassigned__')!,
      expanded: expandedAgents.value.has('__unassigned__'),
    });
  }
  for (const [aid, convs] of convByAgent) {
    if (aid === '__unassigned__') continue;
    groups.push({
      agent: { id: aid, nickname: agentMap.get(aid)?.nickname || `客服(${aid.substring(0, 6)})`, status: -1, currentSessions: convs.length, maxSessions: 0 },
      conversations: convs,
      expanded: expandedAgents.value.has(aid),
    });
  }
  
  // 排序：未分配排第一，然后在线优先，再按对话数量降序
  groups.sort((a, b) => {
    if (a.agent.id === '__unassigned__') return -1;
    if (b.agent.id === '__unassigned__') return 1;
    const statusOrder = (s: number) => s === 1 ? 0 : s === 2 ? 1 : s === 3 ? 2 : 3;
    const sa = statusOrder(a.agent.status);
    const sb = statusOrder(b.agent.status);
    if (sa !== sb) return sa - sb;
    return (b.conversations.length) - (a.conversations.length);
  });
  
  return groups;
});

function toggleAgentExpand(agentId: string) {
  const newSet = new Set(expandedAgents.value);
  if (newSet.has(agentId)) {
    newSet.delete(agentId);
  } else {
    newSet.add(agentId);
  }
  expandedAgents.value = newSet;
}

function getAgentStatusText(status: number) {
  switch (status) {
    case 1: return '在线';
    case 2: return '忙碌';
    case 3: return '隐身';
    default: return '离线';
  }
}

// 消息
const messages = ref<any[]>([]);
const historyPageSize = 100;
const loadingHistory = ref(false);
const hasMoreHistory = ref(true);
const historyBeforeId = ref<string | null>(null);
const displayMessages = computed(() => {
  const list: any[] = [];
  let lastDateKey = '';
  for (const msg of messages.value) {
    if (msg.status === 3) continue;
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
// 输入框文本：保留为 ref 是为了让原有业务函数（sendMessage / appendEmoji / 快捷回复 / AI 建议）
// 不需要全部改写，仍可通过 inputMessage.value 读写。
//
// 但模板里【绝不再】用 v-model 或 :value 引用它，避免变成 render fn 的响应式依赖。
// 7610 行模板 + 100+ 条消息 v-for + 大量 getMediaGridData / isAttachmentImageReady 等函数调用，
// 一次 render fn 重跑成本约 300ms。长按按键 30Hz 触发时，主线程被打满，setTimeout / rAF 被饿死，
// 用户体感是「松开按键过很久才看到字符」。
//
// 改造后：textarea 通过 inputRef 持有原生 DOM，@input 走 onTextareaInput，把字符存到非响应式
// _textBuffer。canSendMessage 不再依赖 inputMessage，改依赖按 rAF 节流更新的 hasInputText。
// 这样长按时模板 render fn 仅在「有字 / 无字」边界变化时跑一次，绝大多数 keystroke 零开销。
const inputMessage = ref('');
let _textBuffer = '';
const hasInputText = ref(false);
let _hasInputTextRafScheduled = false;
function _scheduleHasInputTextSync() {
  if (_hasInputTextRafScheduled) return;
  _hasInputTextRafScheduled = true;
  requestAnimationFrame(() => {
    _hasInputTextRafScheduled = false;
    const next = _textBuffer.trim().length > 0;
    // 仅在「有字 / 无字」边界翻转时写 ref，避免 rAF 内每帧都向 Vue 调度器投递无效任务。
    // inputMessage ref 故意不在 rAF 里同步：它在长按场景下没有任何模板/computed 订阅者，
    // 写入只是空 trigger 但仍会让 Vue scheduler 走一遍 flushJobs 清空 dirty 队列。
    // 临界路径（sendMessage / 失败回滚 / emoji / 快捷回复）会通过 flushInputBuffer / setInputText
    // 主动把 _textBuffer 同步到 ref，业务读取一致性不受影响。
    if (hasInputText.value !== next) hasInputText.value = next;
  });
}
function flushInputBuffer() {
  // 业务进入临界路径（sendMessage 等）前手动同步一次，避免 rAF 节流尾端丢字符。
  if (inputMessage.value !== _textBuffer) inputMessage.value = _textBuffer;
  const next = _textBuffer.trim().length > 0;
  if (hasInputText.value !== next) hasInputText.value = next;
}
function onTextareaInput(e: Event) {
  // 长按 / IME 高频派发场景：只写非响应式 buffer，浏览器立即把字符画到 textarea；
  // 仅 hasInputText 边界变化时通过 rAF 节流通知 Vue，render fn 几乎不会被惊动。
  _textBuffer = (e.target as HTMLTextAreaElement).value;
  _scheduleHasInputTextSync();
}
function setInputText(text: string) {
  // emoji 插入 / 快捷回复填充 / sendMessage 清空 / AI 建议替换 等外部写入，
  // 必须同步刷新 textarea DOM（用户立刻看到）+ 同步刷 ref 和 hasInputText（业务读到一致状态）。
  _textBuffer = text || '';
  if (inputRef.value) inputRef.value.value = _textBuffer;
  inputMessage.value = _textBuffer;
  hasInputText.value = _textBuffer.trim().length > 0;
}
// 发送闸门：sendMessage 一旦进入业务流程就置 true，httpPost 完成（成功/失败）才落回 false。
// 用于：
//   1) handleInputKeydown / sendMessage 入口防重入（按住 Enter 不会把同一段文本打入消息流多次）
//   2) canSendMessage 计算 disabled，发送按钮在请求未回时不可再次点击
//   3) IME 拼音确认导致的额外 Enter 也会被拒绝
const sending = ref(false);
// IME 拼音/选词中：浏览器在确认候选词那一刻仍会派发 keydown.Enter，
// 用本地标志 + e.isComposing 双保险，避免把"上屏候选词"误当作"发送指令"。
let _imeComposing = false;
const attachmentList = ref<any[]>([]);
const uploadFileList = ref<any[]>([]);
const csMediaApi = useCsMessageMedia();
const showEmojiPanel = ref(false);
const messagesRef = ref<HTMLElement | null>(null);
const inputRef = ref();
const messageAvatarSize = 38;

// 流式AI消息临时存储 (messageId -> 累积内容)
const streamingMessages = ref<Map<string, string>>(new Map());

// RAF 批处理缓冲区
const pendingTokens = new Map<string, { tokens: string[]; conversationId: string }>();
let tokenRafId: number | null = null;
let scrollRafId: number | null = null;
let pendingSuggestionTokens: string[] = [];
let suggestionRafId: number | null = null;

// 会话切换序列号，用于丢弃过期的异步请求结果
let switchSeq = 0;

// 访客信息
const visitorInfo = ref<any>({});
const visitorTags = ref<string[]>([]);
const showDetailPanel = ref(true);

// 解析当前会话的自定义字段
const parsedCustomFields = computed(() => {
  const all = parseCustomFieldsRaw(currentConversation.value?.customFields);
  return filterFieldsByLocation(all, 'showInHeader');
});
const parsedCustomFieldsForVisitorInfo = computed(() => {
  const all = parseCustomFieldsRaw(currentConversation.value?.customFields);
  return filterFieldsByLocation(all, 'showInHistory');
});
const userOnline = ref(false);
const userIdBlacklisted = ref(false);
const ipBlacklisted = ref(false);
const blacklistLoading = ref(false);
const banModalVisible = ref(false);
const banModalType = ref<'user' | 'ip'>('user');
const savedScrollTop = ref<number | null>(null);

// 访客信息缓存 (key -> visitorInfo)
const visitorCache = new Map<string, any>();

function getVisitorCacheKey(appId: string | undefined, userId: string | undefined) {
  if (!userId) {
    return '';
  }
  return appId ? `${appId}_${userId}` : userId;
}

// 标签编辑
const showTagInput = ref(false);
const newTag = ref('');
const tagInputRef = ref();

// 字段编辑
// editModalTitle 与 textarea/input 切换逻辑已迁入 components/CsVisitorFieldEditModal.vue
const showEditModal = ref(false);
const editingField = ref('');
const editValue = ref('');

// 回复建议
const aiSuggestion = ref('');
const aiSuggestionLoading = ref(false);
const aiSuggestionDismissed = ref(false);  // 忽略标记，阻止后续流式消息

// 弹窗
const showTransferModal = ref(false);
const showQuickReply = ref(false);
const availableAgents = ref<any[]>([]);
const transferLoading = ref(false);
const quickReplyList = ref<any[]>([]);
const quickReplyLoading = ref(false);
const quickReplyLoaded = ref(false);
const quickReplyKeyword = ref('');
const lastConversationStorageKey = 'cs_last_conversation_id';
let clearUnreadTimer: number | null = null;
let messagesEl: HTMLElement | null = null;
const conversationsCache = new Map<string, any[]>();
const conversationsCacheTime = new Map<string, number>();
let conversationsRequestSeq = 0;
const extraCache = new WeakMap<any, { key: any; value: any }>();
const mediaGridCache = new WeakMap<any, { key: any; value: { items: any[]; extraCount: number; total: number } }>();
// renderCache / maxRenderCacheSize 已迁入 ./render/csMessageRender.ts

// WebSocket
let ws: WebSocket | null = null;
let refreshTimer: number | null = null;
let wsReconnectTimer: number | null = null;
let wsManuallyClosed = false;
const hasMounted = ref(false);
const isActivating = ref(false);
const loadingConversations = ref(false);
let wsHeartbeatTimer: number | null = null;
let wsHealthTimer: number | null = null;
let wsFallbackPollTimer: number | null = null;
let wsReconnectAttempts = 0;
let lastWsMessageAt = 0;
let lastMessageLoadAt = 0;
const wsHeartbeatIntervalMs = 15000;
// P3：fallback 间隔随 WS 状态动态化。健康状态 30s 兜底（WS 实时推送），
// 不在线/重连状态 5s 加密感知。阈值固定 5s 与间隔解耦。
const wsFallbackHealthyMs = 30000;
const wsFallbackDegradedMs = 5000;
const wsFallbackTriggeredAfterWsMs = 5000;
// P2：fallback 内 conv.lastMessageTime 与本地末条/lastMessageLoadAt 的比对容差，
// 用于吸收前后端 NTP 漂移、避免误触发整体 loadMessages。
const messageStaleToleranceMs = 3000;
// WS 状态由 CsBackgroundService 写入 csStore，这里以 computed 暴露给 banner UI
const wsStatus = computed({
  get: () => csStore.wsStatus as 'connected' | 'connecting' | 'reconnecting' | 'disconnected',
  set: (v) => csStore.setWsStatus(v),
});
const wsShowBanner = computed({
  get: () => csStore.wsShowBanner,
  set: (v) => csStore.setWsBanner(v),
});
const wsReconnectCountdown = computed({
  get: () => csStore.wsReconnectCountdown,
  set: (v) => csStore.setWsReconnectCountdown(v),
});
let hasConnectedOnce = false;
let wsConnectedBannerTimer: number | null = null;
let wsCountdownTimer: number | null = null;

const handleVisibilityChange = () => {
  if (!hasMounted.value) return;
  if (document.hidden) return;
  // ws 健康探测已迁出至 CsBackgroundService，这里只关心当前会话 UI 副作用
  if (currentConversation.value?.id && lastWsMessageAt && Date.now() - lastWsMessageAt > 30000) {
    loadMessages(currentConversation.value.id);
  }
  nextTick(() => scrollToBottom());
};

const handleElectronNavigate = async (e: Event) => {
  const { query } = (e as CustomEvent).detail;
  const conversationId = query?.conversationId;
  if (!conversationId) return;
  if (currentConversation.value?.id === conversationId) {
    nextTick(() => scrollToBottom());
    return;
  }
  let targetConv = conversations.value.find(c => c.id === conversationId);
  if (!targetConv) {
    await loadConversations();
    targetConv = conversations.value.find(c => c.id === conversationId);
  }
  if (targetConv) {
    selectConversation(targetConv);
  }
};

// network online / beforeunload / app-logout 全部由 CsBackgroundService 统一处理
const handleNetworkOnline = () => { /* moved to CsBackgroundService */ };
const handleBeforeUnload = () => { /* moved to CsBackgroundService */ };
const handleAppLogout = () => {
  // 工作台只负责自身 UI 状态重置，ws/store 重置由 CsBackgroundService 处理
  agentId.value = '';
  hasMounted.value = false;
  isActivating.value = false;
};

// 跨菜单通知点击：在工作台内尝试切换到目标会话
function handleNotificationClick(payload: any) {
  const id = payload?.conversationId;
  if (!id) return;
  const found = conversations.value.find((c: any) => c.id === id);
  if (found) selectConversation(found);
}

function closeWebSocket() {
  wsManuallyClosed = true;
  stopWsHeartbeat();
  stopWsHealthCheck();
  // 修复：主动关闭 ws 时一并停止 fallback poll，
  // 避免 logout 后 setup 闭包 timer 残留导致旧 agentId/旧 ws 状态污染新组件实例。
  stopFallbackPoll();
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  if (ws) {
    try {
      ws.close();
    } catch {
      // 忽略关闭异常
    }
  }
  ws = null;
  wsShowBanner.value = false;
  stopWsCountdown();
  if (wsConnectedBannerTimer) { clearTimeout(wsConnectedBannerTimer); wsConnectedBannerTimer = null; }
}

function stopWsHeartbeat() {
  if (wsHeartbeatTimer) {
    clearInterval(wsHeartbeatTimer);
    wsHeartbeatTimer = null;
  }
}

function stopWsHealthCheck() {
  if (wsHealthTimer) {
    clearInterval(wsHealthTimer);
    wsHealthTimer = null;
  }
}

function stopFallbackPoll() {
  if (wsFallbackPollTimer) {
    // P3：改用 setTimeout 自驱动，对应清理也改成 clearTimeout
    clearTimeout(wsFallbackPollTimer);
    wsFallbackPollTimer = null;
  }
}

// P3：用 setTimeout 自驱动替代 setInterval。
//   - 进入 tick 立即把 timer 置 null，防止 stop / 重入冲突
//   - finally 统一调度下一轮，所有早返回路径都能拿到下次 timer
//   - 间隔依据 WS 状态动态选择，阈值（5s）固定不变
function startFallbackPoll() {
  stopFallbackPoll();
  const tick = async () => {
    wsFallbackPollTimer = null;
    try {
      if (document.hidden) return;
      if (!agentId.value) {
        try {
          await loadAgentInfo();
          if (agentId.value) {
            connectWebSocket();
            await loadConversations();
          }
        } catch { /* ignore */ }
        return;
      }
      if (loadingConversations.value) return;
      if (ws && ws.readyState === WebSocket.OPEN && lastWsMessageAt) {
        if (Date.now() - lastWsMessageAt < wsFallbackTriggeredAfterWsMs) {
          return;
        }
      }
      await loadConversations();
      const currentId = currentConversation.value?.id;
      if (!currentId) return;
      const conv = conversations.value.find(c => c.id === currentId);
      if (!conv?.lastMessageTime) return;
      // P2：用本地消息流末条 createTime 与 lastMessageLoadAt 取较大者
      // 与 conv.lastMessageTime 比对，超过容差才整体重载消息流。
      const localLast = messages.value[messages.value.length - 1];
      const localLastTime = localLast?.createTime ? new Date(localLast.createTime).getTime() : 0;
      const remoteLastTime = new Date(conv.lastMessageTime).getTime();
      if (remoteLastTime - Math.max(localLastTime, lastMessageLoadAt) > messageStaleToleranceMs) {
        await loadMessages(currentId);
      }
    } catch {
      // 忽略轮询失败，下次 tick 仍会被 finally 调度
    } finally {
      const nextDelay = (ws && ws.readyState === WebSocket.OPEN)
        ? wsFallbackHealthyMs
        : wsFallbackDegradedMs;
      wsFallbackPollTimer = window.setTimeout(tick, nextDelay);
    }
  };
  // 首次延迟用降级值（5s），让 onMounted 后较快感知一次
  wsFallbackPollTimer = window.setTimeout(tick, wsFallbackDegradedMs);
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

function startWsHeartbeat() {
  stopWsHeartbeat();
  wsHeartbeatTimer = window.setInterval(() => {
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    try {
      ws.send(JSON.stringify({ type: 'ping', ts: Date.now() }));
    } catch {
      // 发送失败，触发重连
      try {
        ws?.close();
      } catch {
        // ignore
      }
    }
  }, wsHeartbeatIntervalMs);
}

function startWsHealthCheck() {
  stopWsHealthCheck();
  wsHealthTimer = window.setInterval(() => {
    if (!ws) return;
    if (ws.readyState !== WebSocket.OPEN) {
      scheduleWsReconnect();
      return;
    }
  }, wsHeartbeatIntervalMs);
}

// 计算属性
const inputPlaceholder = computed(() => {
  if (currentReplyMode.value === 0) {
    return 'AI自动回复中，发送消息将切换为手动模式';
  }
  return '输入消息，Enter发送';
});

const filteredQuickReplies = computed(() => {
  const keyword = quickReplyKeyword.value.trim().toLowerCase();
  if (!keyword) {
    return quickReplyList.value;
  }
  return quickReplyList.value.filter(item => {
    const title = (item.title || '').toLowerCase();
    const content = (item.content || '').toLowerCase();
    return title.includes(keyword) || content.includes(keyword);
  });
});

// Electron 通知点击后通过 route query 定位会话（同页面内 query 变化时触发，跨页面跳转由 onActivated 处理）
watch(() => route.query.conversationId, async (newId) => {
  if (!newId || typeof newId !== 'string') return;
  if (route.path !== '/cs/workbench') return;
  await nextTick();
  let targetConv = conversations.value.find(c => c.id === newId);
  if (!targetConv) {
    await loadConversations();
    targetConv = conversations.value.find(c => c.id === newId);
  }
  if (targetConv) {
    selectConversation(targetConv);
  }
  router.replace({ query: {} });
});

// ============ 持续响铃 ============
// 持续响铃实例已迁至 CsBackgroundService（路径 A），状态归 csStore.continuousRingMode 等。
// workbench 仅做：
//   1. 通过 csWorkbenchSettings 暴露 csStore.* 给设置抽屉读写（v-model 自动绑定到 store）。
//   2. 业务回执场景（如 handleSendMessage 客服自发回复）调 csStore.dequeueContinuousRing 即时停响。
//   3. 订阅 csStore.events.on('visitor_waiting_cleared') 同步清除本地 UI 私有状态。

// 初始化
onMounted(async () => {
  await loadAgentInfo();          // 获取 agentId（CsBackgroundService 与本组件并行加载，幂等）
  // WebSocket 连接由 CsBackgroundService 全局接管：
  //   - 工作台 mount 时不再 connectWebSocket / startFallbackPoll；
  //   - 通过 csStore.events 订阅 ws 消息与 banner 状态；
  //   - 多页签关闭时背景服务仍能持续接收消息，触发菜单角标 + 跨菜单通知。
  await Promise.all([
    loadAiAppList(),
    loadAiEnabled(),              // 加载AI开关状态
    loadAiPrologueEnabled(),      // 加载AI开场白开关状态
    loadGlobalVisitorApp(),       // 加载全局访客AI应用配置
    loadAgentTimeoutConfig(),     // 加载客服超时未回复配置
    loadChatWindowSettings(),     // 加载聊天窗口配置
    loadConversations(),
    loadQuickReplies(),           // 预加载快捷回复（快捷键匹配需要）
  ]);
  startWaitingTimer();             // 启动访客等待时长计时器
  hasMounted.value = true;

  document.addEventListener('visibilitychange', handleVisibilityChange);
  window.addEventListener('electron-navigate', handleElectronNavigate);
  // online / beforeunload / app-logout 全部由 CsBackgroundService 接管，工作台不再监听

  // 订阅背景服务的 ws 消息（替代原 workbench 自有 ws.onmessage）
  csStore.events.on('ws_message', handleWsMessage);
  csStore.events.on('notification_click', handleNotificationClick);
  // background 在 csStore.clearVisitorWaiting 时 emit 此事件 → workbench 同步清 UI 私有状态
  csStore.events.on('visitor_waiting_cleared', clearVisitorWaitingUiState);
});

// 【S-P0-9】会话切换时按"上一会话"释放视频 blob：
//  - withMediaCache 没有显式 releaseMedia(url)，全量 releaseAllMedia 会让其他正在渲染的会话视频破图，
//    所以仅在用户主动切到"另一个会话"时调用，且只清未在视图的旧 url；
//  - 没有 oldId 跟踪到具体 url 列表的能力时，当前实现选择「切会话即整体释放」（业务上同一时间只看一个会话，安全）。
watch(
  () => currentConversation.value?.id,
  (newId, oldId) => {
    if (oldId && newId !== oldId) {
      releaseAllMedia();
    }
  },
);

// 【S-P0-9】keep-alive 长时间未激活（>5 分钟）时全量释放 media：
//  - 避免后台 Tab 长期占着大量解密视频 blob 内存；
//  - onActivated 时清除该 timer。
const MEDIA_RELEASE_DEACTIVATE_MS = 5 * 60 * 1000;
let mediaReleaseTimer: ReturnType<typeof setTimeout> | null = null;

onUnmounted(() => {
  // 释放所有视频 blob URL，避免内存泄漏（图片走 LRU 不需此处）
  releaseAllMedia();
  if (mediaReleaseTimer) { clearTimeout(mediaReleaseTimer); mediaReleaseTimer = null; }
  // ws 与 fallback poll 已迁出至 CsBackgroundService，这里只清理工作台自有 timer
  refreshTimer && clearInterval(refreshTimer);
  if (wsConnectedBannerTimer) { clearTimeout(wsConnectedBannerTimer); wsConnectedBannerTimer = null; }
  stopWaitingTimer();
  if (tokenRafId) { cancelAnimationFrame(tokenRafId); tokenRafId = null; }
  if (scrollRafId) { cancelAnimationFrame(scrollRafId); scrollRafId = null; }
  if (suggestionRafId) { cancelAnimationFrame(suggestionRafId); suggestionRafId = null; }
  window.removeEventListener('electron-navigate', handleElectronNavigate);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  if (messagesEl) {
    messagesEl.removeEventListener('scroll', handleMessageScroll);
  }
  if (clearUnreadTimer) {
    clearTimeout(clearUnreadTimer);
    clearUnreadTimer = null;
  }
  if (audioCtx) {
    audioCtx.close();
    audioCtx = null;
  }
  // 取消订阅背景服务事件
  csStore.events.off('ws_message', handleWsMessage);
  csStore.events.off('notification_click', handleNotificationClick);
  csStore.events.off('visitor_waiting_cleared', clearVisitorWaitingUiState);
  // 持续响铃由 CsBackgroundService 全局维护，workbench 卸载时不再清 ring
});

onActivated(async () => {
  // 【S-P0-9】激活时取消"长时间未激活全清 media"的延迟任务
  if (mediaReleaseTimer) { clearTimeout(mediaReleaseTimer); mediaReleaseTimer = null; }
  if (keepConnectionOnDeactivate) {
    restoreMessageScroll();
    if (isActivating.value) return;
    isActivating.value = true;
    try {
      await loadAgentInfo();
      // ws / fallback poll 由 CsBackgroundService 全局接管
      await loadConversations();
      const pendingConvId = route.query.conversationId;
      if (pendingConvId && typeof pendingConvId === 'string') {
        const targetConv = conversations.value.find(c => c.id === pendingConvId);
        if (targetConv) {
          selectConversation(targetConv);
        }
        router.replace({ query: {} });
      }
    } finally {
      isActivating.value = false;
    }
    return;
  }
  // 菜单切换返回时，刷新会话列表即可（ws 全局常驻）
  if (!hasMounted.value || isActivating.value) return;
  isActivating.value = true;
  try {
    await loadAgentInfo();
    await loadConversations();
  } finally {
    isActivating.value = false;
  }
});

onDeactivated(() => {
  // 【S-P0-9】超过 5 分钟未激活则全量释放 media（避免长期占内存）
  if (mediaReleaseTimer) clearTimeout(mediaReleaseTimer);
  mediaReleaseTimer = setTimeout(() => {
    releaseAllMedia();
    mediaReleaseTimer = null;
  }, MEDIA_RELEASE_DEACTIVATE_MS);

  if (keepConnectionOnDeactivate) {
    saveMessageScroll();
    return;
  }
  // ws 与持续响铃均由 CsBackgroundService 全局维护，工作台 deactivate 不再清 ring（跨菜单仍需响）
});

watch(filter, () => {
  const cacheKey = getConversationsCacheKey();
  const cached = conversationsCache.get(cacheKey);
  if (cached) {
    conversations.value = cached;
  }
  loadConversations();
});

// Phase 4.2 (M4)：会话列表变化时按 5 分钟去重批量预热访客头像，
// 让 Electron 长会话场景在切换 filter / 收到新会话时也能立刻命中 IDB 缓存。
// P4：mergeConversations 不再替换数组引用，需要监听 length 变化来感知增删；
// 同字段更新（lastMessageTime 等）走 watch 内 warmupAvatars 自带的 5min 去重，仍然便宜。
watch(() => conversations.value.length, () => {
  const list = conversations.value;
  if (!list || !list.length) return;
  try {
    const urls = list
      .map((c: any) => c?.userAvatar || c?.visitorAvatar || c?.avatar)
      .filter((u: any) => !!u)
      .map((u: string) => getFileAccessHttpUrl(u));
    if (urls.length) warmupAvatars(urls);
  } catch {
    // 预热失败不阻塞业务
  }
}, { flush: 'post' });
watch(messagesRef, (el, prev) => {
  if (prev) {
    prev.removeEventListener('scroll', handleMessageScroll);
  }
  if (el) {
    messagesEl = el;
    el.addEventListener('scroll', handleMessageScroll, { passive: true });
  }
});
watch(agentId, () => {
  quickReplyLoaded.value = false;
  if (showQuickReply.value) {
    loadQuickReplies(true);
  }
});

// 修复（兜底）：监听 token 变化，token 切换时主动通知 background 重连。
// ws 真正的 close + reconnect 由 CsBackgroundService 用最新 token 重新构造 url 完成。
watch(
  () => userStore.getToken,
  (newToken, oldToken) => {
    if (!hasMounted.value) return;
    if (!newToken || newToken === oldToken) return;
    console.log('[CS-WS] token 变化，通知后台服务重连');
    csStore.events.emit('cs_force_reconnect');
  }
);

// 加载客服信息
async function loadAgentInfo() {
  try {
    const res = await httpGet({ url: '/cs/agent/current' });
    if (res?.id) {
      agentId.value = res.id;
      agentName.value = res.nickname || '客服';
      agentAvatar.value = res.avatar || '';
      if (res.avatar) preloadImages([getFileAccessHttpUrl(res.avatar)]);
      agentStatus.value = res.status || 0;
      isOnline.value = res.status === 1;
      agentRole.value = res.role || 0; // 获取角色：0-普通客服, 1-管理者
      
      // 加载回复建议应用设置（每个客服独立配置）
      if (res.defaultAppId) {
        selectedAppId.value = res.defaultAppId;
      }
      // 注意：visitorAppId 是全局配置，在 loadGlobalVisitorApp() 中加载
      
      // 根据DB状态决定是否需要调用上线接口
      if (res.status !== 0) {
        // 已在线(1)/忙碌(2)/隐身(3) → 仅同步本地状态，不重复调接口（避免重复广播+日志）
        agentStatus.value = res.status;
        isOnline.value = res.status === 1 || res.status === 2;
      } else {
        // 离线(0) → 兜底上线（Token缓存恢复等未经Filter的场景）
        const csOnlineLogin = localStorage.getItem('CS_ONLINE_LOGIN');
        try {
          if (csOnlineLogin !== 'false') {
            await httpPost({ url: `/cs/agent/online/${agentId.value}` }, { errorMessageMode: 'none' });
            agentStatus.value = 1;
            isOnline.value = true;
          } else {
            await httpPost({ url: `/cs/agent/offline/${agentId.value}` }, { errorMessageMode: 'none' });
            agentStatus.value = 3;
            isOnline.value = false;
          }
        } catch (e: any) {
          if (e?.message?.includes('坐席已满')) {
            message.warning('客服坐席已满，请联系管理员');
            userStore.logout(true);
            return;
          }
        }
      }
    }
  } catch (e) {
    console.error('加载客服信息失败', e);
  }
}

function toggleQuickReply() {
  showQuickReply.value = !showQuickReply.value;
  if (showQuickReply.value) {
    loadQuickReplies();
  }
}

function getConversationsCacheKey() {
  const colleague = filter.value === 'monitor' ? '1' : '0';
  return `${agentId.value || 'guest'}_${filter.value}_${colleague}`;
}

const canSendMessage = computed(() => {
  // 上一条还没落库 / 没回包：发送按钮直接禁用，按 Enter 也走 sendMessage 入口的同一道闸门。
  if (sending.value) return false;
  // 兜底：万一某次响应式回写漏触发，只要有 1 个已上传完成的附件就放行；
  // sendMessage 内部仍会用 filter(a => !a.uploading && a.url) 二次过滤脏数据。
  const hasReadyAttachment = attachmentList.value.some((a: any) => !a.uploading && a.url);
  const hasUploading = attachmentList.value.some((a: any) => a.uploading);
  // hasInputText 是按 rAF 节流的 ref（_scheduleHasInputTextSync 维护），不是直接读 inputMessage.value。
  // 这是关键：让 canSendMessage 不再依赖 inputMessage，长按打字时 inputMessage 即使变更，
  // 模板 render fn 也不会被惊动；只在「有字 / 无字」边界翻转时 hasInputText 才会变，render fn 才重跑。
  const hasText = hasInputText.value;
  // 纯发文件：只要有 1 个上传完成即可（不强制 noUploading，允许多附件混合时其中 1 个 OK 就发）。
  if (!hasText) return hasReadyAttachment;
  // 有文字：禁止有上传中的附件，避免发出去 url 为空的占位条。
  return !hasUploading;
});

function getAttachmentType(file: any) {
  const type = file?.type || '';
  const name = (file?.name || '').toLowerCase();
  if (type.startsWith('image/') || name.match(/\.(png|jpe?g|gif|webp|bmp)$/)) {
    return 'image';
  }
  if (type.startsWith('video/') || name.match(/\.(mp4|webm|ogg|mov|avi|mkv)$/)) {
    return 'video';
  }
  // M3: audio 单独识别，模板可走 <audio controls> 渲染，否则会落到通用 file
  if (type.startsWith('audio/') || name.match(/\.(mp3|m4a|wav|ogg|opus|aac|flac)$/)) {
    return 'audio';
  }
  return 'file';
}

// 文件大小格式化（与访客端一致）
function formatFileSize(bytes: number): string {
  if (!bytes || bytes < 0) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

const ALLOWED_UPLOAD_EXTS = [
  '.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp',
  '.mp4', '.webm', '.ogg', '.mov',
  '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx',
  '.txt', '.csv', '.zip', '.rar', '.7z',
];
const ALLOWED_UPLOAD_TEXT = '图片(jpg/png/gif/webp/bmp)、视频(mp4/webm/mov)、文档(pdf/doc/xls/ppt/txt/csv)、压缩包(zip/rar/7z)';
const UPLOAD_MAX_SIZE = 20 * 1024 * 1024;

function beforeUploadAttachment(file: File) {
  const name = file.name || '';
  const dotIdx = name.lastIndexOf('.');
  const ext = dotIdx >= 0 ? name.slice(dotIdx).toLowerCase() : '';
  if (!ext || !ALLOWED_UPLOAD_EXTS.includes(ext)) {
    message.warning(`不支持 ${ext || '该'} 格式文件，允许上传：${ALLOWED_UPLOAD_TEXT}`);
    return false;
  }
  if (file.size > UPLOAD_MAX_SIZE) {
    message.warning('文件大小不能超过 20MB');
    return false;
  }
  return true;
}

function handleCustomUpload(options: any) {
  const file = options.file as File;
  uploadAttachmentFile(file);
}

async function uploadAttachmentFile(originalFile: File) {
  // R6: 仅图片走客户端压缩；非图片 / GIF / HEIC 等会自动 fallback 原文件。
  // 压缩 + 上传共享同一个原 file 名（compressImage 内部对 PNG 含 alpha 时仍输出 PNG）。
  let file: File = originalFile;
  try {
    file = await compressImage(originalFile);
  } catch {
    file = originalFile;
  }
  const attType = getAttachmentType(file);

  // M1: 立即 push 上传占位条目（字段命名严格对齐访客端 AttachmentItem）。
  // 图片/视频可显本地原图预览，避免按下「发送」前出现"裸 fid / 空气泡"的窘境。
  let previewUrl: string | undefined = undefined;
  try {
    if (attType === 'image' || attType === 'video' || attType === 'audio') {
      previewUrl = URL.createObjectURL(file);
    }
  } catch {}
  // Vue3 响应式陷阱：直接保存 push 进去的原始对象引用 → 写它属性不经 Proxy → 不触发响应式。
  // 这里用 Symbol __uid 给占位条打唯一标记，后续所有写操作都通过 findCurrent() 从
  // attachmentList.value 里拿到 Proxy 元素再改。Symbol 属性不会被 JSON.stringify 序列化，
  // 不会泄漏到 sendMessage 的 extra 入库 / WS 推送 payload 里。
  const uid = Symbol('attUid');
  const placeholder: any = {
    __uid: uid,
    name: originalFile.name || file.name || 'file',
    url: '',
    previewUrl,
    size: file.size,
    type: attType,
    uploading: true,
    progress: 0,
  };
  attachmentList.value.push(placeholder);
  const findCurrent = () => attachmentList.value.find((a: any) => a.__uid === uid);
  const removePlaceholder = () => {
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

    const checkRes = await defHttp.post(
      { url: '/airag/chat/checkHash', params: { md5, fileSize: file.size } },
      { joinParamsToUrl: true },
    );

    if (checkRes?.exists) {
      message.success('文件秒传成功');
      const cur: any = findCurrent();
      if (cur) {
        cur.url = checkRes.url;
        cur.uploading = false;
        cur.progress = 100;
      }
      // previewUrl 留到 sendMessage 时统一回收，避免气泡内本地预览突变成 cse 占位短暂闪烁
      uploadFileList.value.push(file);
      return;
    }

    let succeeded = false;
    const isReturn = (fileInfo: any) => {
      try {
        if (fileInfo.code === 0) {
          const url = fileInfo.message;
          if (!url) {
            removePlaceholder();
            return;
          }
          const cur: any = findCurrent();
          if (cur) {
            cur.url = url;
            cur.uploading = false;
            cur.progress = 100;
          }
          uploadFileList.value.push(file);
          succeeded = true;
        } else {
          removePlaceholder();
          message.error(fileInfo.message || `${file.name} 上传失败`);
        }
      } catch (error) {
        console.error('上传处理失败', error);
        removePlaceholder();
        message.error(`${file.name} 上传失败`);
      }
    };
    await defHttp.uploadFile(
      { url: '/airag/chat/upload' },
      { file, data: { md5 } },
      {
        success: isReturn,
        onProgress: (p) => {
          // R7: 实时进度回写到占位条 → 模板 a-progress 自动响应
          const cur: any = findCurrent();
          if (cur) cur.progress = p;
        },
        // R6: 通知后端跳过 stripImageMetadata，避免和客户端 Canvas 重新编码形成双重压缩
        headers: file === originalFile ? {} : { 'X-No-Strip-Metadata': '1' },
      },
    );
    if (!succeeded) {
      // 网络层报 200 但回调未走 success 分支的兜底（极端情况）
      removePlaceholder();
    }
  } catch (e) {
    hideLoading?.();
    console.error('上传失败', e);
    removePlaceholder();
    message.error(`${file.name} 上传失败`);
  }
}

function handlePasteUpload(event: ClipboardEvent) {
  const items = event.clipboardData?.items;
  if (!items || items.length === 0) return;
  let hasImage = false;
  for (let i = 0; i < items.length; i++) {
    const item = items[i];
    if (item.type && item.type.startsWith('image/')) {
      const file = item.getAsFile();
      if (file) {
        hasImage = true;
        const namedFile = file.name
          ? file
          : new File([file], `clipboard-${Date.now()}.png`, { type: file.type });
        uploadAttachmentFile(namedFile);
      }
    }
  }
  if (hasImage) {
    event.preventDefault();
  }
}

function toggleEmojiPanel() {
  showEmojiPanel.value = !showEmojiPanel.value;
}

function appendEmoji(emoji: string) {
  // 走 setInputText 统一同步：textarea.value（用户立刻看到）+ _textBuffer + inputMessage ref + hasInputText。
  // 不能直接 inputMessage.value = ... 因为模板已不再 v-model，textarea DOM 不会被自动更新。
  setInputText(`${_textBuffer}${emoji}`);
  nextTick(() => inputRef.value?.focus());
}

function removeAttachment(index: number) {
  const att: any = attachmentList.value[index];
  if (att?.previewUrl) {
    try { URL.revokeObjectURL(att.previewUrl); } catch {}
  }
  attachmentList.value.splice(index, 1);
  // uploadFileList 仅在上传成功后才 push，索引未必和 attachmentList 一致；
  // 这里只删完成态，避免 uploading 项删错位。
  if (att && !att.uploading) {
    uploadFileList.value.splice(index, 1);
  }
}

function getAttachmentUrl(attachment: any) {
  const url = attachment?.url;
  // 上传中显本地原图预览（图片/视频/音频）
  if (!url) {
    return attachment?.previewUrl || '';
  }
  const type = String(attachment?.type || '').toLowerCase();
  const resolved = getFileAccessHttpUrl(url);
  // 图片走 withImageCache（同步占位 + reactive 触发刷新）。
  // 注意：列表 / 缩略场景请用 getAttachmentThumbUrl，本函数返回原图（用于预览大图、open 等）。
  if (type === 'image') {
    return withImageCache(resolved);
  }
  // 视频走 withMediaCache（独立通道：不入 LRU/IDB，引用计数管理；模板必须 v-if 套条件渲染）
  if (type === 'video') {
    if (isCseUrl(resolved)) {
      const mime = String(attachment?.mime || attachment?.contentType || 'video/mp4');
      return withMediaCache(resolved, mime);
    }
    return resolved;
  }
  // M3: audio 走 withMediaCache（同视频通道，<audio> / <video> 都能直接拿 blob URL）
  if (type === 'audio') {
    if (isCseUrl(resolved)) {
      const mime = String(attachment?.mime || attachment?.contentType || 'audio/mpeg');
      return withMediaCache(resolved, mime);
    }
    return resolved;
  }
  // 【S-P0-8】未知/file 等类型：cse:// 不能直接交给 <img>/<video>/window.open，兜底返回 ''；
  // 业务侧用 v-if + 下载入口（downloadCse）处理。
  if (isCseUrl(resolved)) return '';
  return resolved;
}

/**
 * 列表 / 缩略图场景专用：图片走后端 ?thumb=1 通道，体积从 MB 降到几十 KB。
 * 上传中（无 url）继续返回本地 previewUrl 占位。
 * 非图片自动透传到 getAttachmentUrl。
 */
function getAttachmentThumbUrl(attachment: any): string {
  const url = attachment?.url;
  if (!url) return attachment?.previewUrl || '';
  const type = String(attachment?.type || '').toLowerCase();
  if (type !== 'image') return getAttachmentUrl(attachment);
  return withImageThumbCache(getFileAccessHttpUrl(url));
}

/**
 * 图片缩略图加载失败时的兜底：先尝试切原图（webp 兼容性 / 老数据无 thumbObjectKey），
 * 再失败才显示 1x1 透明 PNG。
 */
function onAttachmentImageError(e: Event, attachment: any) {
  const img = e.target as HTMLImageElement;
  if (!img || img.dataset.fallbackApplied) return;
  img.dataset.fallbackApplied = 'true';
  const url = attachment?.url;
  if (url) {
    img.src = withImageCache(getFileAccessHttpUrl(url));
  }
}

// ─── 【retry-storm-fix】cse:// 媒体/图片失败重试统一入口 ───────────
//
// 模板 v-for 内会被高频调用（流式 RAF + 1s 等待计时器叠加），
// 所有 helper 内部都走 imageCache 的 getMediaFailureState / getImageFailureState，
// 这两个探针只读 attempts 字段，不做 Date.now 比较，O(1) 安全。

function resolvedUrlOf(attachment: any): string {
  const url = attachment?.url;
  if (!url) return '';
  return getFileAccessHttpUrl(url);
}

function isVideoFailed(attachment: any): boolean {
  const u = resolvedUrlOf(attachment);
  if (!u || !isCseUrl(u)) return false;
  return getMediaFailureState(u).failed;
}
function isAudioFailed(attachment: any): boolean {
  return isVideoFailed(attachment); // 同走 withMediaCache 通道
}
function isImageFailed(attachment: any): boolean {
  const u = resolvedUrlOf(attachment);
  if (!u || !isCseUrl(u)) return false;
  return getImageFailureState(u).failed;
}

function onVideoSkeletonClick(attachment: any) {
  const u = resolvedUrlOf(attachment);
  if (u && isCseUrl(u) && getMediaFailureState(u).failed) {
    retryMedia(u);
    return;
  }
  // 正常态保留原打开预览行为
  openVideoPreview(attachment);
}
function onAudioSkeletonClick(attachment: any) {
  const u = resolvedUrlOf(attachment);
  if (u && isCseUrl(u) && getMediaFailureState(u).failed) {
    retryMedia(u);
  }
}
function onAttachmentImageRetry(attachment: any) {
  const u = resolvedUrlOf(attachment);
  if (u && isCseUrl(u)) {
    retryImage(u);
  }
}

/**
 * F3: 图片是否就绪（解密完成 / 本地预览可用），用于模板骨架屏判定。
 * 上传中本地有 previewUrl → 直接当 ready。
 */
function isAttachmentImageReady(attachment: any): boolean {
  if (!attachment) return false;
  if (attachment.previewUrl) return true;
  const url = attachment?.url;
  if (!url) return false;
  return isImageReady(getFileAccessHttpUrl(url));
}

function isSmartAssistant(msg: any): boolean {
  return Number(msg?.senderType) === 4;
}

function isAiMessage(msg: any): boolean {
  const st = Number(msg?.senderType);
  return st === 1 || msg?.isAiGenerated || (st === 2 && !msg?.senderId);
}

function getMessageAvatarUrl(msg: any) {
  const avatar = msg?.senderAvatar;
  if (avatar) {
    return withImageCache(getFileAccessHttpUrl(avatar));
  }
  if (isAiMessage(msg)) {
    if (chatWindowLogo.value) {
      return withImageCache(getFileAccessHttpUrl(chatWindowLogo.value));
    }
    const brandLogo = getBrandSetting().logoUrl;
    if (brandLogo) {
      // 品牌 logo 走匿名代理端点（无需 token），与登录页保持一致
      return resolveBrandPublicUrl(brandLogo);
    }
  }
  if (Number(msg?.senderType) !== 0) {
    const conv = conversations.value.find((c) => c.id === msg?.conversationId);
    const ownerAvatar = msg?.ownerAgentAvatar || conv?.ownerAgentAvatar || currentConversation.value?.ownerAgentAvatar;
    if (ownerAvatar) {
      return withImageCache(getFileAccessHttpUrl(ownerAvatar));
    }
    if (agentAvatar.value) {
      return withImageCache(getFileAccessHttpUrl(agentAvatar.value));
    }
  }
  return '';
}

function getAgentItemAvatarUrl(agent: any) {
  const avatar = agent?.avatar;
  return avatar ? withImageCache(getFileAccessHttpUrl(avatar)) : '';
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

function getParsedExtra(msg: any) {
  if (!msg) return null;
  const raw = msg.extra;
  const cached = extraCache.get(msg);
  if (cached && cached.key === raw) {
    return cached.value;
  }
  const parsed = parseExtra(raw);
  extraCache.set(msg, { key: raw, value: parsed });
  return parsed;
}

function getMessageAttachments(msg: any): any[] {
  const extra = getParsedExtra(msg);
  return extra?.attachments || [];
}

function getMediaAttachments(msg: any): any[] {
  return getMessageAttachments(msg).filter(item => item.type === 'image' || item.type === 'video');
}

function getFileAttachments(msg: any): any[] {
  // 含 audio：模板里 audio 走 <audio controls> 单独渲染（M3），其余统一走 FileChip
  return getMessageAttachments(msg).filter(item => item.type === 'file' || item.type === 'audio');
}

function getMediaGridData(msg: any) {
  const cacheKey = msg?.extra;
  const cached = mediaGridCache.get(msg);
  if (cached && cached.key === cacheKey) {
    return cached.value;
  }
  const media = getMediaAttachments(msg);
  const maxItems = 4;
  const items = media.slice(0, maxItems);
  const extraCount = Math.max(0, media.length - maxItems);
  const value = { items, extraCount, total: media.length };
  mediaGridCache.set(msg, { key: cacheKey, value });
  return value;
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

async function openImagePreviewFromList(list: any[], item: any) {
  const images = (list || []).filter(att => att?.type === 'image');
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

function openVideoPreview(item: any) {
  csMediaApi.openVideoPreview(getAttachmentUrl(item));
}

function openFilePreview(item: any) {
  csMediaApi.openFilePreview(getAttachmentUrl(item));
}

function openMediaViewer(msg: any) {
  csMediaApi.openMediaViewer(getMediaAttachments(msg));
}

// stripHtmlTags / buildMessagePreview 已迁移到 ./render/csMessageRender.ts

function isMessagesAtBottom() {
  const el = messagesEl || messagesRef.value;
  if (!el) return false;
  const threshold = 40;
  return el.scrollHeight - (el.scrollTop + el.clientHeight) <= threshold;
}

function handleMessageScroll(event?: Event) {
  scheduleClearUnread();
  const target = event?.target as HTMLElement | undefined;
  const el = target || messagesEl || messagesRef.value;
  if (!el) return;
  showScrollToBottom.value = el.scrollHeight - (el.scrollTop + el.clientHeight) > 300;
  if (loadingHistory.value || !hasMoreHistory.value) return;
  if (el.scrollTop <= 20) {
    loadMoreMessages();
  }
}

function scheduleClearUnread() {
  if (!currentConversation.value) return;
  if (!document.hasFocus()) return;
  if (!isMessagesAtBottom()) return;
  const unreadCount = currentConversation.value.unreadCount;
  if (unreadCount !== undefined && unreadCount <= 0) {
    return;
  }
  if (clearUnreadTimer) {
    clearTimeout(clearUnreadTimer);
  }
  clearUnreadTimer = window.setTimeout(async () => {
    if (!currentConversation.value) return;
    try {
      await httpPost({ url: `/cs/conversation/${currentConversation.value.id}/clear-unread` });
      currentConversation.value.unreadCount = 0;
      const listItem = conversations.value.find(c => c.id === currentConversation.value?.id);
      if (listItem) {
        listItem.unreadCount = 0;
      }
    } catch (e) {
      console.error('[Workbench] 清除未读失败', e);
    }
  }, 300);
}

async function loadQuickReplies(force = false) {
  if (quickReplyLoading.value) return;
  if (quickReplyLoaded.value && !force) return;
  if (!agentId.value) return;
  quickReplyLoading.value = true;
  try {
    const agentList = await httpGet({ url: `/cs/quickReply/agent/${agentId.value}` });
    quickReplyList.value = (agentList || []).map((item: any) => ({
      ...item,
      scope: item.agentId ? 'agent' : 'public',
    }));
    quickReplyLoaded.value = true;
  } catch (e) {
    console.error('加载快捷回复失败', e);
  } finally {
    quickReplyLoading.value = false;
  }
}

function applyQuickReply(item: any) {
  if (item.msgType === 1 || item.msgType === 2) {
    const type = item.msgType === 1 ? 'image' : 'file';
    const url = item.content || '';
    const name = url.split('/').pop() || (type === 'image' ? '图片' : '文件');
    attachmentList.value.push({ url, type, name });
  } else {
    setInputText(item?.content || '');
  }
  showQuickReply.value = false;
  nextTick(() => inputRef.value?.focus());
}

// 加载AI应用列表
async function loadAiAppList() {
  try {
    const res = await httpGet({ 
      url: '/airag/app/list',
      params: { pageNo: 1, pageSize: 100 }
    });
    if (res?.records) {
      aiAppList.value = res.records;
    }
  } catch (e) {
    console.error('加载AI应用列表失败', e);
  }
}

// AI应用切换（设置回复建议应用）
async function onAppChange(appId: string | undefined) {
  if (!agentId.value) return;
  
  try {
    await httpPut({
      url: `/cs/agent/${agentId.value}/default-app`,
      data: { appId: appId || '' }
    });
    selectedAppId.value = appId;
    console.log('[Workbench] 回复建议应用已更新');
  } catch (e) {
    console.error('设置回复建议应用失败', e);
    message.error('设置失败');
  }
}

// 访客AI应用切换（全局配置）
async function onVisitorAppChange(appId: string | undefined) {
  try {
    await httpPut({
      url: '/cs/agent/global/visitor-app',
      data: { appId: appId || '' }
    });
    visitorAppId.value = appId;
    console.log('[Workbench] 访客AI应用已更新（全局生效）');
  } catch (e) {
    console.error('设置访客AI应用失败', e);
    message.error('设置失败');
  }
}

// 加载全局访客AI应用配置
async function loadGlobalVisitorApp() {
  try {
    const res = await httpGet({ url: '/cs/agent/global/visitor-app' });
    const data = decryptApiResponse(res);
    if (data?.appId) {
      visitorAppId.value = data.appId;
    }
  } catch (e) {
    console.error('加载访客AI应用配置失败', e);
  }
}

// 加载聊天窗口配置
async function loadChatWindowSettings() {
  try {
    const rawRes = await httpGet({ url: '/cs/agent/global/chat-window-settings' });
    const res = typeof rawRes === 'string' ? decryptTransport(rawRes) : rawRes;
    let parsed: any = {};
    if (typeof res === 'string') {
      try { parsed = JSON.parse(res); } catch {}
    } else if (res && typeof res === 'object') {
      parsed = res;
    }
    chatWindowSettings.value = parsed;
    if (parsed.logo) {
      chatWindowLogo.value = parsed.logo;
      preloadImages([getFileAccessHttpUrl(parsed.logo)]);
    }
    setCachedChatWindowConfig(parsed);
  } catch {}
}

// 加载AI开关状态
async function loadAiEnabled() {
  try {
    const res = await httpGet({ url: '/cs/agent/global/ai-enabled' });
    const data = decryptApiResponse(res);
    aiEnabled.value = data?.enabled !== false;
  } catch (e) {
    console.error('加载AI开关状态失败', e);
  }
}

// 加载AI开场白开关状态
async function loadAiPrologueEnabled() {
  try {
    const res = await httpGet({ url: '/cs/agent/global/ai-prologue-enabled' });
    aiPrologueEnabled.value = res?.enabled !== false;
  } catch (e) {
    console.error('加载AI开场白开关状态失败', e);
  }
}

// AI开关切换
async function onAiEnabledChange(checked: boolean) {
  try {
    await httpPut({
      url: '/cs/agent/global/ai-enabled',
      data: { enabled: checked }
    });
    aiEnabled.value = checked;
    message.success(checked ? 'AI自动回复已开启' : 'AI自动回复已关闭');
    console.log('[Workbench] AI开关已更新:', checked);
  } catch (e) {
    console.error('设置AI开关失败', e);
    message.error('设置失败');
    // 恢复状态
    aiEnabled.value = !checked;
  }
}

// AI开场白开关切换
async function onAiPrologueEnabledChange(checked: boolean) {
  try {
    await httpPut({
      url: '/cs/agent/global/ai-prologue-enabled',
      data: { enabled: checked }
    });
    aiPrologueEnabled.value = checked;
    message.success(checked ? 'AI开场白已开启' : 'AI开场白已关闭');
    console.log('[Workbench] AI开场白开关已更新:', checked);
  } catch (e) {
    console.error('设置AI开场白开关失败', e);
    message.error('设置失败');
    aiPrologueEnabled.value = !checked;
  }
}

// 切换在线状态
async function toggleOnline(checked: boolean) {
  try {
    if (checked) {
      await httpPost({ url: `/cs/agent/online/${agentId.value}` });
    } else {
      await httpPost({ url: `/cs/agent/offline/${agentId.value}` });
    }
    agentStatus.value = checked ? 1 : 3;
    // 同步到 localStorage，刷新页面后保持当前状态
    localStorage.setItem('CS_ONLINE_LOGIN', String(checked));
  } catch (e) {
    message.error('操作失败');
    isOnline.value = !checked;
  }
}

// 加载会话列表
// 加载统计数据
let statsLoadTimer: any = null;
async function loadStats() {
  try {
    const res = await httpGet({
      url: '/cs/conversation/stats',
      params: { agentId: agentId.value }
    });
    if (res) {
      statsData.value = {
        myCount: res.myCount || 0,
        unassignedCount: res.unassignedCount || 0,
        closedCount: res.closedCount || 0,
        colleagueCount: res.colleagueCount || 0
      };
    }
  } catch (e) {
    console.error('加载统计数据失败', e);
  }
}

// 延迟加载统计数据（防抖，避免频繁调用）
// 高频事件（message/typing/visitor_updated 等）走此路径，500ms 内合并请求；
// 用户主动操作（接入/转接/结束）UI 自带提示，500ms 数字延迟基本无感。
function loadStatsDebounced() {
  if (statsLoadTimer) {
    clearTimeout(statsLoadTimer);
  }
  statsLoadTimer = setTimeout(() => {
    loadStats();
  }, 500);
}

// 关键事件快速刷新统计（assigned/closed/transferred 等会话归属变化）。
// 内部 100ms 微缓冲：
//   1) 后端 broadcast 在事务内发出（commit 前），100ms 留给事务 commit，避免拉到旧值
//   2) 同时 clearTimeout 自动合并 100ms 内的多次触发，防止短时高 QPS
function loadStatsImmediate() {
  if (statsLoadTimer) {
    clearTimeout(statsLoadTimer);
  }
  statsLoadTimer = setTimeout(() => {
    loadStats();
  }, 100);
}

// 延迟加载会话列表（防抖，WebSocket 兜底刷新用）
let conversationsLoadTimer: any = null;
function loadConversationsDebounced() {
  if (conversationsLoadTimer) {
    clearTimeout(conversationsLoadTimer);
  }
  conversationsLoadTimer = setTimeout(() => {
    if (!loadingConversations.value) {
      loadConversations();
    }
  }, 500);
}

// ============ 客服超时未回复 - 访客等待时长 ============

/** 加载超时配置 */
async function loadAgentTimeoutConfig() {
  try {
    const res = await httpGet({ url: '/cs/agent/global/conversation-assign' });
    const data = res?.result || res;
    if (data?.agentTimeoutReminder) {
      agentTimeoutConfig.value = {
        enabled: data.agentTimeoutReminder.enabled === true,
        seconds: data.agentTimeoutReminder.seconds ?? 20,
      };
    }
  } catch (e) {
    console.warn('[CS-Timeout] 加载超时配置失败', e);
  }
}

/**
 * 启动访客等待时长计时器（每秒刷新 UI 私有的 visitorWaitingSeconds）
 *
 * 数据源：csStore.visitorLastMsgTime（background 全局维护，workbench 不再持有副本）。
 * 每秒主动 tick 不依赖响应性，因此 csStore 用普通 Map 即可。
 */
function startWaitingTimer() {
  stopWaitingTimer();
  waitingTimerHandle = setInterval(() => {
    if (!agentTimeoutConfig.value.enabled) return;
    const now = Date.now();
    const threshold = agentTimeoutConfig.value.seconds;
    const newWaiting: Record<string, number> = {};
    let shouldNotify = false;
    csStore.visitorLastMsgTime.forEach((ts, convId) => {
      const elapsed = Math.floor((now - ts) / 1000);
      if (elapsed >= threshold) {
        newWaiting[convId] = elapsed;
        if (!timeoutNotifiedSet.has(convId)) {
          timeoutNotifiedSet.add(convId);
          shouldNotify = true;
        }
      }
    });
    visitorWaitingSeconds.value = newWaiting;
    if (shouldNotify && shouldPlaySound()) {
      playNotificationSound();
    }
  }, 1000);
}

function stopWaitingTimer() {
  if (waitingTimerHandle) {
    clearInterval(waitingTimerHandle);
    waitingTimerHandle = null;
  }
}

/** 格式化等待秒数为可读文本 */
function formatWaitingTime(seconds: number): string {
  if (seconds < 60) return `${seconds}秒`;
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分${seconds % 60}秒`;
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}时${m}分`;
}

/**
 * 清除会话本地 UI 私有状态（timeoutNotifiedSet + visitorWaitingSeconds）
 *
 * background 在调 csStore.clearVisitorWaiting(id) 后会 emit 'visitor_waiting_cleared'，
 * 这里订阅事件做 UI 同步；workbench 自身的客服回复入口也调 csStore.clearVisitorWaiting，
 * 不直接调用本函数（避免双源）。
 */
function clearVisitorWaitingUiState(conversationId: string) {
  if (!conversationId) return;
  timeoutNotifiedSet.delete(conversationId);
  if (conversationId in visitorWaitingSeconds.value) {
    const w = { ...visitorWaitingSeconds.value };
    delete w[conversationId];
    visitorWaitingSeconds.value = w;
  }
}

// ============ 会话列表增量 merge ============

// P4：用 id 索引把远端会话列表合并到本地 conversations.value，
//   - 删除：本地有 / remote 没有，splice 原地删除（保响应式）
//   - 更新：双方都有，仅 patch 接口字段，保留本地特有字段（lastTalkingAgent、userOnline 等）
//   - 新增：remote 有 / 本地没有，push 进数组
// 调用方负责后续 sortConversations() 等副作用。
function mergeConversations(remote: any[]) {
  const remoteIds = new Set<string>(remote.map((c: any) => c.id));
  const localIndex = new Map<string, any>();
  conversations.value.forEach((c: any) => localIndex.set(c.id, c));

  // 1. 删除：从尾向头遍历，splice 不会影响未遍历的索引
  for (let i = conversations.value.length - 1; i >= 0; i--) {
    const c = conversations.value[i];
    if (!remoteIds.has(c.id)) {
      conversations.value.splice(i, 1);
    }
  }

  // 2. 更新 + 新增
  remote.forEach((rc: any) => {
    const local = localIndex.get(rc.id);
    if (!local) {
      conversations.value.push(rc);
    } else {
      // 仅写入接口返回字段，未在 rc 中出现的本地字段保留不变
      Object.keys(rc).forEach((k) => {
        if (local[k] !== rc[k]) {
          local[k] = rc[k];
        }
      });
    }
  });
}

// ============ 加载监控模式客服列表 ============

async function loadMonitorAgents() {
  try {
    const res = await httpGet({
      url: '/cs/agent/list',
      params: { pageNo: 1, pageSize: 200 }
    });
    monitorAgentList.value = res?.records || res || [];
  } catch (e) {
    console.error('[Monitor] 加载客服列表失败', e);
    monitorAgentList.value = [];
  }
}

// ============ 加载会话列表 ============

async function loadConversations() {
  const requestId = ++conversationsRequestSeq;
  loadingConversations.value = true;
  try {
    // 同时加载统计数据（不等待，异步执行）
    loadStatsDebounced();
    
    // 同事会话模式：同时加载客服列表
    if (filter.value === 'monitor') {
      loadMonitorAgents();
    }
    
    // 同事会话模式：查看所有进行中的会话
    const params: any = { 
      agentId: agentId.value, 
      filter: filter.value, 
      pageNo: 1, 
      pageSize: 50 
    };
    
    // 如果是同事会话模式，添加标识
    if (filter.value === 'monitor') {
      params.supervisorMode = true;
    }
    
    const res = await httpGet({
      url: '/cs/conversation/list',
      params
    });
    if (requestId !== conversationsRequestSeq) {
      return;
    }
    const newConversations = res?.records || [];
    
    // 解密会话中的加密字段
    newConversations.forEach((conv: any) => {
      if (conv.lastMessage) conv.lastMessage = decryptMessage(conv.lastMessage);
      if (conv.satisfactionComment) conv.satisfactionComment = decryptMessage(conv.satisfactionComment);
    });

    // 补充访客信息：API返回值 > 缓存 > 旧会话数据
    newConversations.forEach((conv: any) => {
      const cacheKey = getVisitorCacheKey(conv.appId, conv.userId);
      const cached = visitorCache.get(cacheKey);
      const oldConv = conversations.value.find(c => c.id === conv.id);
      
      if (!conv.visitorNickname) {
        if (cached?.nickname) {
          conv.visitorNickname = cached.nickname;
        } else if (oldConv?.visitorNickname) {
          conv.visitorNickname = oldConv.visitorNickname;
        }
      }
      
      if (conv.visitorStar === undefined || conv.visitorStar === null) {
        if (cached?.star !== undefined) {
          conv.visitorStar = cached.star;
          conv.visitorStarTime = cached.starTime;
        }
      }
      
      if (oldConv?.lastTalkingAgent) {
        conv.lastTalkingAgent = oldConv.lastTalkingAgent;
      }
    });
    
    // P4：增量 merge 替代整数组替换。
    //   - 保持 conversations.value 数组引用不变，仅在真正增删时触发浅响应
    //   - 已有项的对象引用保留，仅 patch 接口返回的字段
    //   - 接口未返回的本地特有字段（lastTalkingAgent、userOnline、_clientKey 等）保留不变
    //   - unreadCount 维持原行为（服务端值覆盖本地）
    mergeConversations(newConversations);
    const cacheKey = getConversationsCacheKey();
    // 缓存仍写入接口快照（newConversations）而非 merge 后状态，避免 watch(filter)
    // 切换 tab 读缓存时引入残留对象引用。
    conversationsCache.set(cacheKey, newConversations);
    conversationsCacheTime.set(cacheKey, Date.now());

    // 访客等待追踪 + 持续响铃 队列初始化由 CsBackgroundService 在 loadConversationsList 完成后接管。
    // workbench 仅负责自己的本地 conversations.value 副本视图。

    // 按星标置顶排序
    sortConversations();

    // 异步预取昵称（API已返回的不会触发）
    conversations.value.forEach((conv: any) => {
      if (!conv.visitorNickname) {
        prefetchVisitorNickname(conv);
      }
    });

    if (!currentConversation.value) {
      try {
        const lastId = sessionStorage.getItem(lastConversationStorageKey);
        const lastConv = lastId ? conversations.value.find((c: any) => c.id === lastId) : null;
        if (lastConv) {
          await selectConversation(lastConv);
        }
      } catch {
        // 忽略读取异常
      }
    }
  } catch (e) {
    console.error('加载会话列表失败', e);
  } finally {
    if (requestId === conversationsRequestSeq) {
      loadingConversations.value = false;
    }
  }
}

async function prefetchVisitorNickname(conv: any) {
  const cacheKey = getVisitorCacheKey(conv.appId, conv.userId);
  if (!cacheKey) {
    return;
  }
  const cached = visitorCache.get(cacheKey);
  if (cached?.nickname) {
    conv.visitorNickname = cached.nickname;
    return;
  }
  try {
    const params: any = { userId: conv.userId };
    if (conv.appId) {
      params.appId = conv.appId;
    }
    const res = await httpGet({
      url: '/airag/cs/visitor/getByUser',
      params
    });
    if (res) {
      visitorCache.set(cacheKey, res);
      if (res.nickname) {
        conv.visitorNickname = res.nickname;
      }
    }
  } catch {
    // 忽略预取失败
  }
}

function updateConversationStar(conv: any, star: number, starTime: string | null) {
  conversations.value.forEach(c => {
    if (c.userId === conv.userId) {
      c.visitorStar = star;
      c.visitorStarTime = starTime;
    }
  });
  sortConversations();
}

function sortConversations() {
  conversations.value.sort((a, b) => {
    // 1. 星标优先（star_time 最新的排最前，类似置顶）
    const aStar = a.visitorStar || 0;
    const bStar = b.visitorStar || 0;
    if (aStar !== bStar) return bStar - aStar;
    if (aStar === 1 && bStar === 1) {
      const aStarTime = a.visitorStarTime ? new Date(a.visitorStarTime).getTime() : 0;
      const bStarTime = b.visitorStarTime ? new Date(b.visitorStarTime).getTime() : 0;
      if (aStarTime !== bStarTime) return bStarTime - aStarTime;
    }
    
    // 2. 未读消息优先
    const aUnread = a.unreadCount || 0;
    const bUnread = b.unreadCount || 0;
    if (aUnread > 0 && bUnread === 0) return -1;
    if (aUnread === 0 && bUnread > 0) return 1;
    
    // 3. 按最后消息时间排序
    const aTime = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0;
    const bTime = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0;
    return bTime - aTime;
  });
}

// 选择会话
async function selectConversation(conv: any) {
  if (currentConversation.value?.id === conv.id) return;
  const seq = ++switchSeq;

  // 清理旧会话的AI流式缓冲区，防止幽灵消息
  pendingTokens.clear();
  if (tokenRafId) { cancelAnimationFrame(tokenRafId); tokenRafId = null; }
  streamingMessages.value.clear();
  pendingSuggestionTokens = [];
  if (suggestionRafId) { cancelAnimationFrame(suggestionRafId); suggestionRafId = null; }

  // 清理上一个会话残留，避免昵称/头像短暂闪回
  visitorInfo.value = { level: 1, star: 0 };
  visitorTags.value = [];
  resetBlacklistStatus();

  // 优先使用缓存昵称/信息，避免首次渲染显示为“访客”
  const cacheKey = getVisitorCacheKey(conv.appId, conv.userId);
  const cached = visitorCache.get(cacheKey);
  if (cached) {
    if (cached.nickname) {
      conv.visitorNickname = cached.nickname;
    }
    visitorInfo.value = cached;
    visitorTags.value = cached.tags ? JSON.parse(cached.tags) : [];
  }

  currentConversation.value = conv;
  try {
    sessionStorage.setItem(lastConversationStorageKey, conv.id);
  } catch {
    // 忽略存储异常
  }
  currentReplyMode.value = conv.replyMode || 0;
  
  // 切换会话时清除回复建议（回复建议是针对特定会话的）
  aiSuggestion.value = '';
  aiSuggestionDismissed.value = false;
  aiSuggestionLoading.value = false;
  
  // Phase 1：三个独立请求并行（detail + messages + visitor）
  await Promise.all([
    loadConversationDetail(conv, seq),
    loadMessages(conv.id, seq),
    loadVisitorInfo(conv.appId, conv.userId, seq),
  ]);

  if (seq !== switchSeq) return;

  // Phase 2：依赖 detail 提供的 userIp
  await loadBlacklistStatus(seq);

  if (seq !== switchSeq) return;
  
  // 加载消息后，计算"对话中"的客服并缓存
  const listItem = conversations.value.find(c => c.id === conv.id);
  const lastAgent = getLastAgentFromMessages();
  if (lastAgent) {
    conv.lastTalkingAgent = lastAgent;
    if (listItem) {
      listItem.lastTalkingAgent = lastAgent;
    }
  }
  
  scheduleClearUnread();
  if (conv.userOnline !== undefined) {
    userOnline.value = conv.userOnline;
  }
  nextTick(() => inputRef.value?.focus());
}

// 加载会话详情（补充设备/地理位置等字段）
async function loadConversationDetail(conv: any, seq?: number) {
  try {
    const convDetail = await httpGet({ url: `/cs/conversation/${conv.id}` });
    if (seq !== undefined && seq !== switchSeq) return;
    if (convDetail) {
      const detail = convDetail.result || convDetail;
      if (detail.lastMessage) detail.lastMessage = decryptMessage(detail.lastMessage);
      if (detail.satisfactionComment) detail.satisfactionComment = decryptMessage(detail.satisfactionComment);
      const fieldsToMerge = [
        'userIp', 'userDevice', 'userOs', 'userOsVersion', 
        'userBrowser', 'userBrowserVersion', 'userDeviceId',
        'userCountry', 'userProvince', 'userCity',
        'ownerAgentName', 'ownerAgentAvatar', 'source',
        'lastMessage', 'satisfactionComment',
      ];
      for (const field of fieldsToMerge) {
        if (detail[field] !== undefined && detail[field] !== null) {
          conv[field] = detail[field];
        }
      }
    }
  } catch {
    // 获取会话详情失败不影响主流程
  }
}

// 从消息列表中获取最后一个发消息的客服名称
function getLastAgentFromMessages(): string | null {
  if (!messages.value || messages.value.length === 0) {
    return null;
  }
  
  // 从后往前查找最后一个客服消息（senderType === 2）
  for (let i = messages.value.length - 1; i >= 0; i--) {
    const msg = messages.value[i];
    if (msg.senderType === 2 && msg.senderName) {
      return msg.senderName;
    }
  }
  
  return null;
}

// 获取当前正在对话的客服（从缓存或消息中获取，模板已改为直接使用 conv.lastTalkingAgent）
function _getLastTalkingAgent(conv: any): string | null {
  // 优先使用缓存的lastTalkingAgent
  if (conv.lastTalkingAgent) {
    return conv.lastTalkingAgent;
  }
  
  // 如果是当前选中的会话，从消息中获取
  if (currentConversation.value?.id === conv.id) {
    const lastAgent = getLastAgentFromMessages();
    if (lastAgent) {
      conv.lastTalkingAgent = lastAgent;
      return lastAgent;
    }
  }
  
  return null;
}

// 获取显示名称（仅依赖 conv 自身字段，避免外部 ref 触发列表全量重渲染）
function getDisplayName(conv: any): string {
  return conv.visitorNickname || conv.userName || '访客';
}

function parseCustomFieldsRaw(customFields: any): Array<{label: string, value: string}> {
  if (!customFields) return [];
  try {
    const fields = typeof customFields === 'string' ? JSON.parse(customFields) : customFields;
    if (typeof fields === 'object' && fields !== null) {
      return Object.entries(fields).map(([label, value]) => ({ label, value: String(value) }));
    }
  } catch {}
  return [];
}

function filterFieldsByLocation(allFields: Array<{label: string, value: string}>, location: 'showInHeader' | 'showInConvList' | 'showInHistory'): Array<{label: string, value: string}> {
  const fieldDefs = chatWindowSettings.value?.humanAgentFields;
  if (!Array.isArray(fieldDefs) || !fieldDefs.length) return allFields;
  return allFields.filter((f) => {
    const def = fieldDefs.find((d: any) => d.label === f.label);
    return !def || def[location] !== false;
  });
}

function parseConvCustomFields(conv: any, location?: 'showInHeader' | 'showInConvList' | 'showInHistory'): Array<{label: string, value: string}> {
  const all = parseCustomFieldsRaw(conv?.customFields);
  return location ? filterFieldsByLocation(all, location) : all;
}

function getVisitorDisplayName(msg?: any): string {
  const conv = currentConversation.value;
  if (conv?.visitorNickname) return conv.visitorNickname;
  if (conv?.userName) return conv.userName;
  if (msg?.senderName) return msg.senderName;
  return '访客';
}

// 加载消息
async function loadMessages(conversationId: string, seq?: number) {
  try {
    const res = await httpGet({
      url: `/cs/message/${conversationId}`,
      params: { limit: historyPageSize }
    });
    if (seq !== undefined && seq !== switchSeq) return;
    if (conversationId !== currentConversation.value?.id) return;
    const rawList = Array.isArray(res) ? res : (res?.records || res?.result || []);
    const list = rawList.map((m: any) => ({ ...m, content: decryptMessage(m.content) }));
    messages.value = list;
    historyBeforeId.value = list[0]?.id || null;
    hasMoreHistory.value = list.length >= historyPageSize;
    lastMessageLoadAt = Date.now();
    scrollToBottom();
  } catch (e) {
    console.error('加载消息失败', e);
  }
}

async function loadMoreMessages() {
  if (loadingHistory.value) return;
  if (!hasMoreHistory.value) return;
  const convId = currentConversation.value?.id;
  if (!convId) return;
  const beforeId = historyBeforeId.value;
  if (!beforeId) {
    hasMoreHistory.value = false;
    return;
  }

  const el = messagesEl || messagesRef.value;
  const prevScrollHeight = el?.scrollHeight || 0;
  const prevScrollTop = el?.scrollTop || 0;

  loadingHistory.value = true;
  try {
    const res = await httpGet({
      url: `/cs/message/${convId}/page`,
      params: { beforeId, limit: historyPageSize }
    });
    if (convId !== currentConversation.value?.id) return;
    const rawOlder = Array.isArray(res) ? res : (res?.records || res?.result || []);
    const olderMessages = rawOlder.map((m: any) => ({ ...m, content: decryptMessage(m.content) }));
    if (!olderMessages.length) {
      hasMoreHistory.value = false;
      return;
    }
    const existingIds = new Set(messages.value.map(m => m.id));
    const filtered = olderMessages.filter((m: any) => !existingIds.has(m.id));
    if (filtered.length === 0) {
      hasMoreHistory.value = false;
      return;
    }
    messages.value = [...filtered, ...messages.value];
    historyBeforeId.value = filtered[0]?.id || historyBeforeId.value;
    if (olderMessages.length < historyPageSize) {
      hasMoreHistory.value = false;
    }
    nextTick(() => {
      const nextEl = messagesEl || messagesRef.value;
      if (!nextEl) return;
      const nextScrollHeight = nextEl.scrollHeight;
      nextEl.scrollTop = nextScrollHeight - prevScrollHeight + prevScrollTop;
    });
  } catch (e) {
    console.error('加载历史消息失败', e);
  } finally {
    loadingHistory.value = false;
  }
}

// 加载访客信息（使用缓存优化）
// 注：新版本不再依赖 appId，使用 userId 作为唯一标识
async function loadVisitorInfo(appId: string, userId: string, seq?: number) {
  if (!userId) {
    visitorInfo.value = {};
    visitorTags.value = [];
    return;
  }
  
  const cacheKey = getVisitorCacheKey(appId, userId);
  
  // 1. 先从缓存加载（立即显示）
  const cached = visitorCache.get(cacheKey);
  if (cached && (seq === undefined || seq === switchSeq)) {
    visitorInfo.value = cached;
    visitorTags.value = cached.tags ? JSON.parse(cached.tags) : [];
    if (cached.nickname && currentConversation.value) {
      currentConversation.value.visitorNickname = cached.nickname;
    }
  }
  
  // 2. 异步从API更新（后台刷新）
  try {
    const params: any = { userId };
    if (appId) {
      params.appId = appId;
    }
    
    const res = await httpGet({
      url: '/airag/cs/visitor/getByUser',
      params
    });
    
    if (res) {
      visitorCache.set(cacheKey, res);
      
      if (seq !== undefined && seq !== switchSeq) return;
      if (userId !== currentConversation.value?.userId) return;
      
      visitorInfo.value = res;
      visitorTags.value = res.tags ? JSON.parse(res.tags) : [];
      
      if (currentConversation.value) {
        const conv = conversations.value.find(c => c.id === currentConversation.value?.id);
        if (conv) {
          if (res.nickname) conv.visitorNickname = res.nickname;
          conv.visitorStar = res.star;
          conv.visitorStarTime = res.starTime;
        }
        if (res.nickname) currentConversation.value.visitorNickname = res.nickname;
      }
    } else if (!cached) {
      if (seq !== undefined && seq !== switchSeq) return;
      if (userId !== currentConversation.value?.userId) return;
      visitorInfo.value = { level: 1, star: 0 };
      visitorTags.value = [];
    }
  } catch {
    if (!cached) {
      if (seq !== undefined && seq !== switchSeq) return;
      if (userId !== currentConversation.value?.userId) return;
      visitorInfo.value = { level: 1, star: 0 };
      visitorTags.value = [];
    }
  }
}

function resetBlacklistStatus() {
  userIdBlacklisted.value = false;
  ipBlacklisted.value = false;
}

async function loadBlacklistStatus(seq?: number) {
  resetBlacklistStatus();
  if (!currentConversation.value) {
    return;
  }
  const userId = currentConversation.value.userId;
  const userIp = currentConversation.value.userIp;
  try {
    if (userId) {
      const res = await httpGet({
        url: '/airag/cs/visitor/blacklist/check',
        params: { userId },
      });
      if (seq !== undefined && seq !== switchSeq) return;
      userIdBlacklisted.value = !!res?.blacklisted;
    }
    if (userIp) {
      const res = await httpGet({
        url: '/airag/cs/visitor/blacklist/ip/check',
        params: { ip: userIp },
      });
      if (seq !== undefined && seq !== switchSeq) return;
      ipBlacklisted.value = !!res?.blacklisted;
    }
  } catch (e) {
    console.error('加载黑名单状态失败', e);
  }
}

function openBanModal(type: 'user' | 'ip') {
  banModalType.value = type;
  banModalVisible.value = true;
}

async function onBlacklistConfirm(payload: { type: 'user' | 'ip'; reason: string; ip?: string }) {
  if (!payload.reason) {
    message.warning('请输入拉黑原因');
    return;
  }
  if (payload.type === 'ip') {
    await applyBlacklist('ip', payload.reason, payload.ip || '');
  } else {
    await applyBlacklist('user', payload.reason);
  }
  banModalVisible.value = false;
}

async function applyBlacklist(type: 'user' | 'ip' | 'both', reason?: string, customIp?: string) {
  if (blacklistLoading.value || !currentConversation.value) {
    return;
  }
  const userId = currentConversation.value.userId;
  const userIp = currentConversation.value.userIp;
  if ((type === 'user' || type === 'both') && !userId) {
    message.warning('缺少用户ID');
    return;
  }
  if ((type === 'ip' || type === 'both') && !userIp) {
    message.warning('缺少IP');
    return;
  }
  blacklistLoading.value = true;
  try {
    if (type === 'user' || type === 'both') {
      const visitorName = currentConversation.value.userName || '';
      await httpPost({ url: '/airag/cs/visitor/blacklist/add', data: { userId, visitorName, reason: reason || '' } });
      userIdBlacklisted.value = true;
    }
    if (type === 'ip' || type === 'both') {
      const targetIp = customIp || userIp;
      await httpPost({ url: '/airag/cs/visitor/blacklist/ip/add', data: { ip: targetIp, reason: reason || '' } });
      ipBlacklisted.value = true;
    }
    message.success('已拉黑');
  } catch (e) {
    console.error('拉黑失败', e);
    message.error('拉黑失败');
  } finally {
    blacklistLoading.value = false;
  }
}

async function removeBlacklist(type: 'user' | 'ip' | 'both') {
  if (blacklistLoading.value || !currentConversation.value) {
    return;
  }
  const userId = currentConversation.value.userId;
  const userIp = currentConversation.value.userIp;
  blacklistLoading.value = true;
  try {
    if ((type === 'user' || type === 'both') && userId) {
      await httpPost({ url: '/airag/cs/visitor/blacklist/remove', data: { userId } });
      userIdBlacklisted.value = false;
    }
    if ((type === 'ip' || type === 'both') && userIp) {
      await httpPost({ url: '/airag/cs/visitor/blacklist/ip/remove', data: { ip: userIp } });
      ipBlacklisted.value = false;
    }
    message.success('已取消拉黑');
  } catch (e) {
    console.error('取消拉黑失败', e);
    message.error('取消拉黑失败');
  } finally {
    blacklistLoading.value = false;
  }
}

function buildShortcutString(e: KeyboardEvent): string {
  const parts: string[] = [];
  if (e.ctrlKey) parts.push('Ctrl');
  if (e.altKey) parts.push('Alt');
  if (e.shiftKey) parts.push('Shift');
  const key = e.key.length === 1 ? e.key.toUpperCase() : e.key;
  parts.push(key);
  return parts.join('+');
}

function handleInputCompositionStart() {
  _imeComposing = true;
}
function handleInputCompositionEnd() {
  _imeComposing = false;
}

function handleInputKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.altKey) && e.key !== 'Enter') {
    const pressed = buildShortcutString(e);
    const match = quickReplyList.value.find(
      (item) => item.shortcutKey && item.shortcutKey.toLowerCase() === pressed.toLowerCase(),
    );
    if (match) {
      e.preventDefault();
      applyQuickReply(match);
      return;
    }
  }

  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
    // 浏览器原生 IME 标记 + 本地标志 + Chrome 老式 keyCode 229：
    // 任何一个判定为"正在拼音/选词"的，就把这次 Enter 当作"上屏候选词"，不发送消息。
    // 这样修复了：边打字边按住 Enter 时，第一段拼音上屏的回车被误当作发送、把刚上屏的内容立刻打出去的现象。
    if (_imeComposing || (e as any).isComposing || e.keyCode === 229) return;
    e.preventDefault();
    // 入口闸门：上一条还在 await httpPost，直接吞掉这次 Enter，
    // 修复了"按住 Enter 就把同一段文本重复发送、输入框又一直没空"的核心问题。
    if (sending.value) return;
    sendMessage();
  } else if (e.key === 'Enter' && (e.ctrlKey || e.shiftKey)) {
    // 默认换行行为
  }
}

// 发送消息
async function sendMessage() {
  // 入口闸门：上一条还在 await httpPost，直接吞掉这次调用。
  // 配合 handleInputKeydown 的同名检查 + canSendMessage 的 disabled，
  // 形成"键盘 / 鼠标 / 函数级"三道护栏，杜绝重复发送。
  if (sending.value) return;
  if (!currentConversation.value) return;
  // _textBuffer 是 textarea 真实最新值，inputMessage ref 走 rAF 节流可能落后 1 帧，
  // 在临界路径上手动同步一次保证拿到的是用户刚敲完那一刻的完整字符串。
  flushInputBuffer();
  const content = inputMessage.value.trim();
  // R8: 必须深拷贝 + strip 内部字段。
  // attachmentList 内含 _ 前缀字段、previewUrl: 'blob:...'、uploading 这些纯前端状态，
  // 直接 slice() 会让 JSON.stringify 把它们写进 extra → 落库 → WS 推送给对方，
  // 导致 DB 污染、对方端尝试加载 blob: URL 报跨源错误、blob 内存延迟回收等连锁问题。
  // 同时排除 uploading=true 的占位条目，避免发出 url 为空的脏数据。
  const attachments = attachmentList.value
    .filter((a: any) => !a.uploading && a.url)
    .map((a: any) => {
      const out: any = {
        name: a.name,
        url: a.url,
        size: a.size,
        type: a.type,
      };
      if (a.mime) out.mime = a.mime;
      if (a.contentType) out.contentType = a.contentType;
      return out;
    });
  if (!content && attachments.length === 0) return;

  // 立刻置位 sending 闸门 + 清空输入框 / 附件区。
  // 这两步必须在任何 await 之前同步完成，否则:
  //   1) 用户连按 Enter 时第二次 sendMessage() 走到 inputMessage.value.trim() 仍能拿到旧内容
  //   2) 第二次走到 ws/http 之前看到 sending=false（因为 await 还没开始），重复发送
  // prevInput / prevAttachments 用于网络失败时回滚，让用户能直接修改重发。
  sending.value = true;
  const prevInput = inputMessage.value;
  const prevAttachments = attachmentList.value.slice();
  const prevUploadFileList = uploadFileList.value.slice();
  // 走 setInputText 同步清空 textarea DOM + 各路 ref + buffer，保证连按 Enter 第二次拿到空内容。
  setInputText('');
  attachmentList.value = [];
  uploadFileList.value = [];

  const wasUnassigned = currentConversation.value.status === 0; // 记录是否是待接入状态
  let localMsgId = '';

  // 客服发送消息（local 立即生效）：清访客等待 + 持续响铃 dequeue（即时停响，避免等 ws 回包的 1-2s 内还响）
  // background 在收到 ws 回包时也会再做一次幂等清理（双保险）。
  csStore.clearVisitorWaiting(currentConversation.value.id);
  csStore.dequeueContinuousRing(currentConversation.value.id);

  try {
    if (currentReplyMode.value === 0) {
      currentReplyMode.value = 1;
      currentConversation.value.replyMode = 1;
      // 不再单独调用 changeMode(1)，后端 sendAgentMessage 会自动切换模式并通知
    }
    
    const msgType = attachments.length > 0 ? 5 : 0;
    const extra = attachments.length > 0 ? JSON.stringify({ attachments }) : undefined;
    const nowIso = new Date().toISOString();
    const previewText = buildMessagePreview(content, attachments);
    localMsgId = `local_${Date.now()}`;
    const localMsg = {
      id: localMsgId,
      _clientKey: localMsgId,
      conversationId: currentConversation.value.id,
      content,
      msgType,
      extra: attachments.length > 0 ? { attachments } : undefined,
      senderType: 2,
      senderId: agentId.value,
      senderName: agentName.value,
      senderAvatar: agentAvatar.value || '',
      actualSenderName: agentName.value,
      createTime: nowIso,
    };

    // 本地立即渲染，减少发送等待感
    messages.value.push(localMsg);
    scrollToBottom();

    // 先更新会话列表预览，避免等待接口返回
    currentConversation.value.lastMessage = previewText;
    currentConversation.value.lastMessageTime = nowIso;
    if (currentConversation.value.status === 1) {
      currentConversation.value.lastTalkingAgent = agentName.value;
    }
    const listItem = conversations.value.find(c => c.id === currentConversation.value?.id);
    if (listItem) {
      listItem.lastMessage = previewText;
      listItem.lastMessageTime = nowIso;
      if (listItem.status === 1) {
        listItem.lastTalkingAgent = agentName.value;
      }
    }
    sortConversations();

    const res = await httpPost({
      url: '/cs/message/agent/send',
      data: {
        conversationId: currentConversation.value.id,
        agentId: agentId.value,
        agentName: agentName.value,
        content: encryptTransport(content),
        msgType,
        extra
      }
    });
    
    const resMessage = res?.result || res;
    if (resMessage?.id) {
      const idx = messages.value.findIndex(m => m.id === localMsgId);
      if (idx > -1) {
        Object.assign(messages.value[idx], {
          id: resMessage.id,
          createTime: resMessage.createTime || nowIso,
          ...(resMessage.senderAvatar ? { senderAvatar: resMessage.senderAvatar } : {}),
        });
      }
    }

    // 成功路径：输入框 / 附件区已在入口处清空，这里只需要回收本次提交的 blob 预览资源，
    // 避免气泡内本地预览仍持有的 blob 内存延迟回收。
    for (const a of prevAttachments) {
      if ((a as any)?.previewUrl) {
        try { URL.revokeObjectURL((a as any).previewUrl); } catch {}
      }
    }

    // ★ 发送消息后清除未读数（无论当前计数是否为0）
    currentConversation.value.unreadCount = 0;
    
    // 重新查找 listItem（await 期间 conversations 可能已被替换）
    const listItemAfterSend = conversations.value.find(c => c.id === currentConversation.value?.id);
    // 同步更新会话列表中的未读数
    if (listItemAfterSend) {
      listItemAfterSend.unreadCount = 0;
    }
    
    // 如果之前是待接入状态，刷新会话列表（因为后端会自动接入）
    if (wasUnassigned) {
      currentConversation.value.status = 1;
      currentConversation.value.ownerAgentId = agentId.value;
      currentReplyMode.value = 1; // 手动模式
      if (listItemAfterSend) {
        listItemAfterSend.status = 1;
        listItemAfterSend.ownerAgentId = agentId.value;
      }
      if (filter.value === 'unassigned' && listItem) {
        const idx = conversations.value.findIndex(c => c.id === listItem.id);
        if (idx > -1) {
          conversations.value.splice(idx, 1);
        }
      }
      loadStatsDebounced();
    }

    // P1：成功路径标记本地消息流已是最新，避免 fallback 兜底再整体重载消息流
    lastMessageLoadAt = Date.now();
  } catch (e) {
    if (localMsgId) {
      const idx = messages.value.findIndex(m => m.id === localMsgId);
      if (idx > -1) {
        messages.value.splice(idx, 1);
      }
    }
    // 失败回填：让用户能直接修改 / 重发，不丢内容也不丢附件 blob 预览。
    // 仅在用户在等待期间未手动输入新内容 / 新附件时回填，避免覆盖用户当下的输入。
    // 用 _textBuffer 而不是 inputMessage.value：rAF 节流可能让 ref 落后，buffer 才是用户真实输入。
    if (!_textBuffer) setInputText(prevInput);
    if (!attachmentList.value.length) {
      attachmentList.value = prevAttachments;
      uploadFileList.value = prevUploadFileList;
    } else {
      // 用户已经开始新一轮输入：旧附件 blob 不再可达，立即回收避免泄漏
      for (const a of prevAttachments) {
        if ((a as any)?.previewUrl) {
          try { URL.revokeObjectURL((a as any).previewUrl); } catch {}
        }
      }
    }
    message.error('发送失败');
  } finally {
    sending.value = false;
  }
}

// 接入会话（已隐藏待接入功能，保留函数以备后用）
async function _assignConversation(conversationId: string) {
  try {
    const res = await httpPost({
      url: `/cs/conversation/${conversationId}/assign`,
      data: { agentId: agentId.value }
    }, { isTransformResponse: false });
    
    const result = res.result || res;
    if (result.success) {
      console.log('[Workbench] 接入成功');
      await loadConversations();
      if (result.conversation) {
        await selectConversation(result.conversation);
      }
    } else {
      message.error(result.message || '接入失败');
    }
  } catch (e) {
    message.error('接入失败');
  }
}

// 切换回复模式
async function changeMode(mode: number) {
  if (!currentConversation.value) return;
  
  try {
    await httpPut({
      url: `/cs/conversation/${currentConversation.value.id}/mode`,
      data: { mode }
    });
    currentReplyMode.value = mode;
    currentConversation.value.replyMode = mode;
  } catch (e) {
    message.error('切换失败');
  }
}

// ==================== 满意度评价推送 ====================
const satisfactionPushing = ref(false);
const satisfactionPushed = ref(false);
const satisfactionPushMap = ref<Record<string, boolean>>({});

async function pushSatisfaction() {
  if (!currentConversation.value) return;
  const convId = currentConversation.value.id;
  satisfactionPushing.value = true;
  try {
    await defHttp.post({ url: `/cs/conversation/${convId}/push-satisfaction` });
    message.success('已推送满意度评价');
    satisfactionPushed.value = true;
    satisfactionPushMap.value[convId] = true;
  } catch (e: any) {
    message.error(e?.message || '推送失败');
  } finally {
    satisfactionPushing.value = false;
  }
}

// 切换会话时重置推送状态
watch(() => currentConversation.value?.id, (newId) => {
  if (newId) {
    satisfactionPushed.value = !!satisfactionPushMap.value[newId];
  } else {
    satisfactionPushed.value = false;
  }
});

// 结束会话
async function closeConversation() {
  if (!currentConversation.value) return;
  const closingId = currentConversation.value.id;
  try {
    await httpPost({ url: `/cs/conversation/${closingId}/close` });
    console.log('[Workbench] 会话已结束');

    const idx = conversations.value.findIndex(c => c.id === closingId);
    if (idx > -1) {
      conversations.value.splice(idx, 1);
    }
    if (currentConversation.value?.id === closingId) {
      currentConversation.value = null;
    }
    loadStatsDebounced();
  } catch (e) {
    console.error('[Workbench] 结束会话失败', e);
  }
}

// 删除会话（仅已结束的会话可删除）
async function deleteConversation(conversationId: string) {
  try {
    await httpDelete({ url: `/cs/conversation/${conversationId}` });
    console.log('[Workbench] 删除成功');
    
    // 如果删除的是当前选中的会话，清空选中状态
    if (currentConversation.value?.id === conversationId) {
      currentConversation.value = null;
      messages.value = [];
    }
    
    // 刷新列表
    await loadConversations();
    await loadStats();
  } catch (e) {
    message.error('删除失败');
  }
}

// 打开转接弹窗
async function openTransferModal() {
  showTransferModal.value = true;
  await loadAvailableAgents();
}

// 加载可用客服列表（用于转接）
async function loadAvailableAgents() {
  transferLoading.value = true;
  try {
    const res = await httpGet({
      url: '/cs/agent/list',
      params: { pageNo: 1, pageSize: 100 } // 获取所有客服
    });
    const agents = res?.records || res || [];
    const ownerAgentId = currentConversation.value?.ownerAgentId;
    // 过滤掉当前客服自己，保留完整信息
    availableAgents.value = agents
      .filter((a: any) => a.id !== agentId.value && a.id !== ownerAgentId && a.status === 1); // 只显示在线客服
  } catch (e) {
    console.error('加载可用客服列表失败', e);
    availableAgents.value = [];
  } finally {
    transferLoading.value = false;
  }
}

// 执行转接（点击客服卡片）
async function doTransfer(toAgentId: string) {
  if (!currentConversation.value || !toAgentId) return;
  
  try {
    // 使用查询参数方式传递（直接拼接到URL）
    await httpPost({
      url: `/cs/conversation/${currentConversation.value.id}/transfer?toAgentId=${toAgentId}&fromAgentId=${agentId.value}`
    });
    console.log('[Workbench] 转接成功');
    showTransferModal.value = false;
    await loadConversations();
    // 转接后清空当前会话
    currentConversation.value = null;
  } catch (e: any) {
    console.error('转接失败', e);
    message.error(e?.message || '转接失败');
  }
}

// 访客信息相关
function editField(field: string) {
  editingField.value = field;
  editValue.value = visitorInfo.value[field] || '';
  showEditModal.value = true;
}

async function saveEditField() {
  if (!currentConversation.value) return;
  
  const userId = currentConversation.value.userId;
  if (!userId) {
    message.error('用户ID不能为空');
    return;
  }
  
  try {
    const data: any = {
      userId: userId,
      [editingField.value]: editValue.value
    };
    
    // 如果有 appId，带上（兼容旧数据）
    if (currentConversation.value.appId) {
      data.appId = currentConversation.value.appId;
    }
    
    // 如果已有访客ID，带上ID
    if (visitorInfo.value.id) {
      data.id = visitorInfo.value.id;
    }
    
    const res = await httpPost({ url: '/airag/cs/visitor/update', data });
    // 更新本地访客信息，包括新创建的ID
    if (res && typeof res === 'object') {
      Object.assign(visitorInfo.value, res);
    } else {
      visitorInfo.value[editingField.value] = editValue.value;
    }
    
    // 更新访客缓存
    const cacheKey = getVisitorCacheKey(currentConversation.value.appId, userId);
    visitorCache.set(cacheKey, { ...visitorInfo.value });
    
    // 如果编辑的是昵称，同步更新到会话列表中的会话对象
    if (editingField.value === 'nickname') {
      const conv = conversations.value.find(c => c.id === currentConversation.value?.id);
      if (conv) {
        conv.visitorNickname = editValue.value;
      }
      // 同时更新当前会话对象
      if (currentConversation.value) {
        currentConversation.value.visitorNickname = editValue.value;
      }
    }
    
    showEditModal.value = false;
    console.log('[Workbench] 保存成功');
  } catch {
    message.error('保存失败');
  }
}

// 获取当前会话的访客缓存key
function getCurrentVisitorCacheKey(): string {
  if (!currentConversation.value) return '';
  const userId = currentConversation.value.userId;
  const appId = currentConversation.value.appId;
  return getVisitorCacheKey(appId, userId);
}

async function toggleStar() {
  if (!currentConversation.value) return;

  // 无访客记录时，先创建并直接设 star=1
  if (!visitorInfo.value.id) {
    try {
      const res = await httpPost({
        url: '/airag/cs/visitor/update',
        data: {
          userId: currentConversation.value.userId,
          appId: currentConversation.value.appId,
          star: 1,
          level: visitorInfo.value.level || 1,
        },
      });
      if (res && typeof res === 'object') {
        Object.assign(visitorInfo.value, res);
        const cacheKey = getCurrentVisitorCacheKey();
        if (cacheKey) visitorCache.set(cacheKey, { ...visitorInfo.value });
        updateConversationStar(currentConversation.value, res.star ?? 1, res.starTime ?? null);
      }
    } catch {
      message.error('操作失败');
    }
    return;
  }

  // 已有记录，乐观更新 + toggleStar API
  const prevStar = visitorInfo.value.star;
  const prevStarTime = visitorInfo.value.starTime;
  const newStar = prevStar === 1 ? 0 : 1;
  const newStarTime = newStar === 1 ? new Date().toISOString() : null;

  visitorInfo.value.star = newStar;
  visitorInfo.value.starTime = newStarTime;

  const cacheKey = getCurrentVisitorCacheKey();
  if (cacheKey) {
    visitorCache.set(cacheKey, { ...visitorInfo.value });
  }

  updateConversationStar(currentConversation.value, newStar, newStarTime);

  try {
    await httpPost({
      url: '/airag/cs/visitor/toggleStar',
      data: { id: visitorInfo.value.id },
    });
  } catch {
    visitorInfo.value.star = prevStar;
    visitorInfo.value.starTime = prevStarTime;
    if (cacheKey) {
      visitorCache.set(cacheKey, { ...visitorInfo.value });
    }
    updateConversationStar(currentConversation.value, prevStar, prevStarTime);
    message.error('操作失败');
  }
}

async function updateVisitorLevel(level: number) {
  if (!visitorInfo.value.id || !currentConversation.value) return;
  
  try {
    await httpPost({
      url: '/airag/cs/visitor/updateLevel',
      data: { id: visitorInfo.value.id, level }
    });
    visitorInfo.value.level = level;
    
    // 更新缓存
    const cacheKey = getCurrentVisitorCacheKey();
    if (cacheKey) {
      visitorCache.set(cacheKey, { ...visitorInfo.value });
    }
  } catch {
    message.error('操作失败');
  }
}

async function addTag() {
  if (newTag.value.trim() && !visitorTags.value.includes(newTag.value.trim())) {
    visitorTags.value.push(newTag.value.trim());
    await saveTags();
  }
  showTagInput.value = false;
  newTag.value = '';
}

async function removeTag(tag: string) {
  visitorTags.value = visitorTags.value.filter(t => t !== tag);
  await saveTags();
}

async function saveTags() {
  if (!visitorInfo.value.id || !currentConversation.value) return;
  
  try {
    await httpPost({
      url: '/airag/cs/visitor/updateTags',
      data: { id: visitorInfo.value.id, tags: JSON.stringify(visitorTags.value) }
    });
    
    // 更新缓存
    visitorInfo.value.tags = JSON.stringify(visitorTags.value);
    const cacheKey = getCurrentVisitorCacheKey();
    if (cacheKey) {
      visitorCache.set(cacheKey, { ...visitorInfo.value });
    }
  } catch {
    message.error('保存标签失败');
  }
}

// 回复建议
// 手动请求回复建议（流式）
async function requestAiSuggestion(userMessage: string) {
  if (!currentConversation.value || !userMessage) return;
  
  aiSuggestionDismissed.value = false; // 重置忽略标记
  aiSuggestionLoading.value = true;
  aiSuggestion.value = ''; // 清空之前的建议，准备接收流式内容
  
  try {
    const res = await httpPost({
      url: `/cs/message/ai-generate/${currentConversation.value.id}`,
      data: { userMessage: encryptTransport(userMessage), agentId: agentId.value }
    });
    
    if (res?.streaming) {
      // 流式模式，建议通过WebSocket推送，保持loading状态
      // loading会在收到 ai_suggestion_complete 时关闭
      console.log('[CS] 回复建议正在流式生成...');
    } else if (res?.suggestion) {
      // 非流式模式（兼容旧逻辑）
      aiSuggestion.value = decryptTransport(res.suggestion);
      console.log('[Workbench] 回复建议已生成');
      aiSuggestionLoading.value = false;
    } else {
      message.warning(res?.message || '暂时无法生成回复建议');
      aiSuggestionLoading.value = false;
    }
  } catch (e) {
    console.error('获取回复建议失败', e);
    message.error('获取回复建议失败');
    aiSuggestionLoading.value = false;
  }
}

function useSuggestion(direct: boolean) {
  if (direct) {
    setInputText(aiSuggestion.value);
    sendMessage();
  } else {
    setInputText(aiSuggestion.value);
    inputRef.value?.focus();
  }
  aiSuggestion.value = '';
}

/** 忽略/终止回复建议 */
function dismissSuggestion() {
  aiSuggestion.value = '';
  aiSuggestionLoading.value = false;
  aiSuggestionDismissed.value = true;
  // 通过 csStore 事件转发到 CsBackgroundService 的 wsClient.send
  if (currentConversation.value?.id) {
    csStore.events.emit('cs_command', {
      type: 'stop_ai_suggestion',
      conversationId: currentConversation.value.id,
    });
  }
}

// WebSocket
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
  // ws 由 CsBackgroundService 全局接管。"立即重连"按钮仍走此入口，转发命令给 background。
  csStore.events.emit('cs_force_reconnect');
}

// 以下保留为兼容旧 onMounted/onActivated 中的"假命中"短路语义；当 hasMounted 为 true 时实际不会被调用
function _legacyConnectWebSocket() {
  if (!agentId.value) {
    console.warn('[CS-WS] 缺少agentId，无法连接WebSocket');
    return;
  }
  if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
    return;
  }
  if (ws) {
    try {
      ws.close();
    } catch {
      // 忽略关闭异常
    }
  }
  wsManuallyClosed = false;
  stopWsHeartbeat();
  stopWsHealthCheck();
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  stopWsCountdown();
  if (hasConnectedOnce) {
    wsStatus.value = wsReconnectAttempts > 0 ? 'reconnecting' : 'connecting';
    wsShowBanner.value = true;
  }
  const wsBase = getWsBaseUrl();
  const token = getToken();
  const wsUrl = `${wsBase}/ws/cs/agent?userId=${agentId.value}&token=${encodeURIComponent(token || '')}`;
  
  console.log('[CS-WS] connectWebSocket', {
    agentId: agentId.value,
    hasToken: !!token,
    wsState: ws?.readyState,
    reconnectAttempts: wsReconnectAttempts,
    url: wsUrl,
  });
  ws = new WebSocket(wsUrl);
  const thisWs = ws;
  
  ws.onopen = async () => {
    const isReconnect = wsReconnectAttempts > 0;
    wsReconnectAttempts = 0;
    lastWsMessageAt = Date.now();
    startWsHeartbeat();
    startWsHealthCheck();
    if (isReconnect) {
      console.log('[CS-WS] 重连成功，恢复业务数据');
      await loadAgentInfo();
      await loadConversations();
      if (currentConversation.value?.id) {
        await loadMessages(currentConversation.value.id);
      }
    }
    if (ws === thisWs && hasConnectedOnce) {
      wsStatus.value = 'connected';
      if (wsConnectedBannerTimer) clearTimeout(wsConnectedBannerTimer);
      wsConnectedBannerTimer = window.setTimeout(() => {
        wsShowBanner.value = false;
        wsConnectedBannerTimer = null;
      }, 1500);
    }
    hasConnectedOnce = true;
  };
  ws.onmessage = (event) => {
    try {
      handleWsMessage(JSON.parse(event.data));
    } catch (e) {
      console.error('[CS-WS] 解析消息失败', e);
    }
  };
  ws.onerror = () => {
    if (!wsManuallyClosed && ws === thisWs) {
      try {
        ws?.close();
      } catch {
        // 忽略关闭异常
      }
    }
  };
  ws.onclose = (event) => {
    if (ws === thisWs) {
      ws = null;
      if (event.code === 4002) {
        // 修复：4002（被另一个会话替换）原本静默不重连，遇到"幽灵 ws 抢占"场景会导致客服收不到消息。
        // 现改为：保留 banner 提示 + 1.5s 后做一次自愈重连。
        // 真正的多端登录冲突：1.5s 后新建的 ws 会再次被踢，但用户能看到状态；
        // 残留 timer 误踢的"假冲突"：1.5s 后幽灵 ws 已自然结束，自愈重连成功。
        console.warn('[CS-WS] WebSocket 被另一个会话替换 (4002)，1.5s 后尝试自愈重连');
        stopWsHeartbeat();
        stopWsHealthCheck();
        if (wsReconnectTimer) { clearTimeout(wsReconnectTimer); wsReconnectTimer = null; }
        if (!wsManuallyClosed) {
          wsStatus.value = 'reconnecting';
          wsShowBanner.value = true;
          setTimeout(() => {
            if (!wsManuallyClosed && !ws && hasMounted.value && agentId.value) {
              connectWebSocket();
            }
          }, 1500);
        } else {
          stopFallbackPoll();
        }
      } else if (event.code === 4005) {
        stopWsHeartbeat();
        stopWsHealthCheck();
        stopFallbackPoll();
        if (wsReconnectTimer) { clearTimeout(wsReconnectTimer); wsReconnectTimer = null; }
        message.warning('客服坐席已满，无法上线');
        userStore.logout(true);
      } else if (!wsManuallyClosed) {
        scheduleWsReconnect();
      }
    }
  };
}

async function handleWsMessage(data: any) {
  lastWsMessageAt = Date.now();
  switch (data.type) {
    case 'message':
      // 解密消息内容（双层：传输层+存储层）
      data.content = decryptMessage(data.content);
      // 更新会话列表中的最后消息（不管是否是当前会话）
      const conv = conversations.value.find(c => c.id === data.conversationId);
      if (conv) {
        // ★ 问题1修复：更新最后消息和时间（包含客服消息）
        const previewText = buildMessagePreview(data.content || '', getMessageAttachments({ extra: data.extra }));
        conv.lastMessage = previewText || stripHtmlTags(data.content) || data.content;
        conv.lastMessageTime = new Date().toISOString();
        if (data.senderType === 0) {
          conv.userOnline = true;
        }

        // ★ 问题2修复：如果是客服消息，更新"对话中"的客服名称
        if (data.senderType === 2 && data.senderName && conv.status === 1) {
          conv.lastTalkingAgent = data.senderName;
        }
        // 访客等待 / 持续响铃由 CsBackgroundService 接管（markVisitorWaiting / dequeueContinuousRing 全在 background）。

        // 如果不是当前会话，增加未读数
        if (currentConversation.value?.id !== data.conversationId) {
          conv.unreadCount = (conv.unreadCount || 0) + 1;
        }
      }
      
      // 如果是当前选中的会话，也要更新currentConversation
      if (currentConversation.value?.id === data.conversationId) {
        // ★ 问题2修复：实时更新当前会话中"对话中"的客服
        if (data.senderType === 0) {
          userOnline.value = true;
          currentConversation.value.userOnline = true;
        }
        if (data.senderType === 2 && data.senderName) {
          currentConversation.value.lastTalkingAgent = data.senderName;
        }
        
        // 实时添加消息到列表，避免重新加载
        const newMsg: any = {
          id: data.messageId || Date.now().toString(),
          conversationId: data.conversationId,
          content: data.content,
          msgType: data.msgType,
          extra: data.extra,
          senderType: data.senderType,
          senderId: data.senderId,
          senderName: data.senderName,
          senderAvatar: data.senderAvatar,
          isAiGenerated: data.isAiGenerated,
          createTime: data.timestamp || new Date().toISOString(),
          _clientKey: data._clientKey,
        };
        // 避免重复添加（也检查 local_ 前缀消息以防 WebSocket 先于 HTTP 响应到达）
        const matchLocalMsg = (m) => {
          if (!String(m.id).startsWith('local_') || m._matched) return false;
          if (m.senderId !== newMsg.senderId) return false;
          // 优先用 _clientKey 匹配，其次用 content + 时间窗口匹配
          if (m._clientKey && newMsg._clientKey) return m._clientKey === newMsg._clientKey;
          if (m.content !== newMsg.content) return false;
          const timeDiff = Math.abs(new Date(m.createTime).getTime() - new Date(newMsg.createTime || newMsg.timestamp).getTime());
          return timeDiff < 5000;
        };
        const isDuplicate = messages.value.find(m => m.id === newMsg.id) ||
          (newMsg.senderId === agentId.value && Number(newMsg.senderType) === 2 &&
           messages.value.find(matchLocalMsg));
        if (!isDuplicate) {
          messages.value.push(newMsg);
          scrollToBottom();
          scheduleClearUnread();
        } else if (newMsg.senderId === agentId.value) {
          const localIdx = messages.value.findIndex(matchLocalMsg);
          if (localIdx > -1) {
            messages.value[localIdx].id = newMsg.id;
            messages.value[localIdx]._matched = true;
          }
        }
        // P1：当前会话有 WS 消息（无论 push / 替换 local / 完全重复）都视为消息流已最新，
        // 阻断 fallback 兜底在 5/30 秒后无意义重载消息流。
        lastMessageLoadAt = Date.now();
      }
      
      // 对会话列表进行重新排序（未读消息优先，然后按时间）
      sortConversations();
      
      // 延迟刷新统计数据（防抖）
      loadStatsDebounced();

      // 访客消息单次提示音 / 桌面通知 / 持续响铃 全部由 CsBackgroundService 统一处理。
      break;
    case 'delivery_failed': {
      const failedConversationId = data.conversationId;
      const failedConv = conversations.value.find(c => c.id === failedConversationId);
      if (failedConv) {
        failedConv.userOnline = false;
      }
      if (currentConversation.value?.id === failedConversationId) {
        userOnline.value = false;
        currentConversation.value.userOnline = false;
        const noticeMsg = {
          id: `delivery_failed_${Date.now()}`,
          conversationId: failedConversationId,
          content: decryptTransport(data.content) || '用户不在线，消息未送达',
          senderType: 3,
          createTime: data.timestamp || new Date().toISOString(),
        };
        messages.value.push(noticeMsg);
        scrollToBottom();
      }
      break;
    }
    case 'conversation_assigned':
      // 会话被接入 - 实时推送
      {
        const extraData = data.extra || data;
        const assignedAgentId = extraData.agentId;
        const assignedAgentAvatar = extraData.agentAvatar || '';
        const assignedAgentName = extraData.agentName
          || (assignedAgentId && assignedAgentId === agentId.value ? agentName.value : '其他客服');
        const assignedConv = conversations.value.find(c => c.id === extraData.conversationId);
        if (assignedConv) {
          // 更新会话状态
          assignedConv.status = 1; // 已分配
          assignedConv.ownerAgentId = assignedAgentId;
          assignedConv.ownerAgentName = assignedAgentName;
          assignedConv.ownerAgentAvatar = assignedAgentAvatar;
          assignedConv.assignTime = new Date().toISOString();
          if (extraData.customFields !== undefined) {
            assignedConv.customFields = extraData.customFields;
          }
          
          // 如果当前是待接入列表，从列表中移除该会话
          if (filter.value === 'unassigned') {
            const index = conversations.value.findIndex(c => c.id === extraData.conversationId);
            if (index > -1) {
              conversations.value.splice(index, 1);
            }
          }
          
          // 如果是当前选中的会话，更新当前会话对象
          if (currentConversation.value?.id === extraData.conversationId) {
            currentConversation.value.status = 1;
            currentConversation.value.ownerAgentId = assignedAgentId;
            currentConversation.value.ownerAgentName = assignedAgentName;
            currentConversation.value.ownerAgentAvatar = assignedAgentAvatar;
            if (extraData.customFields !== undefined) {
              currentConversation.value.customFields = extraData.customFields;
            }
          }
          
          // 如果不是当前客服接入的，显示提示
          if (assignedAgentId && assignedAgentId !== agentId.value) {
            console.log('[Workbench] 会话已被接入:', assignedAgentName);
          }
        } else if (
          (filter.value === 'mine' && assignedAgentId === agentId.value) ||
          filter.value === 'all'
        ) {
          await loadConversations();
        }

        // 接入提示：单次声音 + 桌面通知 由 CsBackgroundService 统一处理

        // 关键事件：会话归属变化，100ms 微缓冲后立即刷新 my/colleague 徽标
        loadStatsImmediate();
        // 监控模式：刷新整个列表和客服状态
        if (filter.value === 'monitor') {
          loadConversations();
        }
      }
      break;
    case 'new_conversation':
      // 新会话通知 - 自动分配模式下，新会话已分配给客服
      {
        console.log('[Workbench] 有新的会话接入');
        
        const convOwnerAgentId = data.extra?.ownerAgentId;
        const convStatus = data.extra?.status;
        
        // 检查是否已经在列表中（避免重复）
        const exists = conversations.value.find(c => c.id === data.conversationId);
        if (!exists) {
          // 兜底：extra 关键字段缺失时，通过 API 刷新列表
          if (!data.extra || data.extra.status === undefined) {
            loadConversationsDebounced();
            loadStatsDebounced();
            if (filter.value === 'monitor') {
              loadMonitorAgents();
            }
            break;
          }

          // 已结束会话静默处理：访客重新打开旧已结束会话页面时，
          // 后端可能仍会广播 new_conversation（旧版本兼容），前端兜底过滤。
          // 不加列表、不弹"新访客接入"提示、不放提示音；统计仍要更新。
          if (convStatus === 2) {
            loadStatsDebounced();
            break;
          }

          // 判断是否应该显示在当前列表
          const shouldAdd = 
            (filter.value === 'mine' && convOwnerAgentId === agentId.value) ||  // 我的：分配给当前客服
            (filter.value === 'unassigned' && convStatus === 0) ||               // 待接入：未分配
            (filter.value === 'all') ||                                           // 全部
            (filter.value === 'monitor');                                          // 监控：显示所有活跃会话
          
          if (shouldAdd) {
            const newConv: any = {
              id: data.conversationId,
              userId: data.senderId,
              userName: data.senderName || '访客',
              appId: data.extra?.appId,
              status: convStatus ?? 0,
              replyMode: data.extra?.replyMode || 0,
              ownerAgentId: convOwnerAgentId,
              createTime: data.extra?.createTime || new Date().toISOString(),
              lastMessageTime: data.extra?.createTime || new Date().toISOString(),
              lastMessage: decryptTransport(data.content) || '',
              unreadCount: 0,
              messageCount: 0,
              // 设备信息
              userIp: data.extra?.userIp,
              userOs: data.extra?.userOs,
              userOsVersion: data.extra?.userOsVersion,
              userBrowser: data.extra?.userBrowser,
              userBrowserVersion: data.extra?.userBrowserVersion,
              userDeviceId: data.extra?.userDeviceId,
              // 地理位置
              userCountry: data.extra?.userCountry,
              userProvince: data.extra?.userProvince,
              userCity: data.extra?.userCity,
              // 浏览器语言
              userLang: data.extra?.userLang,
              // 访客备注信息
              visitorNickname: data.extra?.visitorNickname,
              visitorStar: data.extra?.visitorStar,
              visitorStarTime: data.extra?.visitorStarTime,
            };
            
            // 添加到列表并按星标排序
            conversations.value.unshift(newConv);
            sortConversations();
          }
        }
        
        // ★ 无论在哪个列表，都更新统计数据（防抖）
        loadStatsDebounced();
        // 监控模式：刷新客服列表以更新 currentSessions
        if (filter.value === 'monitor') {
          loadMonitorAgents();
        }

        // 新访客接入提示：单次声音 + 桌面通知 由 CsBackgroundService 统一处理
      }
      break;
    case 'conversation_closed':
      // 会话结束通知 - 广播给所有客服
      {
        // ★ 兼容 extra 字段（与转接事件保持一致）
        const extraData = data.extra || data;
        const conversationId = extraData.conversationId || data.conversationId;
        const reason = extraData.reason || data.reason || '会话已结束';
        
        console.log('[Workbench] 收到会话结束事件:', {
          conversationId,
          reason,
          ownerAgentId: extraData.ownerAgentId,
          currentAgentId: agentId.value,
          currentFilter: filter.value,
          rawData: data
        });

        // 访客等待 / 持续响铃由 CsBackgroundService 接管（在收到 conversation_closed 时已 clearVisitorWaiting + dequeue），
        // 这里仅同步 workbench 自己的本地 conversations.value 视图。

        const closedConv = conversations.value.find(c => c.id === conversationId);
        if (closedConv) {
          // 更新会话状态
          closedConv.status = 2; // 已结束
          closedConv.endTime = extraData.endTime || data.endTime || new Date().toISOString();
          
          // 如果当前在"我的"或其他进行中的列表，从列表中移除
          if (filter.value !== 'closed') {
            const index = conversations.value.findIndex(c => c.id === conversationId);
            if (index > -1) {
              conversations.value.splice(index, 1);
              console.log('[Workbench] 已从列表移除会话');
            }
          }
          
          // 如果是当前选中的会话，提示并清空选中
          if (currentConversation.value?.id === conversationId) {
            console.log('[Workbench] 会话已结束:', reason);
            currentConversation.value = null;
          }
        } else {
          // 会话不在当前列表，但仍需更新统计
          console.log('[Workbench] 会话不在当前列表，仅更新统计');
        }
        
        // 关键事件：会话已结束，100ms 微缓冲后立即刷新 my/colleague/closed 徽标
        loadStatsImmediate();
        // 监控模式：刷新客服列表以更新 currentSessions
        if (filter.value === 'monitor') {
          loadMonitorAgents();
        }
      }
      break;
    case 'conversation_transferred':
      // 会话转接通知 - 广播给所有客服
      {
        // ★ 数据在 extra 字段中
        const extraData = data.extra || data;
        const newStatus = extraData.conversation?.status;
        
        console.log('[Workbench] 收到转接事件:', {
          conversationId: extraData.conversationId || data.conversationId,
          fromAgentId: extraData.fromAgentId,
          toAgentId: extraData.toAgentId,
          currentAgentId: agentId.value,
          currentFilter: filter.value,
          hasConversationData: !!extraData.conversation,
          rawData: data
        });
        
        const conversationId = extraData.conversationId || data.conversationId;
        const transferredConv = conversations.value.find(c => c.id === conversationId);
        
        // 如果当前客服是新负责人
        if (extraData.toAgentId === agentId.value) {
          console.log('[Workbench] 我是新负责人');
          // 转接提示：单次声音 + 桌面通知 由 CsBackgroundService 统一处理
          loadConversations();
        }
        // 如果当前客服是原负责人
        else if (extraData.fromAgentId === agentId.value) {
          console.log('[Workbench] 我是原负责人，从列表移除');
          
          if (transferredConv) {
            // 从列表中移除
            const index = conversations.value.findIndex(c => c.id === conversationId);
            if (index > -1) {
              conversations.value.splice(index, 1);
              console.log('[Workbench] 已从列表移除');
            }
          }
          
          // 如果是当前选中的会话，清空选中
          if (currentConversation.value?.id === conversationId) {
            console.log('[Workbench] 会话已转接给', extraData.toAgentName);
            currentConversation.value = null;
          }

          // 持续响铃 dequeue 由 CsBackgroundService 接管（fromAgentId === me 时已 clearVisitorWaiting + dequeue）。

          // 关键事件：会话归属变化，100ms 微缓冲后立即刷新 my/colleague 徽标
          loadStatsImmediate();
        }
        // 如果是其他客服（旁观者）
        else {
          console.log('[Workbench] 我是旁观者');
          
          // 如果会话在列表中，更新负责客服信息
          if (transferredConv) {
            transferredConv.ownerAgentId = extraData.toAgentId;
            transferredConv.ownerAgentName = extraData.toAgentName;
            transferredConv.ownerAgentAvatar = extraData.toAgentAvatar || extraData.conversation?.ownerAgentAvatar || '';
          }
          
          // 如果是当前选中的会话，更新显示
          if (currentConversation.value?.id === conversationId) {
            currentConversation.value.ownerAgentId = extraData.toAgentId;
            currentConversation.value.ownerAgentName = extraData.toAgentName;
            currentConversation.value.ownerAgentAvatar = extraData.toAgentAvatar || extraData.conversation?.ownerAgentAvatar || '';
          }

          // 如果在待接入列表里且已分配，移除该会话
          if (filter.value === 'unassigned' && newStatus === 1 && transferredConv) {
            const index = conversations.value.findIndex(c => c.id === conversationId);
            if (index > -1) {
              conversations.value.splice(index, 1);
            }
          }

          // 关键事件：会话归属变化，100ms 微缓冲后立即刷新 colleague 徽标
          loadStatsImmediate();
        }
        // 监控模式：刷新整个列表和客服状态
        if (filter.value === 'monitor') {
          loadConversations();
        }
      }
      break;
    case 'mode_changed':
      // 回复模式切换通知 - 广播给所有客服
      {
        const extraData = data.extra || data;
        const modeConv = conversations.value.find(c => c.id === extraData.conversationId);
        if (modeConv) {
          // 更新回复模式
          modeConv.replyMode = extraData.newMode;
          
          // 如果是当前选中的会话，更新显示
          if (currentConversation.value?.id === extraData.conversationId) {
            currentConversation.value.replyMode = extraData.newMode;
            currentReplyMode.value = extraData.newMode;
            const modeName = extraData.modeName
              || (extraData.newMode === 0 ? 'AI自动' : extraData.newMode === 1 ? '手动' : extraData.newMode === 2 ? 'AI辅助' : '未知');
            console.log('[Workbench] 回复模式已切换为:', modeName);
          }
        }
      }
      break;
    case 'agent_status_changed':
      // 客服状态变化通知 - 广播给所有客服
      {
        const statusData = data.extra || data;
        const changedAgentId = statusData.agentId;
        const newStatus = statusData.status;
        
        // 如果转接列表正在显示，刷新可用客服列表
        if (showTransferModal.value) {
          loadAvailableAgents();
        }
        
        // 实时更新监控模式下的客服状态
        const monitorAgent = monitorAgentList.value.find(a => a.id === changedAgentId);
        if (monitorAgent) {
          monitorAgent.status = newStatus;
        }
        
        // 显示提示（仅当其他客服状态变化时）
        if (changedAgentId !== agentId.value) {
          const agentName = statusData.agentName || '客服';
          if (newStatus === 1) {
            console.log('[Workbench] 客服已上线:', agentName);
          } else if (newStatus === 3) {
            console.log('[Workbench] 客服已隐身:', agentName);
          } else if (newStatus === 0) {
            console.log('[Workbench] 客服已离线:', agentName);
          }
        }
      }
      break;
    case 'ai_suggestion':
      if (currentConversation.value?.id === data.conversationId && !aiSuggestionDismissed.value) {
        aiSuggestion.value = decryptTransport(data.content);
      }
      break;
    case 'ai_suggestion_stream':
      // 回复建议流式消息 — dismissed 时丢弃（RAF 批处理）
      if (currentConversation.value?.id === data.conversationId && !aiSuggestionDismissed.value) {
        pendingSuggestionTokens.push(decryptTransport(data.content));
        if (!suggestionRafId) {
          suggestionRafId = requestAnimationFrame(() => {
            suggestionRafId = null;
            aiSuggestion.value = (aiSuggestion.value || '') + pendingSuggestionTokens.join('');
            pendingSuggestionTokens = [];
          });
        }
      }
      break;
    case 'ai_suggestion_complete':
      // 回复建议生成完成 — dismissed 时丢弃
      pendingSuggestionTokens = [];
      if (suggestionRafId) { cancelAnimationFrame(suggestionRafId); suggestionRafId = null; }
      if (currentConversation.value?.id === data.conversationId && !aiSuggestionDismissed.value) {
        aiSuggestion.value = decryptTransport(data.content);
        aiSuggestionLoading.value = false;
        console.log('[Workbench] 回复建议已生成');
      } else if (aiSuggestionDismissed.value) {
        aiSuggestionLoading.value = false;
      }
      break;
    case 'ai_suggestion_error':
      // 回复建议生成失败
      if (currentConversation.value?.id === data.conversationId) {
        aiSuggestionLoading.value = false;
        if (!aiSuggestionDismissed.value) {
          message.error(data.error || '回复建议生成失败');
        }
      }
      break;
    case 'ai_typing':
      // AI正在输入状态（可选显示）
      break;
    case 'ai_stream':
      // AI流式消息 - 实时显示在聊天窗口
      if (currentConversation.value?.id === data.conversationId) {
        handleAiStreamToken(data);
      }
      break;
    case 'ai_stream_complete': {
      // AI流式消息完成（先解密，后续统一使用明文）
      const decryptedAiComplete = decryptMessage(data.content);
      data.content = decryptedAiComplete;
      if (currentConversation.value?.id === data.conversationId) {
        handleAiStreamComplete(data);
        currentConversation.value.lastMessage = decryptedAiComplete || currentConversation.value.lastMessage;
        currentConversation.value.lastMessageTime = data.timestamp || new Date().toISOString();
        sortConversations();
      } else {
        // 非当前会话的 AI 完成消息，更新会话列表预览（切换时会自动重新加载消息）
        const streamConv = conversations.value.find(c => c.id === data.conversationId);
        if (streamConv) {
          streamConv.lastMessage = decryptedAiComplete || streamConv.lastMessage;
          streamConv.lastMessageTime = data.timestamp || new Date().toISOString();
        }
        sortConversations();
      }
    }
      break;
    case 'user_offline':
      if (currentConversation.value?.id === data.conversationId) {
        userOnline.value = false;
      }
      // 更新会话列表中的在线状态
      const offlineConv = conversations.value.find(c => c.id === data.conversationId);
      if (offlineConv) {
        offlineConv.userOnline = false;
      }
      break;
    case 'user_online':
      // 用户上线
      const onlineConv = conversations.value.find(c => c.id === data.conversationId);
      if (onlineConv) {
        onlineConv.userOnline = true;
      }
      if (currentConversation.value?.id === data.conversationId) {
        userOnline.value = true;
      }
      break;
    case 'unread_cleared':
      {
        const clearedConv = conversations.value.find(c => c.id === data.conversationId);
        if (clearedConv) {
          clearedConv.unreadCount = 0;
        }
        if (currentConversation.value?.id === data.conversationId) {
          currentConversation.value.unreadCount = 0;
        }
      }
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
    case 'agent_timeout_reminder':
      // 提示音 + 访客等待 + 持续响铃 兜底全部由 CsBackgroundService 接管。
      break;
    case 'visitor_updated': {
      const extraData = data.extra || data;
      const visitor = extraData.visitor || extraData;
      if (!visitor?.userId) {
        break;
      }
      const cacheKey = getVisitorCacheKey(visitor.appId, visitor.userId);
      if (cacheKey) {
        visitorCache.set(cacheKey, visitor);
      }
      // 更新会话列表中的访客信息缓存
      conversations.value.forEach(conv => {
        if (conv.userId !== visitor.userId) {
          return;
        }
        if (visitor.appId && conv.appId && conv.appId !== visitor.appId) {
          return;
        }
        if (visitor.nickname) {
          conv.visitorNickname = visitor.nickname;
        }
        if (visitor.star !== undefined) {
          conv.visitorStar = visitor.star;
          conv.visitorStarTime = visitor.starTime || null;
        }
      });
      sortConversations();
      // 更新当前会话的访客信息
      if (currentConversation.value?.userId === visitor.userId) {
        if (!visitor.appId || currentConversation.value?.appId === visitor.appId) {
          visitorInfo.value = { ...visitorInfo.value, ...visitor };
          try {
            visitorTags.value = visitor.tags ? JSON.parse(visitor.tags) : [];
          } catch {
            visitorTags.value = [];
          }
          if (visitor.nickname) {
            currentConversation.value.visitorNickname = visitor.nickname;
          }
        }
      }
      break;
    }
    case 'blacklist_changed': {
      const extraInfo = data.extra || data;
      const blType = extraInfo.blacklistType; // "user" | "ip"
      const blAction = extraInfo.action;       // "block" | "unblock"
      const blTarget = extraInfo.target;       // userId 或 IP
      if (!blTarget) break;

      // 如果当前会话匹配，实时更新拉黑状态
      if (currentConversation.value) {
        if (blType === 'user' && currentConversation.value.userId === blTarget) {
          userIdBlacklisted.value = blAction === 'block';
        }
        if (blType === 'ip' && currentConversation.value.userIp === blTarget) {
          ipBlacklisted.value = blAction === 'block';
        }
      }
      break;
    }
    case 'quota_kick': {
      // CsBackgroundService 已处理 ws.close + logout，这里只展示提示
      message.warning(decryptTransport(data.content) || '客服坐席已满，您已被强制下线');
      break;
    }
  }
}

// audioCtx / lastSoundTime / SOUND_THROTTLE_MS 已在顶部声明区上移，避免在 ring composable 实例化前被引用

function playNotificationSound() {
  if (!soundEnabled.value) return;
  const now = Date.now();
  if (now - lastSoundTime < SOUND_THROTTLE_MS) return;
  lastSoundTime = now;
  try {
    if (!audioCtx) audioCtx = new AudioContext();
    if (audioCtx.state === 'suspended') audioCtx.resume();
    const mult = Math.max(0, Math.min(CS_NOTIFY_MAX_GAIN, soundVolumePercent.value / 100));
    playCsNotificationSound(audioCtx, mult);
  } catch { /* 忽略音频播放异常 */ }
}

function shouldPlaySound() {
  return soundEnabled.value && route.path === '/cs/workbench';
}

// notifyNewMessage 已迁至 CsBackgroundService（跨菜单全局通知），workbench 不再维护本地副本。

// 工具函数
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

function formatTime(time: string) {
  if (!time) return '';
  const date = new Date(time);
  const now = new Date();
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
}

function formatMessageTime(time: string) {
  if (!time) return '';
  const date = new Date(time);
  const datePart = date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' });
  const timePart = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  return `${datePart} ${timePart}`;
}

function formatDateTime(time: string) {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN');
}

// ==================== 消息撤回 ====================

async function recallMessage(msg: any) {
  if (String(msg.id).startsWith('local_')) {
    message.warning('消息正在发送中，请稍后再撤回');
    return;
  }
  try {
    await defHttp.put({ url: `/cs/message/${msg.id}/recall` });
    message.success('消息已撤回');
    const idx = messages.value.findIndex((m) => m.id === msg.id);
    if (idx !== -1) {
      messages.value[idx].status = 3;
    }
  } catch (e: any) {
    message.error(e?.message || '撤回失败');
  }
}

// ==================== 设备/地理位置展示辅助函数 ====================

function formatGeoLocation(conv: any): string {
  if (!conv) return '-';
  const parts: string[] = [];
  if (conv.userCountry) parts.push(conv.userCountry);
  if (conv.userProvince) parts.push(conv.userProvince);
  if (conv.userCity && conv.userCity !== conv.userProvince) parts.push(conv.userCity);
  return parts.length > 0 ? parts.join(' ') : '-';
}

function formatBrowserVersion(version: string): string {
  if (!version) return '';
  // 只显示主版本号，例如 "120.0.6099.109" → "120"
  const dotIndex = version.indexOf('.');
  return dotIndex > 0 ? version.substring(0, dotIndex) : version;
}

function getOsIcon(os: string): string {
  if (!os) return '';
  const name = os.toLowerCase();
  if (name.includes('windows')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M1 3.2L6.6 2.5V7.7H1V3.2ZM7.4 2.4L14.6 1.3V7.7H7.4V2.4ZM1 8.3H6.6V13.5L1 12.8V8.3ZM7.4 8.3H14.6V14.7L7.4 13.6V8.3Z" fill="#0078D4"/></svg>';
  }
  if (name.includes('android')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M4.2 2.8L3.2 1.2M11.8 2.8L12.8 1.2M3 6.5C3 5.4 3.4 4.4 4.2 3.6 5 2.8 6.2 2.4 7.5 2.2H8.5C9.8 2.4 11 2.8 11.8 3.6 12.6 4.4 13 5.4 13 6.5V10C13 10.3 12.9 10.5 12.7 10.7 12.5 10.9 12.3 11 12 11H4C3.7 11 3.5 10.9 3.3 10.7 3.1 10.5 3 10.3 3 10V6.5Z" stroke="#3DDC84" stroke-width="1.2" stroke-linecap="round"/><circle cx="5.5" cy="5.5" r="0.8" fill="#3DDC84"/><circle cx="10.5" cy="5.5" r="0.8" fill="#3DDC84"/><path d="M4 11V13.5M12 11V13.5M1.5 6.5V10M14.5 6.5V10" stroke="#3DDC84" stroke-width="1.2" stroke-linecap="round"/></svg>';
  }
  if (name.includes('ios') || name.includes('macos')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M11.3 8.4C11.3 6.8 12.3 5.9 12.4 5.8 11.5 4.5 10.2 4.3 9.8 4.3 8.6 4.2 7.5 5 6.9 5 6.3 5 5.4 4.3 4.4 4.3 3.1 4.3 1.9 5.1 1.2 6.4 -0.3 9 0.8 12.7 2.3 14.5 3 15.4 3.8 16 4.7 16 5.7 16 6 15.4 7.1 15.4 8.2 15.4 8.5 16 9.4 16 10.4 16 11 15.4 11.7 14.5 12.5 13.5 13 12.6 13.1 12.2 11.3 11.1 11.3 8.4ZM9.3 3C9.9 2.3 10.3 1.3 10.2 0.3 9.4 0.4 8.4 0.8 7.7 1.5 7.1 2.1 6.6 3.2 6.8 4.1 7.6 4.2 8.6 3.7 9.3 3Z" fill="#555"/></svg>';
  }
  if (name.includes('linux')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 1C5.8 1 4 3.2 4 6C4 8 4.5 9.5 5.5 11C4.5 11.5 3.5 12.5 3.5 13.5C3.5 14.3 4.2 15 5 15H11C11.8 15 12.5 14.3 12.5 13.5C12.5 12.5 11.5 11.5 10.5 11C11.5 9.5 12 8 12 6C12 3.2 10.2 1 8 1Z" stroke="#333" stroke-width="1" fill="#FDB813"/><circle cx="6.5" cy="5" r="1" fill="#333"/><circle cx="9.5" cy="5" r="1" fill="#333"/><path d="M6.5 7.5Q8 9 9.5 7.5" stroke="#333" stroke-width="0.8" fill="none"/></svg>';
  }
  if (name.includes('chromeos')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="#4285F4" stroke-width="1.5" fill="none"/><circle cx="8" cy="8" r="3" fill="#4285F4"/></svg>';
  }
  return '';
}

function getBrowserIcon(browser: string): string {
  if (!browser) return '';
  const name = browser.toLowerCase();
  if (name.includes('chrome')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" fill="none" stroke="#4285F4" stroke-width="1"/><circle cx="8" cy="8" r="2.8" fill="#4285F4"/><circle cx="8" cy="8" r="1.5" fill="white"/><path d="M8 5.2L12.5 5.2" stroke="#EA4335" stroke-width="1.5"/><path d="M5.6 9.4L3.3 5" stroke="#FBBC05" stroke-width="1.5"/><path d="M10.4 9.4L8 13.3" stroke="#34A853" stroke-width="1.5"/></svg>';
  }
  if (name.includes('safari')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" fill="#1B8BF0" stroke="#1B8BF0" stroke-width="0.5"/><path d="M5 11L7 7L11 5L9 9Z" fill="white"/><circle cx="8" cy="8" r="0.8" fill="#EA4335"/></svg>';
  }
  if (name.includes('firefox')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="6.5" fill="#FF9500"/><path d="M8 2C5.5 2 3.5 4 3.5 6.5C3.5 9 5.5 13 8 14C10.5 13 12.5 9 12.5 6.5C12.5 4 10.5 2 8 2Z" fill="#FF4500" opacity="0.7"/><circle cx="8" cy="7" r="3" fill="#FFD700" opacity="0.5"/></svg>';
  }
  if (name.includes('edge')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 1.5C4.4 1.5 1.5 4.4 1.5 8C1.5 10.2 2.6 12.1 4.2 13.2C4.8 11.8 6.3 10.8 8 10.8C9.5 10.8 10.8 11.6 11.5 12.8C13.3 11.7 14.5 9.9 14.5 8C14.5 4.4 11.6 1.5 8 1.5Z" fill="#0078D4"/><path d="M4.2 13.2C5.2 14 6.5 14.5 8 14.5C10.5 14.5 12.7 13 13.8 10.8C12.4 10 11.2 10.8 8 10.8C6.3 10.8 4.8 11.8 4.2 13.2Z" fill="#50E6FF"/></svg>';
  }
  if (name.includes('opera')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" fill="#FF1B2D"/><ellipse cx="8" cy="8" rx="3" ry="5.5" fill="white" opacity="0.9"/></svg>';
  }
  if (name.includes('wechat')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M6 3C3.2 3 1 4.8 1 7C1 8.3 1.8 9.5 3 10.2L2.5 12L4.5 11C5 11.2 5.5 11.3 6 11.3C6.2 11.3 6.3 11.3 6.5 11.2C6.2 10.8 6 10.3 6 9.8C6 7.8 7.8 6.2 10 6.2C10.2 6.2 10.3 6.2 10.5 6.2C10 4.3 8.2 3 6 3Z" fill="#51C332"/><path d="M14.5 9.8C14.5 8 12.5 6.5 10 6.5C7.5 6.5 5.5 8 5.5 9.8C5.5 11.5 7.5 13 10 13C10.5 13 11 12.9 11.5 12.7L13 13.5L12.7 11.8C13.8 11.2 14.5 10.5 14.5 9.8Z" fill="#51C332"/></svg>';
  }
  if (name.includes('qq')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><ellipse cx="8" cy="9" rx="5" ry="5.5" fill="#12B7F5"/><circle cx="6.5" cy="7" r="1.2" fill="white"/><circle cx="9.5" cy="7" r="1.2" fill="white"/><circle cx="6.8" cy="7" r="0.5" fill="#333"/><circle cx="9.8" cy="7" r="0.5" fill="#333"/></svg>';
  }
  if (name.includes('uc')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" fill="#F77F00"/><text x="8" y="11" text-anchor="middle" fill="white" font-size="8" font-weight="bold">UC</text></svg>';
  }
  if (name.includes('samsung')) {
    return '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" fill="#1428A0"/><text x="8" y="11" text-anchor="middle" fill="white" font-size="6" font-weight="bold">S</text></svg>';
  }
  return '';
}

function getModeColor(mode: number) {
  return mode === 0 ? 'green' : (mode === 1 ? 'orange' : 'purple');
}

function getModeName(mode: number) {
  return mode === 0 ? 'AI自动' : (mode === 1 ? '手动' : 'AI辅助');
}

function getStatusColor(status: number) {
  return status === 0 ? 'blue' : (status === 1 ? 'green' : 'default');
}

function getStatusName(status: number) {
  return status === 0 ? '待接入' : (status === 1 ? '服务中' : '已结束');
}

function getMessageClass(msg: any) {
  if (msg.isDateSeparator) return 'date-sep';
  if (msg.senderType === 3) return 'system';
  if (msg.senderType === 4) return 'agent smart-assistant';
  return msg.senderType === 0 ? 'user' : 'agent';
}

// renderMessage / renderMarkdown / renderStreamingText / sanitizeHtml /
// linkifyPlainText / normalizeImgUrls / md(MarkdownIt) / renderCache 等
// 已迁移到 ./render/csMessageRender.ts，本文件通过 import 直接复用

// 处理AI流式token（RAF 批处理，每帧最多刷新一次 DOM）
function handleAiStreamToken(data: any) {
  const messageId = data.messageId;
  const token = decryptTransport(data.content);

  if (!messageId || !token) return;

  let entry = pendingTokens.get(messageId);
  if (!entry) {
    entry = { tokens: [], conversationId: data.conversationId, senderName: data.senderName };
    pendingTokens.set(messageId, entry);
  }
  entry.tokens.push(token);

  if (!tokenRafId) {
    tokenRafId = requestAnimationFrame(flushPendingTokens);
  }
}

function flushPendingTokens() {
  tokenRafId = null;
  for (const [messageId, entry] of pendingTokens) {
    if (entry.conversationId !== currentConversation.value?.id) continue;
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
  scrollToBottom();
}

// 处理AI流式消息完成（data.content 已在调用方解密为明文）
function handleAiStreamComplete(data: any) {
  const messageId = data.messageId;
  const fullContent = data.content;
  
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
  
  scrollToBottom();
}

function scrollToBottom() {
  showScrollToBottom.value = false;
  if (scrollRafId) return;
  scrollRafId = requestAnimationFrame(() => {
    scrollRafId = null;
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
      scheduleClearUnread();
    }
  });
}

function scrollToBottomClick() {
  scrollToBottom();
}

function saveMessageScroll() {
  const el = messagesRef.value;
  if (el) {
    savedScrollTop.value = el.scrollTop;
  }
}

function restoreMessageScroll() {
  if (savedScrollTop.value == null) {
    return;
  }
  nextTick(() => {
    const el = messagesRef.value;
    if (el) {
      el.scrollTop = savedScrollTop.value as number;
    }
  });
}

// ==================== 工作台上下文：暴露给 components/* 子组件 ====================
//
// 仅提供子组件确实需要的最小集合（只读 ref / computed + 必要 callback）；
// WebSocket 实例、消息缓冲、sendMessage 等核心业务保留在父级。
//
// 持续响铃相关字段全部来自 csStore（state owner），抽屉 v-model 自动绑定到 store；
// setter 走 csStore.setXxx，timer / Electron flash 副作用由 background 内 ring 实例 watch 自动驱动。
const csWorkbenchSettings: CsWorkbenchSettings = {
  selectedAppId,
  visitorAppId,
  aiEnabled,
  aiPrologueEnabled,
  soundEnabled,
  soundVolumePercent,
  aiAppList,
  continuousRingMode: csContinuousRingMode,
  ringStopCondition: csRingStopCondition,
  ringIntervalSeconds: csRingIntervalSeconds,
  continuousRingActive: csContinuousRingActive,
  isRingPaused: csIsRingPaused,
  pauseRemainSeconds: csPauseRemainSeconds,
  onAppChange,
  onVisitorAppChange,
  onAiEnabledChange,
  onAiPrologueEnabledChange,
  onSoundEnabledChange,
  onContinuousRingModeChange: csStore.setContinuousRingMode,
  onRingStopConditionChange: csStore.setRingStopCondition,
  onRingIntervalChange: csStore.setRingIntervalSeconds,
  onPauseRing: csStore.pauseRing,
  onResumeRing: csStore.resumeRing,
};
const csWorkbenchContext: CsWorkbenchContext = {
  agentId,
  agentName,
  agentAvatar,
  isOnline,
  isColleagueReadonly,
  currentConversation,
  visitorInfo,
  userOnline,
  parsedCustomFields,
  currentReplyMode,
  satisfactionPushing,
  satisfactionPushed,
  themeVars,
  wsStatus,
  wsShowBanner,
  wsReconnectCountdown,
  showDetailPanel,
  showSettingsDrawer,
  settings: csWorkbenchSettings,
  getDisplayName,
  getModeName,
  formatTime,
  toggleOnline,
  changeMode,
  pushSatisfaction,
  openTransferModal,
  closeConversation,
  connectWebSocket,
};
provide(CS_WORKBENCH_CONTEXT_KEY, csWorkbenchContext);
</script>

<style lang="less" scoped>
// ==================== 全局变量 ====================
@radius-card: 12px;
@ease-smooth: cubic-bezier(0.4, 0, 0.2, 1);

// ==================== 全局自定义滚动条 ====================
.cs-workbench ::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
.cs-workbench ::-webkit-scrollbar-track {
  background: transparent;
}
.cs-workbench ::-webkit-scrollbar-thumb {
  background: rgba(128, 128, 128, 0.3);
  border-radius: 3px;
  &:hover {
    background: rgba(128, 128, 128, 0.5);
  }
}

// ==================== 全局动画 ====================
@keyframes fadeSlideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes badgePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

// ==================== 过渡类 ====================
.fade-up-enter-active, .fade-up-leave-active {
  transition: all 0.25s @ease-smooth;
}
.fade-up-enter-from, .fade-up-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

// ==================== 整体布局 ====================
.empty-icon {
  font-size: 48px;
  color: var(--cs-text-muted);
  &.large { font-size: 64px; }
}
.version-text {
  color: var(--cs-text-muted);
  margin-left: 2px;
}

.cs-workbench {
  display: flex;
  /* 无 PageContext 时兜底；有 contentHeight 时由内联 style 覆盖 */
  height: calc(100vh - 110px);
  max-height: calc(100vh - 110px);
  background: var(--cs-bg-page);
  overflow: hidden;
  position: relative;
  box-sizing: border-box;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

// ==================== 左侧会话列表 ====================
.sidebar {
  width: 300px;
  height: 100%;
  background: var(--cs-bg-surface);
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 0;
  z-index: 2;
}

// .agent-bar / .ws-status-banner 样式已迁移到 components/CsAgentBar.vue 与 components/CsWsStatusBanner.vue

// ==================== 搜索框 ====================
.search-bar {
  padding: 10px 12px 6px;
  :deep(.ant-input-affix-wrapper) {
    border-radius: 20px;
    background: var(--cs-bg-page);
    border: 1px solid var(--cs-border);
    &:hover, &:focus, &.ant-input-affix-wrapper-focused {
      border-color: var(--cs-brand-start);
      box-shadow: 0 0 0 2px rgba(var(--cs-brand-rgb), 0.1);
    }
  }
  :deep(.ant-input) {
    background: transparent;
  }
  :deep(.anticon-search) {
    color: var(--cs-text-muted);
  }
}

// ==================== 设置抽屉 ====================
// .settings-content / .theme-presets / .color-row 等样式已迁移到 components/CsWorkbenchSettingsDrawer.vue

// ==================== 筛选标签（合并去重） ====================
.filter-tabs {
  display: flex;
  border-bottom: 1px solid var(--cs-border);
  flex-shrink: 0;

  .filter-tab {
    flex: 1;
    padding: 10px;
    text-align: center;
    cursor: pointer;
    font-size: 13px;
    color: var(--cs-text-muted);
    border-bottom: 3px solid transparent;
    transition: all 0.25s @ease-smooth;

    &:hover { color: var(--cs-brand-start); }

    &.active {
      color: var(--cs-brand-start);
      border-bottom-color: var(--cs-brand-start);
      font-weight: 600;
    }

    .count {
      background: #eef0f5;
      padding: 1px 7px;
      border-radius: 10px;
      font-size: 11px;
      margin-left: 4px;
    }

    &.supervisor-tab {
      color: #722ed1;
      &:hover { color: #9254de; }
      &.active {
        color: #722ed1;
        border-bottom-color: #722ed1;
        background: linear-gradient(to bottom, rgba(114, 46, 209, 0.04), transparent);
      }
    }
  }
}

// ==================== 会话列表 ====================
.conversation-list {
  flex: 1;
  overflow-y: auto;
}

// ==================== 监控模式 ====================
.monitor-agent-list {
  .empty-state {
    padding: 40px 0;
    text-align: center;
    color: var(--cs-text-muted);
  }
}

.monitor-agent-group {
  border-bottom: 1px solid var(--cs-border);

  &.expanded {
    .monitor-agent-header {
      background: #f8f9fc;
      border-bottom: 1px solid var(--cs-border);
    }
  }
}

.monitor-agent-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.2s @ease-smooth;
  user-select: none;

  &:hover { background: var(--cs-bg-input); }

  .monitor-expand-icon {
    font-size: 12px;
    color: var(--cs-text-muted);
    width: 14px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.2s @ease-smooth;
  }

  .monitor-agent-name {
    font-weight: 500;
    font-size: 14px;
    color: var(--cs-text-primary);
    flex-shrink: 0;
  }

  .monitor-agent-avatar {
    flex-shrink: 0;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  }

  .monitor-agent-status {
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 12px;
    flex-shrink: 0;
    font-weight: 500;

    &.status-1 {
      color: #52c41a;
      background: rgba(82, 196, 26, 0.1);
      box-shadow: 0 0 0 1px rgba(82, 196, 26, 0.2);
    }
    &.status-2 {
      color: #faad14;
      background: rgba(250, 173, 20, 0.1);
      box-shadow: 0 0 0 1px rgba(250, 173, 20, 0.2);
    }
    &.status-3 {
      color: #999;
      background: rgba(0, 0, 0, 0.04);
    }
    &.status-0 {
      color: #ff4d4f;
      background: rgba(255, 77, 79, 0.08);
    }
    &.status--1 {
      color: var(--cs-text-muted);
      background: rgba(0, 0, 0, 0.04);
    }
  }

  .monitor-agent-sessions {
    margin-left: auto;
    font-size: 12px;
    color: var(--cs-text-muted);
    flex-shrink: 0;
  }
}

.monitor-agent-conversations {
  background: var(--cs-bg-card);

  .monitor-no-conv {
    padding: 12px 20px 12px 40px;
    font-size: 12px;
    color: var(--cs-text-muted);
    font-style: italic;
  }

  .monitor-conv-item {
    padding-left: 40px;
    border-bottom-color: var(--cs-border);

    .conv-avatar .visitor-avatar {
      width: 36px !important;
      height: 36px !important;
      line-height: 36px !important;
      font-size: 14px !important;
    }

    .conv-content {
      .conv-name { font-size: 13px; }
      .conv-preview { font-size: 12px; }
    }
  }
}

// ==================== 会话列表项 ====================
.conversation-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  border-left: 3px solid transparent;
  position: relative;
  transition: all 0.2s @ease-smooth;

  &:hover {
    background: #f8f9fc;
    border-left-color: var(--cs-brand-start);
    .conv-actions { opacity: 1; }
  }

  &.active {
    background: linear-gradient(135deg, rgba(var(--cs-brand-rgb), 0.05), rgba(var(--cs-brand-rgb), 0.03));
    border-left-color: var(--cs-brand-start);
  }

  &.unread {
    border-left-color: #ff4d4f;
    background: var(--cs-bg-surface);
  }

  &.closed {
    opacity: 0.6;
    .conv-avatar { filter: grayscale(0.5); }
  }

  .conv-avatar {
    position: relative;
    flex-shrink: 0;
  }

  .conv-star-badge {
    position: absolute;
    top: -4px;
    left: -4px;
    font-size: 16px;
    color: #faad14;
    filter: drop-shadow(0 1px 2px rgba(0, 0, 0, 0.25));
    z-index: 1;
  }

  .conv-content {
    flex: 1;
    min-width: 0;
  }

  .conv-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 3px;
  }

  .conv-name {
    font-weight: 500;
    font-size: 14px;
    color: var(--cs-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .conv-time {
    font-size: 11px;
    color: var(--cs-text-muted);
    flex-shrink: 0;
  }

  .conv-custom-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 2px;
    margin-top: 2px;
    :deep(.ant-tag) {
      font-size: 11px;
      line-height: 18px;
      margin-right: 0;
    }
  }

  .conv-preview {
    font-size: 13px;
    color: var(--cs-text-muted);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .conv-agent {
    font-size: 11px;
    margin-top: 4px;
    display: flex;
    align-items: center;
    gap: 4px;
    .agent-label { color: var(--cs-text-muted); }
    .agent-name { color: var(--cs-brand-start); font-weight: 500; }
  }

  .conv-waiting {
    font-size: 11px;
    margin-top: 3px;
    display: flex;
    align-items: center;
    gap: 3px;
    color: #ff4d4f;
    font-weight: 500;
    animation: waiting-pulse 2s ease-in-out infinite;
    .waiting-icon { font-size: 12px; }
    .waiting-text { white-space: nowrap; }
  }

  @keyframes waiting-pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.6; }
  }

  .conv-badge {
    background: #ff4d4f;
    color: #fff;
    font-size: 11px;
    padding: 1px 7px;
    border-radius: 10px;
    min-width: 18px;
    text-align: center;
    flex-shrink: 0;
    animation: badgePulse 2s ease-in-out infinite;
  }

  .conv-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
    opacity: 0;
    transition: opacity 0.2s @ease-smooth;
  }
}

:deep(.ant-badge-dot) {
  box-shadow: 0 0 0 2px #fff, 0 0 6px rgba(82, 196, 26, 0.4);
}

.empty-state {
  padding: 48px 20px;
  text-align: center;
  color: var(--cs-text-muted);
  font-size: 13px;
}

// ==================== 中间聊天区域 ====================
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--cs-bg-surface);
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

// .chat-header 样式已迁移到 components/CsChatHeader.vue

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  position: relative;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 24px;
  min-height: 0;
  background: var(--cs-bg-chat);
  // 同步隔离：消息列表本身是独立滚动容器，允许浏览器把它的 layout / paint 与兄弟节点解耦。
  // 和底部 .chat-input-area 的 contain 一起，AntD a-textarea autoSize 每次测量 scrollHeight 时
  // 不再把庞大的消息树拖进 forced synchronous layout，打字卡顿由此消失。
  contain: layout paint style;
}

// ==================== 加载历史消息 ====================
.loading-history {
  text-align: center;
  padding: 12px 0;
  color: var(--cs-text-muted);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  animation: fadeIn 0.3s ease;
}

// ==================== 消息 ====================
.message-wrapper {
  display: flex;
  margin-bottom: 18px;
  animation: fadeSlideIn 0.3s @ease-smooth;

  // 日期分隔符
  &.date-sep {
    justify-content: center;
    margin: 24px 0 16px;

    .date-separator {
      position: relative;
      display: flex;
      align-items: center;
      width: 100%;
      justify-content: center;

      &::before, &::after {
        content: '';
        flex: 1;
        height: 1px;
        background: rgba(0, 0, 0, 0.06);
      }

      span {
        padding: 0 16px;
        font-size: 11px;
        color: var(--cs-text-muted);
        white-space: nowrap;
      }
    }
  }

  // 系统消息
  &.system {
    justify-content: center;

    .system-msg {
      background: rgba(0, 0, 0, 0.04);
      color: var(--cs-text-muted);
      font-size: 12px;
      padding: 4px 14px;
      border-radius: 16px;
    }
  }

  // 用户消息
  &.user {
    .msg-body { align-items: flex-start; }
    .msg-avatar {
      background: linear-gradient(135deg, var(--cs-brand-start), var(--cs-brand-end));
    }
  }

  // 智能助手消息（右侧，与客服消息同方向）
  &.smart-assistant {
    .msg-bubble { overflow-wrap: anywhere; }
  }

  // 客服/AI消息
  &.agent {
    flex-direction: row-reverse;
    .msg-body { align-items: flex-end; margin-left: auto; }
    .msg-info { justify-content: flex-end; }
    .msg-bubble { align-self: flex-end; }
    .msg-meta { justify-content: flex-end; }
    .msg-avatar {
      background: linear-gradient(135deg, #11998e, #38ef7d);
    }
  }
}

.visitor-avatar {
  background: linear-gradient(135deg, var(--cs-brand-start), var(--cs-brand-end));
  color: #fff;
}

.msg-avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

.msg-body {
  display: flex;
  flex-direction: column;
  max-width: 65%;
  margin: 0 10px;
  min-width: 0;
}

.msg-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;

  .sender-name {
    font-size: 12px;
    color: var(--cs-text-muted);
  }

  .msg-avatar-inline {
    background: linear-gradient(135deg, #11998e, #38ef7d);
    color: #fff;
    font-size: 14px;
    flex-shrink: 0;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  }
}

// ==================== 消息气泡 ====================
.msg-bubble {
  padding: 10px 14px;
  border-radius: 16px;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s @ease-smooth;

  &.user-bubble {
    background: var(--cs-bubble-user);
    border: 1px solid var(--cs-border);
    border-top-left-radius: 4px;
  }

  &.agent-bubble {
    background: linear-gradient(135deg, var(--cs-bubble-agent), var(--cs-bubble-agent-end));
    border-top-right-radius: 4px;

    &.ai-bubble {
      background: linear-gradient(135deg, var(--cs-bubble-ai), var(--cs-bubble-ai-end));
    }

    &.assistant-bubble {
      background: linear-gradient(135deg, var(--cs-bubble-assistant), var(--cs-bubble-assistant-end));
    }
  }

  .msg-text {
    font-size: 14px;
    line-height: 1.6;
    overflow-wrap: anywhere;

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
      &:last-child { margin-bottom: 0; }
    }

    :deep(pre) {
      background: rgba(0, 0, 0, 0.04);
      padding: 8px 12px;
      border-radius: 8px;
      max-width: 100%;
      white-space: pre-wrap;
      overflow-wrap: anywhere;
      word-break: break-all;
    }

    :deep(code) {
      background: rgba(0, 0, 0, 0.06);
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

    :deep(img) {
      max-width: 100%;
      height: auto;
      border-radius: 8px;
      display: block;
      margin: 4px 0;
    }
  }
}

.msg-meta {
  font-size: 11px;
  color: var(--cs-text-muted);
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;

  .ai-assist-btn {
    font-size: 11px;
    padding: 0 4px;
    height: auto;
    color: #7c3aed;
    &:hover { color: #6d28d9; }
  }

  .recall-btn {
    cursor: pointer;
    color: #ff7875;
    opacity: 0;
    transition: opacity 0.2s, color 0.2s;
    font-size: 14px;
    margin-left: 2px;
    &:hover { color: #f5222d; }
  }
}

.message-wrapper:hover .recall-btn {
  opacity: 1;
}

// ==================== 回到底部按钮 ====================
.scroll-to-bottom {
  position: absolute;
  bottom: 16px;
  right: 24px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--cs-brand-start), var(--cs-brand-end));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(var(--cs-brand-rgb), 0.4);
  z-index: 10;
  font-size: 16px;
  transition: all 0.2s @ease-smooth;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 16px rgba(var(--cs-brand-rgb), 0.5);
  }
}

// ==================== 回复建议 ====================
.ai-suggestion {
  margin: 0 20px 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f5f0ff, #ede7f6);
  border-radius: 10px;
  border: 1px solid #d3adf7;

  .suggestion-label {
    color: #722ed1;
    font-weight: 500;
    font-size: 13px;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .suggestion-text {
    background: var(--cs-bg-surface);
    padding: 12px 16px;
    border-radius: 8px;
    margin-bottom: 10px;
    font-size: 14px;
    max-height: 300px;
    overflow-y: auto;
    word-wrap: break-word;
    line-height: 1.6;

    :deep(p) { margin: 0 0 8px; &:last-child { margin-bottom: 0; } }
    :deep(ul), :deep(ol) { padding-left: 20px; margin: 8px 0; }
    :deep(pre) { background: var(--cs-bg-code); padding: 8px 12px; border-radius: 8px; overflow-x: auto; }
    :deep(code) { background: var(--cs-bg-code); padding: 2px 6px; border-radius: 4px; font-size: 13px; }
    :deep(img) { max-width: 100%; height: auto; border-radius: 8px; display: block; margin: 4px 0; }
  }

  .suggestion-btns {
    display: flex;
    gap: 8px;
  }
}

// ==================== 输入区域 ====================
.chat-input-area {
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.04);
  padding: 12px 20px;
  flex-shrink: 0;
  background: var(--cs-bg-surface);
  // 把底部输入区从主文档流中隔离出来：AntD a-textarea 的 autoSize 每次 input 都会读 hidden
  // textarea 的 scrollHeight / getComputedStyle，触发全局强制同步 layout。消息列表里如果有
  // 大量气泡 / 视频 / 图片，每次按键都要把整棵消息树重新布局一次，直接表现为"打字卡顿"。
  // contain: layout style 告诉浏览器本区域内的布局与样式变化不会外溢，reflow 只在本区内完成。
  contain: layout style;

  .input-toolbar {
    display: flex;
    gap: 4px;
    margin-bottom: 8px;

    .toolbar-icon {
      font-size: 18px;
      color: var(--cs-text-muted);
      cursor: pointer;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: all 0.2s @ease-smooth;

      &:hover {
        color: var(--cs-brand-start);
        background: var(--cs-bg-input);
      }
    }
  }

  .quick-reply-panel {
    margin-bottom: 8px;
    padding: 10px;
    background: rgba(250, 250, 252, 0.95);
    backdrop-filter: blur(10px);
    border: 1px solid var(--cs-border);
    border-radius: 10px;

    .quick-reply-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .quick-reply-list {
      max-height: 200px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .quick-reply-item {
      padding: 8px 10px;
      background: var(--cs-bg-surface);
      border: 1px solid var(--cs-border);
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s @ease-smooth;

      &:hover {
        border-color: var(--cs-brand-start);
        box-shadow: 0 2px 6px rgba(var(--cs-brand-rgb), 0.1);
      }
    }

    .quick-reply-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-weight: 500;
      margin-bottom: 4px;
      font-size: 13px;

      .quick-reply-title-left {
        display: flex;
        align-items: center;
        gap: 4px;
        flex: 1;
        min-width: 0;
      }

      .quick-reply-title-right {
        flex-shrink: 0;
        margin-left: 8px;
      }
    }

    .quick-reply-content {
      font-size: 12px;
      color: var(--cs-text-muted);
      white-space: pre-wrap;
    }

    .quick-reply-img {
      max-width: 80px;
      max-height: 60px;
      border-radius: 4px;
      object-fit: cover;
    }

    .quick-reply-richtext {
      max-height: 60px;
      overflow: hidden;
      line-height: 1.4;

      img {
        max-height: 40px;
        max-width: 80px;
        vertical-align: middle;
      }

      p {
        margin: 0;
      }
    }
  }

  .input-wrapper {
    // 与内部 textarea 同宽，避免只点到左侧小条才出现光标（field-sizing: content 时空 textarea 会按内容缩窄宽度）
    width: 100%;
    min-width: 0;
    box-sizing: border-box;
    background: var(--cs-bg-input);
    border-radius: 12px;
    padding: 6px 12px;
    transition: box-shadow 0.2s @ease-smooth;

    &:focus-within {
      box-shadow: 0 0 0 2px rgba(var(--cs-brand-rgb), 0.15);
    }

    :deep(.ant-input) {
      border: none;
      resize: none;
      font-size: 14px;
      background: transparent;
      &:focus { box-shadow: none; }
    }

    // 关掉 AntD autoSize 后用浏览器原生 field-sizing 接管高度计算。
    // line-height 22px × minRows 1 = 22px；× maxRows 8 = 176px。
    // padding 上下 4px × 2 = 8px，所以 min/max-height 对应 30 / 184。
    // 不支持 field-sizing 的浏览器 textarea 会保持 rows=1 的固定高度，体验降级但仍可用。
    :deep(.cs-fast-textarea) {
      // 必须显式拉满行宽，否则在 field-sizing: content 下会缩成「占位符/首行」宽度，整根灰条只有左侧可点
      display: block;
      width: 100%;
      min-width: 0;
      max-width: 100%;
      box-sizing: border-box;
      field-sizing: content;
      min-height: 30px;
      max-height: 184px;
      line-height: 22px;
      padding-top: 4px;
      padding-bottom: 4px;
      overflow-y: auto;
    }
  }

  .input-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;

    .input-hint {
      font-size: 12px;
      color: var(--cs-text-muted);
    }
  }

  .send-btn {
    border-radius: 20px;
    background: linear-gradient(135deg, var(--cs-brand-start), var(--cs-brand-end));
    border: none;
    padding: 0 24px;
    font-weight: 500;
    box-shadow: 0 2px 8px rgba(var(--cs-brand-rgb), 0.3);
    transition: all 0.2s @ease-smooth;

    &:hover:not(:disabled) {
      box-shadow: 0 4px 12px rgba(var(--cs-brand-rgb), 0.4);
      transform: translateY(-1px);
    }

    &:disabled {
      opacity: 0.5;
      background: var(--cs-border);
      box-shadow: none;
    }
  }

  .attachment-preview {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 8px;

    .attachment-item {
      position: relative;
      width: 72px;
      height: 72px;
      border: 1px solid var(--cs-border);
      border-radius: 10px;
      overflow: hidden;
      background: var(--cs-bg-surface);
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

      img, video {
        max-width: 100%;
        max-height: 100%;
        cursor: pointer;
      }

      .attachment-file {
        font-size: 12px;
        padding: 4px;
        text-align: center;
        cursor: pointer;
      }

      .remove-attachment {
        position: absolute;
        top: 2px;
        right: 2px;
        font-size: 10px;
        color: #fff;
        background: rgba(255, 77, 79, 0.85);
        border-radius: 50%;
        cursor: pointer;
        width: 18px;
        height: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.2s @ease-smooth;
      }

      &:hover .remove-attachment { opacity: 1; }

      // 【retry-storm-fix】上传中遮罩 + 失败态遮罩，绝对定位铺满 72×72 居中显示
      .upload-mask {
        position: absolute;
        inset: 0;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 4px;
        background: rgba(255, 255, 255, 0.72);
        color: #555;
        font-size: 16px;
        backdrop-filter: blur(1px);
        cursor: default;

        .upload-percent {
          font-size: 11px;
          line-height: 1;
          color: #555;
        }

        &.is-failed {
          background: rgba(255, 77, 79, 0.08);
          color: #cf1322;
          cursor: pointer;
          .upload-percent { color: #cf1322; }
        }
      }

      // 【retry-storm-fix】输入区视频附件骨架屏（small 尺寸），铺满 72×72
      .video-skeleton.small {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--cs-text-secondary, #aaa);
        font-size: 22px;
        background: var(--cs-bg-code, #f4f4f6);
        cursor: pointer;

        &.is-failed {
          color: #cf1322;
          background: rgba(255, 77, 79, 0.06);
        }
      }
    }
  }
}

// ==================== 媒体相关 ====================
.msg-attachments {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .attachment-item {
    max-width: 240px;
    img, video {
      max-width: 100%;
      border-radius: 10px;
      cursor: pointer;
    }
  }
}

.msg-media-grid {
  display: grid;
  gap: 4px;
  margin-top: 6px;

  .media-item {
    position: relative;
    border-radius: 10px;
    overflow: hidden;
    background: var(--cs-bg-code);
    cursor: pointer;
    transition: transform 0.15s @ease-smooth;

    img, video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &:hover { transform: scale(1.02); }

    .media-skeleton-overlay {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 4px;
      background: var(--cs-bg-code);
      color: var(--cs-text-secondary, #aaa);
      font-size: 22px;
      pointer-events: auto;
      cursor: progress;

      // 【retry-storm-fix】失败态：红色 reload 图标 + 提示文字 + 可点击
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

    // 【retry-storm-fix】消息区视频骨架屏，铺满 .media-item，含失败态视觉差异
    .video-skeleton {
      width: 100%;
      height: 100%;
      min-height: 80px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 6px;
      background: var(--cs-bg-code, #f4f4f6);
      color: var(--cs-text-secondary, #888);
      font-size: 13px;
      cursor: pointer;
      .anticon { font-size: 28px; }

      &.is-failed {
        background: rgba(255, 77, 79, 0.06);
        color: #cf1322;
        border: 1px dashed rgba(255, 77, 79, 0.5);
        .anticon { color: #cf1322; }
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
      font-size: 18px;
      font-weight: 600;
    }
  }
}

.media-grid--1 {
  grid-template-columns: 1fr;
  .media-item { aspect-ratio: 3 / 2; }
}
.media-grid--2 {
  grid-template-columns: repeat(2, 1fr);
  .media-item { aspect-ratio: 1 / 1; }
}
.media-grid--3 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  .media-item { aspect-ratio: 1 / 1; }
  .media-item:nth-child(1) { grid-row: span 2; aspect-ratio: auto; }
}
.media-grid--4 {
  grid-template-columns: repeat(2, 1fr);
  .media-item { aspect-ratio: 1 / 1; }
}

.msg-file-list {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .file-item {
    padding: 8px 10px;
    background: rgba(var(--cs-brand-rgb), 0.04);
    border-left: 3px solid var(--cs-brand-start);
    border-radius: 0 8px 8px 0;
    cursor: pointer;
    font-size: 12px;
    transition: all 0.15s @ease-smooth;

    &:hover {
      background: rgba(var(--cs-brand-rgb), 0.06);
      box-shadow: 0 1px 4px rgba(var(--cs-brand-rgb), 0.1);
    }
  }

  // 【retry-storm-fix】音频骨架屏：cse:// 解密未就绪 / 失败时的占位
  .audio-skeleton {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 14px;
    background: var(--cs-bg-code, #f4f4f6);
    color: var(--cs-text-secondary, #888);
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

// .media-viewer-grid / .media-viewer-item / .media-viewer-header / .media-viewer-tip
// 已迁移到 components/CsMediaPreviewModals.vue

// ==================== 会话结束状态 ====================
.chat-ended {
  padding: 20px;
  text-align: center;
  color: var(--cs-text-muted);
  box-shadow: 0 -1px 4px rgba(0, 0, 0, 0.03);
  flex-shrink: 0;
  background: var(--cs-bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;

  .ended-icon {
    font-size: 16px;
    color: var(--cs-text-muted);
  }
}

// .chat-empty 样式已迁移到 components/CsChatEmptyState.vue

// ==================== 右侧详情面板 ====================
.detail-panel {
  width: 320px;
  height: 100%;
  background: var(--cs-bg-surface);
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 0;
  z-index: 2;
}

.panel-header {
  padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  flex-shrink: 0;
  background: var(--cs-bg-surface);
  color: var(--cs-text-primary);
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px;
  min-height: 0;
  background: var(--cs-bg-card);
}

.info-section {
  margin-bottom: 12px;
  background: var(--cs-bg-surface);
  border-radius: 10px;
  padding: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

  .section-title {
    font-size: 13px;
    color: var(--cs-text-muted);
    margin-bottom: 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-left: 3px solid var(--cs-brand-start);
    padding-left: 8px;
    font-weight: 500;
  }
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 7px 0;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.15s @ease-smooth;

  &:last-child { border-bottom: none; }

  label {
    color: var(--cs-text-muted);
    font-size: 13px;
  }

  .info-value {
    color: var(--cs-text-primary);
    font-size: 13px;
    text-align: right;
    max-width: 180px;
    word-break: break-all;

    &.device-info {
      font-size: 12px;
      max-width: 160px;
    }

    &.device-info-icon {
      display: inline-flex;
      align-items: center;
      font-size: 13px;
    }
  }

  &.editable {
    cursor: pointer;
    border-radius: 6px;
    padding: 7px 4px;
    margin: 0 -4px;

    &:hover {
      background: var(--cs-bg-input);
      .edit-icon { opacity: 1; }
    }

    .edit-icon {
      margin-left: 6px;
      color: var(--cs-brand-start);
      opacity: 0;
      transition: opacity 0.2s @ease-smooth;
    }
  }
}

.star-btn {
  font-size: 18px;
  cursor: pointer;
  color: var(--cs-text-muted);
  transition: color 0.2s @ease-smooth;
  &.active { color: #faad14; }
  &:hover { color: #faad14; }
}

.notes-content {
  background: #f8f9fc;
  padding: 12px;
  border-radius: 8px;
  border-left: 3px solid var(--cs-border);
  font-size: 13px;
  color: var(--cs-text-secondary);
  min-height: 48px;
  white-space: pre-wrap;
}

.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  .add-tag {
    border-style: dashed;
    cursor: pointer;
    &:hover { border-color: var(--cs-brand-start); color: var(--cs-brand-start); }
  }
}

// ==================== 转接弹窗 ====================
// .transfer-content / .agent-card 等样式已迁移到 components/CsTransferConversationModal.vue

// ==================== EmojiPicker 协调 ====================
:deep(.emoji-picker) {
  border-radius: 12px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  border: 1px solid var(--cs-border);
}

// ==================== 拉黑状态高亮 ====================
.info-item.blacklisted {
  background: #fff2f0;
  border-radius: 6px;
  padding: 8px;
  margin: 0 -4px;
}

// ==================== Ant Design 深色模式穿透 ====================
:deep(.ant-drawer-body) {
  background: var(--cs-bg-surface);
  color: var(--cs-text-primary);
}
:deep(.ant-drawer-header) {
  background: var(--cs-bg-surface);
  border-bottom: 3px solid transparent;
  border-image: linear-gradient(135deg, var(--cs-brand-start), var(--cs-brand-end)) 1;
  .ant-drawer-title { color: var(--cs-text-primary); }
  .ant-drawer-close { color: var(--cs-text-secondary); }
}
:deep(.ant-modal-content) {
  background: var(--cs-bg-surface);
  color: var(--cs-text-primary);
}
:deep(.ant-modal-header) {
  background: var(--cs-bg-surface);
  .ant-modal-title { color: var(--cs-text-primary); }
}
:deep(.ant-modal-close) { color: var(--cs-text-secondary); }
:deep(.ant-input),
:deep(.ant-input-affix-wrapper),
:deep(.ant-select-selector) {
  background: var(--cs-bg-input) !important;
  color: var(--cs-text-primary) !important;
  border-color: var(--cs-border) !important;
}
:deep(.ant-select-dropdown) {
  background: var(--cs-bg-surface);
  .ant-select-item { color: var(--cs-text-primary); }
  .ant-select-item-option-active { background: var(--cs-bg-input); }
}
:deep(.ant-tag) {
  background: var(--cs-bg-card);
  color: var(--cs-text-secondary);
  border-color: var(--cs-border);
}
:deep(.ant-divider) {
  border-color: var(--cs-border);
}
:deep(.ant-form-item-label > label) {
  color: var(--cs-text-primary);
}
:deep(.ant-alert) {
  background: var(--cs-bg-card);
  border-color: var(--cs-border);
}

.quick-reply-preview-richtext {
  max-height: 400px;
  overflow-y: auto;
  line-height: 1.6;
  font-size: 14px;

  img {
    max-width: 100%;
    border-radius: 4px;
  }

  p {
    margin: 0 0 8px 0;
  }
}

</style>