<template>
  <div style="max-width: 500px; margin: 0 auto">
    <h2 style="margin-bottom: 24px">修改密码</h2>
    <a-form
      :model="form"
      :rules="rules"
      ref="formRef"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      @finish="handleSubmit"
    >
      <a-form-item label="旧密码" name="oldPassword">
        <a-input-password v-model:value="form.oldPassword" placeholder="请输入旧密码" />
      </a-form-item>
      <a-form-item label="新密码" name="newPassword">
        <a-input-password v-model:value="form.newPassword" placeholder="请输入新密码（至少6位）" />
      </a-form-item>
      <a-form-item label="确认密码" name="confirmPassword">
        <a-input-password v-model:value="form.confirmPassword" placeholder="请再次输入新密码" />
      </a-form-item>
      <a-form-item :wrapper-col="{ offset: 6, span: 16 }">
        <a-button type="primary" html-type="submit" :loading="loading">
          确认修改
        </a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { request } from '../../utils/request'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = async (_rule: any, value: string) => {
  if (value && value !== form.newPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码' }],
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码至少6位' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码' },
    { validator: validateConfirm },
  ],
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const res = await request.post('/admin/auth/change-password', {
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    })
    if (res.data.code === 200) {
      message.success('密码修改成功，请重新登录')
      authStore.clearTokens()
      window.location.href = '/login'
    } else {
      message.error(res.data.message || '修改失败')
    }
  } finally {
    loading.value = false
  }
}
</script>
