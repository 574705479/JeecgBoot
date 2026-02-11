<template>
  <div class="access-layout">
    <aside class="side-nav">
      <div class="nav-group">
        <div class="nav-title">Start Here</div>
        <a class="nav-link" href="#start">快速开始</a>
        <a class="nav-link" v-if="tokenMode" href="#token">获取 Token</a>
        <a class="nav-link" href="#access">接入方式</a>
        <a class="nav-link" href="#preview">预览效果</a>
      </div>
      <div class="nav-group">
        <div class="nav-title">Help</div>
        <a class="nav-link" href="#faq">常见问题</a>
      </div>
    </aside>

    <main class="doc-main">
      <div class="header">
        <div class="title">第三方接入示例与调试</div>
        <div class="subtitle">独立访客入口、嵌入与挂件效果演示</div>
        <a-tag v-if="!loadingMode" :color="tokenMode ? 'blue' : 'green'" style="margin-top: 8px; font-size: 13px;">
          {{ tokenMode ? 'Token模式（需获取Token）' : '免Token模式（设备码自动标识）' }}
        </a-tag>
      </div>

      <section id="start" class="doc-section">
        <div class="section-title">快速开始</div>
        <div class="section-desc">{{ tokenMode ? '按步骤填入参数，即可完成接入调试' : '免Token模式下，访客通过设备码自动标识，以下参数均为可选' }}</div>
        <div class="step">
          <div class="step-title">1) {{ tokenMode ? '填写用户参数' : '填写可选参数' }}</div>
          <div class="step-body">
            <div class="form">
              <div class="field" v-if="tokenMode">
                <span class="label">externalUserId（第三方用户唯一标识）</span>
                <a-input v-model:value="externalUserId" placeholder="第三方用户唯一标识" />
              </div>
              <div class="field">
                <span class="label">userName（用于显示的用户昵称{{ tokenMode ? '' : '，可选'}}）</span>
                <a-input v-model:value="userName" placeholder="展示昵称" />
              </div>
              <div class="field">
                <span class="label">source（第三方系统标识{{ tokenMode ? '，用于避免ID冲突' : '，可选'}}）</span>
                <a-input v-model:value="source" placeholder="第三方系统标识" />
              </div>
              <div class="field">
                <span class="label">{{ tokenMode ? 'X-App-Secret（全局接入密钥，后端保存）' : '接入密钥 key（免Token模式下通过 ?key= 传递）' }}</span>
                <a-input v-model:value="secretKey" :placeholder="tokenMode ? '全局接入密钥（仅本地调试）' : '接入密钥（后台配置后必传）'" />
                <span class="label" v-if="tokenMode">生产环境请放在你自己的后端，不要写进前端页面</span>
                <span class="label" v-else>如后台配置了密钥，访客必须携带 ?key=xxx 参数才能访问</span>
              </div>
              <a-alert v-if="!tokenMode" message="免Token模式下，系统自动使用设备码作为访客唯一标识。如配置了接入密钥，访客需通过 ?key=xxx 参数传递。" type="info" show-icon style="margin-top: 4px;" />
            </div>
          </div>
        </div>
      </section>

      <section id="token" class="doc-section" v-if="tokenMode">
        <div class="section-title">获取 Token</div>
        <div class="section-desc">本地调试可直接调用，生产环境请通过后端代理</div>
        <div class="step">
          <div class="step-title">2) 获取短时 token</div>
          <div class="step-body step-grid">
            <div class="card">
              <a-tabs>
                <a-tab-pane key="local" tab="本地调试（当前系统）">
                  <div class="card-desc">用于调试，正式接入请由服务端调用</div>
                  <div class="row">
                    <a-button type="primary" :loading="loading" @click="runTokenTest">测试调用</a-button>
                    <a-button :disabled="!token" @click="copyToken">复制token</a-button>
                  </div>
                  <div v-if="token" class="md-block">
                    <div class="md-title">```text</div>
                    <pre class="md-code">token: {{ token }}
expireAt: {{ expireAt || '' }}</pre>
                    <div class="md-title">```</div>
                  </div>
                </a-tab-pane>
                <a-tab-pane key="backend" tab="第三方后端代理">
                  <div class="card-desc">你自己的后端负责调用 /airag/cs/visitor/token，密钥只保存在后端</div>
                  <div class="field">
                    <span class="label">你的后端接口</span>
                    <a-input v-model:value="backendTokenUrl" placeholder="/your-backend/visitor-token" />
                  </div>
                  <div class="row">
                    <a-button type="primary" :loading="backendTokenLoading" @click="runBackendTokenFetch">通过后端获取token</a-button>
                    <a-button :disabled="!token" @click="copyToken">复制token</a-button>
                  </div>
                </a-tab-pane>
              </a-tabs>
            </div>
          <div class="card">
            <div class="card-desc">后端获取 token（带密钥）</div>
            <div class="doc-summary">密钥只保存在你的后端环境变量或配置中，前端不可见。</div>
            <a-tabs v-model:activeKey="tokenDocTab">
              <a-tab-pane key="curl" tab="cURL">
                <div class="doc-viewer markdown-body" v-html="tokenDocCurlHtml"></div>
              </a-tab-pane>
              <a-tab-pane key="java" tab="Java">
                <div class="doc-viewer markdown-body" v-html="tokenDocJavaHtml"></div>
              </a-tab-pane>
              <a-tab-pane key="php" tab="PHP">
                <div class="doc-viewer markdown-body" v-html="tokenDocPhpHtml"></div>
              </a-tab-pane>
              <a-tab-pane key="node" tab="Node.js">
                <div class="doc-viewer markdown-body" v-html="tokenDocNodeHtml"></div>
              </a-tab-pane>
              <a-tab-pane key="frontend" tab="前端调用">
                <div class="doc-viewer markdown-body" v-html="tokenDocFrontendHtml"></div>
              </a-tab-pane>
            </a-tabs>
          </div>
          </div>
        </div>
      </section>

      <section id="access" class="doc-section">
        <div class="section-title">接入方式</div>
        <div class="section-desc">按类型查看示例</div>
        <a-tabs v-model:activeKey="accessType">
          <a-tab-pane key="url" tab="URL / WebView">
            <div class="doc-viewer markdown-body" v-html="accessDocUrlHtml"></div>
          </a-tab-pane>
          <a-tab-pane key="iframe" tab="iframe">
            <div class="doc-viewer markdown-body" v-html="accessDocIframeHtml"></div>
          </a-tab-pane>
          <a-tab-pane key="widget" tab="右下角挂件">
            <div class="doc-viewer markdown-body" v-html="accessDocWidgetHtml"></div>
          </a-tab-pane>
        </a-tabs>
      </section>

      <section id="preview" class="doc-section">
        <div class="section-title">预览效果</div>
        <div class="section-desc">{{ tokenMode ? '根据当前接入类型展示（需要先获取 token）' : '免Token模式可直接预览' }}</div>
        <div class="step-body preview-grid">
          <div class="card" v-if="accessType === 'url'">
            <div class="card-desc">访客页预览</div>
            <a-alert v-if="tokenMode && !token" message="请先测试获取token后再预览效果" type="warning" show-icon />
            <iframe v-else class="preview" :src="accessUrl" />
          </div>
          <div class="card" v-if="accessType === 'iframe'">
            <div class="card-desc">iframe 预览</div>
            <a-alert v-if="tokenMode && !token" message="请先测试获取token后再预览效果" type="warning" show-icon />
            <iframe v-else class="preview" :src="accessUrl" />
          </div>
          <div class="card" v-if="accessType === 'widget'">
            <div class="card-desc">挂件预览（右下角）</div>
            <div class="row">
              <a-button type="primary" :disabled="(tokenMode && !token) || widgetLoaded" @click="loadWidget">加载挂件预览</a-button>
              <a-button :disabled="!widgetLoaded" @click="unloadWidget">关闭挂件预览</a-button>
            </div>
            <a-alert v-if="tokenMode && !token" message="请先测试获取token后再预览效果" type="warning" show-icon />
          </div>
        </div>
      </section>

      <section id="faq" class="doc-section">
        <div class="section-title">常见问题</div>
        <div class="doc-viewer markdown-body" v-html="faqDocHtml"></div>
      </section>
    </main>

    <aside class="toc">
      <div class="toc-title">On this page</div>
      <a class="toc-link" href="#start">快速开始</a>
      <a class="toc-link" v-if="tokenMode" href="#token">获取 Token</a>
      <a class="toc-link" href="#access">接入方式</a>
      <a class="toc-link" href="#preview">预览效果</a>
      <a class="toc-link" href="#faq">常见问题</a>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { message } from 'ant-design-vue';
