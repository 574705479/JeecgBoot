<template>
  <div>
    <a-modal v-model:open="videoPreviewVisible" :footer="null" width="720px">
      <video v-if="videoPreviewUrl" :src="videoPreviewUrl" controls style="width: 100%;" />
    </a-modal>

    <a-modal
      v-model:open="mediaViewerVisible"
      :footer="null"
      width="820px"
      class="media-viewer-modal"
      title="媒体预览"
    >
      <div class="media-viewer-header">
        <span>共 {{ mediaViewerList.length }} 项</span>
        <span class="media-viewer-tip">点击图片可放大，视频可播放</span>
      </div>
      <div class="media-viewer-grid">
        <div
          v-for="(item, index) in mediaViewerList"
          :key="`${item.url}_${index}`"
          class="media-viewer-item"
        >
          <img
            v-if="item.type === 'image'"
            :src="props.getThumbUrl(item)"
            @click="props.onImageClick(mediaViewerList, item)"
            @error="props.onImageError($event, item)"
          />
          <video
            v-else-if="props.getUrl(item)"
            :src="props.getUrl(item)"
            controls
            preload="metadata"
            @click="onVideoClick(item)"
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { useCsMessageMedia } from '../composables/useCsMessageMedia';

defineOptions({ name: 'CsMediaPreviewModals' });

const props = defineProps<{
  getThumbUrl: (item: any) => string;
  getUrl: (item: any) => string;
  onImageError: (e: Event, item: any) => void;
  onImageClick: (list: any[], item: any) => void;
}>();

const {
  videoPreviewVisible,
  videoPreviewUrl,
  mediaViewerVisible,
  mediaViewerList,
  openVideoPreview,
} = useCsMessageMedia();

function onVideoClick(item: any) {
  const url = props.getUrl(item);
  openVideoPreview(url);
}
</script>

<style lang="less" scoped>
@ease-smooth: cubic-bezier(0.34, 1.56, 0.64, 1);

.media-viewer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 10px;
}

.media-viewer-item {
  border-radius: 10px;
  overflow: hidden;
  background: var(--cs-bg-card);
  border: 1px solid var(--cs-border);
  aspect-ratio: 16 / 9;
  transition: transform 0.15s @ease-smooth, box-shadow 0.15s @ease-smooth;
  img, video {
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
  color: var(--cs-text-secondary);
  font-size: 13px;
}

.media-viewer-tip {
  color: var(--cs-text-muted);
}
</style>
