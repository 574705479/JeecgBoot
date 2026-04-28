import { ref } from 'vue';

const videoPreviewVisible = ref(false);
const videoPreviewUrl = ref('');

const mediaViewerVisible = ref(false);
const mediaViewerList = ref<any[]>([]);

export function useCsMessageMedia() {
  function openVideoPreview(url: string) {
    videoPreviewUrl.value = url || '';
    videoPreviewVisible.value = !!url;
  }

  function closeVideoPreview() {
    videoPreviewVisible.value = false;
    videoPreviewUrl.value = '';
  }

  function openMediaViewer(list: any[]) {
    mediaViewerList.value = Array.isArray(list) ? list : [];
    mediaViewerVisible.value = true;
  }

  function closeMediaViewer() {
    mediaViewerVisible.value = false;
  }

  function openFilePreview(url: string) {
    if (url) {
      window.open(url, '_blank');
    }
  }

  return {
    videoPreviewVisible,
    videoPreviewUrl,
    mediaViewerVisible,
    mediaViewerList,
    openVideoPreview,
    closeVideoPreview,
    openMediaViewer,
    closeMediaViewer,
    openFilePreview,
  };
}
