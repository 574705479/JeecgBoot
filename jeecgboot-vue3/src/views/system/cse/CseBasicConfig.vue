<template>
  <div class="cse-basic-config">
    <a-spin :spinning="loading">
      <!-- ① 总开关 -->
      <a-card :bordered="false" class="cse-card">
        <template #title>
          <span class="card-title-with-icon">
            <Icon icon="ant-design:safety-certificate-outlined" />
            文件加密总开关
          </span>
        </template>
        <div class="switch-row">
          <a-switch
            v-model:checked="form.enabled"
            checked-children="启用"
            un-checked-children="关闭"
            :disabled="!loaded"
          />
          <span class="switch-hint">
            {{ form.enabled
              ? '已启用：新上传的文件会按下方白名单 / 黑名单决定是否加密'
              : '已关闭：新上传的文件不再加密；已加密的旧文件仍可正常访问' }}
          </span>
        </div>
        <a-alert
          v-if="!form.enabled"
          type="warning"
          show-icon
          banner
          class="warn-banner"
          message="总开关已关闭，所有新上传的文件将不再加密"
        />
      </a-card>

      <!-- ② 加密业务（白名单） -->
      <a-card :bordered="false" class="cse-card">
        <template #title>
          <span class="card-title-with-icon">
            <Icon icon="ant-design:lock-outlined" />
            加密业务（白名单）
          </span>
          <span class="card-subtitle">勾选的业务，新上传的文件会自动加密</span>
        </template>
        <a-table
          :columns="encryptColumns"
          :data-source="encryptDict"
          :pagination="false"
          row-key="path"
          size="middle"
          class="dict-table"
          :row-class-name="(r: any) => (r.forceLocked ? 'row-locked' : 'row-clickable')"
          :custom-row="(record: any) => ({
            onClick: () => onToggleEncrypt(record),
          })"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'check'">
              <a-checkbox
                :checked="encryptSelections.has(record.path)"
                :disabled="record.forceLocked"
                @click.stop
                @change="onToggleEncrypt(record)"
              />
            </template>
            <template v-else-if="column.key === 'name'">
              <span class="name-text">{{ record.name }}</span>
              <a-tag v-if="record.forceLocked" color="warning" class="lock-tag">
                <Icon icon="ant-design:lock-filled" /> 系统强制
              </a-tag>
            </template>
            <template v-else-if="column.key === 'description'">
              <span class="desc-text">{{ record.description }}</span>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- ③ 公开业务（黑名单） -->
      <a-card :bordered="false" class="cse-card">
        <template #title>
          <span class="card-title-with-icon">
            <Icon icon="ant-design:unlock-outlined" />
            公开业务（黑名单）
          </span>
          <span class="card-subtitle">勾选的业务，新上传的文件不加密（用于公开下载场景）。黑名单优先级高于白名单</span>
        </template>
        <a-table
          :columns="publicColumns"
          :data-source="publicDict"
          :pagination="false"
          row-key="path"
          size="middle"
          class="dict-table"
          :row-class-name="(r: any) => (r.forceLocked ? 'row-locked' : 'row-clickable')"
          :custom-row="(record: any) => ({
            onClick: () => onTogglePublic(record),
          })"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'check'">
              <a-checkbox
                :checked="publicSelections.has(record.path)"
                :disabled="record.forceLocked"
                @click.stop
                @change="onTogglePublic(record)"
              />
            </template>
            <template v-else-if="column.key === 'name'">
              <span class="name-text">{{ record.name }}</span>
              <a-tag v-if="record.forceLocked" color="warning" class="lock-tag">
                <Icon icon="ant-design:lock-filled" /> 系统强制
              </a-tag>
            </template>
            <template v-else-if="column.key === 'description'">
              <span class="desc-text">{{ record.description }}</span>
            </template>
          </template>
        </a-table>
      </a-card>

      <!-- ④ 高级（自定义路径兜底） -->
      <a-collapse v-model:activeKey="advancedActive" class="advanced-collapse" :bordered="false">
        <a-collapse-panel key="advanced" header="高级设置">
          <a-alert
            type="info"
            show-icon
            class="adv-alert"
            message="一般无需配置"
            description="上方两个列表已覆盖常用业务。仅当上方列表里没有，又需要临时调整时，才在此处填写业务路径。格式：以 / 结尾，例如 my-biz/"
          />
          <a-form layout="vertical">
            <a-form-item label="自定义加密路径">
              <a-select
                v-model:value="form.customEncrypted"
                mode="tags"
                placeholder="例如：my-biz/、order-attach/"
                :token-separators="[',', ' ']"
                style="width: 100%"
              />
            </a-form-item>
            <a-form-item label="自定义公开路径">
              <a-select
                v-model:value="form.customPublic"
                mode="tags"
                placeholder="例如：special-public/"
                :token-separators="[',', ' ']"
                style="width: 100%"
              />
            </a-form-item>
          </a-form>
        </a-collapse-panel>
      </a-collapse>

      <!-- ⑤ 效果测试 -->
      <a-card :bordered="false" class="cse-card">
        <template #title>
          <span class="card-title-with-icon">
            <Icon icon="ant-design:experiment-outlined" />
            效果测试
          </span>
          <span class="card-subtitle">输入业务路径，预览该业务上传时是否会加密（仅模拟，不会真的上传文件）</span>
        </template>
        <div class="dryrun-row">
          <a-input
            v-model:value="dryRunPathInput"
            placeholder="例如：airag、cs-visitor、avatar/cs-agent"
            allow-clear
            style="max-width: 320px"
            @press-enter="handleDryRun"
          />
          <a-radio-group v-model:value="dryRunMode" button-style="solid">
            <a-radio-button value="current">当前生效配置</a-radio-button>
            <a-radio-button value="preview">待保存配置（含未提交修改）</a-radio-button>
          </a-radio-group>
          <a-button type="primary" @click="handleDryRun" :loading="dryRunLoading">
            <Icon icon="ant-design:thunderbolt-outlined" /> 测试
          </a-button>
        </div>
        <div v-if="dryRunResult" class="dryrun-result">
          <a-tag
            :color="dryRunResult.shouldEncrypt ? 'success' : 'warning'"
            class="result-tag"
          >
            <Icon :icon="dryRunResult.shouldEncrypt ? 'ant-design:check-circle-filled' : 'ant-design:warning-filled'" />
            {{ dryRunResult.shouldEncrypt ? '会加密' : '不加密' }}
          </a-tag>
          <span v-if="dryRunResult.matchedRule" class="rule-text">
            命中规则：<code>{{ dryRunResult.matchedRule }}</code>
          </span>
          <div class="reason-text">{{ dryRunResult.reason }}</div>
        </div>
      </a-card>

      <!-- ⑥ 保存 -->
      <a-card :bordered="false" class="cse-card save-card">
        <a-alert
          type="info"
          show-icon
          class="save-alert"
          message="保存说明"
          description="修改仅影响新上传的文件，已加密的旧文件不受影响。保存后立即生效（约 60 秒内同步到全部节点），所有变更会写入审计日志。"
        />
        <div class="save-row">
          <a-space>
            <a-button @click="loadConfig" :loading="loading">
              <Icon icon="ant-design:reload-outlined" /> 撤销修改
            </a-button>
            <a-button type="primary" :loading="submitting" @click="openSave">
              <Icon icon="ant-design:save-outlined" /> 保存配置
            </a-button>
          </a-space>
        </div>
      </a-card>
    </a-spin>

    <!-- 二次密码确认 -->
    <a-modal v-model:open="saveVisible" title="保存配置" :confirm-loading="submitting" @ok="submitSave">
      <a-form layout="vertical">
        <a-form-item label="即将保存">
          <div class="preview-block">
            <div>总开关：<a-tag :color="form.enabled ? 'success' : 'warning'">{{ form.enabled ? '启用' : '关闭' }}</a-tag></div>
            <div class="preview-row">
              <span class="preview-label">加密白名单（{{ finalEncrypted.length }}）：</span>
              <span class="preview-paths">
                <a-tag v-for="p in finalEncrypted" :key="'e-' + p" color="blue">{{ p }}</a-tag>
              </span>
            </div>
            <div class="preview-row">
              <span class="preview-label">公开黑名单（{{ finalPublic.length }}）：</span>
              <span class="preview-paths">
                <a-tag v-for="p in finalPublic" :key="'p-' + p" color="orange">{{ p }}</a-tag>
              </span>
            </div>
          </div>
        </a-form-item>
        <a-form-item label="超管登录密码（二次确认）" required>
          <a-input-password v-model:value="savePassword" placeholder="输入您当前的登录密码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import Icon from '@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';