import { defHttp } from '/@/utils/http/axios';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';

const baseUrl = window.location.origin;
const tokenMode = ref(true); // true=Token模式, false=免Token模式
const loadingMode = ref(true); // 正在查询模式
const externalUserId = ref('U1001');
const userName = ref('Tom');
const source = ref('partnerA');
const secretKey = ref('');
const token = ref('');
const expireAt = ref(0);
const loading = ref(false);
const backendTokenUrl = ref('/your-backend/visitor-token');
const backendTokenLoading = ref(false);
const tokenDocTab = ref('curl');
const accessType = ref('url');

onMounted(async () => {
  try {
    const res = await defHttp.get(
      { url: '/airag/cs/visitor/token/required' },
      { successMessageMode: 'none', isTransformResponse: false },
    );
    if (res?.success && res.result === false) {
      tokenMode.value = false;
    }
  } catch {
    // 默认Token模式
  } finally {
    loadingMode.value = false;
  }
  // 自动加载系统已配置的密钥
  try {
    const accessRes = await defHttp.get(
      { url: '/cs/agent/global/visitor-access' },
      { successMessageMode: 'none', isTransformResponse: false },
    );
    if (accessRes?.success && accessRes.result?.secretKey) {
      secretKey.value = accessRes.result.secretKey;
    }
  } catch {
    // 忽略
  }
});

