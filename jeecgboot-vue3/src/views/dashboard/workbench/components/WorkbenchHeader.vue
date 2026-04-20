<template>
  <div class="lg:flex">
    <Avatar :src="avatarSrc" :size="72" class="!mx-auto !block" />
    <div class="md:ml-6 flex flex-col justify-center md:mt-0 mt-2">
      <h1 class="md:text-lg text-md">早安, {{ userinfo.realname }}, 开始您一天的工作吧！</h1>
      <span class="text-secondary"> 今日晴，20℃ - 32℃！ </span>
    </div>
    <div class="flex flex-1 justify-end md:mt-0 mt-4">
      <div class="flex flex-col justify-center text-right">
        <span class="text-secondary"> 待办 </span>
        <span class="text-2xl">2/10</span>
      </div>

      <div class="flex flex-col justify-center text-right md:mx-16 mx-12">
        <span class="text-secondary"> 项目 </span>
        <span class="text-2xl">8</span>
      </div>
      <div class="flex flex-col justify-center text-right md:mr-10 mr-4">
        <span class="text-secondary"> 团队 </span>
        <span class="text-2xl">300</span>
      </div>
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { computed } from 'vue';
  import { Avatar } from 'ant-design-vue';
  import { useUserStore } from '/@/store/modules/user';
  import headerImg from '/@/assets/images/header.jpg';
  import { withImageCache } from '/@/utils/file/imageCache';
  import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';

  const userStore = useUserStore();
  const userinfo = computed(() => userStore.getUserInfo);
  // CSE: cse:// 头像必须经 withImageCache 解密成 blob URL
  const avatarSrc = computed(() => {
    const a = userinfo.value?.avatar;
    if (!a) return headerImg;
    return withImageCache(getFileAccessHttpUrl(a));
  });
</script>
