<template>
  <div class="sensitive-words-page">
    <a-card title="访客敏感词配置" :bordered="false" style="max-width: 700px">
      <a-alert type="info" show-icon style="margin-bottom: 16px">
        <template #message>
          启用后，访客发送的消息如果包含敏感词将被拦截，无法发送。支持前后端双重校验。
        </template>
      </a-alert>

      <a-form layout="vertical">
        <a-form-item label="启用敏感词过滤">
          <a-switch v-model:checked="config.enabled" />
        </a-form-item>

        <a-form-item label="敏感词列表（每行一个）">
          <a-textarea
            v-model:value="wordsText"
            :rows="12"
            placeholder="每行输入一个敏感词&#10;例如：&#10;敏感词1&#10;敏感词2"
            :disabled="!config.enabled"
          />
          <div class="word-count">共 {{ wordCount }} 个敏感词</div>
        </a-form-item>

        <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts" name="SensitiveWordsPage">
import { computed, onMounted, reactive, ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';

defineOptions({ name: 'SensitiveWordsPage' });

const { createMessage } = useMessage();
const saving = ref(false);

const config = reactive({
  enabled: false,
  words: [] as string[],
});

const wordsText = ref('');

const wordCount = computed(() => {
  return wordsText.value
    .split('\n')
    .map((w) => w.trim())
    .filter((w) => w.length > 0).length;
});

async function fetchConfig() {
  try {
    const res = await defHttp.get({ url: '/cs/agent/global/sensitive-words' }, { isTransformResponse: false });
    const data = res?.result || res;
    let parsed: any = {};
    if (typeof data === 'string') {
      try { parsed = JSON.parse(data); } catch {}
    } else if (data && typeof data === 'object') {
      parsed = data;
    }
    config.enabled = !!parsed.enabled;
    config.words = Array.isArray(parsed.words) ? parsed.words : [];
    wordsText.value = config.words.join('\n');
  } catch (e) {
    console.error('获取敏感词配置失败', e);
  }
}

async function handleSave() {
  // 解析文本为数组
  const words = wordsText.value
    .split('\n')
    .map((w) => w.trim())
    .filter((w) => w.length > 0);
  config.words = words;

  saving.value = true;
  try {
    await defHttp.put({
      url: '/cs/agent/global/sensitive-words',
      data: { enabled: config.enabled, words: config.words },
    }, { isTransformResponse: false });
    createMessage.success('保存成功');
  } catch (e) {
    createMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
}

onMounted(() => {
  fetchConfig();
});
</script>

<style scoped>
.sensitive-words-page {
  padding: 16px;
}
.word-count {
  margin-top: 4px;
  color: #999;
  font-size: 12px;
}
</style>
