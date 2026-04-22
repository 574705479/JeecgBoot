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

      <div v-if="errorAlert" class="error-msg">
        <a-alert type="error" show-icon :message="errorAlert.title">
          <template v-if="errorAlert.hint" #description>
            <span class="err-hint">{{ errorAlert.hint }}</span>
          </template>
        </a-alert>
      </div>

      <div v-if="activated" class="success-msg">
        <a-alert type="success" show-icon>
          <template #message>激活成功！系统将自动跳转...</template>
          <template #description>
            <a-button type="primary" size="small" class="goto-login-btn" @click="gotoLoginFresh">
              立即返回登录页
            </a-button>
          </template>
        </a-alert>
      </div>

      <div v-if="electronApi" class="clear-cache-section">
        <a-button type="link" danger size="small" @click="handleClearCache">
          清除授权缓存并重新激活
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { defHttp } from '/@/utils/http/axios';
import { useGlobSetting } from '/@/hooks/setting';
import { ElectronEnum } from '/@/enums/jeecgEnum';
import { resetElectronDomainCache } from '/@/utils/env';
import { useUserStore } from '/@/store/modules/user';

const glob = useGlobSetting();
const electronApi = (window as any)[ElectronEnum.ELECTRON_API];
const userStore = useUserStore();
const router = useRouter();

const licenseKey = ref('');
const loading = ref(false);
const keyError = ref('');
const activated = ref(false);

type FriendlyError = { title: string; hint?: string };
const errorAlert = ref<FriendlyError | null>(null);

function humanizeLicenseError(rawInput: unknown, channel: 'web' | 'electron'): FriendlyError {
  const raw = (() => {
    if (!rawInput) return '';
    if (typeof rawInput === 'string') return rawInput;
    const e = rawInput as any;
    return e?.message || (typeof e?.toString === 'function' ? e.toString() : '') || '';
  })();
  const lower = raw.toLowerCase();

  // 优先级：精确白名单 > 网络/超时/SSL 关键字 > HTTP 状态码 > JSON 解析 > 兜底。
  // 白名单刻意不含 '激活失败' / '许可证'（前者无信息量，后者过宽会吞掉中英混合）。

  // 1) 已是友好中文：精确匹配 + 已知前缀
  const cnExact = new Set([
    '请输入许可证密钥', '许可证密钥格式无效', '服务端签名验证失败',
    '系统未授权', '获取域名失败', '所有业务域名均无法访问',
    '未配置授权服务器地址', '请求过于频繁', '激活成功',
    // 与后端 mapServerCallExceptionToChinese 保持口径一致：
    '授权服务器响应超时，请稍后重试',
    '无法解析授权服务器域名，请检查网络或域名配置',
    '授权服务器证书无效，请联系运维确认 HTTPS 配置',
    '无法连接到授权服务器，请检查网络与防火墙',
    '授权服务调用失败，请稍后重试或联系管理员',
  ]);
  const cnPrefixes = ['许可证密钥无效', '许可证已', '授权服务器内部错误（HTTP', '授权服务接口请求异常（HTTP'];
  if (cnExact.has(raw) || cnPrefixes.some((p) => raw.startsWith(p))) {
    return { title: raw };
  }

  // 2) 网络层 / 超时 / SSL（兜底覆盖：旧版后端 / Electron IPC / axios 直接漏出的英文）
  if (lower.includes('timeout') || lower.includes('econnaborted') || raw === 'timeout') {
    return { title: '授权服务器响应超时', hint: '请检查网络后重试，或联系管理员确认授权服务器是否正常' };
  }
  if (lower.includes('enotfound') || lower.includes('eai_again') || lower.includes('getaddrinfo')) {
    return { title: '无法解析授权服务器域名', hint: '请检查本机 DNS 或授权服务器域名配置是否正确' };
  }
  if (
    lower.includes('econnrefused') || lower.includes('econnreset') ||
    lower.includes('connection refused') || lower.includes('connection reset') ||
    lower.includes('host unreachable') || lower.includes('no route to host') ||
    lower.includes('network error')
  ) {
    return { title: '无法连接到授权服务器', hint: '请检查网络与防火墙，或确认授权服务器是否在线' };
  }
  if (lower.includes('certificate') || lower.includes('cert_') || lower.includes('unable to verify') || lower.includes('ssl')) {
    return { title: '授权服务器证书无效', hint: '请联系运维确认 HTTPS 证书配置' };
  }

  // 3) HTTP 状态码（axios 默认 message: "Request failed with status code 500"）
  const httpMatch = raw.match(/status code (\d{3})/i);
  if (httpMatch) {
    const code = httpMatch[1];
    return {
      title: `授权服务返回异常（HTTP ${code}）`,
      hint: code.startsWith('5') ? '服务器内部错误，请稍后重试或联系管理员' : '请确认授权服务地址与请求是否正确',
    };
  }

  // 4) JSON 解析失败（Electron fetchDomains body 非 JSON）
  if (lower.includes('unexpected token') || lower.includes('syntaxerror')) {
    return { title: '授权服务器返回内容无法解析', hint: '请确认授权服务器版本与本客户端是否匹配' };
  }

  // 5) 旧版后端可能仍返回 "无法连接授权服务器: <英文>"，兜底翻译
  if (raw.startsWith('无法连接授权服务器')) {
    return { title: '无法连接到授权服务器', hint: '请检查网络与防火墙，或确认授权服务器是否在线' };
  }

  // 6) 空 / 无信息量
  if (!raw || raw === '激活失败') {
    return { title: '激活失败', hint: '未获取到错误原因，请稍后重试或联系管理员' };
  }

  // 7) 其他未识别原文：纯中文兜底，raw 仅写 console 不外露
  console.warn('[License] unhandled error message:', raw);
  return {
    title: '激活失败',
    hint: channel === 'electron'
      ? '请检查密钥与网络后重试，或联系管理员'
      : '请检查密钥是否正确，或联系管理员',
  };
}