const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value;
      } catch (e) {}
    }
    return md.utils.escapeHtml(str);
  },
});

let widgetInstance: any = null;
let widgetScriptEl: HTMLScriptElement | null = null;
const widgetLoaded = ref(false);

const accessUrl = computed(() => {
  if (!tokenMode.value) {
    // 免Token模式：拼接可选参数 + 接入密钥
    const params = new URLSearchParams();
    if (secretKey.value) params.set('key', secretKey.value);
    if (userName.value) params.set('userName', userName.value);
    if (source.value) params.set('source', source.value);
    const qs = params.toString();
    return `${baseUrl}/cs/userChat${qs ? '?' + qs : ''}`;
  }
  if (!token.value) {
    return `${baseUrl}/cs/userChat?token=短时token&externalUserId=${externalUserId.value}&userName=${userName.value}&source=${source.value}`;
  }
  const params = new URLSearchParams({
    token: token.value,
    externalUserId: externalUserId.value,
    userName: userName.value,
    source: source.value,
  });
  return `${baseUrl}/cs/userChat?${params.toString()}`;
});

const tokenDocCurlMarkdown = computed(() => {
  return `
\`\`\`bash
curl -X POST "${baseUrl}/jeecgboot/airag/cs/visitor/token" \\
  -H "Content-Type: application/json" \\
  -H "X-App-Secret: YOUR_KEY" \\
  -d '{"externalUserId":"${externalUserId.value}","userName":"${userName.value}","source":"${source.value}"}'
\`\`\`
`;
});

const tokenDocJavaMarkdown = computed(() => {
  return `
\`\`\`java
@RestController
public class CsTokenProxyController {
  @Value("\${jeecg.cs.secret:}")
  private String secret;

  @PostMapping("/your-backend/visitor-token")
  public Object proxy(@RequestBody Map<String, Object> body) {
    RestTemplate rest = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-App-Secret", secret);
    HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
    String url = "${baseUrl}/jeecgboot/airag/cs/visitor/token";
    return rest.postForObject(url, req, Object.class);
  }
}
\`\`\`
`;
});

