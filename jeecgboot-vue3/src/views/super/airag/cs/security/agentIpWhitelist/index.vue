<template>
  <div class="cs-agent-ip-whitelist-page">
    <!-- 开关区域 -->
    <a-card class="mb-4" :bordered="true">
      <div class="flex items-center gap-4">
        <span class="font-medium text-base">客服IP白名单开关</span>
        <a-switch v-model:checked="whitelistEnabled" @change="handleSwitchChange" :loading="switchLoading" />
        <span class="text-gray-400 text-sm">{{ whitelistEnabled ? '已启用：仅白名单IP的客服可登录系统' : '已关闭：所有客服可从任意IP登录' }}</span>
      </div>
      <div class="current-ip-info" v-if="currentIp">
        <span>当前您的IP：</span>
        <span class="ip-value">{{ currentIp }}</span>
        <a-tag v-if="currentIpInWhitelist" color="green" class="ip-tag">已在白名单中</a-tag>
        <a-tag v-else color="orange" class="ip-tag">不在白名单中</a-tag>
      </div>
    </a-card>

    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">添加白名单</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="[
          { label: '编辑', onClick: () => handleEdit(record) },
          { label: '删除', color: 'error', icon: 'ant-design:delete-outlined', popConfirm: { title: getDeleteConfirmTitle(record), confirm: () => handleDelete(record) } },
        ]" />
      </template>
    </BasicTable>

    <!-- 添加/编辑弹窗 -->
    <BasicModal @register="registerModal" :title="isEdit ? '编辑白名单' : '添加白名单'" @ok="handleSubmit" :minHeight="60">
      <a-form :model="formState" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="IP/IP段" required>
          <a-input v-model:value="formState.ip" placeholder="如 192.168.1.1 或 192.168.1.0/24" />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="formState.remark" :rows="3" placeholder="备注信息（选填）" />
        </a-form-item>
      </a-form>
    </BasicModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { BasicModal, useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { Modal } from 'ant-design-vue';

const { createMessage } = useMessage();

const [registerModal, { openModal, closeModal }] = useModal();

const whitelistEnabled = ref(false);
const switchLoading = ref(false);
const isEdit = ref(false);
const currentIp = ref('');
const currentIpInWhitelist = ref(false);

const formState = reactive({
  id: '',
  ip: '',
  remark: '',
});

const columns = [
  { title: 'IP/IP段', dataIndex: 'ip', width: 200 },
  { title: '备注', dataIndex: 'remark', width: 250 },
  { title: '创建时间', dataIndex: 'createTime', width: 180 },
  { title: '修改时间', dataIndex: 'updateTime', width: 180 },
];

const [registerTable, { reload }] = useTable({
  title: '客服IP白名单',
  api: async (params) => {
    const res = await defHttp.get({ url: '/cs/security/agent-ip-whitelist/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'ip', label: 'IP', component: 'Input', colProps: { span: 6 } },
    ],
  },
  useSearchForm: true,
  showTableSetting: true,
  bordered: true,
  actionColumn: {
    width: 150,
    title: '操作',
    dataIndex: 'action',
    slots: { customRender: 'action' },
  },
});

onMounted(async () => {
  await Promise.all([loadSwitchState(), refreshCurrentIpStatus()]);
});

async function refreshCurrentIpStatus() {
  try {
    const res = await defHttp.get({ url: '/cs/security/agent-ip-whitelist/current-ip' });
    currentIp.value = res?.ip || '';
    currentIpInWhitelist.value = res?.inWhitelist === true;
  } catch (e) {
    console.error('获取当前IP状态失败', e);
  }
}

async function loadSwitchState() {
  try {
    const res = await defHttp.get({ url: '/cs/security/agent-ip-whitelist/enabled' });
    whitelistEnabled.value = res.enabled === true;
  } catch (e) {
    console.error('加载白名单开关状态失败', e);
  }
}

async function handleSwitchChange(checked: boolean) {
  if (checked && !currentIpInWhitelist.value && currentIp.value) {
    Modal.confirm({
      title: '安全提示',
      content: `您当前的IP（${currentIp.value}）不在白名单中，开启后您可能无法继续访问系统。是否确认开启？`,
      okText: '确认开启',
      cancelText: '取消',
      onOk: async () => {
        await doSwitchChange(true);
      },
      onCancel: () => {
        whitelistEnabled.value = false;
      },
    });
    return;
  }
  await doSwitchChange(checked);
}

async function doSwitchChange(checked: boolean) {
  switchLoading.value = true;
  try {
    await defHttp.put({ url: '/cs/security/agent-ip-whitelist/enabled', params: { enabled: checked } });
    createMessage.success(checked ? '白名单已开启' : '白名单已关闭');
    await refreshCurrentIpStatus();
  } catch (e) {
    whitelistEnabled.value = !checked;
    createMessage.error('操作失败');
  } finally {
    switchLoading.value = false;
  }
}

function handleAdd() {
  isEdit.value = false;
  formState.id = '';
  formState.ip = '';
  formState.remark = '';
  openModal(true);
}

function handleEdit(record: any) {
  isEdit.value = true;
  formState.id = record.id;
  formState.ip = record.ip;
  formState.remark = record.remark || '';
  openModal(true);
}

async function handleSubmit() {
  if (!formState.ip) {
    createMessage.warning('请输入IP/IP段');
    return;
  }
  if (isEdit.value) {
    await defHttp.put({ url: '/cs/security/agent-ip-whitelist/edit', params: formState });
    createMessage.success('编辑成功');
  } else {
    await defHttp.post({ url: '/cs/security/agent-ip-whitelist/add', params: formState });
    createMessage.success('添加成功');
  }
  closeModal();
  reload();
  const prevInWhitelist = currentIpInWhitelist.value;
  await refreshCurrentIpStatus();
  if (whitelistEnabled.value && !currentIpInWhitelist.value && currentIp.value) {
    createMessage.warning(`您当前的IP（${currentIp.value}）不在白名单中，可能会导致无法访问系统，建议添加当前IP到白名单`);
  }
  if (prevInWhitelist && !currentIpInWhitelist.value && whitelistEnabled.value) {
    createMessage.warning(`注意：编辑后您当前的IP已不在白名单覆盖范围内`);
  }
}

function getDeleteConfirmTitle(record: any) {
  if (whitelistEnabled.value && currentIp.value && record.ip === currentIp.value) {
    return `该条目覆盖了您当前的IP（${currentIp.value}），删除后可能导致无法继续访问系统。是否确认删除？`;
  }
  return '确定删除该白名单记录吗?';
}

async function handleDelete(record: any) {
  await defHttp.delete({ url: `/cs/security/agent-ip-whitelist/delete/${record.id}` });
  createMessage.success('删除成功');
  reload();
  const prevInWhitelist = currentIpInWhitelist.value;
  await refreshCurrentIpStatus();
  if (prevInWhitelist && !currentIpInWhitelist.value && whitelistEnabled.value) {
    createMessage.warning(`注意：删除后您当前的IP（${currentIp.value}）已不在白名单中，可能会导致无法访问系统`);
  }
}
</script>

<style lang="less" scoped>
.cs-agent-ip-whitelist-page {
  padding: 16px;
}
.mb-4 {
  margin-bottom: 16px;
}
.flex {
  display: flex;
}
.items-center {
  align-items: center;
}
.gap-4 {
  gap: 16px;
}
.font-medium {
  font-weight: 500;
}
.text-base {
  font-size: 15px;
}
.text-gray-400 {
  color: #999;
}
.text-sm {
  font-size: 13px;
}
.current-ip-info {
  margin-top: 12px;
  font-size: 14px;
  color: #666;
}
.ip-value {
  font-weight: 600;
  color: #333;
  margin-right: 8px;
}
.ip-tag {
  margin-left: 4px;
}
</style>