import {
  getCseConfig,
  saveCseConfig,
  dryRunPath,
  type CseBizDef,
  type CseDryRunVO,
} from './cseConfig.api';

defineOptions({ name: 'CseBasicConfig' });

const { createMessage } = useMessage();

const loading = ref(false);
const submitting = ref(false);
const loaded = ref(false);

const encryptDict = ref<CseBizDef[]>([]);
const publicDict = ref<CseBizDef[]>([]);

const encryptSelections = ref<Set<string>>(new Set());
const publicSelections = ref<Set<string>>(new Set());

const form = reactive<{
  enabled: boolean;
  customEncrypted: string[];
  customPublic: string[];
}>({
  enabled: true,
  customEncrypted: [],
  customPublic: [],
});

const advancedActive = ref<string[]>([]);

const dryRunPathInput = ref('');
const dryRunMode = ref<'current' | 'preview'>('current');
const dryRunLoading = ref(false);
const dryRunResult = ref<CseDryRunVO | null>(null);

const saveVisible = ref(false);
const savePassword = ref('');

const encryptColumns = [
  { title: '加密', key: 'check', width: 70, align: 'center' as const },
  { title: '业务名称', key: 'name', width: 240 },
  { title: '说明', key: 'description', ellipsis: true },
];

const publicColumns = [
  { title: '公开', key: 'check', width: 70, align: 'center' as const },
  { title: '业务名称', key: 'name', width: 240 },
  { title: '说明', key: 'description', ellipsis: true },
];

