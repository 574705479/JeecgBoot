<template>
  <div class="license-activate-container">
    <div class="license-activate-card">
      <div class="license-header">
        <img src="/@/assets/images/logo.png" alt="logo" class="logo" />
        <h2>系统授权激活</h2>
        <p class="sub-title">请输入许可证密钥以激活系统</p>
      </div>

      <div v-if="existingLicense.status" class="existing-license">
        <a-alert :type="existingLicense.status === 'EXPIRED' ? 'warning' : 'error'" show-icon>
          <template #message>
            <span>{{ statusLabel[existingLicense.status] || '授权异常' }}</span>
          </template>
          <template #description>
            <div class="existing-detail">
              <p>许可证密钥：<strong>{{ existingLicense.licenseKey }}</strong></p>
              <p v-if="existingLicense.expireDate">到期时间：{{ existingLicense.expireDate }}</p>
              <p class="existing-hint">请输入新的许可证密钥以重新激活系统</p>
            </div>
          </template>
        </a-alert>
      </div>

      <a-form layout="vertical" @submit.prevent="handleActivate">
        <a-form-item label="许可证密钥">
          <a-input
            v-model:value="licenseKey"
            placeholder="LIC-XXXX-XXXXXXXXXXXXXXXX-XX"
            size="large"
            allow-clear
          />
          <div v-if="keyError" class="field-error">{{ keyError }}</div>
        </a-form-item>

        <a-form-item>
          <a-button type="primary" size="large" block :loading="loading" @click="handleActivate">
            激活
          </a-button>
        </a-form-item>
      </a-form>

      <div v-if="errorMsg" class="error-msg">
        <a-alert :message="errorMsg" type="error" show-icon />
      </div>

      <div v-if="activated" class="success-msg">
        <a-alert message="激活成功！系统将自动跳转..." type="success" show-icon />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { defHttp } from '/@/utils/http/axios';

const router = useRouter();
const licenseKey = ref('');
const loading = ref(false);
const errorMsg = ref('');
const keyError = ref('');
const activated = ref(false);

const statusLabel: Record<string, string> = {
  EXPIRED: '当前许可证已过期',
  REVOKED: '当前许可证已被吊销',
  SUSPENDED: '当前许可证已被暂停',
};

const existingLicense = ref<{ licenseKey?: string; status?: string; expireDate?: string }>({});

onMounted(async () => {
  try {
    const res = await defHttp.get({ url: '/license/status' }, { errorMessageMode: 'none' });
    if (res && !res.licensed && res.status) {
      existingLicense.value = {
        licenseKey: res.licenseKey,
        status: res.status,
        expireDate: res.expireDate,
      };
    }
  } catch {
    // license endpoint unavailable, ignore
  }
});

function validateKeyFormat(key: string): boolean {
  return /^LIC-[A-Z0-9]{4}-[A-Z0-9]{16}-[A-Z0-9]{2}$/.test(key);
}

async function handleActivate() {
  errorMsg.value = '';
  keyError.value = '';

  const key = licenseKey.value.trim().toUpperCase();
  if (!key) {
    keyError.value = '请输入许可证密钥';
    return;
  }
  if (!validateKeyFormat(key)) {
    keyError.value = '许可证密钥格式无效，格式为：LIC-XXXX-XXXXXXXXXXXXXXXX-XX';
    return;
  }

  loading.value = true;
  try {
    await defHttp.post({ url: '/license/activate', params: { licenseKey: key } }, { errorMessageMode: 'none' });
    activated.value = true;
    existingLicense.value = {};
    setTimeout(() => {
      window.location.href = '/';
    }, 1500);
  } catch (e: any) {
    errorMsg.value = e?.message || '激活失败，请检查密钥是否正确';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.license-activate-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.license-activate-card {
  width: 480px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.license-header {
  text-align: center;
  margin-bottom: 32px;
}

.license-header .logo {
  height: 48px;
  margin-bottom: 16px;
}

.license-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 8px;
}

.sub-title {
  color: #666;
  font-size: 14px;
}

.existing-license {
  margin-bottom: 24px;
}

.existing-detail p {
  margin: 4px 0;
  font-size: 13px;
}

.existing-hint {
  margin-top: 8px !important;
  color: #666;
}

.field-error {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
}

.error-msg,
.success-msg {
  margin-top: 16px;
}
</style>