function setError(rawInput: unknown, channel: 'web' | 'electron' = 'web') {
  errorAlert.value = humanizeLicenseError(rawInput, channel);
}
function clearError() {
  errorAlert.value = null;
}

const statusLabel: Record<string, string> = {
  EXPIRED: '当前许可证已过期',
  REVOKED: '当前许可证已被吊销',
  SUSPENDED: '当前许可证已被暂停',
};

const existingLicense = ref<{ licenseKey?: string; status?: string; expireDate?: string }>({});

// 激活成功后跳到登录页：
// 1) 通过 store setter 同步清掉 token / userInfo / loginInfo / tenant / csAgentInfo / sessionTimeout，
//    避免吊销前残留的 stale token 让路由守卫把人从 /login 推回 /，又在 buildRoutesAction 阶段
//    被任何含「未授权」字样的错误兜底回 /license/activate。
// 2) Web 使用 history 路由（VITE_PUBLIC_PATH=/），用 router.resolve 生成正确 href 后整页跳转。
//    Electron 使用 hash 路由 + file:// 协议，必须保留原 pathname 仅替换 hash，否则 origin=file:// 加载失败。
function gotoLoginFresh() {
  try {
    userStore.setToken(undefined);
    userStore.setUserInfo(null);
    userStore.setLoginInfo(null);
    userStore.setTenant(null);
    userStore.setCsAgentInfo(null);
    userStore.setSessionTimeout(false);
  } catch (e) {
    console.warn('[License] reset user store failed', e);
  }

  if (glob.isElectronPlatform) {
    // Electron 下 permissionGuard 在守卫创建时把 glob.apiUrl 闭包绑定为空字符串
    // (_electronDomainCache 首次 IPC 在未激活前返回 null)。仅改 hash 不重启 JS 进程，
    // 守卫第 56 行 `!glob.apiUrl` 会把 /login 立刻推回 /license/activate，表现为"没反应"。
    // 必须整页 reload 让 _electronDomainCache 重新走 IPC 取激活后的 apiUrl。
    window.location.hash = '#/login';
    window.location.reload();
  } else {
    const loginHref = router.resolve({ path: '/login' }).href;
    window.location.replace(window.location.origin + loginHref);
  }
}

onMounted(async () => {
  // 兼容旧版本残留：早期实现用 sessionStorage 标志位接力跳转，新版不再依赖，无脑清一次
  try { sessionStorage.removeItem('__license_activated__'); } catch {}

  // Electron 且无 apiUrl：尚未激活/无域名，直接显示激活表单
  if (glob.isElectronPlatform && !glob.apiUrl) {
    const storedKey = electronApi?.getStoredLicenseKey?.();
    if (storedKey) {
      licenseKey.value = storedKey;
    }
    return;
  }

  // Electron + 有域名 + 有存储的 key：可能是 901 重定向过来的，尝试自动激活后端
  if (glob.isElectronPlatform && glob.apiUrl) {
    const storedKey = electronApi?.getStoredLicenseKey?.();
    if (storedKey) {
      licenseKey.value = storedKey;
      try {
        await defHttp.post(
          { url: '/license/activate', params: { licenseKey: storedKey } },
          { errorMessageMode: 'none' }
        );
        activated.value = true;
        setTimeout(gotoLoginFresh, 800);
        return;
      } catch (e) {
        setError(e, 'web');
      }
    }
  }

  // 非 Electron / 已有域名的通用逻辑：查询后端授权状态
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

async function handleClearCache() {
  clearError();
  try {
    await electronApi?.clearLicense?.();
    resetElectronDomainCache();
    licenseKey.value = '';
    existingLicense.value = {};
    activated.value = false;
    window.location.reload();
  } catch (e) {
    setError(e, 'electron');
  }
}

async function handleActivate() {
  clearError();
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
    if (electronApi?.activateLicense) {
      // Electron 模式：通过 IPC 激活（获取域名 + 测速 + 存储）
      const result = await electronApi.activateLicense(key);
      if (result.error) {
        setError(result.error, 'electron');
        return;
      }
      activated.value = true;
      resetElectronDomainCache();
      setTimeout(gotoLoginFresh, 800);
    } else {
      // 非 Electron / Web 模式：直接调后端激活
      await defHttp.post({ url: '/license/activate', params: { licenseKey: key } }, { errorMessageMode: 'none' });
      activated.value = true;
      existingLicense.value = {};
      setTimeout(gotoLoginFresh, 800);
    }
  } catch (e) {
    setError(e, 'web');
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

.err-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.6;
}

.goto-login-btn {
  margin-top: 8px;
}

.clear-cache-section {
  margin-top: 16px;
  text-align: center;
}
</style>
