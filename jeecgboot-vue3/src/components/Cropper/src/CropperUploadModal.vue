<template>
  <BasicModal
    v-bind="$attrs"
    @register="register"
    title="图片裁剪"
    width="800px"
    :canFullscreen="false"
    @ok="handleOk"
    okText="确认上传"
  >
    <div :class="prefixCls">
      <div :class="`${prefixCls}-left`">
        <div :class="`${prefixCls}-cropper`">
          <CropperImage
            v-if="src"
            :src="src"
            height="300px"
            :circled="circled"
            :options="cropperOptions"
            @cropend="handleCropend"
            @ready="handleReady"
          />
        </div>

        <div :class="`${prefixCls}-toolbar`">
          <Upload :fileList="[]" :accept="accept" :beforeUpload="handleBeforeUpload">
            <Tooltip title="选择图片" placement="bottom">
              <a-button size="small" preIcon="ant-design:upload-outlined" type="primary" />
            </Tooltip>
          </Upload>
          <Space>
            <Tooltip title="重置" placement="bottom">
              <a-button type="primary" preIcon="ant-design:reload-outlined" size="small" :disabled="!src" @click="handlerToolbar('reset')" />
            </Tooltip>
            <Tooltip title="左旋转" placement="bottom">
              <a-button type="primary" preIcon="ant-design:rotate-left-outlined" size="small" :disabled="!src" @click="handlerToolbar('rotate', -45)" />
            </Tooltip>
            <Tooltip title="右旋转" placement="bottom">
              <a-button type="primary" preIcon="ant-design:rotate-right-outlined" size="small" :disabled="!src" @click="handlerToolbar('rotate', 45)" />
            </Tooltip>
            <Tooltip title="水平翻转" placement="bottom">
              <a-button type="primary" preIcon="vaadin:arrows-long-h" size="small" :disabled="!src" @click="handlerToolbar('scaleX')" />
            </Tooltip>
            <Tooltip title="垂直翻转" placement="bottom">
              <a-button type="primary" preIcon="vaadin:arrows-long-v" size="small" :disabled="!src" @click="handlerToolbar('scaleY')" />
            </Tooltip>
            <Tooltip title="放大" placement="bottom">
              <a-button type="primary" preIcon="ant-design:zoom-in-outlined" size="small" :disabled="!src" @click="handlerToolbar('zoom', 0.1)" />
            </Tooltip>
            <Tooltip title="缩小" placement="bottom">
              <a-button type="primary" preIcon="ant-design:zoom-out-outlined" size="small" :disabled="!src" @click="handlerToolbar('zoom', -0.1)" />
            </Tooltip>
          </Space>
        </div>
      </div>
      <div :class="`${prefixCls}-right`">
        <div :class="[`${prefixCls}-preview`, { [`${prefixCls}-preview--circled`]: circled }]">
          <img :src="previewSource" v-if="previewSource" alt="预览" />
          <span v-else class="preview-placeholder">预览区域</span>
        </div>
        <template v-if="previewSource && circled">
          <div :class="`${prefixCls}-group`">
            <Avatar :src="previewSource" size="large" />
            <Avatar :src="previewSource" :size="48" />
            <Avatar :src="previewSource" :size="64" />
            <Avatar :src="previewSource" :size="80" />
          </div>
        </template>
        <div v-if="previewSource && !circled" :class="`${prefixCls}-info`">
          <span v-if="cropInfo">{{ cropInfo.width?.toFixed(0) }} × {{ cropInfo.height?.toFixed(0) }} px</span>
        </div>
      </div>
    </div>
  </BasicModal>
</template>

