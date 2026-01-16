<template>
  <div class="cs-workbench">
    <!-- 左侧会话列表 -->
    <div class="sidebar">
      <!-- 客服状态栏 -->
      <div class="agent-bar">
        <div class="agent-info">
          <a-badge :status="agentStatus === 1 ? 'success' : 'default'" />
          <span class="agent-name">{{ agentName }}</span>
        </div>
        <div class="agent-actions">
          <a-switch 
            v-model:checked="isOnline" 
            checked-children="在线" 
            un-checked-children="离线"
            @change="toggleOnline"
          />
          <a-tooltip title="设置">
            <SettingOutlined class="setting-icon" @click="showSettingsDrawer = true" />
          </a-tooltip>
        </div>
      </div>
      
      <!-- 设置抽屉 -->
      <a-drawer
        v-model:open="showSettingsDrawer"
        title="客服设置"
        placement="right"
        :width="360"
      >
        <div class="settings-content">
          <!-- 客服AI建议应用 -->
          <div class="setting-item">
            <div class="setting-label">
              <ThunderboltOutlined />
              <span>客服AI建议应用</span>
            </div>
            <div class="setting-desc">AI辅助模式下，为客服生成回复建议</div>
            <a-select 
              v-model:value="selectedAppId" 
              placeholder="选择AI应用"
              style="width: 100%;"
              allowClear
              @change="onAppChange"
            >
              <a-select-option v-for="app in aiAppList" :key="app.id" :value="app.id">
                {{ app.name }}
              </a-select-option>
            </a-select>
          </div>
          
          <!-- 访客AI应用（全局配置） -->
          <div class="setting-item">
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
              v-model:value="visitorAppId" 
              placeholder="选择AI应用"
              style="width: 100%;"
              allowClear
              @change="onVisitorAppChange"
            >
              <a-select-option v-for="app in aiAppList" :key="app.id" :value="app.id">
                {{ app.name }}
              </a-select-option>
            </a-select>
          </div>
        </div>
      </a-drawer>

      <!-- 筛选标签 -->
      <div class="filter-tabs">
        <div 
          class="filter-tab" 
          :class="{ active: filter === 'mine' }"
          @click="filter = 'mine'"
        >
          我的 <span class="count">{{ myCount }}</span>
        </div>
        <div 
          class="filter-tab" 
          :class="{ active: filter === 'unassigned' }"
          @click="filter = 'unassigned'"
        >
          待接入 <span class="count">{{ unassignedCount }}</span>
        </div>
        <div 
          class="filter-tab" 
          :class="{ active: filter === 'closed' }"
          @click="filter = 'closed'"
        >
          已结束 <span class="count">{{ closedCount }}</span>
        </div>
        <!-- 全部标签暂时隐藏，避免界面拥挤 -->
        <!-- <div 
          class="filter-tab" 
          :class="{ active: filter === 'all' }"
          @click="filter = 'all'"
        >
          全部
        </div> -->
        <!-- 管理者专属：监控所有进行中的会话 -->
        <div 
          v-if="isSupervisor"
          class="filter-tab supervisor-tab" 
          :class="{ active: filter === 'monitor' }"
          @click="filter = 'monitor'"
        >
          <EyeOutlined /> 监控
        </div>
      </div>

      <!-- 会话列表 -->
      <div class="conversation-list">
        <div
          v-for="conv in conversations"
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
          </div>
          <div class="conv-content">
            <div class="conv-header">
              <span class="conv-name">{{ getDisplayName(conv) }}</span>
              <span class="conv-time">{{ formatTime(conv.lastMessageTime) }}</span>
            </div>
            <div class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</div>
            <!-- 显示当前对话客服 - 从消息列表中获取最后一个发消息的客服 -->
            <div class="conv-agent" v-if="getLastTalkingAgent(conv) && conv.status === 1">
              <span class="agent-label">对话中:</span>
              <span class="agent-name">{{ getLastTalkingAgent(conv) }}</span>
            </div>
          </div>
          <div class="conv-badge" v-if="conv.unreadCount > 0">
            {{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}
          </div>
          <!-- 操作按钮组 -->
          <div class="conv-actions">
            <a-button 
              v-if="conv.status === 0" 
              type="primary" 
              size="small"
              @click.stop="assignConversation(conv.id)"
            >
              接入
            </a-button>
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
        
        <div class="empty-state" v-if="conversations.length === 0">
          <InboxOutlined style="font-size: 48px; color: #ccc;" />
          <p>暂无会话</p>
        </div>
      </div>
    </div>

    <!-- 中间聊天区域 -->
    <div class="chat-area" v-if="currentConversation">
      <!-- 聊天头部 -->
      <div class="chat-header">
        <div class="chat-user">
          <a-avatar :size="40" class="visitor-avatar">{{ getDisplayName(currentConversation).charAt(0) }}</a-avatar>
          <div class="user-info">
            <div class="user-name">
              {{ getDisplayName(currentConversation) }}
              <StarFilled v-if="visitorInfo.star === 1" class="star-icon" />
              <a-tag v-if="visitorInfo.level === 3" color="gold" size="small">VIP</a-tag>
              <a-tag v-else-if="visitorInfo.level === 2" color="blue" size="small">重要</a-tag>
            </div>
            <div class="user-status">
              <a-badge :status="userOnline ? 'success' : 'default'" :text="userOnline ? '在线' : '离线'" />
              <span class="status-divider">|</span>
              <span class="status-text">{{ getModeName(currentReplyMode) }}模式</span>
              <span v-if="currentConversation.status === 1 && currentConversation.ownerAgentName" class="status-divider">|</span>
              <span v-if="currentConversation.status === 1 && currentConversation.ownerAgentName" class="status-text">
                首次接入: {{ currentConversation.ownerAgentName }}
              </span>
            </div>
          </div>
        </div>
        <div class="chat-tools">
          <a-select 
            v-model:value="currentReplyMode" 
            size="small" 
            style="width: 100px"
            @change="changeMode"
            v-if="currentConversation.status === 1"
          >
            <a-select-option :value="0">AI自动</a-select-option>
            <a-select-option :value="1">手动</a-select-option>
          </a-select>
          <a-button size="small" @click="openTransferModal" v-if="currentConversation.status !== 2">
            <SwapOutlined /> 转接
          </a-button>
          <a-button size="small" danger @click="closeConversation" v-if="currentConversation.status !== 2">
            结束
          </a-button>
          <a-button size="small" type="text" @click="showDetailPanel = !showDetailPanel">
            <MenuUnfoldOutlined v-if="!showDetailPanel" />
            <MenuFoldOutlined v-else />
          </a-button>
        </div>
      </div>

      <!-- 消息容器 -->
      <div class="chat-body">
        <div class="messages-container" ref="messagesRef">
          <div v-for="msg in messages" :key="msg.id" class="message-wrapper" :class="getMessageClass(msg)">
            <!-- 系统消息 -->
            <div v-if="msg.senderType === 3" class="system-msg">
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
                  <div v-if="msg.content" class="msg-text" v-html="renderMessage(msg.content)"></div>
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
                      <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview(msg, item)" />
                      <video v-else :src="getAttachmentUrl(item)" controls playsinline />
                      <span v-if="item.type === 'video'" class="play-badge">▶</span>
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
                    <div
                      class="file-item"
                      v-for="(item, index) in getFileAttachments(msg)"
                      :key="`${item.url}_${index}`"
                      @click="openFilePreview(item)"
                    >
                      <span class="file-name">{{ item.name || item.url }}</span>
                    </div>
                  </div>
                </div>
                <div class="msg-meta">
                  {{ formatMessageTime(msg.createTime) }}
                  <!-- AI辅助按钮 - 手动触发AI建议 -->
                  <a-button 
                    type="link" 
                    size="small" 
                    class="ai-assist-btn"
                    :loading="aiSuggestionLoading"
                    @click="requestAiSuggestion(msg.content)"
                    title="获取AI建议回复"
                  >
                    <RobotOutlined /> AI建议
                  </a-button>
                </div>
              </div>
            </template>
            <!-- 客服/AI消息 (显示在右边，类似Telegram自己发送的消息) -->
            <template v-else>
              <div class="msg-body">
                <div class="msg-info">
                  <span class="sender-name">{{ msg.actualSenderName || msg.senderName }}</span>
                  <a-tag v-if="msg.senderType === 1 || msg.isAiGenerated" color="purple" size="small">AI</a-tag>
                  <a-avatar :size="messageAvatarSize" class="msg-avatar-inline">
                    {{ (msg.actualSenderName || msg.senderName)?.charAt(0) || (msg.senderType === 1 ? 'AI' : '客') }}
                  </a-avatar>
                </div>
                <div class="msg-bubble agent-bubble" :class="{ 'ai-bubble': msg.senderType === 1 || msg.isAiGenerated }">
                  <div v-if="msg.content" class="msg-text" v-html="renderMessage(msg.content)"></div>
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
                      <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreview(msg, item)" />
                      <video v-else :src="getAttachmentUrl(item)" controls playsinline />
                      <span v-if="item.type === 'video'" class="play-badge">▶</span>
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
                    <div
                      class="file-item"
                      v-for="(item, index) in getFileAttachments(msg)"
                      :key="`${item.url}_${index}`"
                      @click="openFilePreview(item)"
                    >
                      <span class="file-name">{{ item.name || item.url }}</span>
                    </div>
                  </div>
                </div>
                <div class="msg-meta">{{ formatMessageTime(msg.createTime) }}</div>
              </div>
            </template>
          </div>
        </div>

        <!-- AI建议 -->
        <!-- AI建议展示区（手动触发） -->
        <div class="ai-suggestion" v-if="aiSuggestion">
          <div class="suggestion-label">
            <RobotOutlined /> AI建议回复
            <a-tag v-if="aiSuggestionLoading" color="processing" size="small">生成中...</a-tag>
          </div>
          <div class="suggestion-text" v-html="renderMarkdown(aiSuggestion)"></div>
          <div class="suggestion-btns">
            <a-button type="primary" size="small" @click="useSuggestion(true)" :disabled="aiSuggestionLoading">直接发送</a-button>
            <a-button size="small" @click="useSuggestion(false)" :disabled="aiSuggestionLoading">填入编辑</a-button>
            <a-button size="small" type="text" @click="aiSuggestion = ''">忽略</a-button>
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
            :action="uploadUrl"
            :headers="uploadHeaders"
            :showUploadList="false"
            :multiple="true"
            :beforeUpload="beforeUploadAttachment"
            @change="handleAttachmentChange"
          >
            <a-tooltip title="上传附件">
              <PaperClipOutlined class="toolbar-icon" />
            </a-tooltip>
          </a-upload>
          <a-tooltip title="快捷回复">
            <ThunderboltOutlined class="toolbar-icon" @click="toggleQuickReply" />
          </a-tooltip>
        </div>
        <div class="emoji-panel" v-if="showEmojiPanel">
          <span
            v-for="emoji in emojiList"
            :key="emoji"
            class="emoji-item"
            @click="appendEmoji(emoji)"
          >
            {{ emoji }}
          </span>
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
                  <span>{{ item.title || '无标题' }}</span>
                  <a-tag size="small">{{ item.scope === 'public' ? '公共' : '我的' }}</a-tag>
                </div>
                <div class="quick-reply-content">{{ item.content }}</div>
              </div>
            </div>
            <a-empty v-else description="暂无快捷回复" />
          </a-spin>
        </div>
        <div class="attachment-preview" v-if="attachmentList.length">
          <div
            class="attachment-item"
            v-for="(item, index) in attachmentList"
            :key="`${item.url}_${index}`"
          >
            <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreviewFromList(attachmentList, item)" />
            <video v-else-if="item.type === 'video'" :src="getAttachmentUrl(item)" @click="openVideoPreview(item)" />
            <div v-else class="attachment-file">
              <span class="file-name" @click="openFilePreview(item)">{{ item.name }}</span>
            </div>
            <CloseOutlined class="remove-attachment" @click="removeAttachment(index)" />
          </div>
        </div>
        <div class="input-wrapper">
          <a-textarea
            ref="inputRef"
            v-model:value="inputMessage"
            :placeholder="inputPlaceholder"
            :auto-size="{ minRows: 1, maxRows: 4 }"
            @keydown="handleInputKeydown"
            @paste="handlePasteUpload"
          />
        </div>
        <div class="input-footer">
          <span class="input-hint">Enter 发送 / Ctrl+Enter 或 Shift+Enter 换行</span>
          <a-button type="primary" @click="sendMessage" :disabled="!canSendMessage">
            发送
          </a-button>
        </div>
      </div>
      <div class="chat-ended" v-else>
        <span>会话已结束</span>
      </div>
      <a-modal v-model:open="videoPreviewVisible" :footer="null" width="720px">
        <video v-if="videoPreviewUrl" :src="videoPreviewUrl" controls style="width: 100%;" />
      </a-modal>
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
            <img v-if="item.type === 'image'" :src="getAttachmentUrl(item)" @click="openImagePreviewFromList(mediaViewerList, item)" />
            <video v-else :src="getAttachmentUrl(item)" controls @click="openVideoPreview(item)" />
          </div>
        </div>
      </a-modal>
    </div>

    <!-- 空状态 -->
    <div class="chat-empty" v-else>
      <MessageOutlined style="font-size: 64px; color: #d9d9d9;" />
      <p>选择一个会话开始聊天</p>
    </div>

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

        <!-- 访问信息 -->
        <div class="info-section">
          <div class="section-title">访问信息</div>
          <div class="info-item">
            <label>IP地址</label>
            <span class="info-value">{{ currentConversation.userIp || '-' }}</span>
          </div>
          <div class="info-item">
            <label>设备信息</label>
            <span class="info-value device-info">{{ currentConversation.userDevice || '-' }}</span>
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
    <a-modal 
      v-model:open="showTransferModal" 
      title="转接会话" 
      width="480px"
      :footer="null"
    >
      <div class="transfer-content">
        <div v-if="transferLoading" class="transfer-loading">
          <a-spin />
          <span>加载客服列表...</span>
        </div>
        <div v-else-if="availableAgents.length === 0" class="transfer-empty">
          <InboxOutlined style="font-size: 48px; color: #ccc;" />
          <p>暂无其他在线客服</p>
        </div>
        <div v-else class="agent-list">
          <div 
            v-for="agent in availableAgents" 
            :key="agent.id" 
            class="agent-card"
            @click="doTransfer(agent.id)"
          >
            <a-avatar :size="48" :src="agent.avatar" class="agent-avatar">
              {{ agent.nickname?.charAt(0) || '客' }}
            </a-avatar>
            <div class="agent-info">
              <div class="agent-name">{{ agent.nickname || '客服' }}</div>
              <div class="agent-stats">
                <span>
                  <a-badge :status="agent.status === 1 ? 'success' : 'default'" />
                  {{ agent.status === 1 ? '在线' : '离线' }}
                </span>
                <span>当前接待: {{ agent.currentSessions || 0 }}/{{ agent.maxSessions || 10 }}</span>
              </div>
            </div>
            <a-button type="primary" size="small" class="transfer-btn">
              转接
            </a-button>
          </div>
        </div>
      </div>
    </a-modal>

    <!-- 编辑字段弹窗 -->
    <a-modal 
      v-model:open="showEditModal" 
      :title="editModalTitle" 
      @ok="saveEditField"
      width="400px"
    >
      <a-textarea 
        v-if="editingField === 'notes'"
        v-model:value="editValue" 
        :rows="4"
        placeholder="请输入备注内容"
      />
      <a-input 
        v-else
        v-model:value="editValue" 
        :placeholder="'请输入' + editModalTitle"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, onActivated, onDeactivated, watch, nextTick } from 'vue';
