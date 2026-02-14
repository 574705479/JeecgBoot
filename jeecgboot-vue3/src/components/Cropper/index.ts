import { withInstall } from '/@/utils';
import cropperImage from './src/Cropper.vue';
import avatarCropper from './src/CropperAvatar.vue';
import cropperUpload from './src/CropperUpload.vue';

export * from './src/typing';
export const CropperImage = withInstall(cropperImage);
export const CropperAvatar = withInstall(avatarCropper);
export const CropperUpload = withInstall(cropperUpload);