/** 拼装最终要提交的列表（字典勾选 + 自定义 tag，强制锁项后端会兜底再 union） */
const finalEncrypted = computed(() => {
  const set = new Set<string>(encryptSelections.value);
  for (const p of form.customEncrypted) {
    const n = normalizePath(p);
    if (n) set.add(n);
  }
  return Array.from(set);
});

const finalPublic = computed(() => {
  const set = new Set<string>(publicSelections.value);
  for (const p of form.customPublic) {
    const n = normalizePath(p);
    if (n) set.add(n);
  }
  return Array.from(set);
});

function normalizePath(p: string): string {
  if (!p) return '';
  let s = String(p).trim().replace(/\\/g, '/');
  if (!s) return '';
  if (!s.endsWith('/')) s += '/';
  return s;
}

function onToggleEncrypt(record: CseBizDef) {
  if (record.forceLocked) return;
  const next = new Set(encryptSelections.value);
  if (next.has(record.path)) {
    next.delete(record.path);
  } else {
    next.add(record.path);
    // 互斥：从公开列表移除
    if (publicSelections.value.has(record.path)) {
      const pub = new Set(publicSelections.value);
      pub.delete(record.path);
      publicSelections.value = pub;
    }
  }
  encryptSelections.value = next;
}

function onTogglePublic(record: CseBizDef) {
  if (record.forceLocked) return;
  const next = new Set(publicSelections.value);
  if (next.has(record.path)) {
    next.delete(record.path);
  } else {
    next.add(record.path);
    if (encryptSelections.value.has(record.path)) {
      const enc = new Set(encryptSelections.value);
      enc.delete(record.path);
      encryptSelections.value = enc;
    }
  }
  publicSelections.value = next;
}

async function loadConfig() {
  loading.value = true;
  loaded.value = false;
  try {
    const data = await getCseConfig();
    if (!data) {
      createMessage.error('配置加载失败：返回为空');
      return;
    }
    form.enabled = !!data.enabled;
    encryptDict.value = (data.dictionary || []).filter((d) => d.category === 'ENCRYPT');
    publicDict.value = (data.dictionary || []).filter((d) => d.category === 'PUBLIC');

    const dictEncryptSet = new Set(encryptDict.value.map((d) => d.path));
    const dictPublicSet = new Set(publicDict.value.map((d) => d.path));

    encryptSelections.value = new Set((data.encryptedPaths || []).filter((p) => dictEncryptSet.has(p)));
    publicSelections.value = new Set((data.publicPaths || []).filter((p) => dictPublicSet.has(p)));
    form.customEncrypted = data.customEncrypted || [];
    form.customPublic = data.customPublic || [];

    if (form.customEncrypted.length || form.customPublic.length) {
      advancedActive.value = ['advanced'];
    }
    loaded.value = true;
  } catch (e: any) {
    createMessage.error('配置加载失败：' + (e?.message || e));
  } finally {
    loading.value = false;
  }
}