const tokenDocPhpMarkdown = computed(() => {
  return `
\`\`\`php
$data = json_decode(file_get_contents("php://input"), true);
$secret = getenv("JEECG_CS_SECRET") ?: "";
$ch = curl_init("${baseUrl}/jeecgboot/airag/cs/visitor/token");
curl_setopt_array($ch, [
  CURLOPT_RETURNTRANSFER => true,
  CURLOPT_POST => true,
  CURLOPT_HTTPHEADER => [
    "Content-Type: application/json",
    "X-App-Secret: " . $secret
  ],
  CURLOPT_POSTFIELDS => json_encode($data),
]);
$resp = curl_exec($ch);
curl_close($ch);
header("Content-Type: application/json");
echo $resp;
\`\`\`
`;
});

const tokenDocNodeMarkdown = computed(() => {
  return `
\`\`\`js
app.post("/your-backend/visitor-token", async (req, res) => {
  const resp = await fetch("${baseUrl}/jeecgboot/airag/cs/visitor/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-App-Secret": process.env.JEECG_CS_SECRET || ""
    },
    body: JSON.stringify(req.body || {})
  });
  const data = await resp.json();
  res.json(data?.result || data);
});
\`\`\`
`;
});

const tokenDocFrontendMarkdown = computed(() => {
  return `
\`\`\`js
fetch("${backendTokenUrl.value}", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    externalUserId: "${externalUserId.value}",
    userName: "${userName.value}",
    source: "${source.value}"
  })
}).then(r => r.json());
\`\`\`
`;
});

const tokenDocCurlHtml = computed(() => md.render(tokenDocCurlMarkdown.value));
const tokenDocJavaHtml = computed(() => md.render(tokenDocJavaMarkdown.value));
const tokenDocPhpHtml = computed(() => md.render(tokenDocPhpMarkdown.value));
const tokenDocNodeHtml = computed(() => md.render(tokenDocNodeMarkdown.value));
const tokenDocFrontendHtml = computed(() => md.render(tokenDocFrontendMarkdown.value));

const accessDocUrlMarkdown = computed(() => {
  if (!tokenMode.value) {
    // 免Token模式
    const params: string[] = [];
    if (secretKey.value) params.push(`key=${encodeURIComponent(secretKey.value)}`);
    if (userName.value) params.push(`userName=${encodeURIComponent(userName.value)}`);
    if (source.value) params.push(`source=${encodeURIComponent(source.value)}`);
    const freeUrl = params.length
      ? `${baseUrl}/cs/userChat?${params.join('&')}`
      : `${baseUrl}/cs/userChat`;
    const keyNote = secretKey.value
      ? '免Token模式下需携带 `key` 参数（接入密钥）。系统自动使用设备码标识用户。'
      : '免Token模式下，访客直接通过URL访问，无需任何凭证。系统自动使用设备码标识用户。';
    const jsExample = secretKey.value
      ? `window.location.href = "${baseUrl}/cs/userChat?key=" + encodeURIComponent("你的接入密钥");`
      : `window.location.href = "${baseUrl}/cs/userChat";`;
    return `
${keyNote}

\`\`\`text
${freeUrl}
\`\`\`

${secretKey.value ? '必传参数：`key`（接入密钥）。可选参数' : '可选参数'}：\`userName\`（昵称）、\`source\`（来源标识）。

\`\`\`js
// 直接跳转即可，无需获取Token
${jsExample}
\`\`\`
`;
  }
  const previewToken = token.value || '短时token';
  const accessPreviewUrl = `${baseUrl}/cs/userChat?token=${encodeURIComponent(previewToken)}&externalUserId=${encodeURIComponent(externalUserId.value)}&userName=${encodeURIComponent(userName.value)}&source=${encodeURIComponent(source.value)}`;
  return `
\`\`\`text
${accessPreviewUrl}
\`\`\`

\`\`\`js
fetch("${backendTokenUrl.value}", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    externalUserId: "${externalUserId.value}",
    userName: "${userName.value}",
    source: "${source.value}"
  })
})
  .then(r => r.json())
  .then(d => {
    const t = d.token || d.result?.token || "";
    const url = "${baseUrl}/cs/userChat?token=" + encodeURIComponent(t)
      + "&externalUserId=${externalUserId.value}&userName=${userName.value}&source=${source.value}";
    window.location.href = url;
  });
\`\`\`
`;
});

