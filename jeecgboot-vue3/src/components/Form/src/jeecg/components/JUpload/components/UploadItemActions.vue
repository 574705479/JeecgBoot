<template>
  <div v-show="download" class="upload-download-handler">
    <a class="download" title="下载" @click="onDownload">
      <Icon icon="ant-design:download" />
    </a>
  </div>
  <div v-show="mover && list.length > 1" class="upload-mover-handler">
    <a title="向前移动" @click="onMoveForward">
      <Icon icon="ant-design:arrow-left" />
    </a>
    <a title="向后移动" @click="onMoveBack">
      <Icon icon="ant-design:arrow-right" />
    </a>
  </div>
</template>

<script lang="ts" setup>
  import { unref, computed } from 'vue';
  import { Icon } from '/@/components/Icon';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { isCseUrl } from '/@/utils/cse/cseUrl';
  import { downloadCse } from '/@/utils/cse/downloadCse';

  const { createMessage } = useMessage();

  const props = defineProps({
    element: { type: HTMLElement, required: true },
    fileList: { type: Object, required: true },
    /** 【S-P0-6】当前 ant-upload item 在 fileList 中的 uid，用于精准反查（不依赖 img.src 比对） */
    uid: { type: String, required: false, default: '' },
    mover: { type: Boolean, required: true },
    download: { type: Boolean, required: true },
    emitValue: { type: Function, required: true },
  });
  const list = computed(() => unref(props.fileList));

  // 向前移动图片
  function onMoveForward() {
    let index = getIndexByUrl();
    if (index === -1) {
      createMessage.warn('移动失败：' + index);
      return;
    }
    if (index === 0) {
      doSwap(index, unref(list).length - 1);
      return;
    }
    doSwap(index, index - 1);
  }

  // 向后移动图片
  function onMoveBack() {
    let index = getIndexByUrl();
    if (index === -1) {
      createMessage.warn('移动失败：' + index);
      return;
    }
    if (index == unref(list).length - 1) {
      doSwap(index, 0);
      return;
    }
    doSwap(index, index + 1);
  }

  function doSwap(oldIndex, newIndex) {
    if (oldIndex !== newIndex) {
      let array: any[] = [...(unref(list) as Array<any>)];
      let temp = array[oldIndex];
      array[oldIndex] = array[newIndex];
      array[newIndex] = temp;
      props.emitValue(array.map((i) => i.url).join(','));
    }
  }

  function getIndexByUrl() {
    const fileList: any = unref(list);
    // 【S-P0-6 复审修订】优先用 uid 精准反查：cse:// 会被 withImageCache 替换为 blob URL，
    // 而 fileList[i].url 仍是 cse://，img.src 比对永远不命中导致下载/排序错乱。
    // 关键：从宿主 DOM 的 dataset.uid 实时读取（JUpload.addActionsListener 在
    // watch(fileList) 后会把最新 uid 写入 dataset.uid），避免 createApp 挂载时 props.uid
    // 快照陈旧（fileList parsePathsValue 会重新生成所有 uid）。
    const liveUid = props.element?.dataset?.uid || props.uid;
    if (liveUid && fileList?.length) {
      for (let i = 0; i < fileList.length; i++) {
        if (String(fileList[i]?.uid) === String(liveUid)) {
          return i;
        }
      }
    }
    // fallback：旧的 url 比对（向后兼容无 uid 的调用方）
    const url = props.element?.getElementsByTagName('img')[0]?.src;
    if (url && fileList?.length) {
      for (let i = 0; i < fileList.length; i++) {
        let current = fileList[i].url;
        const replace = url.replace(window.location.origin, '');
        if (current === replace || encodeURI(current) === replace) {
          return i;
        }
      }
    }
    return -1;
  }

  function onDownload() {
    // 优先从 fileList 找到当前 item 拿原始 url + 文件名（保留下载文件名 + 兼容 cse://）
    const idx = getIndexByUrl();
    if (idx >= 0) {
      const file = unref(list)[idx];
      const rawUrl = file?.url || '';
      if (isCseUrl(rawUrl)) {
        downloadCse(rawUrl, file?.name).catch(() => {});
        return;
      }
      // 普通 URL：用 a download 触发，保留文件名
      const a = document.createElement('a');
      a.href = rawUrl;
      if (file?.name) a.download = file.name;
      a.style.display = 'none';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      return;
    }
    // fallback：拿 img.src（可能是 blob URL）
    const url = props.element?.getElementsByTagName('img')[0]?.src;
    if (url) window.open(url);
  }
</script>