import { message } from 'ant-design-vue';
import { 
  StarFilled, StarOutlined, SwapOutlined, MenuUnfoldOutlined, MenuFoldOutlined,
  CloseOutlined, EditOutlined, PlusOutlined, InboxOutlined, MessageOutlined,
  SmileOutlined, ThunderboltOutlined, RobotOutlined, EyeOutlined, SettingOutlined,
  MoreOutlined, DeleteOutlined, PaperClipOutlined
} from '@ant-design/icons-vue';
import { defHttp } from '/@/utils/http/axios';
import { useGlobSetting } from '/@/hooks/setting';
import { getFileAccessHttpUrl, getHeaders } from '/@/utils/common/compUtils';
import { createImgPreview } from '/@/components/Preview';
// ★ 为AI建议保留Markdown渲染能力
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';

const silentRequestOptions = { successMessageMode: 'none' as const };
const globSetting = useGlobSetting();
const uploadUrl = `${globSetting.uploadUrl}/airag/chat/upload`;
const uploadHeaders = getHeaders();
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

// 客服信息
const agentId = ref('');
const agentName = ref('');
const agentStatus = ref(0);
const isOnline = ref(false);
const agentRole = ref(0); // 0-普通客服, 1-管理者
const isSupervisor = computed(() => agentRole.value === 1);

