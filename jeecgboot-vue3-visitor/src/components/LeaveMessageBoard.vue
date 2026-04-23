<template>
  <div class="leave-message-board">
    <div class="board-header" :style="headerStyle">
      <div class="header-info">
        <img
          class="app-avatar"
          :src="
            chatWindowConfig.logo
              ? resolveFileUrl(chatWindowConfig.logo)
              : appInfo.avatar
              ? resolveFileUrl(appInfo.avatar)
              : defaultAvatar
          "
          @error="onImageError"
        />
        <div class="app-info">
          <span class="app-name">{{ chatWindowConfig.pageTitle || appInfo.name || '在线客服' }}</span>
          <span class="board-subtitle">{{ messageBoardConfig.subtitle || '客服不在线，请留言' }}</span>
        </div>
      </div>
      <div class="header-icons" v-if="chatWindowConfig.headerIcons?.length">
        <a
          v-for="(item, idx) in chatWindowConfig.headerIcons"
          :key="idx"
          :href="item.link || '#'"
          target="_blank"
          rel="noopener"
          class="header-icon-item"
        >
          <img
            v-if="item.icon"
            :src="resolveFileUrl(item.icon)"
            :class="['header-icon-img', { 'header-icon-transparent': item.transparent }]"
            :style="{ width: (item.size || 32) + 'px', height: (item.size || 32) + 'px' }"
            @error="onImageError"
          />
          <span
            class="header-icon-name"
            :style="{
              fontSize: Math.max(10, Math.round((item.size || 32) * 0.35)) + 'px',
              maxWidth: Math.max(40, (item.size || 32) * 1.8) + 'px',
            }"
            >{{ item.name }}</span
          >
        </a>
      </div>
    </div>
    <div class="board-body">
      <a-form :model="leaveMessageForm" layout="vertical">
        <a-form-item label="留言内容" :rules="[{ required: true, message: '请输入留言内容' }]">
          <a-textarea v-model:value="leaveMessageForm.content" placeholder="请输入您的留言" :rows="4" />
        </a-form-item>
        <a-form-item
          v-if="messageBoardConfig.fields?.name?.show"
          label="姓名"
          :rules="messageBoardConfig.fields?.name?.required ? [{ required: true, message: '请输入姓名' }] : []"
        >
          <a-input v-model:value="leaveMessageForm.name" placeholder="请输入姓名" />
        </a-form-item>
        <a-form-item
          v-if="messageBoardConfig.fields?.phone?.show"
          label="手机"
          :rules="messageBoardConfig.fields?.phone?.required ? [{ required: true, message: '请输入手机号' }] : []"
        >
          <a-input v-model:value="leaveMessageForm.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item
          v-if="messageBoardConfig.fields?.email?.show"
          label="邮箱"
          :rules="messageBoardConfig.fields?.email?.required ? [{ required: true, message: '请输入邮箱' }] : []"
        >
          <a-input v-model:value="leaveMessageForm.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item v-if="messageBoardConfig.fields?.qq?.show" label="QQ">
          <a-input v-model:value="leaveMessageForm.qq" placeholder="请输入QQ号" />
        </a-form-item>
        <a-form-item v-if="messageBoardConfig.fields?.wechat?.show" label="微信">
          <a-input v-model:value="leaveMessageForm.wechat" placeholder="请输入微信号" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" block :loading="submitting" @click="onSubmit">提交留言</a-button>
        </a-form-item>
      </a-form>
      <div v-if="submitted" class="submit-success">
        <CheckCircleOutlined style="font-size: 32px; color: #52c41a" />
        <p>留言已提交，客服会尽快回复您</p>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
/**
 * 留言板（独立懒加载组件）
 *
 * 职责：纯 UI + 表单状态。提交逻辑由父级注入（submitFn），保持子组件
 * 不依赖 http / encrypt / auth 模块，使其可在「无访客在线客服」分支
 * 单独 lazy-import，避免与聊天主流程耦合。
 *
 * 父级（ChatMain.vue）必须用 v-if + defineAsyncComponent 包裹，
 * 这样普通聊天链路完全不会拉这个 chunk。
 */
import { ref } from 'vue';
import { message } from 'ant-design-vue';

interface MessageBoardField {
  show?: boolean;
  required?: boolean;
}

interface MessageBoardConfig {
  subtitle?: string;
  fields?: Record<string, MessageBoardField>;
}

interface ChatWindowConfig {
  logo?: string;
  pageTitle?: string;
  headerIcons?: Array<{
    icon?: string;
    link?: string;
    name?: string;
    size?: number;
    transparent?: boolean;
  }>;
  [key: string]: any;
}

interface AppInfo {
  avatar?: string;
  name?: string;
  [key: string]: any;
}

export interface LeaveMessageFormData {
  content: string;
  name: string;
  phone: string;
  email: string;
  qq: string;
  wechat: string;
}

const props = defineProps<{
  messageBoardConfig: MessageBoardConfig;
  chatWindowConfig: ChatWindowConfig;
  appInfo: AppInfo;
  defaultAvatar: string;
  headerStyle: Record<string, any>;
  resolveFileUrl: (url: string) => string;
  onImageError: (e: Event, fallback?: string) => void;
  submitFn: (formData: LeaveMessageFormData) => Promise<void>;
}>();

const leaveMessageForm = ref<LeaveMessageFormData>({
  content: '',
  name: '',
  phone: '',
  email: '',
  qq: '',
  wechat: '',
});
const submitting = ref(false);
const submitted = ref(false);

async function onSubmit() {
  const form = leaveMessageForm.value;
  if (!form.content?.trim()) {
    message.warning('请输入留言内容');
    return;
  }
  const fields = props.messageBoardConfig.fields || {};
  for (const [key, cfg] of Object.entries(fields) as [string, MessageBoardField][]) {
    if (cfg.show && cfg.required && !(form as any)[key]?.trim?.()) {
      const labels: Record<string, string> = {
        name: '姓名',
        phone: '手机',
        email: '邮箱',
        qq: 'QQ',
        wechat: '微信',
        image: '图片',
      };
      message.warning(`请填写${labels[key] || key}`);
      return;
    }
  }

  submitting.value = true;
  try {
    await props.submitFn({ ...form });
    submitted.value = true;
    message.success('留言已提交');
  } catch (e) {
    console.error('[LeaveMessageBoard] 提交留言失败', e);
    message.error('提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
}
</script>

<style lang="less" scoped>
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
</style>
