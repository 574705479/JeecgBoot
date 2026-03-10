<template>
  <div class="cs-sub-agent-page">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增子客服</a-button>
      </template>
      <template #avatar="{ record }">
        <a-avatar :src="getAvatarUrl(record.avatar)">{{ (record.nickname || '客').charAt(0) }}</a-avatar>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getActions(record)" :dropDownActions="getDropDownActions(record)" />
      </template>
      <template #status="{ record }">
        <a-badge :status="getStatusBadge(record.status)" :text="getStatusText(record.status)" />
      </template>
    </BasicTable>

    <!-- 新增/编辑弹窗 -->
    <BasicModal
      v-bind="$attrs"
      @register="registerModal"
      :title="modalTitle"
      @ok="handleSubmit"
      width="700px"
      :destroyOnClose="true"
    >
      <a-form :model="formState" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="登录用户名" required v-if="!isUpdate">
          <a-input v-model:value="formState.username" placeholder="请输入登录用户名" :disabled="isUpdate" />
        </a-form-item>
        <a-form-item label="登录密码" required v-if="!isUpdate">
          <a-input-password v-model:value="formState.password" placeholder="请输入登录密码" />
        </a-form-item>
        <a-form-item label="客服昵称" required>
          <a-input v-model:value="formState.nickname" placeholder="请输入客服昵称" />
        </a-form-item>
        <a-form-item label="头像">
          <CropperAvatar
            :uploadApi="uploadImg"
            :value="getAvatarFullUrl(formState.avatar)"
            @change="handleAvatarChange"
            width="100"
          />
        </a-form-item>
        <a-form-item label="最大接待数">
          <a-input-number v-model:value="formState.maxSessions" :min="1" :max="50" style="width: 100%" />
        </a-form-item>
        <a-form-item label="可见菜单">
          <a-tree
            v-if="menuTreeData.length > 0"
            v-model:checkedKeys="checkedMenuKeys"
            :tree-data="menuTreeData"
            checkable
            :defaultExpandAll="true"
            :fieldNames="{ children: 'children', title: 'title', key: 'key' }"
          />
          <a-empty v-else description="暂无菜单数据" />
        </a-form-item>
      </a-form>
    </BasicModal>

    <!-- 重置密码弹窗 -->
    <BasicModal
      @register="registerResetModal"
      title="重置密码"
      @ok="handleResetPassword"
      width="400px"
    >
      <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="resetPasswordForm.newPassword" placeholder="请输入新密码" />
        </a-form-item>
        <a-form-item label="确认密码" required>
          <a-input-password v-model:value="resetPasswordForm.confirmPassword" placeholder="请再次输入新密码" />
        </a-form-item>
      </a-form>
    </BasicModal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { BasicTable, useTable, TableAction } from '/@/components/Table';
import { BasicModal, useModal } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { CropperAvatar } from '/@/components/Cropper';
import { uploadImg } from '/@/api/sys/upload';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { isMenuAllowed } from '/@/utils/license/featureMenuMap';

const { createConfirm, createMessage } = useMessage();
const MENU_STORAGE_KEY = 'cs_sub_agent_last_menus';

