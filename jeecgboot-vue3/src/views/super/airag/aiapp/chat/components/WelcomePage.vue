<template>
  <div class="welcome-page" :style="{ '--primary-color': primaryColor }">
    <div class="welcome-content">
      <!-- AI头像/Logo -->
      <div class="welcome-avatar">
        <div class="avatar-circle">
          <img v-if="appIcon" :src="appIcon" alt="AI" class="avatar-img" />
          <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" class="avatar-svg">
            <path fill="currentColor" d="M573 421c-23.1 0-41 17.9-41 40s17.9 40 41 40c21.1 0 39-17.9 39-40s-17.9-40-39-40m-280 0c-23.1 0-41 17.9-41 40s17.9 40 41 40c21.1 0 39-17.9 39-40s-17.9-40-39-40"/>
            <path fill="currentColor" d="M894 345c-48.1-66-115.3-110.1-189-130v.1c-17.1-19-36.4-36.5-58-52.1c-163.7-119-393.5-82.7-513 81c-96.3 133-92.2 311.9 6 439l.8 132.6c0 3.2.5 6.4 1.5 9.4c5.3 16.9 23.3 26.2 40.1 20.9L309 806c33.5 11.9 68.1 18.7 102.5 20.6l-.5.4c89.1 64.9 205.9 84.4 313 49l127.1 41.4c3.2 1 6.5 1.6 9.9 1.6c17.7 0 32-14.3 32-32V753c88.1-119.6 90.4-284.9 1-408M323 735l-12-5l-99 31l-1-104l-8-9c-84.6-103.2-90.2-251.9-11-361c96.4-132.2 281.2-161.4 413-66c132.2 96.1 161.5 280.6 66 412c-80.1 109.9-223.5 150.5-348 102m505-17l-8 10l1 104l-98-33l-12 5c-56 20.8-115.7 22.5-171 7l-.2-.1C613.7 788.2 680.7 742.2 729 676c76.4-105.3 88.8-237.6 44.4-350.4l.6.4c23 16.5 44.1 37.1 62 62c72.6 99.6 68.5 235.2-8 330"/>
            <path fill="currentColor" d="M433 421c-23.1 0-41 17.9-41 40s17.9 40 41 40c21.1 0 39-17.9 39-40s-17.9-40-39-40"/>
          </svg>
        </div>
      </div>

      <!-- 欢迎标题 -->
      <h1 class="welcome-title">{{ displayTitle }}</h1>
      
      <!-- 欢迎描述 -->
      <p class="welcome-desc">{{ displayDesc }}</p>

      <!-- 用户信息 -->
      <div v-if="externalUserName" class="user-info">
        <span class="user-label">您好，</span>
        <span class="user-name">{{ externalUserName }}</span>
      </div>

      <!-- 快捷问题列表 -->
      <div v-if="quickQuestions && quickQuestions.length > 0" class="quick-questions">
        <p class="section-title">您可能想问：</p>
        <div class="question-list">
          <div 
            v-for="(question, index) in quickQuestions" 
            :key="index"
            class="question-item"
            @click="handleQuestionClick(question)"
          >
            <span class="question-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                <line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
            </span>
            <span class="question-text">{{ question.name || question }}</span>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <button class="btn-primary" @click="handleStartChat">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          开始对话
        </button>
        <button v-if="showTransferHuman" class="btn-secondary" @click="handleTransferHuman">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
            <circle cx="12" cy="7" r="4"/>
          </svg>
          转人工客服
        </button>
      </div>

      <!-- 隐私声明 -->
      <div class="privacy-notice">
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <span>您的对话内容将被安全加密存储</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface QuickQuestion {
  name?: string;
  descr?: string;
}

const props = defineProps<{
  // 应用名称
  appName?: string;
  // 应用图标
  appIcon?: string;
  // 欢迎标题（URL参数传入）
  welcomeTitle?: string;
  // 欢迎描述（URL参数传入）
  welcomeDesc?: string;
  // 开场白（应用配置）
  prologue?: string;
  // 快捷问题列表
  quickQuestions?: (QuickQuestion | string)[];
  // 第三方用户名称
  externalUserName?: string;
  // 主题色
  primaryColor?: string;
  // 是否显示转人工按钮
  showTransferHuman?: boolean;
}>();