<script lang="ts">
  import type { CropendResult, Cropper } from './typing';

  import { computed, defineComponent, ref } from 'vue';
  import CropperImage from './Cropper.vue';
  import { Space, Upload, Avatar, Tooltip } from 'ant-design-vue';
  import { useDesign } from '/@/hooks/web/useDesign';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { dataURLtoBlob } from '/@/utils/file/base64Conver';
  import { isFunction } from '/@/utils/is';

  const props = {
    circled: { type: Boolean, default: false },
    aspectRatio: { type: Number, default: NaN },
    accept: { type: String, default: 'image/*' },
    uploadApi: {
      type: Function as PropType<(...args: any[]) => Promise<any>>,
    },
  };

  export default defineComponent({
    name: 'CropperUploadModal',
    components: { BasicModal, Space, CropperImage, Upload, Avatar, Tooltip },
    props,
    emits: ['uploadSuccess', 'register'],
    setup(props, { emit }) {
      let filename = '';
      const src = ref('');
      const previewSource = ref('');
      const cropper = ref<Cropper>();
      const cropInfo = ref<Cropper.Data | null>(null);
      let scaleX = 1;
      let scaleY = 1;

      const { prefixCls } = useDesign('cropper-upload-modal');
      const [register, { closeModal, setModalProps }] = useModalInner();

      const cropperOptions = computed(() => {
        const opts: Record<string, any> = {};
        // NaN = 自由裁剪, 数值 = 固定比例
        if (!isNaN(props.aspectRatio)) {
          opts.aspectRatio = props.aspectRatio;
        } else {
          opts.aspectRatio = NaN;
        }
        return opts;
      });

      function handleBeforeUpload(file: File) {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        src.value = '';
        previewSource.value = '';
        cropInfo.value = null;
        reader.onload = function (e) {
          src.value = (e.target?.result as string) ?? '';
          filename = file.name;
        };
        return false;
      }

      function handleCropend({ imgBase64, imgInfo }: CropendResult) {
        previewSource.value = imgBase64;
        cropInfo.value = imgInfo;
      }

      function handleReady(cropperInstance: Cropper) {
        cropper.value = cropperInstance;
      }

      function handlerToolbar(event: string, arg?: number) {
        if (event === 'scaleX') {
          scaleX = arg = scaleX === -1 ? 1 : -1;
        }
        if (event === 'scaleY') {
          scaleY = arg = scaleY === -1 ? 1 : -1;
        }
        cropper?.value?.[event]?.(arg);
      }

      async function handleOk() {
        const uploadApi = props.uploadApi;
        if (uploadApi && isFunction(uploadApi)) {
          const blob = dataURLtoBlob(previewSource.value);
          try {
            setModalProps({ confirmLoading: true });
            // 裁剪后 canvas.toBlob 统一输出 PNG，修正扩展名以匹配实际内容
            const pngFilename = filename.replace(/\.\w+$/, '.png');
            const result = await uploadApi({ name: 'file', file: blob, filename: pngFilename });
            // 防御性解析：兼容各种返回格式，确保提取到 URL 字符串
            const body = result?.data || result;
            const data = body?.result || body;
            const url = data?.url || data?.fileUrl || data?.path || data?.message;
            emit('uploadSuccess', {
              source: previewSource.value,
              url: url || '',
            });
            closeModal();
          } finally {
            setModalProps({ confirmLoading: false });
          }
        }
      }

      return {
        prefixCls,
        src,
        register,
        previewSource,
        cropInfo,
        cropperOptions,
        handleBeforeUpload,
        handleCropend,
        handleReady,
        handlerToolbar,
        handleOk,
      };
    },
  });
</script>

<style lang="less">
  @prefix-cls: ~'@{namespace}-cropper-upload-modal';

  .@{prefix-cls} {
    display: flex;

    &-left,
    &-right {
      height: 340px;
    }

    &-left {
      width: 55%;
    }

    &-right {
      width: 45%;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    &-cropper {
      height: 300px;
      background: #eee;
      background-image: linear-gradient(
          45deg,
          rgba(0, 0, 0, 0.25) 25%,
          transparent 0,
          transparent 75%,
          rgba(0, 0, 0, 0.25) 0
        ),
        linear-gradient(45deg, rgba(0, 0, 0, 0.25) 25%, transparent 0, transparent 75%, rgba(0, 0, 0, 0.25) 0);
      background-position: 0 0, 12px 12px;
      background-size: 24px 24px;
    }

    &-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-top: 10px;
    }

    &-preview {
      width: 220px;
      height: 220px;
      margin: 0 auto;
      overflow: hidden;
      border: 1px solid @border-color-base;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #fafafa;

      img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
      }

      .preview-placeholder {
        color: #ccc;
        font-size: 13px;
      }

      &--circled {
        border-radius: 50%;

        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
    }

    &-group {
      display: flex;
      padding-top: 8px;
      margin-top: 8px;
      border-top: 1px solid @border-color-base;
      justify-content: space-around;
      align-items: center;
      width: 100%;
    }

    &-info {
      margin-top: 12px;
      font-size: 12px;
      color: #999;
    }
  }
</style>
