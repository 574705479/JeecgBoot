<template>
  <div
    class="cs-file-chip"
    :class="{ 'is-uploading': uploading, 'is-clickable': !uploading }"
    @click="onClick"
  >
    <div class="cs-file-chip__icon">
      <LoadingOutlined v-if="uploading" spin />
      <component v-else :is="iconComponent" />
    </div>
    <div class="cs-file-chip__main">
      <div class="cs-file-chip__name" :title="displayName">{{ displayName }}</div>
      <div class="cs-file-chip__meta">
        <span v-if="uploading">上传中{{ progressText }}</span>
        <template v-else>
          <span>{{ formattedSize }}</span>
          <span v-if="extLabel" class="cs-file-chip__ext">{{ extLabel }}</span>
        </template>
      </div>
    </div>
    <a-progress
      v-if="uploading && typeof progress === 'number'"
      :percent="progress"
      :show-info="false"
      size="small"
      class="cs-file-chip__progress"
    />
    <a-button
      v-else-if="downloadable && !uploading"
      type="text"
      size="small"
      class="cs-file-chip__action"
      @click.stop="onDownload"
    >
      <DownloadOutlined />
    </a-button>
  </div>
</template>

<script setup lang="ts">
/**
 * FileChip —— 客服 / 访客侧统一的文件附件卡片。
 *
 * 用途：
 *  - 输入区附件预览：上传中（loading + 进度条）/ 上传完成（图标+下载按钮）
 *  - 消息气泡里的文件分支：替换原有的裸 `<span>{{ name }}</span>`，
 *    解决截图 2 中"裸 fid 字符串"的丑陋兜底
 *  - audio 类型由调用方用 `<audio controls>` 直接渲染，本组件不强求覆盖
 *
 * 不在本组件做 cse:// 解密：调用方传入已解析过的 url（withImageCache /
 * downloadCse 由调用方决定），保持组件纯展示。
 */
import { computed } from 'vue';
import {
  FileOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  FileZipOutlined,
  FileImageOutlined,
  FileTextOutlined,
  VideoCameraOutlined,
  CustomerServiceOutlined,
  DownloadOutlined,
  LoadingOutlined,
} from '@ant-design/icons-vue';
import { downloadByUrl } from '/@/utils/file/downloadCse';

interface Props {
  /** 文件名 */
  name?: string;
  /** 字节大小 */
  size?: number;
  /** 文件类型分类（image/video/audio/file/pdf 等） */
  type?: string;
  /** 资源 URL（cse://{fid} 或 http(s)://） */
  url?: string;
  /** 是否处于上传中 */
  uploading?: boolean;
  /** 上传进度百分比（0~100） */
  progress?: number;
  /** 是否显示下载按钮（默认 true，预览区可传 false） */
  downloadable?: boolean;
  /** 点击整张卡片时的行为：'preview' 用 window.open / 'download' 触发下载 / 'none' 不响应 */
  clickAction?: 'preview' | 'download' | 'none';
}

const props = withDefaults(defineProps<Props>(), {
  name: '',
  size: 0,
  type: 'file',
  url: '',
  uploading: false,
  downloadable: true,
  clickAction: 'preview',
});

const emit = defineEmits<{
  (e: 'click', payload: { url: string; name: string }): void;
}>();

const displayName = computed(() => props.name || props.url || '未命名文件');

const ext = computed(() => {
  const n = (props.name || '').toLowerCase();
  const idx = n.lastIndexOf('.');
  return idx >= 0 ? n.slice(idx + 1) : '';
});

const extLabel = computed(() => (ext.value ? ext.value.toUpperCase() : ''));

const formattedSize = computed(() => {
  const bytes = Number(props.size || 0);
  if (!bytes || bytes < 0) return '';
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1024 / 1024).toFixed(1) + ' MB';
});

const progressText = computed(() => (typeof props.progress === 'number' ? ` ${props.progress}%` : ''));

const iconComponent = computed(() => {
  const e = ext.value;
  if (props.type === 'audio' || ['mp3', 'm4a', 'wav', 'ogg', 'opus', 'aac', 'flac'].includes(e)) {
    return CustomerServiceOutlined;
  }
  if (props.type === 'video' || ['mp4', 'webm', 'mov', 'avi', 'mkv', 'flv', '3gp', 'wmv'].includes(e)) {
    return VideoCameraOutlined;
  }
  if (props.type === 'image' || ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg'].includes(e)) {
    return FileImageOutlined;
  }
  if (e === 'pdf') return FilePdfOutlined;
  if (['doc', 'docx', 'rtf'].includes(e)) return FileWordOutlined;
  if (['xls', 'xlsx', 'csv'].includes(e)) return FileExcelOutlined;
  if (['ppt', 'pptx'].includes(e)) return FilePptOutlined;
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(e)) return FileZipOutlined;
  if (['txt', 'md', 'log', 'json', 'xml', 'yml', 'yaml'].includes(e)) return FileTextOutlined;
  return FileOutlined;
});

function onClick() {
  if (props.uploading) return;
  emit('click', { url: props.url, name: props.name });
  if (props.clickAction === 'download') {
    onDownload();
  } else if (props.clickAction === 'preview' && props.url) {
    // 简单预览：cse:// 走解密下载，普通 URL 走 window.open
    if (props.url.startsWith('cse://')) {
      downloadByUrl(props.url, props.name).catch((e) => console.warn('[FileChip] download fail', e));
    } else {
      window.open(props.url, '_blank');
    }
  }
}

async function onDownload() {
  if (!props.url) return;
  try {
    await downloadByUrl(props.url, props.name);
  } catch (e) {
    console.warn('[FileChip] download fail', e);
  }
}
</script>

<style scoped lang="less">
.cs-file-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  background: #fafafa;
  min-width: 180px;
  max-width: 280px;
  position: relative;
  transition: background 0.2s, border-color 0.2s;

  &.is-clickable {
    cursor: pointer;
    &:hover {
      background: #f0f5ff;
      border-color: #91caff;
    }
  }

  &.is-uploading {
    background: #f5f5f5;
    border-style: dashed;
    color: #888;
  }

  &__icon {
    font-size: 24px;
    flex-shrink: 0;
    color: #1677ff;
    .anticon {
      display: block;
    }
  }

  &__main {
    min-width: 0;
    flex: 1;
  }

  &__name {
    font-size: 13px;
    color: #333;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 1.4;
  }

  &__meta {
    font-size: 11px;
    color: #999;
    margin-top: 2px;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  &__ext {
    background: #f0f0f0;
    padding: 0 4px;
    border-radius: 3px;
    font-size: 10px;
  }

  &__progress {
    position: absolute;
    left: 8px;
    right: 8px;
    bottom: 2px;
    margin: 0;
  }

  &__action {
    flex-shrink: 0;
    color: #888;
    &:hover {
      color: #1677ff;
    }
  }
}
</style>