const accessDocIframeMarkdown = computed(() => {
  if (!tokenMode.value) {
    const freeUrl = secretKey.value
      ? `${baseUrl}/cs/userChat?key=${encodeURIComponent(secretKey.value)}`
      : `${baseUrl}/cs/userChat`;
    const note = secretKey.value ? '免Token模式下需携带 `key` 参数：' : '免Token模式下直接嵌入即可：';
    return `
${note}

\`\`\`html
<iframe src="${freeUrl}" style="width:420px;height:640px;border:0;"></iframe>
\`\`\`
`;
  }
  const previewToken = token.value || '短时token';
  const accessPreviewUrl = `${baseUrl}/cs/userChat?token=${encodeURIComponent(previewToken)}&externalUserId=${encodeURIComponent(externalUserId.value)}&userName=${encodeURIComponent(userName.value)}&source=${encodeURIComponent(source.value)}`;
  return `
\`\`\`html
<iframe src="${accessPreviewUrl}" style="width:420px;height:640px;border:0;"></iframe>
\`\`\`

\`\`\`js
fetch("${backendTokenUrl.value}", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    externalUserId: "${externalUserId.value}",
    userName: "${userName.value}",
    source: "${source.value}"
  })
})
  .then(r => r.json())
  .then(d => {
    const t = d.token || d.result?.token || "";
    const url = "${baseUrl}/cs/userChat?token=" + encodeURIComponent(t)
      + "&externalUserId=${externalUserId.value}&userName=${userName.value}&source=${source.value}";
    document.getElementById("cs-iframe").src = url;
  });
\`\`\`
`;
});

const accessDocWidgetMarkdown = computed(() => {
  const scriptCloseTag = '</scr' + 'ipt>';
  if (!tokenMode.value) {
    const keyLine = secretKey.value ? `\n  key: "${secretKey.value}",       // 接入密钥（后台配置后必传）` : '';
    return `
免Token模式下，无需传入token相关参数${secretKey.value ? '，但需传入接入密钥' : ''}：

\`\`\`html
<script src="${baseUrl}/cs-widget.js">${scriptCloseTag}
JeecgCsWidget.init({
  baseUrl: "${baseUrl}",${keyLine}
  userName: "${userName.value}",  // 可选
  source: "${source.value}",     // 可选
});
\`\`\`
`;
  }
  return `
\`\`\`html
<script src="${baseUrl}/cs-widget.js">${scriptCloseTag}
JeecgCsWidget.init({
  baseUrl: "${baseUrl}",
  externalUserId: "${externalUserId.value}",
  userName: "${userName.value}",
  source: "${source.value}",
  getToken: () => fetch("/your-backend/visitor-token", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      externalUserId: "${externalUserId.value}",
      userName: "${userName.value}",
      source: "${source.value}"
    })
  })
    .then(r => r.json())
    .then(d => d.token || d.result?.token || "")
});
\`\`\`
`;
});

const accessDocUrlHtml = computed(() => md.render(accessDocUrlMarkdown.value));
const accessDocIframeHtml = computed(() => md.render(accessDocIframeMarkdown.value));
const accessDocWidgetHtml = computed(() => md.render(accessDocWidgetMarkdown.value));