async function handleDryRun() {
  if (!dryRunPathInput.value.trim()) {
    createMessage.warning('请输入要测试的业务路径');
    return;
  }
  dryRunLoading.value = true;
  try {
    const req: any = {
      bizPath: dryRunPathInput.value.trim(),
      mode: dryRunMode.value,
    };
    if (dryRunMode.value === 'preview') {
      req.previewEnabled = form.enabled;
      req.previewEncrypted = finalEncrypted.value;
      req.previewPublic = finalPublic.value;
    }
    const r = await dryRunPath(req);
    dryRunResult.value = r;
  } catch (e: any) {
    dryRunResult.value = null;
    createMessage.error('测试失败：' + (e?.message || e));
  } finally {
    dryRunLoading.value = false;
  }
}

function openSave() {
  savePassword.value = '';
  saveVisible.value = true;
}

async function submitSave() {
  if (!savePassword.value) {
    createMessage.warning('请输入超管登录密码');
    return;
  }
  submitting.value = true;
  try {
    await saveCseConfig({
      enabled: form.enabled,
      encryptedPaths: finalEncrypted.value,
      publicPaths: finalPublic.value,
      password: savePassword.value,
    });
    createMessage.success('保存成功');
    saveVisible.value = false;
    await loadConfig();
  } catch (e: any) {
    createMessage.error(e?.message || '保存失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => loadConfig());
</script>

<style scoped>
.cse-basic-config {
  /* 容器内边距由外层 Tab 控制 */
}
.cse-card {
  border-radius: 8px;
  box-shadow: rgba(0, 0, 0, 0.22) 3px 5px 30px 0;
  margin-bottom: 16px;
}
.card-title-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1d1d1f;
}
.card-subtitle {
  margin-left: 12px;
  font-size: 12px;
  font-weight: normal;
  color: rgba(0, 0, 0, 0.45);
}
.switch-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.switch-hint {
  color: rgba(0, 0, 0, 0.6);
  font-size: 13px;
}
.warn-banner {
  margin-top: 12px;
}
.dict-table :deep(.row-clickable) {
  cursor: pointer;
}
.dict-table :deep(.row-clickable:hover) {
  background: rgba(24, 144, 255, 0.04);
}
.dict-table :deep(.row-locked) {
  background: rgba(255, 193, 7, 0.04);
}
.name-text {
  color: #1d1d1f;
  font-weight: 500;
}
.lock-tag {
  margin-left: 8px;
  display: inline-flex;
  align-items: center;
  gap: 2px;
}
.desc-text {
  color: rgba(0, 0, 0, 0.65);
  font-size: 12.5px;
  line-height: 1.5;
}
.advanced-collapse {
  margin-bottom: 16px;
  background: transparent;
}
.advanced-collapse :deep(.ant-collapse-item) {
  border-radius: 8px;
  background: #fff;
  box-shadow: rgba(0, 0, 0, 0.08) 2px 3px 18px 0;
  border-bottom: none;
}
.adv-alert {
  margin-bottom: 16px;
}
.dryrun-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.dryrun-result {
  margin-top: 16px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 6px;
}
.result-tag {
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.rule-text {
  margin-left: 12px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}
.rule-text code {
  background: #f5f5f7;
  padding: 1px 6px;
  border-radius: 4px;
  color: #d4380d;
}
.reason-text {
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.5);
  font-size: 12.5px;
}
.save-card {
  margin-bottom: 0;
}
.save-alert {
  margin-bottom: 16px;
}
.save-row {
  display: flex;
  justify-content: flex-end;
}
.preview-block {
  background: #fafafa;
  padding: 12px 16px;
  border-radius: 6px;
  font-size: 13px;
}
.preview-row {
  margin-top: 8px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.preview-label {
  flex-shrink: 0;
  color: rgba(0, 0, 0, 0.6);
  width: 130px;
}
.preview-paths {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