const emit = defineEmits<{
  (e: 'start-chat'): void;
  (e: 'question-click', question: QuickQuestion | string): void;
  (e: 'transfer-human'): void;
}>();

// 显示标题（优先URL参数，其次应用名称）
const displayTitle = computed(() => {
  if (props.welcomeTitle) {
    return props.welcomeTitle;
  }
  if (props.appName) {
    return `欢迎使用 ${props.appName}`;
  }
  return '欢迎使用智能客服';
});

// 显示描述（优先URL参数，其次开场白）
const displayDesc = computed(() => {
  if (props.welcomeDesc) {
    return props.welcomeDesc;
  }
  if (props.prologue) {
    return props.prologue;
  }
  return '我是您的AI助手，有什么可以帮您的吗？';
});

// 主题色
const primaryColor = computed(() => {
  return props.primaryColor || '#155eef';
});

// 点击快捷问题
function handleQuestionClick(question: QuickQuestion | string) {
  emit('question-click', question);
}

// 开始对话
function handleStartChat() {
  emit('start-chat');
}

// 转人工客服
function handleTransferHuman() {
  emit('transfer-human');
}
</script>

<style scoped lang="less">
.welcome-page {
  --primary-color: #155eef;
  
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  min-height: 100%;
  flex: 1;
  padding: 40px 20px;
  background: linear-gradient(180deg, #f8fafc 0%, #fff 100%);
  overflow-y: auto;
  box-sizing: border-box;
}

.welcome-content {
  max-width: 500px;
  width: 100%;
  text-align: center;
}

.welcome-avatar {
  margin-bottom: 24px;
  
  .avatar-circle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--primary-color) 0%, #4f87f7 100%);
    box-shadow: 0 8px 24px rgba(21, 94, 239, 0.25);
    
    .avatar-img {
      width: 60px;
      height: 60px;
      border-radius: 50%;
      object-fit: cover;
    }
    
    .avatar-svg {
      width: 40px;
      height: 40px;
      color: white;
    }
  }
}

.welcome-title {
  margin: 0 0 12px;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}

.welcome-desc {
  margin: 0 0 24px;
  font-size: 15px;
  color: #666;
  line-height: 1.6;
}

.user-info {
  display: inline-flex;
  align-items: center;
  padding: 8px 16px;
  margin-bottom: 24px;
  background: #f0f5ff;
  border-radius: 20px;
  font-size: 14px;
  
  .user-label {
    color: #666;
  }
  
  .user-name {
    color: var(--primary-color);
    font-weight: 500;
    margin-left: 4px;
  }
}

.quick-questions {
  margin-bottom: 32px;
  text-align: left;
  
  .section-title {
    margin: 0 0 12px;
    font-size: 14px;
    color: #999;
  }
  
  .question-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  
  .question-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    background: white;
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
    
    &:hover {
      border-color: var(--primary-color);
      background: #f8faff;
      transform: translateX(4px);
    }
    
    .question-icon {
      flex-shrink: 0;
      width: 20px;
      height: 20px;
      margin-right: 12px;
      color: var(--primary-color);
      
      svg {
        width: 100%;
        height: 100%;
      }
    }
    
    .question-text {
      font-size: 14px;
      color: #333;
      line-height: 1.4;
    }
  }
}

.action-buttons {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 32px;
  
  button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 12px 24px;
    font-size: 15px;
    font-weight: 500;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
    
    svg {
      width: 18px;
      height: 18px;
    }
  }
  
  .btn-primary {
    background: var(--primary-color);
    color: white;
    border: none;
    box-shadow: 0 4px 12px rgba(21, 94, 239, 0.3);
    
    &:hover {
      background: #0d4ed3;
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(21, 94, 239, 0.4);
    }
  }
  
  .btn-secondary {
    background: white;
    color: #666;
    border: 1px solid #e5e7eb;
    
    &:hover {
      border-color: var(--primary-color);
      color: var(--primary-color);
      background: #f8faff;
    }
  }
}

.privacy-notice {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #999;
  
  svg {
    width: 14px;
    height: 14px;
  }
}

// 移动端适配
@media (max-width: 480px) {
  .welcome-page {
    padding: 30px 16px;
  }
  
  .welcome-title {
    font-size: 20px;
  }
  
  .action-buttons {
    flex-direction: column;
    
    button {
      width: 100%;
    }
  }
}
</style>