const faqDocMarkdown = computed(() => {
  return `
### 没有密钥，前端怎么获取 token？
未配置密钥时可以直接调用 \`/airag/cs/visitor/token\` 获取短时 token。

### 配了密钥会不会泄露？
密钥只放后端，前端只拿 token，不接触密钥。建议配合白名单和频控。

### 为什么 URL 里都是 token？
聊天入口只认 token，不需要密钥。密钥只用于“获取 token”这一步。
`;
});

const faqTokenModeNote = computed(() => {
  if (tokenMode.value) return '';
  return `

### Token模式和免Token模式有什么区别？
**Token模式**：第三方后端先调用接口获取短时Token，传给前端后才能访问。适合需要严格身份验证的场景。

**免Token模式**：访客通过设备码自动标识，无需获取Token即可直接接入。适合官网客服、公开咨询等场景。注意：用户清除浏览器数据后会被视为新访客。

可在「客服工作台 → 设置 → 访客接入设置」中切换。`;
});

const faqDocHtml = computed(() => md.render(faqDocMarkdown.value + faqTokenModeNote.value));

async function runTokenTest() {
  loading.value = true;
  token.value = '';
  expireAt.value = 0;
  try {
    const headers: Record<string, string> = {};
    if (secretKey.value) {
      headers['X-App-Secret'] = secretKey.value;
    }
    const res = await defHttp.post({
      url: '/airag/cs/visitor/token',
      data: {
        externalUserId: externalUserId.value,
        userName: userName.value,
        source: source.value,
      },
      headers,
    }, { successMessageMode: 'none' });
    const payload = res?.result || res;
    if (payload?.token) {
      token.value = payload.token;
      expireAt.value = payload.expireAt || 0;
      message.success('获取token成功');
    } else {
      message.error(payload?.message || '获取token失败');
    }
  } catch {
    message.error('获取token失败');
  } finally {
    loading.value = false;
  }
}

async function runBackendTokenFetch() {
  backendTokenLoading.value = true;
  token.value = '';
  expireAt.value = 0;
  try {
    const res = await defHttp.post({
      url: backendTokenUrl.value,
      data: {
        externalUserId: externalUserId.value,
        userName: userName.value,
        source: source.value,
      },
    }, { successMessageMode: 'none' });
    const payload = res?.result || res;
    if (payload?.token) {
      token.value = payload.token;
      expireAt.value = payload.expireAt || 0;
      message.success('获取token成功');
    } else {
      message.error(payload?.message || '获取token失败');
    }
  } catch {
    message.error('获取token失败');
  } finally {
    backendTokenLoading.value = false;
  }
}

function copyToken() {
  if (!token.value) return;
  navigator.clipboard?.writeText(token.value);
  message.success('已复制');
}

async function ensureWidgetScript() {
  const w = window as any;
  if (w.JeecgCsWidget) {
    return;
  }
  await new Promise<void>((resolve, reject) => {
    if (widgetScriptEl) {
      widgetScriptEl.addEventListener('load', () => resolve());
      widgetScriptEl.addEventListener('error', () => reject(new Error('load failed')));
      return;
    }
    widgetScriptEl = document.createElement('script');
    widgetScriptEl.src = '/cs-widget.js';
    widgetScriptEl.onload = () => resolve();
    widgetScriptEl.onerror = () => reject(new Error('load failed'));
    document.body.appendChild(widgetScriptEl);
  });
}

async function loadWidget() {
  if (tokenMode.value && !token.value) return;
  if (widgetLoaded.value) return;
  try {
    await ensureWidgetScript();
    const w = window as any;
    if (!w.JeecgCsWidget) {
      message.error('挂件脚本加载失败');
      return;
    }
    const opts: any = {
      baseUrl,
      userName: userName.value,
      source: source.value,
    };
    if (tokenMode.value) {
      opts.externalUserId = externalUserId.value;
      opts.token = token.value;
      opts.getToken = () => Promise.resolve(token.value);
    } else if (secretKey.value) {
      opts.key = secretKey.value;
    }
    widgetInstance = w.JeecgCsWidget.init(opts);
    widgetLoaded.value = true;
  } catch {
    message.error('挂件脚本加载失败');
  }
}