// AI应用选择
const selectedAppId = ref<string | undefined>(undefined);  // 客服AI建议应用
const visitorAppId = ref<string | undefined>(undefined);   // 访客AI应用
const aiAppList = ref<any[]>([]);
const showSettingsDrawer = ref(false);

// 会话列表
const filter = ref('mine');
const conversations = ref<any[]>([]);
const currentConversation = ref<any>(null);
const currentReplyMode = ref(0);

// 统计
// 统计数据（从后端获取）
const statsData = ref({ myCount: 0, unassignedCount: 0, closedCount: 0 });
const myCount = computed(() => statsData.value.myCount);
const unassignedCount = computed(() => statsData.value.unassignedCount);
const closedCount = computed(() => statsData.value.closedCount);

// 消息
const messages = ref<any[]>([]);
const inputMessage = ref('');
const attachmentList = ref<any[]>([]);
const uploadFileList = ref<any[]>([]);
const videoPreviewVisible = ref(false);
const videoPreviewUrl = ref('');
const mediaViewerVisible = ref(false);
const mediaViewerList = ref<any[]>([]);
const showEmojiPanel = ref(false);
const emojiList = [
  '😀','😁','😂','🤣','😊','😍','😘','😗','😙','😚','😋','😜',
  '🤪','😎','😭','😢','😤','😡','👍','👎','🙏','👏','💪','🔥',
  '🎉','❤️','⭐','🌟','💯','✅'
];
const messagesRef = ref<HTMLElement | null>(null);
const inputRef = ref();
const messageAvatarSize = 32;

// 流式AI消息临时存储 (messageId -> 累积内容)
const streamingMessages = ref<Map<string, string>>(new Map());

// 访客信息
const visitorInfo = ref<any>({});
const visitorTags = ref<string[]>([]);
const showDetailPanel = ref(true);
const userOnline = ref(false);

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
const showEditModal = ref(false);
const editingField = ref('');
const editValue = ref('');
const editModalTitle = computed(() => {
  const titles: Record<string, string> = {
    nickname: '备注昵称',
    realName: '真实姓名',
    phone: '手机号',
    notes: '备注'
  };
  return titles[editingField.value] || '';
});

// AI建议
const aiSuggestion = ref('');
const aiSuggestionLoading = ref(false);

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

// WebSocket
let ws: WebSocket | null = null;
let refreshTimer: number | null = null;
let wsReconnectTimer: number | null = null;
let wsManuallyClosed = false;
const hasMounted = ref(false);
const isActivating = ref(false);
const loadingConversations = ref(false);

