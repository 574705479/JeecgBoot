<template>
  <div :class="prefixCls">
    <div :class="`${prefixCls}-row`">
      <a-button size="small" @click="openModal(true)">{{ btnText }}</a-button>
      <a-input
        v-if="showInput"
        v-model:value="innerValue"
        :placeholder="inputPlaceholder"
        size="small"
        :style="{ width: inputWidth }"
        @change="onInputChange"
      />
    </div>
    <div v-if="displayUrl" :class="[`${prefixCls}-preview`, previewClass]" :style="mergedPreviewStyle">
      <img :src="displayUrl" alt="preview" @click="openModal(true)" />
      <span :class="`${prefixCls}-delete`" @click.stop="handleDelete" title="删除">
        <DeleteOutlined />
      </span>
    </div>

    <CropperUploadModal
      @register="register"
      :uploadApi="uploadApi"
      :circled="circled"
      :aspectRatio="aspectRatio"
      :accept="accept"
      @uploadSuccess="handleUploadSuccess"
    />
  </div>
</template>

<script lang="ts">
  import { computed, defineComponent, ref, watchEffect, PropType } from 'vue';
  import CropperUploadModal from './CropperUploadModal.vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
  import { DeleteOutlined } from '@ant-design/icons-vue';

  export default defineComponent({
    name: 'CropperUpload',
    components: { CropperUploadModal, DeleteOutlined },
    props: {
      value: { type: String, default: '' },
      uploadApi: { type: Function as PropType<(...args: any[]) => Promise<any>> },
      aspectRatio: { type: Number, default: NaN },
      circled: { type: Boolean, default: false },
      btnText: { type: String, default: '上传图片' },
      showInput: { type: Boolean, default: true },
      inputPlaceholder: { type: String, default: '图片URL' },
      inputWidth: { type: String, default: '180px' },
      accept: { type: String, default: 'image/*' },
      previewStyle: { type: Object, default: () => ({}) },
      previewClass: { type: String, default: '' },
    },
    emits: ['update:value', 'change'],
    setup(props, { emit }) {
      const innerValue = ref(props.value || '');
      const { prefixCls } = useDesign('cropper-upload');
      const [register, { openModal }] = useModal();
      const { createMessage } = useMessage();

      watchEffect(() => {
        innerValue.value = props.value || '';
      });

      /** 将相对路径转换为完整 URL 用于预览显示 */
      const displayUrl = computed(() => {
        const v = innerValue.value;
        if (!v) return '';
        if (v.startsWith('http://') || v.startsWith('https://') || v.startsWith('data:')) return v;
        return getFileAccessHttpUrl(v);
      });

      const mergedPreviewStyle = computed(() => ({
        ...props.previewStyle,
      }));

      function handleUploadSuccess({ url }: { source: string; url: string }) {
        if (!url) {
          createMessage.error('上传失败：未获取到文件地址');
          return;
        }
        innerValue.value = url;
        emit('update:value', url);
        emit('change', url);
        createMessage.success('上传成功');
      }

      function handleDelete() {
        innerValue.value = '';
        emit('update:value', '');
        emit('change', '');
      }

      function onInputChange() {
        emit('update:value', innerValue.value);
        emit('change', innerValue.value);
      }

      return {
        prefixCls,
        innerValue,
        displayUrl,
        mergedPreviewStyle,
        register,
        openModal,
        handleUploadSuccess,
        handleDelete,
        onInputChange,
      };
    },
  });
</script>

<style lang="less" scoped>
  @prefix-cls: ~'@{namespace}-cropper-upload';

  .@{prefix-cls} {
    &-row {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    &-preview {
      position: relative;
      display: inline-block;
      margin-top: 6px;
      border: 1px solid #eee;
      border-radius: 4px;
      overflow: hidden;
      max-width: 200px;
      max-height: 120px;

      img {
        display: block;
        max-width: 100%;
        max-height: 120px;
        object-fit: contain;
        cursor: pointer;
      }

      &:hover .@{prefix-cls}-delete {
        opacity: 1;
      }
    }

    &-delete {
      position: absolute;
      top: 2px;
      right: 2px;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.45);
      color: #fff;
      border-radius: 50%;
      cursor: pointer;
      opacity: 0;
      transition: opacity 0.2s;
      font-size: 11px;

      &:hover {
        background: rgba(255, 0, 0, 0.7);
      }
    }
  }
</style>