function unloadWidget() {
  if (widgetInstance?.destroy) {
    widgetInstance.destroy();
  }
  widgetInstance = null;
  widgetLoaded.value = false;
}

onBeforeUnmount(() => {
  unloadWidget();
  if (widgetScriptEl && widgetScriptEl.parentNode) {
    widgetScriptEl.parentNode.removeChild(widgetScriptEl);
  }
});
</script>

<style scoped>
.access-layout {
  min-height: 100vh;
  background: #f5f7fb;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 220px;
  gap: 16px;
  padding: 24px;
}

.side-nav,
.toc {
  position: sticky;
  top: 16px;
  align-self: start;
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #f0f0f0;
  padding: 16px;
}

.nav-group + .nav-group {
  margin-top: 16px;
}

.nav-title,
.toc-title {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.nav-link,
.toc-link {
  display: block;
  font-size: 13px;
  color: #1f2937;
  text-decoration: none;
  padding: 6px 8px;
  border-radius: 8px;
}

.nav-link:hover,
.toc-link:hover {
  background: #f3f4f6;
}

.doc-main {
  min-width: 0;
}

.header {
  margin-bottom: 16px;
}

.title {
  font-size: 22px;
  font-weight: 600;
  color: #1f1f1f;
}

.subtitle {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
}

.doc-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
}

.section-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.step {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
}

.step-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.step-body {
  display: grid;
  gap: 12px;
}

.step-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.preview-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.card {
  background: #fafafa;
  border-radius: 10px;
  padding: 12px;
  border: 1px solid #f0f0f0;
}

.card-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
}

.doc-summary {
  font-size: 13px;
  color: #111827;
  margin-bottom: 12px;
}

.form {
  display: grid;
  gap: 10px;
}

.field {
  display: grid;
  gap: 6px;
}

.label {
  font-size: 12px;
  color: #8c8c8c;
}

.row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.md-block {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 8px;
  background: #fff;
}

.md-title {
  background: #f5f5f5;
  color: #666;
  font-size: 12px;
  padding: 6px 10px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}

.md-code {
  white-space: pre-wrap;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  background: #ffffff;
  padding: 10px;
  font-size: 12px;
  margin: 0;
}

.doc-viewer {
  background: #ffffff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px;
  overflow: auto;
}

.doc-viewer :deep(*) {
  font-size: 13px;
  line-height: 1.7;
  color: #111827;
  max-width: 100%;
  overflow-wrap: anywhere;
  word-break: break-word;
  column-count: 1;
  column-gap: 0;
}

.doc-viewer :deep(h1),
.doc-viewer :deep(h2),
.doc-viewer :deep(h3),
.doc-viewer :deep(h4),
.doc-viewer :deep(h5),
.doc-viewer :deep(h6) {
  margin: 16px 0 8px;
  line-height: 1.4;
  clear: both;
}

.doc-viewer :deep(p),
.doc-viewer :deep(li) {
  margin: 6px 0;
  clear: both;
}

.doc-viewer :deep(pre) {
  background: #f8fafc;
  color: #111827;
  padding: 12px;
  border-radius: 8px;
  overflow: auto;
  white-space: pre;
  line-height: 1.6;
  display: block;
  width: 100%;
  clear: both;
  position: static;
  float: none;
  box-sizing: border-box;
}

.doc-viewer :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
  color: inherit;
}

.doc-viewer :deep(pre code) {
  display: block;
  white-space: pre-wrap;
  word-break: break-word;
  color: #111827;
}

.doc-viewer :deep(.hljs) {
  color: #111827;
  background: transparent;
}

.preview {
  width: 100%;
  height: 560px;
  border: 0;
}

@media (max-width: 1200px) {
  .access-layout {
    grid-template-columns: 1fr;
  }
  .side-nav,
  .toc {
    position: static;
    order: -1;
  }
}

@media (max-width: 1100px) {
  .step-grid,
  .preview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