function closeWebSocket() {
  wsManuallyClosed = true;
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

// 初始化
onMounted(async () => {
  await loadAgentInfo();
  await loadAiAppList();
  await loadGlobalVisitorApp();  // 加载全局访客AI应用配置
  await loadConversations();
  connectWebSocket();
  hasMounted.value = true;
  
  // ★ 移除定时轮询，完全依赖 WebSocket 实时推送
  // refreshTimer = window.setInterval(() => {
  //   loadConversations();
  // }, 10000);
});

onUnmounted(() => {
  closeWebSocket();
  refreshTimer && clearInterval(refreshTimer);
  if (messagesEl) {
    messagesEl.removeEventListener('scroll', handleMessageScroll);
  }
  if (clearUnreadTimer) {
    clearTimeout(clearUnreadTimer);
    clearUnreadTimer = null;
  }
});

onActivated(async () => {
  // 菜单切换返回时，确保客服在线、会话和WebSocket正常
  if (!hasMounted.value || isActivating.value) return;
  isActivating.value = true;
  try {
    await loadAgentInfo();
    await loadConversations();
    closeWebSocket();
    connectWebSocket();
  } finally {
    isActivating.value = false;
  }
});

onDeactivated(() => {
  // 离开菜单时断开连接，避免后台连接失效导致不再接收消息
  closeWebSocket();
});

watch(filter, () => {
  const cacheKey = getConversationsCacheKey();
  const cached = conversationsCache.get(cacheKey);
  if (cached) {
    conversations.value = cached;
  }
  loadConversations();
});
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

// 加载客服信息
async function loadAgentInfo() {
  try {
    const res = await httpGet({ url: '/cs/agent/current' });
    if (res?.id) {
      agentId.value = res.id;
      agentName.value = res.nickname || '客服';
      agentStatus.value = res.status || 0;
      isOnline.value = res.status === 1;
      agentRole.value = res.role || 0; // 获取角色：0-普通客服, 1-管理者
      
      // 加载客服的AI建议应用设置（每个客服独立配置）
      if (res.defaultAppId) {
        selectedAppId.value = res.defaultAppId;
      }
      // 注意：visitorAppId 是全局配置，在 loadGlobalVisitorApp() 中加载
      
      if (!isOnline.value) {
        await httpPost({ url: `/cs/agent/online/${agentId.value}` });
        agentStatus.value = 1;
        isOnline.value = true;
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
  const supervisor = filter.value === 'monitor' && isSupervisor.value ? '1' : '0';
  return `${agentId.value || 'guest'}_${filter.value}_${supervisor}`;
}

const canSendMessage = computed(() => {
  return inputMessage.value.trim().length > 0 || attachmentList.value.length > 0;
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
  return 'file';
}

function beforeUploadAttachment() {
  return true;
}

function handleAttachmentChange(info: any) {
  const { file, fileList } = info;
  uploadFileList.value = fileList;
  if (file.status === 'error' || (file.response && file.response.code === 500)) {
    message.error(file.response?.message || `${file.name} 上传失败`);
    return;
  }
  if (file.status === 'done') {
    const url = file.response?.message;
    if (!url) return;
    attachmentList.value.push({
      name: file.name,
      url,
      size: file.size,
      type: getAttachmentType(file),
    });
  }
}

async function uploadAttachmentFile(file: File) {
  const isReturn = (fileInfo: any) => {
    try {
      if (fileInfo.code === 0) {
        const url = fileInfo.message;
        if (!url) return;
        attachmentList.value.push({
          name: file.name || 'image',
          url,
          size: file.size,
          type: getAttachmentType(file),
        });
        uploadFileList.value.push(file);
      } else {
        message.error(fileInfo.message || `${file.name} 上传失败`);
      }
    } catch (error) {
      console.error('上传处理失败', error);
      message.error(`${file.name} 上传失败`);
    }
  };
  await defHttp.uploadFile({ url: '/airag/chat/upload' }, { file }, { success: isReturn });
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
  inputMessage.value = `${inputMessage.value}${emoji}`;
  showEmojiPanel.value = false;
  nextTick(() => inputRef.value?.focus());
}

function removeAttachment(index: number) {
  attachmentList.value.splice(index, 1);
  uploadFileList.value.splice(index, 1);
}

function getAttachmentUrl(attachment: any) {
  return getFileAccessHttpUrl(attachment?.url);
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

function openImagePreviewFromList(list: any[], item: any) {
  const images = (list || []).filter(att => att.type === 'image');
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

function openVideoPreview(item: any) {
  videoPreviewUrl.value = getAttachmentUrl(item);
  videoPreviewVisible.value = true;
}

function openFilePreview(item: any) {
  const url = getAttachmentUrl(item);
  if (url) {
    window.open(url, '_blank');
  }
}

function openMediaViewer(msg: any) {
  mediaViewerList.value = getMediaAttachments(msg);
  mediaViewerVisible.value = true;
}

function buildMessagePreview(content: string, attachments: any[]) {
  if (content) return content;
  if (!attachments || attachments.length === 0) return '';
  const labels = new Set<string>();
  attachments.forEach(att => {
    if (att.type === 'image') labels.add('图片');
    else if (att.type === 'video') labels.add('视频');
    else labels.add('文件');
  });
  return `[${Array.from(labels).join('/')}]`;
}

function isMessagesAtBottom() {
  const el = messagesEl || messagesRef.value;
  if (!el) return false;
  const threshold = 40;
  return el.scrollHeight - (el.scrollTop + el.clientHeight) <= threshold;
}

function handleMessageScroll() {
  scheduleClearUnread();
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
  inputMessage.value = item?.content || '';
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

// AI应用切换（设置客服AI建议应用）
async function onAppChange(appId: string | undefined) {
  if (!agentId.value) return;
  
  try {
    await httpPut({
      url: `/cs/agent/${agentId.value}/default-app`,
      data: { appId: appId || '' }
    });
    selectedAppId.value = appId;
    console.log('[Workbench] 客服AI建议应用已更新');
  } catch (e) {
    console.error('设置客服AI建议应用失败', e);
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
    if (res?.appId) {
      visitorAppId.value = res.appId;
    }
  } catch (e) {
    console.error('加载访客AI应用配置失败', e);
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
    agentStatus.value = checked ? 1 : 0;
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
        closedCount: res.closedCount || 0
      };
    }
  } catch (e) {
    console.error('加载统计数据失败', e);
  }
}

// 延迟加载统计数据（防抖，避免频繁调用）
function loadStatsDebounced() {
  if (statsLoadTimer) {
    clearTimeout(statsLoadTimer);
  }
  statsLoadTimer = setTimeout(() => {
    loadStats();
  }, 500); // 500ms 延迟
}

async function loadConversations() {
  const requestId = ++conversationsRequestSeq;
  loadingConversations.value = true;
  try {
    // 同时加载统计数据（不等待，异步执行）
    loadStats().catch(() => {});
    
    // 监控模式：管理者查看所有进行中的会话
    const params: any = { 
      agentId: agentId.value, 
      filter: filter.value, 
      pageNo: 1, 
      pageSize: 50 
    };
    
    // 如果是监控模式，添加管理者标识
    if (filter.value === 'monitor' && isSupervisor.value) {
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
    
    // 保留已有的访客昵称和"对话中"客服名称（从缓存或旧数据中获取）
    newConversations.forEach((conv: any) => {
      // 1. 尝试从访客缓存获取昵称
      const cacheKey = getVisitorCacheKey(conv.appId, conv.userId);
      const cached = visitorCache.get(cacheKey);
      if (cached?.nickname) {
        conv.visitorNickname = cached.nickname;
      } else {
        // 2. 尝试从旧会话列表获取昵称
        const oldConv = conversations.value.find(c => c.id === conv.id);
        if (oldConv?.visitorNickname) {
          conv.visitorNickname = oldConv.visitorNickname;
        }
      }
      
      // 3. 尝试从旧数据中保留"对话中"的客服名称（lastTalkingAgent）
      const oldConv = conversations.value.find(c => c.id === conv.id);
      if (oldConv?.lastTalkingAgent) {
        conv.lastTalkingAgent = oldConv.lastTalkingAgent;
      }
    });
    
    conversations.value = newConversations;
    const cacheKey = getConversationsCacheKey();
    conversationsCache.set(cacheKey, newConversations);
    conversationsCacheTime.set(cacheKey, Date.now());
    
    // 异步预取昵称，避免首次加载显示为“访客”
    newConversations.forEach((conv: any) => {
      if (!conv.visitorNickname) {
        prefetchVisitorNickname(conv);
      }
    });

    if (!currentConversation.value) {
      try {
        const lastId = sessionStorage.getItem(lastConversationStorageKey);
        const lastConv = lastId ? newConversations.find((c: any) => c.id === lastId) : null;
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

// 对会话列表进行排序（未读消息优先，然后按最后消息时间）
function sortConversations() {
  conversations.value.sort((a, b) => {
    // 1. 未读消息优先
    const aUnread = a.unreadCount || 0;
    const bUnread = b.unreadCount || 0;
    if (aUnread > 0 && bUnread === 0) return -1;
    if (aUnread === 0 && bUnread > 0) return 1;
    
    // 2. 按最后消息时间排序
    const aTime = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0;
    const bTime = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0;
    return bTime - aTime;
  });
}

// 选择会话
async function selectConversation(conv: any) {
  // 清理上一个会话残留，避免昵称/头像短暂闪回
  visitorInfo.value = { level: 1, star: 0 };
  visitorTags.value = [];

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
  
  // 切换会话时清除AI建议（AI建议是针对特定会话的）
  aiSuggestion.value = '';
  
  // AI应用使用客服全局设置，不跟随会话变化
  
  await loadMessages(conv.id);
  await loadVisitorInfo(conv.appId, conv.userId);
  
  // 加载消息后，计算"对话中"的客服并缓存
  const listItem = conversations.value.find(c => c.id === conv.id);
  const lastAgent = getLastAgentFromMessages();
  if (lastAgent) {
    conv.lastTalkingAgent = lastAgent;
    // 同步更新会话列表中的对应项
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

// 获取当前正在对话的客服（从缓存或消息中获取）
function getLastTalkingAgent(conv: any): string | null {
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

// 获取显示名称
function getDisplayName(conv: any): string {
  // 优先使用会话对象中缓存的昵称
  if (conv.visitorNickname) {
    return conv.visitorNickname;
  }
  // 如果是当前选中的会话，使用visitorInfo中的昵称
  if (visitorInfo.value.nickname && currentConversation.value?.id === conv.id) {
    return visitorInfo.value.nickname;
  }
  return conv.userName || '访客';
}

function getVisitorDisplayName(msg?: any): string {
  const nickname = currentConversation.value?.visitorNickname || visitorInfo.value?.nickname;
  if (nickname) {
    return nickname;
  }
  if (msg?.senderName) {
    return msg.senderName;
  }
  return '访客';
}

// 加载消息
async function loadMessages(conversationId: string) {
  try {
    const res = await httpGet({
      url: `/cs/message/${conversationId}`,
      params: { limit: 100 }
    });
    messages.value = res || [];
    scrollToBottom();
  } catch (e) {
    console.error('加载消息失败', e);
  }
}

// 加载访客信息（使用缓存优化）
// 注：新版本不再依赖 appId，使用 userId 作为唯一标识
async function loadVisitorInfo(appId: string, userId: string) {
  if (!userId) {
    visitorInfo.value = {};
    visitorTags.value = [];
    return;
  }
  
  // 使用 userId 作为缓存key（兼容有无appId的情况）
  const cacheKey = getVisitorCacheKey(appId, userId);
  const currentCacheKey = getVisitorCacheKey(
    currentConversation.value?.appId,
    currentConversation.value?.userId
  );
  
  // 1. 先从缓存加载（立即显示）
  const cached = visitorCache.get(cacheKey);
  if (cached) {
    if (cacheKey === currentCacheKey) {
      visitorInfo.value = cached;
      visitorTags.value = cached.tags ? JSON.parse(cached.tags) : [];
      // 同步更新会话列表中的昵称
      if (cached.nickname && currentConversation.value) {
        currentConversation.value.visitorNickname = cached.nickname;
      }
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
      // 更新缓存
      visitorCache.set(cacheKey, res);
      
      if (cacheKey !== currentCacheKey) {
        return;
      }
      
      visitorInfo.value = res;
      visitorTags.value = res.tags ? JSON.parse(res.tags) : [];
      
      // 如果有昵称，同步更新到会话列表中对应的会话对象
      if (res.nickname && currentConversation.value) {
        const conv = conversations.value.find(c => c.id === currentConversation.value?.id);
        if (conv) {
          conv.visitorNickname = res.nickname;
        }
        currentConversation.value.visitorNickname = res.nickname;
      }
    } else if (!cached) {
      // 只有没有缓存时才设置默认值
      if (cacheKey === currentCacheKey) {
        visitorInfo.value = { level: 1, star: 0 };
        visitorTags.value = [];
      }
    }
  } catch {
    if (!cached) {
      if (cacheKey === currentCacheKey) {
        visitorInfo.value = { level: 1, star: 0 };
        visitorTags.value = [];
      }
    }
  }
}

// 处理输入框按键事件
function handleInputKeydown(e: KeyboardEvent) {
  // Enter键发送消息（不按其他修饰键）
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault();
    sendMessage();
  }
  // Ctrl+Enter 或 Shift+Enter 换行
  else if (e.key === 'Enter' && (e.ctrlKey || e.shiftKey)) {
    // 让默认行为发生（换行）
    // 不需要手动处理，textarea会自动换行
  }
}

// 发送消息
async function sendMessage() {
  const content = inputMessage.value.trim();
  if (!currentConversation.value) return;
  const attachments = attachmentList.value.slice();
  if (!content && attachments.length === 0) return;
  
  const wasUnassigned = currentConversation.value.status === 0; // 记录是否是待接入状态
  
  try {
    if (currentReplyMode.value === 0) {
      await changeMode(1);
    }
    
    const msgType = attachments.length > 0 ? 5 : 0;
    const extra = attachments.length > 0 ? JSON.stringify({ attachments }) : undefined;
    await httpPost({
      url: '/cs/message/agent/send',
      data: {
        conversationId: currentConversation.value.id,
        agentId: agentId.value,
        agentName: agentName.value,
        content: content,
        msgType,
        extra
      }
    });
    
    // 发送成功后，立即更新会话列表的最后消息和“对话中”客服
    const nowIso = new Date().toISOString();
    currentConversation.value.lastMessage = buildMessagePreview(content, attachments);
    currentConversation.value.lastMessageTime = nowIso;
    if (currentConversation.value.status === 1) {
      currentConversation.value.lastTalkingAgent = agentName.value;
    }
    const listItem = conversations.value.find(c => c.id === currentConversation.value?.id);
    if (listItem) {
      listItem.lastMessage = buildMessagePreview(content, attachments);
      listItem.lastMessageTime = nowIso;
      if (listItem.status === 1) {
        listItem.lastTalkingAgent = agentName.value;
      }
    }
    
    inputMessage.value = '';
    attachmentList.value = [];
    uploadFileList.value = [];
    await loadMessages(currentConversation.value.id);
    
    // ★ 问题3修复：发送消息后清除未读数
    if (currentConversation.value.unreadCount > 0) {
      await httpPost({ url: `/cs/conversation/${currentConversation.value.id}/clear-unread` });
      currentConversation.value.unreadCount = 0;
      
      // 同步更新会话列表中的未读数
      if (listItem) {
        listItem.unreadCount = 0;
      }
    }
    
    // 如果之前是待接入状态，刷新会话列表（因为后端会自动接入）
    if (wasUnassigned) {
      await loadConversations();
      // 更新当前会话状态
      currentConversation.value.status = 1;
      currentConversation.value.ownerAgentId = agentId.value;
      currentReplyMode.value = 1; // 手动模式
    }
  } catch (e) {
    message.error('发送失败');
  }
}

// 接入会话
async function assignConversation(conversationId: string) {
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

// 结束会话
async function closeConversation() {
  if (!currentConversation.value) return;
  
  try {
    await httpPost({ url: `/cs/conversation/${currentConversation.value.id}/close` });
    console.log('[Workbench] 会话已结束');
    currentConversation.value.status = 2;
    
    // 刷新会话列表和统计数据
    await loadConversations();
    await loadStats();
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
    // 过滤掉当前客服自己，保留完整信息
    availableAgents.value = agents
      .filter((a: any) => a.id !== agentId.value && a.status === 1); // 只显示在线客服
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
  if (!visitorInfo.value.id || !currentConversation.value) return;
  
  try {
    await httpPost({
      url: '/airag/cs/visitor/toggleStar',
      data: { id: visitorInfo.value.id }
    });
    visitorInfo.value.star = visitorInfo.value.star === 1 ? 0 : 1;
    
    // 更新缓存
    const cacheKey = getCurrentVisitorCacheKey();
    if (cacheKey) {
      visitorCache.set(cacheKey, { ...visitorInfo.value });
    }
  } catch {
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

// AI建议
// 手动请求AI建议（流式）
async function requestAiSuggestion(userMessage: string) {
  if (!currentConversation.value || !userMessage) return;
  
  aiSuggestionLoading.value = true;
  aiSuggestion.value = ''; // 清空之前的建议，准备接收流式内容
  
  try {
    const res = await httpPost({
      url: `/cs/message/ai-generate/${currentConversation.value.id}`,
      data: { userMessage, agentId: agentId.value }
    });
    
    if (res?.streaming) {
      // 流式模式，建议通过WebSocket推送，保持loading状态
      // loading会在收到 ai_suggestion_complete 时关闭
      console.log('[CS] AI建议正在流式生成...');
    } else if (res?.suggestion) {
      // 非流式模式（兼容旧逻辑）
      aiSuggestion.value = res.suggestion;
      console.log('[Workbench] AI建议已生成');
      aiSuggestionLoading.value = false;
    } else {
      message.warning(res?.message || 'AI暂时无法生成建议');
      aiSuggestionLoading.value = false;
    }
  } catch (e) {
    console.error('获取AI建议失败', e);
    message.error('获取AI建议失败');
    aiSuggestionLoading.value = false;
  }
}

function useSuggestion(direct: boolean) {
  if (direct) {
    inputMessage.value = aiSuggestion.value;
    sendMessage();
  } else {
    inputMessage.value = aiSuggestion.value;
    inputRef.value?.focus();
  }
  aiSuggestion.value = '';
}

// WebSocket
function connectWebSocket() {
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
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer);
    wsReconnectTimer = null;
  }
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host; // 使用当前页面的host（包含端口），通过代理连接
  // 通过前端代理连接后端WebSocket，路径前缀 /jeecgboot
  const wsUrl = `${protocol}//${host}/jeecgboot/ws/cs/agent?userId=${agentId.value}`;
  
  console.log('[CS-WS] 连接WebSocket:', wsUrl);
  ws = new WebSocket(wsUrl);
  
  ws.onopen = () => console.log('[CS-WS] 连接成功');
  ws.onmessage = (event) => {
    try {
      handleWsMessage(JSON.parse(event.data));
    } catch (e) {
      console.error('[CS-WS] 解析消息失败', e);
    }
  };
  ws.onerror = () => {
    if (!wsManuallyClosed) {
      try {
        ws?.close();
      } catch {
        // 忽略关闭异常
      }
    }
  };
  ws.onclose = () => {
    ws = null;
    if (!wsManuallyClosed) {
      wsReconnectTimer = window.setTimeout(connectWebSocket, 5000);
    }
  };
}

function handleWsMessage(data: any) {
  switch (data.type) {
    case 'message':
      // 更新会话列表中的最后消息（不管是否是当前会话）
      const conv = conversations.value.find(c => c.id === data.conversationId);
      if (conv) {
        // ★ 问题1修复：更新最后消息和时间（包含客服消息）
        const previewText = buildMessagePreview(data.content || '', getMessageAttachments({ extra: data.extra }));
        conv.lastMessage = previewText || data.content;
        conv.lastMessageTime = new Date().toISOString();
        if (data.senderType === 0) {
          conv.userOnline = true;
        }
        
        // ★ 问题2修复：如果是客服消息，更新"对话中"的客服名称
        if (data.senderType === 2 && data.senderName && conv.status === 1) {
          conv.lastTalkingAgent = data.senderName;
        }
        
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
        const newMsg = {
          id: data.messageId || Date.now().toString(),
          conversationId: data.conversationId,
          content: data.content,
          msgType: data.msgType,
          extra: data.extra,
          senderType: data.senderType,
          senderId: data.senderId,
          senderName: data.senderName,
          createTime: data.timestamp || new Date().toISOString(),
        };
        // 避免重复添加
        if (!messages.value.find(m => m.id === newMsg.id)) {
          messages.value.push(newMsg);
          scrollToBottom();
          scheduleClearUnread();
        }
      }
      
      // 对会话列表进行重新排序（未读消息优先，然后按时间）
      sortConversations();
      
      // 延迟刷新统计数据（防抖）
      loadStatsDebounced();
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
          content: data.content || '用户不在线，消息未送达',
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
        const assignedAgentName = extraData.agentName
          || (assignedAgentId && assignedAgentId === agentId.value ? agentName.value : '其他客服');
        const assignedConv = conversations.value.find(c => c.id === extraData.conversationId);
        if (assignedConv) {
          // 更新会话状态
          assignedConv.status = 1; // 已分配
          assignedConv.ownerAgentId = assignedAgentId;
          assignedConv.ownerAgentName = assignedAgentName;
          assignedConv.assignTime = new Date().toISOString();
          
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
          }
          
          // 如果不是当前客服接入的，显示提示
          if (assignedAgentId && assignedAgentId !== agentId.value) {
            console.log('[Workbench] 会话已被接入:', assignedAgentName);
          }
        }
        
        // 延迟刷新统计数据（防抖）
        loadStatsDebounced();
      }
      break;
    case 'new_conversation':
      // 新会话通知 - 只在待接入列表中实时添加
      {
        console.log('[Workbench] 有新的会话接入');
        
        // ★ 只有在待接入列表时才添加到当前列表
        if (filter.value === 'unassigned') {
          // 检查是否已经在列表中（避免重复）
          const exists = conversations.value.find(c => c.id === data.conversationId);
          if (!exists) {
            // 构建新会话对象
            const newConv: any = {
              id: data.conversationId,
              userId: data.senderId,
              userName: data.senderName || '访客',
              appId: data.extra?.appId,
              status: data.extra?.status || 0,
              replyMode: data.extra?.replyMode || 0,
              createTime: data.extra?.createTime || new Date().toISOString(),
              lastMessageTime: data.extra?.createTime || new Date().toISOString(),
              lastMessage: '会话已创建',
              unreadCount: 0,
              messageCount: 0,
            };
            
            // 添加到列表头部
            conversations.value.unshift(newConv);
          }
        }
        
        // ★ 无论在哪个列表，都更新统计数据（防抖）
        loadStatsDebounced();
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
        
        // 刷新统计数据
        loadStatsDebounced();
      }
      break;
    case 'conversation_transferred':
      // 会话转接通知 - 广播给所有客服
      {
        // ★ 数据在 extra 字段中
        const extraData = data.extra || data;
        
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
          
          // 直接刷新列表，确保统计数据和列表数据一致
          loadConversations();
          
          // 显示提示消息
          if (extraData.fromAgentName) {
            console.log('[Workbench] 收到转接会话:', extraData.fromAgentName, extraData.conversation?.userName || '访客');
          } else {
            console.log('[Workbench] 收到新的转接会话');
          }
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
          
          // 刷新统计数据
          loadStatsDebounced();
        }
        // 如果是其他客服（旁观者）
        else {
          console.log('[Workbench] 我是旁观者');
          
          // 如果会话在列表中，更新负责客服信息
          if (transferredConv) {
            transferredConv.ownerAgentId = extraData.toAgentId;
            transferredConv.ownerAgentName = extraData.toAgentName;
          }
          
          // 如果是当前选中的会话，更新显示
          if (currentConversation.value?.id === conversationId) {
            currentConversation.value.ownerAgentId = extraData.toAgentId;
            currentConversation.value.ownerAgentName = extraData.toAgentName;
          }
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
        const statusText = statusData.statusText;
        
        // 如果转接列表正在显示，刷新可用客服列表
        if (showTransferModal.value) {
          loadAvailableAgents();
        }
        
        // 显示提示（仅当其他客服状态变化时）
        if (changedAgentId !== agentId.value) {
          const agentName = statusData.agentName || '客服';
          // 只在上线/下线时提示，忙碌状态不提示
          if (newStatus === 1) {
            console.log('[Workbench] 客服已上线:', agentName);
          } else if (newStatus === 0) {
            console.log('[Workbench] 客服已下线:', agentName);
          }
        }
      }
      break;
    case 'ai_suggestion':
      if (currentConversation.value?.id === data.conversationId) {
        aiSuggestion.value = data.content;
      }
      break;
    case 'ai_suggestion_stream':
      // AI建议流式消息
      if (currentConversation.value?.id === data.conversationId) {
        // 累积流式内容
        aiSuggestion.value = (aiSuggestion.value || '') + data.content;
      }
      break;
    case 'ai_suggestion_complete':
      // AI建议生成完成
      if (currentConversation.value?.id === data.conversationId) {
        aiSuggestion.value = data.content;
        aiSuggestionLoading.value = false;
        console.log('[Workbench] AI建议已生成');
      }
      break;
    case 'ai_suggestion_error':
      // AI建议生成失败
      if (currentConversation.value?.id === data.conversationId) {
        aiSuggestionLoading.value = false;
        message.error(data.error || 'AI建议生成失败');
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
    case 'ai_stream_complete':
      // AI流式消息完成
      if (currentConversation.value?.id === data.conversationId) {
        handleAiStreamComplete(data);
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
    case 'conversation_closed':
      if (currentConversation.value?.id === data.conversationId) {
        currentConversation.value.status = 2;
      }
      loadConversations();
      break;
  }
}

// 工具函数
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
  return new Date(time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

function formatDateTime(time: string) {
  if (!time) return '-';
  return new Date(time).toLocaleString('zh-CN');
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
  if (msg.senderType === 3) return 'system';
  return msg.senderType === 0 ? 'user' : 'agent';
}

// 渲染消息内容（与访客端保持一致：简单HTML转义 + 换行转换）
// ★ 初始化Markdown渲染器（仅用于AI建议）
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value;
      } catch (__) {}
    }
    return '';
  }
});

// 渲染消息内容（普通消息 - 简单换行转换）
function renderMessage(content: string) {
  if (!content) return '';
  const hasHtml = /<([a-z][\s\S]*?)>/i.test(content);
  const hasMarkdown = /!\[[^\]]*]\([^)]*\)|\*\*[^*]+\*\*|```|^\s*#/m.test(content);
  if (hasHtml || hasMarkdown) {
    return md.render(content);
  }
  // 简单的换行转换，与访客端相同
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>');
}

// ★ 渲染AI建议内容（保留Markdown渲染）
function renderMarkdown(content: string) {
  if (!content) return '';
  try {
    return md.render(content);
  } catch (e) {
    console.error('Markdown渲染失败', e);
    return renderMessage(content); // 降级到普通文本
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
      isStreaming: true,
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
  
  scrollToBottom();
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
      scheduleClearUnread();
    }
  });
}
</script>

<style lang="less" scoped>
.cs-workbench {
  display: flex;
  height: calc(100vh - 110px); // 减去顶部导航、标签页和底部边距
  max-height: calc(100vh - 110px);
  background: #f0f2f5;
  overflow: hidden;
  position: relative;
  box-sizing: border-box;
}

// 左侧会话列表
.sidebar {
  width: 280px;
  height: 100%;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.agent-bar {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  
  .agent-info {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .agent-name {
    font-weight: 500;
  }
  
  .agent-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .setting-icon {
    font-size: 18px;
    color: #666;
    cursor: pointer;
    transition: color 0.2s;
    
    &:hover {
      color: #1890ff;
    }
  }
}

// 设置抽屉样式
.settings-content {
  .setting-item {
    margin-bottom: 24px;
    
    .setting-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 8px;
      color: #333;
      
      .anticon {
        color: #1890ff;
      }
    }
    
    .setting-desc {
      font-size: 12px;
      color: #999;
      margin-bottom: 12px;
    }
  }
}

.ai-app-selector-hidden {
  // 移除原来的AI应用选择器样式（已移到设置抽屉中）
  display: none;
}

.filter-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
  
  .selector-label {
    font-size: 12px;
    color: #666;
    white-space: nowrap;
  }
}

.filter-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  
  .filter-tab {
    flex: 1;
    padding: 10px;
    text-align: center;
    cursor: pointer;
    font-size: 13px;
    color: #666;
    border-bottom: 2px solid transparent;
    transition: all 0.2s;
    
    &:hover {
      color: #1890ff;
    }
    
    &.active {
      color: #1890ff;
      border-bottom-color: #1890ff;
      font-weight: 500;
    }
    
    .count {
      background: #f0f0f0;
      padding: 0 6px;
      border-radius: 10px;
      font-size: 12px;
      margin-left: 4px;
    }
    
    // 管理者监控标签特殊样式
    &.supervisor-tab {
      color: #722ed1;
      
      &:hover {
        color: #9254de;
      }
      
      &.active {
        color: #722ed1;
        border-bottom-color: #722ed1;
        background: linear-gradient(to bottom, rgba(114, 46, 209, 0.05), transparent);
      }
    }
  }
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  border-bottom: 1px solid #f5f5f5;
  position: relative;
  transition: background 0.2s;
  
  &:hover {
    background: #fafafa;
    
    .conv-actions {
      opacity: 1;
    }
  }
  
  &.active {
    background: #e6f7ff;
  }
  
  &.unread {
    background: #fffbe6;
  }
  
  &.closed {
    opacity: 0.7;
    
    .conv-avatar {
      filter: grayscale(0.5);
    }
  }
  
  .conv-content {
    flex: 1;
    min-width: 0;
  }
  
  .conv-header {
    display: flex;
    justify-content: space-between;
    margin-bottom: 4px;
  }
  
  .conv-name {
    font-weight: 500;
    font-size: 14px;
  }
  
  .conv-time {
    font-size: 12px;
    color: #999;
  }
  
  .conv-preview {
    font-size: 13px;
    color: #666;
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
    
    .agent-label {
      color: #999;
    }
    
    .agent-name {
      color: #1890ff;
      font-weight: 500;
    }
  }
  
  .conv-badge {
    background: #ff4d4f;
    color: #fff;
    font-size: 12px;
    padding: 0 6px;
    border-radius: 10px;
    min-width: 18px;
    text-align: center;
    flex-shrink: 0;
  }
  
  .conv-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
    opacity: 0;
    transition: opacity 0.2s;
  }
}

.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: #999;
}

// 中间聊天区域
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  min-width: 0;
  height: 100%;
  overflow: hidden;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
  background: #fff;
  z-index: 1;
  
  .chat-user {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  
  .user-info {
    .user-name {
      font-size: 16px;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 6px;
      
      .star-icon {
        color: #faad14;
      }
    }
    
    .user-status {
      font-size: 12px;
      color: #999;
      margin-top: 2px;
      display: flex;
      align-items: center;
      gap: 8px;
      
      .status-divider {
        color: #d9d9d9;
      }
    }
  }
  
  .chat-tools {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.chat-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0; // 关键：让flex子元素可以收缩
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 20px;
  min-height: 0; // 关键：让flex子元素可以收缩
}

.message-wrapper {
  display: flex;
  margin-bottom: 16px;
  
  &.system {
    justify-content: center;
    
    .system-msg {
      background: #f5f5f5;
      color: #999;
      font-size: 12px;
      padding: 4px 12px;
      border-radius: 12px;
    }
  }
  
  &.user {
    .msg-avatar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }
  }
  
  &.agent {
    flex-direction: row-reverse;
    
    .msg-body {
      align-items: flex-end;
    }
    
    .msg-avatar {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    }
  }
}

.visitor-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  flex-shrink: 0;
}

.msg-body {
  display: flex;
  flex-direction: column;
  max-width: 65%;
  margin: 0 10px;
}

.msg-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  
  .sender-name {
    font-size: 12px;
    color: #999;
  }
  
  .msg-avatar-inline {
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    color: #fff;
    font-size: 12px;
    flex-shrink: 0;
  }
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  word-break: break-word;
  
  &.user-bubble {
    background: #f0f0f0;
    border-top-left-radius: 4px;
  }
  
  &.agent-bubble {
    background: #e6f7ff;
    border-top-right-radius: 4px;
    
    &.ai-bubble {
      background: #f9f0ff;
    }
  }
  
  .msg-text {
    // 简单的文本显示，支持换行
    font-size: 14px;
    line-height: 1.6;
    word-wrap: break-word;

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
  color: #bbb;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 8px;
  
  .ai-assist-btn {
    font-size: 11px;
    padding: 0 4px;
    height: auto;
    color: #7c3aed;
    
    &:hover {
      color: #6d28d9;
    }
  }
}

// AI建议
.ai-suggestion {
  margin: 0 20px 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f5f0ff 0%, #ede7f6 100%);
  border-radius: 8px;
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
    background: #fff;
    padding: 12px 16px;
    border-radius: 6px;
    margin-bottom: 10px;
    font-size: 14px;
    max-height: 300px;
    overflow-y: auto;
    word-wrap: break-word;
    line-height: 1.6;

    :deep(p) {
      margin: 0 0 8px;
      &:last-child {
        margin-bottom: 0;
      }
    }

    :deep(ul), :deep(ol) {
      padding-left: 20px;
      margin: 8px 0;
    }

    :deep(pre) {
      background: #f0f0f0;
      padding: 8px 12px;
      border-radius: 8px;
      overflow-x: auto;
    }

    :deep(code) {
      background: #e8e8e8;
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 13px;
    }

    :deep(img) {
      max-width: 100%;
      height: auto;
      border-radius: 8px;
      display: block;
      margin: 4px 0;
    }
  }
  
  .suggestion-btns {
    display: flex;
    gap: 8px;
  }
}

// 输入区域 - 固定在底部
.chat-input-area {
  border-top: 1px solid #f0f0f0;
  padding: 12px 20px;
  flex-shrink: 0;
  background: #fff;
  
  .input-toolbar {
    display: flex;
    gap: 12px;
    margin-bottom: 8px;
    
    .toolbar-icon {
      font-size: 18px;
      color: #999;
      cursor: pointer;
      
      &:hover {
        color: #1890ff;
      }
    }
  }

  .quick-reply-panel {
    margin-bottom: 8px;
    padding: 8px;
    background: #fafafa;
    border: 1px solid #f0f0f0;
    border-radius: 6px;

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
      gap: 8px;
    }

    .quick-reply-item {
      padding: 8px;
      background: #fff;
      border: 1px solid #f0f0f0;
      border-radius: 6px;
      cursor: pointer;

      &:hover {
        border-color: #1890ff;
      }
    }

    .quick-reply-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-weight: 500;
      margin-bottom: 4px;
      font-size: 13px;
    }

    .quick-reply-content {
      font-size: 12px;
      color: #666;
      white-space: pre-wrap;
    }
  }
  
  .input-wrapper {
    :deep(.ant-input) {
      border: none;
      resize: none;
      font-size: 14px;
      
      &:focus {
        box-shadow: none;
      }
    }
  }
  
  .input-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 8px;
    
    .input-hint {
      font-size: 12px;
      color: #bbb;
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
      border: 1px solid #f0f0f0;
      border-radius: 6px;
      overflow: hidden;
      background: #fff;
      display: flex;
      align-items: center;
      justify-content: center;

      img,
      video {
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
        font-size: 12px;
        color: #666;
        background: #fff;
        border-radius: 50%;
        cursor: pointer;
      }
    }
  }

  .emoji-panel {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    padding: 8px;
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 6px;
    margin-bottom: 8px;

    .emoji-item {
      font-size: 18px;
      cursor: pointer;
      line-height: 1;
    }
  }
}

.msg-attachments {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .attachment-item {
    max-width: 240px;
    img,
    video {
      max-width: 100%;
      border-radius: 6px;
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
    border-radius: 6px;
    overflow: hidden;
    background: #f5f5f5;
    cursor: pointer;

    img,
    video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .play-badge {
      position: absolute;
      right: 6px;
      bottom: 6px;
      background: rgba(0, 0, 0, 0.6);
      color: #fff;
      font-size: 12px;
      padding: 2px 4px;
      border-radius: 4px;
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


.msg-file-list {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 6px;

  .file-item {
    padding: 6px 8px;
    background: #f7f7f7;
    border-radius: 6px;
    cursor: pointer;
    font-size: 12px;
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

.chat-ended {
  padding: 16px 20px;
  text-align: center;
  color: #999;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
  background: #fff;
}

.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  background: #fff;
  height: 100%;
}

// 右侧详情面板
.detail-panel {
  width: 320px;
  height: 100%;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
  flex-shrink: 0;
  background: #fff;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px;
  min-height: 0;
}

.info-section {
  margin-bottom: 20px;
  
  .section-title {
    font-size: 13px;
    color: #999;
    margin-bottom: 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
  
  label {
    color: #666;
    font-size: 13px;
  }
  
  .info-value {
    color: #333;
    font-size: 13px;
    text-align: right;
    max-width: 180px;
    word-break: break-all;
    
    &.device-info {
      font-size: 12px;
      max-width: 160px;
    }
  }
  
  &.editable {
    cursor: pointer;
    
    &:hover {
      background: #fafafa;
      
      .edit-icon {
        opacity: 1;
      }
    }
    
    .edit-icon {
      margin-left: 6px;
      color: #1890ff;
      opacity: 0;
      transition: opacity 0.2s;
    }
  }
}

.star-btn {
  font-size: 18px;
  cursor: pointer;
  color: #d9d9d9;
  
  &.active {
    color: #faad14;
  }
  
  &:hover {
    color: #faad14;
  }
}

.notes-content {
  background: #fafafa;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #666;
  min-height: 60px;
  white-space: pre-wrap;
}

.tags-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  
  .add-tag {
    border-style: dashed;
    cursor: pointer;
  }
}

// 转接弹窗样式
.transfer-content {
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.transfer-loading, .transfer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
  gap: 12px;
}

.agent-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agent-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  
  &:hover {
    background: #e6f7ff;
    border-color: #1890ff;
    
    .transfer-btn {
      opacity: 1;
    }
  }
  
  .agent-avatar {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    flex-shrink: 0;
  }
  
  .agent-info {
    flex: 1;
    min-width: 0;
    
    .agent-name {
      font-size: 15px;
      font-weight: 500;
      color: #333;
      margin-bottom: 4px;
    }
    
    .agent-stats {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: #999;
    }
  }
  
  .transfer-btn {
    opacity: 0;
    transition: opacity 0.2s;
  }
}
</style>
