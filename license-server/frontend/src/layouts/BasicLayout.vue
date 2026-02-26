<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible :trigger="null" theme="dark">
      <div class="logo">
        <SafetyCertificateOutlined style="font-size: 24px; color: #1890ff" />
        <span v-if="!collapsed" class="logo-text">License Server</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        v-model:openKeys="openKeys"
        theme="dark"
        mode="inline"
        @click="onMenuClick"
      >
        <a-menu-item key="/dashboard">
          <DashboardOutlined />
          <span>仪表盘</span>
        </a-menu-item>
        <a-sub-menu key="auth">
          <template #icon><SafetyCertificateOutlined /></template>
          <template #title>授权管理</template>
          <a-menu-item key="/app">
            <AppstoreOutlined />
            <span>应用管理</span>
          </a-menu-item>
          <a-menu-item key="/plan">
            <CrownOutlined />
            <span>套餐管理</span>
          </a-menu-item>
          <a-menu-item key="/license">
            <KeyOutlined />
            <span>许可证管理</span>
          </a-menu-item>
          <a-menu-item key="/customer">
            <TeamOutlined />
            <span>客户管理</span>
          </a-menu-item>
          <a-menu-item key="/log">
            <FileTextOutlined />
            <span>操作日志</span>
          </a-menu-item>
        </a-sub-menu>
        <a-sub-menu key="server">
          <template #icon><ToolOutlined /></template>
          <template #title>服务器运维</template>
          <a-menu-item key="/server/info">
            <CloudServerOutlined />
            <span>服务器管理</span>
          </a-menu-item>
          <a-menu-item key="/server/docker">
            <DeploymentUnitOutlined />
            <span>Docker服务管理</span>
          </a-menu-item>
          <a-menu-item key="/server/info/log">
            <ProfileOutlined />
            <span>服务器日志</span>
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="layout-header">
        <div class="header-left">
          <MenuFoldOutlined
            v-if="!collapsed"
            class="trigger"
            @click="collapsed = true"
          />
          <MenuUnfoldOutlined
            v-else
            class="trigger"
            @click="collapsed = false"
          />
          <a-breadcrumb style="margin-left: 16px">
            <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="header-right">
          <a-dropdown>
            <span class="user-info">
              <UserOutlined />
              <span style="margin-left: 8px">管理员</span>
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="router.push('/settings/password')">
                  <LockOutlined />
                  修改密码
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item @click="handleLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="layout-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import {
  DashboardOutlined,
  AppstoreOutlined,
  CrownOutlined,
  KeyOutlined,
  TeamOutlined,
  FileTextOutlined,
  CloudServerOutlined,
  DeploymentUnitOutlined,
  ProfileOutlined,
  ToolOutlined,
  LockOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])
const openKeys = ref<string[]>([])

const authMenuPaths = ['/app', '/plan', '/license', '/customer', '/log']
const serverMenuPaths = ['/server']

const currentTitle = computed(() => {
  return (route.meta?.title as string) || '仪表盘'
})

watch(
  () => route.path,
  (path) => {
    selectedKeys.value = [resolveMenuKey(path)]
    openKeys.value = resolveOpenKeys(path)
  },
  { immediate: true },
)

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}

function resolveMenuKey(path: string) {
  if (path.startsWith('/server/info/log')) return '/server/info/log'
  if (path.startsWith('/server/docker')) return '/server/docker'
  if (path.startsWith('/server/info')) return '/server/info'
  if (path.startsWith('/settings/password')) return ''
  const base = '/' + path.split('/').filter(Boolean)[0]
  return base || '/dashboard'
}

function resolveOpenKeys(path: string): string[] {
  if (serverMenuPaths.some(p => path.startsWith(p))) return ['server']
  if (authMenuPaths.some(p => path.startsWith(p))) return ['auth']
  return openKeys.value
}

function handleLogout() {
  authStore.logout()
}
</script>

<style scoped>
.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 64px;
  gap: 8px;
}
.logo-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}
.layout-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.header-left {
  display: flex;
  align-items: center;
}
.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}
.trigger:hover {
  color: #1890ff;
}
.header-right {
  display: flex;
  align-items: center;
}
.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
}
.layout-content {
  margin: 24px;
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  min-height: 280px;
}
</style>
