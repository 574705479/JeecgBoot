<template>
  <div class="cse-kek-panel">
    <a-card :bordered="false" class="cse-kek-card">
      <div class="toolbar">
        <a-space>
          <a-button type="primary" preIcon="ant-design:plus-outlined" @click="openGenerate">
            生成新密钥
          </a-button>
          <a-button preIcon="ant-design:cloud-download-outlined" @click="openExport">导出备份</a-button>
          <a-button preIcon="ant-design:cloud-upload-outlined" @click="openImport">导入备份</a-button>
          <a-button preIcon="ant-design:reload-outlined" @click="loadAll">刷新</a-button>
        </a-space>
      </div>

      <a-alert type="info" show-icon class="kek-alert" message="密钥状态说明">
        <template #description>
          <div><b>使用中</b> — 当前用于加密新上传的文件</div>
          <div><b>待启用</b> — 已生成、可随时切换为使用中</div>
          <div><b>已下线</b> — 仅用于解密旧文件，不再加密新文件</div>
          <div><b>已归档</b> — 从列表中收起，密钥仍然保留</div>
          <div class="alert-note">每次激活待启用密钥时，原使用中密钥会自动切换为已下线。</div>
        </template>
      </a-alert>

      <a-table
        :columns="kekColumns"
        :data-source="kekList"
        :loading="loading"
        row-key="kid"
        :pagination="false"
        class="kek-table"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                v-if="record.status !== 'ACTIVE' && record.status !== 'ARCHIVED'"
                size="small"
                type="link"
                @click="openActivate(record)"
              >激活</a-button>
              <a-button
                v-if="record.status === 'DEPRECATED' || record.status === 'STAGED'"
                size="small"
                type="link"
                danger
                @click="openArchive(record)"
              >归档</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-card :bordered="false" class="cse-kek-card audit-card">
      <template #title>
        <span class="card-title-with-icon">
          <Icon icon="ant-design:audit-outlined" />
          审计日志（最近 100 条）
        </span>
      </template>
      <a-table
        :columns="auditColumns"
        :data-source="auditList"
        :loading="auditLoading"
        row-key="id"
        :pagination="{ pageSize: 20, showSizeChanger: false }"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-tag :color="actionColor(record.action)">{{ actionText(record.action) }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 生成新密钥 -->
    <a-modal v-model:open="genVisible" title="生成新密钥" :confirm-loading="submitting" @ok="submitGenerate">
      <a-form layout="vertical">
        <a-form-item label="备注（可选）">
          <a-input v-model:value="genForm.remark" placeholder="例如：2026Q1 季度轮换" allow-clear />
        </a-form-item>
        <a-form-item label="超管登录密码（二次确认）" required>
          <a-input-password v-model:value="genForm.password" placeholder="输入您当前的登录密码" />
        </a-form-item>
        <a-alert type="warning" show-icon message="新生成的密钥状态为「待启用」，需要点击「激活」后才会用于加密新文件。" />
      </a-form>
    </a-modal>

    <!-- 激活 -->
    <a-modal v-model:open="actVisible" title="激活密钥" :confirm-loading="submitting" @ok="submitActivate">
      <a-form layout="vertical">
        <a-form-item label="目标密钥编号">
          <a-input :value="actForm.kid" disabled />
        </a-form-item>
        <a-form-item label="超管登录密码（二次确认）" required>
          <a-input-password v-model:value="actForm.password" placeholder="输入您当前的登录密码" />
        </a-form-item>
        <a-alert type="warning" show-icon message="激活后，当前「使用中」的密钥会自动切换为「已下线」，新上传的文件将改用此密钥加密。" />
      </a-form>
    </a-modal>

    <!-- 归档 -->
    <a-modal v-model:open="arcVisible" title="归档密钥" :confirm-loading="submitting" @ok="submitArchive">
      <a-form layout="vertical">
        <a-form-item label="目标密钥编号">
          <a-input :value="arcForm.kid" disabled />
        </a-form-item>
        <a-form-item label="超管登录密码（二次确认）" required>
          <a-input-password v-model:value="arcForm.password" placeholder="输入您当前的登录密码" />
        </a-form-item>
        <a-alert type="warning" show-icon message="归档只是从列表收起，密钥仍然保留并可继续解密旧文件。" />
      </a-form>
    </a-modal>

    <!-- 导出 -->
    <a-modal v-model:open="expVisible" title="导出备份" :confirm-loading="submitting" @ok="submitExport">
      <a-form layout="vertical">
        <a-form-item label="备份密码（不少于 8 位）" required>
          <a-input-password v-model:value="expForm.zipPassword" placeholder="设置一个高强度密码" />
        </a-form-item>
        <a-form-item label="超管登录密码（二次确认）" required>
          <a-input-password v-model:value="expForm.password" placeholder="输入您当前的登录密码" />
        </a-form-item>
        <a-alert
          type="info"
          show-icon
          message="备份文件已使用上方设置的密码加密。请妥善保管备份文件和密码，丢失后无法找回。"
        />
      </a-form>
    </a-modal>

    <!-- 导入 -->
    <a-modal v-model:open="impVisible" title="导入备份" :confirm-loading="submitting" @ok="submitImport">
      <a-form layout="vertical">
        <a-form-item label="备份文件" required>
          <a-upload
            :before-upload="beforeUpload"
            :file-list="impForm.fileList"
            :max-count="1"
            @remove="onRemoveFile"
            accept=".zip"
          >
            <a-button preIcon="ant-design:upload-outlined">选择备份文件</a-button>
          </a-upload>
        </a-form-item>
        <a-form-item label="备份密码" required>
          <a-input-password v-model:value="impForm.zipPassword" placeholder="导出时设置的备份密码" />
        </a-form-item>
        <a-form-item label="超管登录密码（二次确认）" required>
          <a-input-password v-model:value="impForm.password" placeholder="输入您当前的登录密码" />
        </a-form-item>
        <a-alert type="warning" show-icon message="重名密钥会被跳过；导入完成后，如需启用其中一个，请手动点击「激活」。" />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import Icon from '@/components/Icon';
import { useMessage } from '/@/hooks/web/useMessage';
import {
  activateKek,
  archiveKek,
  exportKekZip,
  generateKek,
  importKekZip,
  listKek,
  listKekAudit,
  type CseKekAuditLogVO,
  type CseKekVO,
} from './cseKek.api';

defineOptions({ name: 'CseKekPanel' });

const { createMessage } = useMessage();

const loading = ref(false);
const auditLoading = ref(false);
const submitting = ref(false);
const kekList = ref<CseKekVO[]>([]);
const auditList = ref<CseKekAuditLogVO[]>([]);

const kekColumns = [
  { title: '密钥编号', dataIndex: 'kid', key: 'kid', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '创建人', dataIndex: 'createdBy', key: 'createdBy', width: 120 },
  { title: '创建时间', dataIndex: 'createdTime', key: 'createdTime', width: 170 },
  { title: '激活时间', dataIndex: 'activatedTime', key: 'activatedTime', width: 170 },
  { title: '下线时间', dataIndex: 'deprecatedTime', key: 'deprecatedTime', width: 170 },
  { title: '加密文件数', dataIndex: 'fileCount', key: 'fileCount', width: 110 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
];

const auditColumns = [
  { title: '时间', dataIndex: 'operateTime', key: 'operateTime', width: 170 },
  { title: '动作', dataIndex: 'action', key: 'action', width: 120 },
  { title: '密钥编号', dataIndex: 'kid', key: 'kid', width: 100 },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 120 },
  { title: 'IP', dataIndex: 'operatorIp', key: 'operatorIp', width: 140 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true },
];

const STATUS_TEXT: Record<string, string> = {
  ACTIVE: '使用中',
  STAGED: '待启用',
  DEPRECATED: '已下线',
  ARCHIVED: '已归档',
};

const ACTION_TEXT: Record<string, string> = {
  INIT: '初始化',
  GENERATE: '生成密钥',
  ACTIVATE: '激活',
  DEPRECATE: '下线',
  ARCHIVE: '归档',
  EXPORT: '导出备份',
  IMPORT: '导入备份',
  CONFIG_UPDATE: '修改配置',
};

const statusText = (s: string) => STATUS_TEXT[s] || s || '-';
const actionText = (a: string) => ACTION_TEXT[a] || a || '-';

function statusColor(s: string) {
  return s === 'ACTIVE' ? 'success' : s === 'STAGED' ? 'processing' : s === 'DEPRECATED' ? 'warning' : 'default';
}

function actionColor(a: string) {
  if (a === 'GENERATE' || a === 'INIT') return 'processing';
  if (a === 'ACTIVATE') return 'success';
  if (a === 'DEPRECATE' || a === 'ARCHIVE') return 'warning';
  if (a === 'EXPORT' || a === 'IMPORT') return 'blue';
  if (a === 'CONFIG_UPDATE') return 'purple';
  return 'default';
}

async function loadAll() {
  loading.value = true;
  auditLoading.value = true;
  try {
    const [kRes, aRes] = await Promise.allSettled([listKek(), listKekAudit(100)]);
    if (kRes.status === 'fulfilled') {
      kekList.value = kRes.value || [];
    } else {
      kekList.value = [];
      createMessage.error('密钥列表加载失败：' + (kRes.reason?.message || kRes.reason));
    }
    if (aRes.status === 'fulfilled') {
      auditList.value = aRes.value || [];
    } else {
      auditList.value = [];
      createMessage.error('审计日志加载失败：' + (aRes.reason?.message || aRes.reason));
    }
  } finally {
    loading.value = false;
    auditLoading.value = false;
  }
}

const genVisible = ref(false);
const genForm = reactive({ password: '', remark: '' });
function openGenerate() {
  genForm.password = '';
  genForm.remark = '';
  genVisible.value = true;
}
async function submitGenerate() {
  if (!genForm.password) {
    createMessage.warning('请输入超管登录密码');
    return;
  }
  submitting.value = true;
  try {
    await generateKek(genForm.password, genForm.remark);
    createMessage.success('已生成新密钥（待启用），请按需激活');
    genVisible.value = false;
    await loadAll();
  } catch (e: any) {
    createMessage.error(e?.message || '生成失败');
  } finally {
    submitting.value = false;
  }
}

const actVisible = ref(false);
const actForm = reactive({ kid: '', password: '' });
function openActivate(r: CseKekVO) {
  actForm.kid = r.kid;
  actForm.password = '';
  actVisible.value = true;
}
async function submitActivate() {
  if (!actForm.password) {
    createMessage.warning('请输入超管登录密码');
    return;
  }
  submitting.value = true;
  try {
    await activateKek(actForm.password, actForm.kid);
    createMessage.success(`已激活密钥 ${actForm.kid}`);
    actVisible.value = false;
    await loadAll();
  } catch (e: any) {
    createMessage.error(e?.message || '激活失败');
  } finally {
    submitting.value = false;
  }
}

const arcVisible = ref(false);
const arcForm = reactive({ kid: '', password: '' });
function openArchive(r: CseKekVO) {
  arcForm.kid = r.kid;
  arcForm.password = '';
  arcVisible.value = true;
}
async function submitArchive() {
  if (!arcForm.password) {
    createMessage.warning('请输入超管登录密码');
    return;
  }
  submitting.value = true;
  try {
    await archiveKek(arcForm.password, arcForm.kid);
    createMessage.success(`已归档密钥 ${arcForm.kid}`);
    arcVisible.value = false;
    await loadAll();
  } catch (e: any) {
    createMessage.error(e?.message || '归档失败');
  } finally {
    submitting.value = false;
  }
}

const expVisible = ref(false);
const expForm = reactive({ password: '', zipPassword: '' });
function openExport() {
  expForm.password = '';
  expForm.zipPassword = '';
  expVisible.value = true;
}
async function submitExport() {
  if (!expForm.password || !expForm.zipPassword) {
    createMessage.warning('请填写两套密码');
    return;
  }
  if (expForm.zipPassword.length < 8) {
    createMessage.warning('备份密码不少于 8 位');
    return;
  }
  submitting.value = true;
  try {
    const blob = await exportKekZip(expForm.password, expForm.zipPassword);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `file-encrypt-backup-${Date.now()}.zip`;
    a.click();
    URL.revokeObjectURL(url);
    createMessage.success('备份已下载，请妥善保管');
    expVisible.value = false;
    await loadAll();
  } catch (e: any) {
    createMessage.error(e?.message || '导出失败');
  } finally {
    submitting.value = false;
  }
}

const impVisible = ref(false);
const impForm = reactive<{ password: string; zipPassword: string; fileList: any[]; file: File | null }>({
  password: '',
  zipPassword: '',
  fileList: [],
  file: null,
});
function openImport() {
  impForm.password = '';
  impForm.zipPassword = '';
  impForm.fileList = [];
  impForm.file = null;
  impVisible.value = true;
}
function beforeUpload(file: File) {
  impForm.file = file;
  impForm.fileList = [{ uid: '-1', name: file.name, status: 'done' }];
  return false;
}
function onRemoveFile() {
  impForm.file = null;
  impForm.fileList = [];
}
async function submitImport() {
  if (!impForm.file) {
    createMessage.warning('请选择备份文件');
    return;
  }
  if (!impForm.password || !impForm.zipPassword) {
    createMessage.warning('请填写两套密码');
    return;
  }
  submitting.value = true;
  try {
    const fd = new FormData();
    fd.append('file', impForm.file);
    fd.append('password', impForm.password);
    fd.append('zipPassword', impForm.zipPassword);
    const n = await importKekZip(fd);
    createMessage.success(`已导入 ${n} 个密钥`);
    impVisible.value = false;
    await loadAll();
  } catch (e: any) {
    createMessage.error(e?.message || '导入失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => loadAll());
</script>

<style scoped>
.cse-kek-panel {
  /* 容器内边距由外层 Tab 控制 */
}
.cse-kek-card {
  border-radius: 8px;
  box-shadow: rgba(0, 0, 0, 0.22) 3px 5px 30px 0;
  margin-bottom: 16px;
}
.audit-card {
  margin-bottom: 0;
}
.toolbar {
  margin-bottom: 16px;
}
.kek-alert {
  margin-bottom: 16px;
}
.kek-table {
  margin-top: 4px;
}
.alert-note {
  margin-top: 6px;
  color: rgba(0, 0, 0, 0.55);
}
.card-title-with-icon {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #1d1d1f;
}
</style>