// ==================== 表格 ====================
const columns = [
  { title: '头像', dataIndex: 'avatar', width: 80, slots: { customRender: 'avatar' } },
  { title: '客服昵称', dataIndex: 'nickname', width: 120 },
  { title: '登录账号', dataIndex: 'username', width: 120 },
  { title: '最大接待数', dataIndex: 'maxSessions', width: 100 },
  { title: '当前接待数', dataIndex: 'currentSessions', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100, slots: { customRender: 'status' } },
  { title: '累计服务', dataIndex: 'totalServed', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
];

const [registerTable, { reload }] = useTable({
  title: '子客服管理',
  api: async (params) => {
    const res = await defHttp.get({ url: '/cs/sub-agent/list', params });
    return res;
  },
  columns,
  formConfig: {
    schemas: [
      { field: 'nickname', label: '昵称', component: 'Input', colProps: { span: 6 } },
    ],
  },
  useSearchForm: true,
  showTableSetting: true,
  bordered: true,
  actionColumn: {
    width: 200,
    title: '操作',
    dataIndex: 'action',
    slots: { customRender: 'action' },
  },
});

function getStatusBadge(status: number) {
  switch (status) {
    case 1: return 'success';
    case 2: return 'warning';
    case 0: return 'error';
    default: return 'default';
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 1: return '在线';
    case 2: return '忙碌';
    case 3: return '隐身';
    default: return '离线';
  }
}

function getActions(record: any) {
  return [
    { label: '编辑', onClick: () => handleEdit(record) },
    { label: '删除', color: 'error', popConfirm: { title: '确定删除该子客服吗?', confirm: () => handleDelete(record) } },
  ];
}

function getDropDownActions(record: any) {
  return [
    { label: '重置密码', onClick: () => openResetPasswordModal(record) },
  ];
}

// ==================== 新增/编辑弹窗 ====================
const [registerModal, { openModal, closeModal, setModalProps }] = useModal();

const isUpdate = ref(false);
const editingId = ref('');
const formState = reactive({
  username: '',
  password: '',
  nickname: '',
  avatar: '',
  phone: '',
  email: '',
  maxSessions: 10,
  welcomeMessage: '',
  allowedMenus: '',
});

const modalTitle = computed(() => isUpdate.value ? '编辑子客服' : '新增子客服');

function getAvatarUrl(avatar?: string) {
  return avatar ? getFileAccessHttpUrl(avatar) : '';
}

function getAvatarFullUrl(path: string) {
  return path ? getFileAccessHttpUrl(path) : '';
}

function handleAvatarChange(src: string, data: string) {
  formState.avatar = data;
}

// ==================== 菜单树 ====================
const menuTreeData = ref<any[]>([]);
const checkedMenuKeys = ref<string[]>([]);

async function loadMenuTree() {
  try {
    const [res, licenseRes] = await Promise.all([
      defHttp.get({ url: '/cs/sub-agent/menus' }),
      defHttp.get({ url: '/license/status' }, { errorMessageMode: 'none' }).catch(() => null),
    ]);
    if (res && Array.isArray(res)) {
      const features: string[] | null = licenseRes?.licensed ? licenseRes.features : null;
      const filtered = features ? res.filter((item) => !item.url || isMenuAllowed(item.url, features)) : res;
      menuTreeData.value = buildTree(filtered);
    }
  } catch (e) {
    console.error('加载菜单失败', e);
  }
}

function buildTree(flatList: any[]) {
  const map = new Map();
  const roots: any[] = [];
  for (const item of flatList) {
    map.set(item.id, {
      key: item.id,
      title: item.name,
      children: [],
      ...item,
    });
  }
  for (const item of flatList) {
    const node = map.get(item.id);
    if (item.parentId && map.has(item.parentId)) {
      map.get(item.parentId).children.push(node);
    } else {
      roots.push(node);
    }
  }
  return roots;
}

onMounted(() => {
  loadMenuTree();
});

function handleAdd() {
  isUpdate.value = false;
  editingId.value = '';
  Object.assign(formState, {
    username: '',
    password: '',
    nickname: '',
    avatar: '',
    phone: '',
    email: '',
    maxSessions: 10,
    welcomeMessage: '',
    allowedMenus: '',
  });
  try {
    checkedMenuKeys.value = JSON.parse(localStorage.getItem(MENU_STORAGE_KEY) || '[]');
  } catch {
    checkedMenuKeys.value = [];
  }
  openModal(true);
}

async function handleEdit(record: any) {
  isUpdate.value = true;
  editingId.value = record.id;
  try {
    const res = await defHttp.get({ url: '/cs/sub-agent/detail', params: { id: record.id } });
    const agent = res.agent || record;
    Object.assign(formState, {
      username: res.username || '',
      password: '',
      nickname: agent.nickname || '',
      avatar: agent.avatar || '',
      phone: res.phone || '',
      email: res.email || '',
      maxSessions: agent.maxSessions || 10,
      welcomeMessage: agent.welcomeMessage || '',
      allowedMenus: agent.allowedMenus || '',
    });
    // 解析已选菜单
    if (agent.allowedMenus) {
      try {
        checkedMenuKeys.value = JSON.parse(agent.allowedMenus);
      } catch {
        checkedMenuKeys.value = [];
      }
    } else {
      checkedMenuKeys.value = [];
    }
  } catch (e) {
    Object.assign(formState, {
      username: '',
      password: '',
      nickname: record.nickname || '',
      avatar: record.avatar || '',
      phone: '',
      email: '',
      maxSessions: record.maxSessions || 10,
      welcomeMessage: record.welcomeMessage || '',
      allowedMenus: record.allowedMenus || '',
    });
    checkedMenuKeys.value = [];
  }
  openModal(true);
}

async function handleSubmit() {
  // 校验
  if (!isUpdate.value) {
    if (!formState.username?.trim()) {
      createMessage.warning('请输入登录用户名');
      return;
    }
    if (!formState.password?.trim()) {
      createMessage.warning('请输入登录密码');
      return;
    }
  }
  if (!formState.nickname?.trim()) {
    createMessage.warning('请输入客服昵称');
    return;
  }

  try {
    setModalProps({ confirmLoading: true });
    const submitData = {
      ...formState,
      allowedMenus: JSON.stringify(checkedMenuKeys.value),
    };

    if (isUpdate.value) {
      await defHttp.put({ url: '/cs/sub-agent/edit', data: { ...submitData, id: editingId.value } });
      createMessage.success('编辑成功');
    } else {
      await defHttp.post({ url: '/cs/sub-agent/add', data: submitData });
      localStorage.setItem(MENU_STORAGE_KEY, JSON.stringify(checkedMenuKeys.value));
      createMessage.success('添加成功');
    }
    closeModal();
    reload();
  } finally {
    setModalProps({ confirmLoading: false });
  }
}

async function handleDelete(record: any) {
  await defHttp.delete({ url: `/cs/sub-agent/delete?id=${record.id}` });
  createMessage.success('删除成功');
  reload();
}

// ==================== 重置密码弹窗 ====================
const [registerResetModal, { openModal: openResetModal, closeModal: closeResetModal, setModalProps: setResetModalProps }] = useModal();

const resetPasswordForm = reactive({
  agentId: '',
  newPassword: '',
  confirmPassword: '',
});

function openResetPasswordModal(record: any) {
  resetPasswordForm.agentId = record.id;
  resetPasswordForm.newPassword = '';
  resetPasswordForm.confirmPassword = '';
  openResetModal(true);
}

async function handleResetPassword() {
  if (!resetPasswordForm.newPassword) {
    createMessage.warning('请输入新密码');
    return;
  }
  if (resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword) {
    createMessage.warning('两次密码输入不一致');
    return;
  }
  try {
    setResetModalProps({ confirmLoading: true });
    await defHttp.put({
      url: '/cs/sub-agent/resetPassword',
      data: { id: resetPasswordForm.agentId, newPassword: resetPasswordForm.newPassword },
    });
    createMessage.success('密码重置成功');
    closeResetModal();
  } finally {
    setResetModalProps({ confirmLoading: false });
  }
}
</script>

<style lang="less" scoped>
.cs-sub-agent-page {
  padding: 16px;
}
</style>
