<template>
  <div class="access-layout">
    <aside class="side-nav">
      <div class="nav-group">
        <div class="nav-title">Settings</div>
        <a class="nav-link" href="#security">安全配置</a>
      </div>
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
        <div class="title">第三方接入设置与调试</div>
        <div class="subtitle">配置访客接入方式、安全策略，并在线调试接入效果</div>
      </div>

      <section id="security" class="doc-section">
        <div class="section-title">安全配置</div>
        <div class="section-desc">配置访客接入的认证方式和密钥，修改后实时生效</div>
        <div class="security-card">
          <div class="security-row">
            <div class="security-label">
              <span>启用 Token 验证</span>
              <a-tag :color="tokenMode ? 'blue' : 'green'" size="small" style="margin-left: 8px">
                {{ tokenMode ? 'Token模式' : '免Token模式' }}
              </a-tag>
            </div>
            <a-switch v-model:checked="visitorTokenRequired" :loading="tokenSwitchSaving" @change="onTokenSwitchChange" />
          </div>
          <div class="security-desc">
            {{ tokenMode ? '第三方接入需先通过后端获取短时Token，适合需要身份验证的场景' : '访客通过设备码自动标识，无需获取Token。适合官网公开客服等场景' }}
          </div>

          <a-alert
            v-if="!tokenMode && !secretKey"
            type="error"
            show-icon
            style="margin: 12px 0"
          >
            <template #message>
              <span style="font-weight: 600">安全警告：当前为免Token模式且未配置接入密钥，任何人可通过链接直接访问客服！</span>
            </template>
            <template #description>
              建议至少配置一个接入密钥，访客需携带 <code>?key=xxx</code> 参数才能访问。
            </template>
          </a-alert>

          <a-alert
            v-else-if="!tokenMode && secretKey"
            message="当前为免Token模式，访客需携带 ?key=密钥 参数才能访问客服"
            type="warning"
            show-icon
            style="margin: 12px 0"
          />

          <div class="security-row" style="margin-top: 12px">
            <span class="security-label">接入密钥</span>
          </div>
          <div style="display: flex; gap: 8px; align-items: center">
            <a-input
              v-model:value="secretKey"
              :placeholder="tokenMode ? '密钥（留空则不校验）' : '接入密钥（配置后访客需通过 ?key= 传递）'"
              allowClear
              style="flex: 1"
            />
            <a-button size="small" @click="generateVisitorSecretKey" :loading="visitorSecretGenerating">
              {{ secretKey ? '重新生成' : '生成密钥' }}
            </a-button>
            <a-button type="primary" size="small" @click="saveVisitorSecretConfig" :loading="visitorSecretSaving">
              保存
            </a-button>
          </div>
          <div class="security-desc" style="margin-top: 4px">
            {{ tokenMode ? '生产环境请将密钥放在你自己的后端，不要写进前端页面' : '配置密钥后，访客必须携带 ?key=xxx 参数才能访问' }}
          </div>
        </div>
      </section>

      <section id="start" class="doc-section">
        <div class="section-title">快速开始</div>
        <div class="section-desc">{{ tokenMode ? '按步骤填入参数，即可完成接入调试' : '免Token模式下，访客通过设备码自动标识，以下参数均为可选' }}</div>
        <div class="step">
          <div class="step-title">1) {{ tokenMode ? '填写用户参数' : '填写可选参数' }}</div>
          <div class="step-body">
            <div class="form">
              <div class="field">
                <span class="label">访问地址（baseUrl）</span>
                <div style="display:flex;gap:8px">
                  <a-select v-model:value="baseUrl" style="flex:1" :options="domainOptions.map(d => ({ label: d, value: d }))" />
                  <a-input v-model:value="baseUrl" placeholder="或手动输入" style="width:200px" />
                </div>
              </div>
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
                <span class="label">agentId（指定客服ID，可选，填写后访客将直接接入该客服）</span>
                <a-input v-model:value="agentId" placeholder="客服ID（可选）" />
              </div>
              <div class="field" v-if="secretKey">
                <span class="label">{{ tokenMode ? '接入密钥（用于本地调试获取Token，生产环境请放后端）' : '接入密钥（已在上方安全配置中设置）' }}</span>
                <a-input :value="secretKey" disabled />
              </div>
              <a-alert v-if="!tokenMode" message="免Token模式下，系统自动使用设备码作为访客唯一标识。密钥已在上方「安全配置」中管理。" type="info" show-icon style="margin-top: 4px;" />
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
            <div class="widget-configurator">
              <!-- 挂件配置器 -->
              <div class="wc-section">
                <div class="wc-title">挂件外观配置</div>
                <div class="wc-grid">
                  <div class="wc-field">
                    <span class="wc-label">按钮尺寸</span>
                    <a-input-number v-model:value="wc.buttonSize" :min="32" :max="120" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">按钮颜色</span>
                    <input type="color" v-model="wc.buttonColor" class="wc-color" />
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">按钮圆角</span>
                    <a-input-number v-model:value="wc.buttonBorderRadius" :min="0" :max="100" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field" style="flex-wrap:wrap">
                    <span class="wc-label">自定义图标</span>
                    <CropperUpload
                      v-model:value="wc.buttonIcon"
                      :uploadApi="uploadImg"
                      :aspectRatio="1"
                      btnText="上传图标"
                      accept="image/png,image/jpeg,image/gif,image/svg+xml,image/x-icon,image/webp"
                    />
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">按钮文字</span>
                    <a-input v-model:value="wc.buttonText" placeholder="无图标时显示" size="small" style="width:120px" />
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">定位位置</span>
                    <a-radio-group v-model:value="wc.positionMode" size="small">
                      <a-radio-button value="rightBottom">右下</a-radio-button>
                      <a-radio-button value="leftBottom">左下</a-radio-button>
                      <a-radio-button value="rightTop">右上</a-radio-button>
                      <a-radio-button value="leftTop">左上</a-radio-button>
                    </a-radio-group>
                  </div>
                  <div class="wc-field" v-if="wc.positionMode.includes('right')">
                    <span class="wc-label">距右</span>
                    <a-input-number v-model:value="wc.positionRight" :min="0" :max="500" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field" v-if="wc.positionMode.includes('left')">
                    <span class="wc-label">距左</span>
                    <a-input-number v-model:value="wc.positionLeft" :min="0" :max="500" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field" v-if="wc.positionMode.includes('Bottom')">
                    <span class="wc-label">距下</span>
                    <a-input-number v-model:value="wc.positionBottom" :min="0" :max="500" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field" v-if="wc.positionMode.includes('Top')">
                    <span class="wc-label">距上</span>
                    <a-input-number v-model:value="wc.positionTop" :min="0" :max="500" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">弹窗标题</span>
                    <a-input v-model:value="wc.panelTitle" size="small" style="width:140px" />
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">弹窗主题色</span>
                    <input type="color" v-model="wc.panelColor" class="wc-color" />
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">弹窗宽度</span>
                    <a-input-number v-model:value="wc.panelWidth" :min="300" :max="800" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">弹窗高度</span>
                    <a-input-number v-model:value="wc.panelHeight" :min="400" :max="900" size="small" /> <span class="wc-unit">px</span>
                  </div>
                  <div class="wc-field">
                    <span class="wc-label">z-index</span>
                    <a-input-number v-model:value="wc.zIndex" :min="1" :max="99999" size="small" />
                  </div>
                </div>
              </div>
              <!-- 实时预览 -->
              <div class="wc-section">
                <div class="wc-title">实时预览</div>
                <div class="wc-preview-window">
                  <div class="wc-preview-titlebar">
                    <span class="wc-dot" style="background:#ff5f57"></span>
                    <span class="wc-dot" style="background:#febc2e"></span>
                    <span class="wc-dot" style="background:#28c840"></span>
                    <span style="margin-left:8px;font-size:11px;color:#999">example.com</span>
                  </div>
                  <div class="wc-preview-content">
                    <div class="wc-preview-text">第三方网站页面</div>
                    <div
                      class="wc-preview-btn"
                      :style="{
                        width: Math.min(wc.buttonSize, 56) + 'px',
                        height: Math.min(wc.buttonSize, 56) + 'px',
                        borderRadius: Math.min(wc.buttonBorderRadius, 28) + 'px',
                        background: wc.buttonColor,
                        right: wc.positionMode.includes('right') ? Math.round(wc.positionRight / 10) + 'px' : 'auto',
                        left: wc.positionMode.includes('left') ? Math.round(wc.positionLeft / 10) + 'px' : 'auto',
                        bottom: wc.positionMode.includes('Bottom') ? Math.round(wc.positionBottom / 10) + 'px' : 'auto',
                        top: wc.positionMode.includes('Top') ? Math.round(wc.positionTop / 10) + 'px' : 'auto',
                      }"
                    >
                      <img v-if="wc.buttonIcon" :src="resolveUrl(wc.buttonIcon)" style="width:60%;height:60%;object-fit:contain" />
                      <span v-else-if="wc.buttonText" style="font-size:10px;color:#fff">{{ wc.buttonText }}</span>
                      <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="width:50%;height:50%;color:#fff">
                        <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                    </div>
                  </div>
                </div>
              </div>
              <!-- 生成的嵌入代码 -->
              <div class="wc-section">
                <div class="wc-title">嵌入代码</div>
                <pre class="wc-code">{{ widgetEmbedCode }}</pre>
                <div class="row" style="margin-top:8px">
                  <a-button type="primary" size="small" @click="copyWidgetCode">复制代码</a-button>
                  <a-button size="small" @click="downloadWidgetHtml">下载演示HTML</a-button>
                </div>
              </div>
            </div>
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
      <a class="toc-link" href="#security">安全配置</a>
      <a class="toc-link" href="#start">快速开始</a>
      <a class="toc-link" v-if="tokenMode" href="#token">获取 Token</a>
      <a class="toc-link" href="#access">接入方式</a>
      <a class="toc-link" href="#preview">预览效果</a>
      <a class="toc-link" href="#faq">常见问题</a>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { defHttp } from '/@/utils/http/axios';
import { uploadImg } from '/@/api/sys/upload';
import { CropperUpload } from '/@/components/Cropper';
import { getFileAccessHttpUrl } from '/@/utils/common/compUtils';
import { useGlobSetting } from '/@/hooks/setting';
import MarkdownIt from 'markdown-it';
import hljs from 'highlight.js';

const globSetting = useGlobSetting();
function getOriginUrl() {
  return globSetting.isElectronPlatform ? globSetting.apiUrl : window.location.origin;
}
const baseUrl = ref(getOriginUrl());
const domainOptions = ref<string[]>([]);
// 安全配置（从后端加载，可编辑）
const visitorTokenRequired = ref(true); // Token验证开关
const tokenSwitchSaving = ref(false);
const secretKey = ref('');
const visitorSecretSaving = ref(false);
const visitorSecretGenerating = ref(false);
const loadingMode = ref(true); // 正在加载配置
const tokenMode = computed(() => visitorTokenRequired.value);
// 调试参数
const externalUserId = ref('U1001');
const userName = ref('Tom');
const source = ref('partnerA');
const agentId = ref('');
const token = ref('');
const expireAt = ref(0);
const loading = ref(false);
const backendTokenUrl = ref('/your-backend/visitor-token');
const backendTokenLoading = ref(false);
const tokenDocTab = ref('curl');
const accessType = ref('url');

// 挂件可视化配置（纯前端状态）
const wc = reactive({
  buttonSize: 56,
  buttonColor: '#4c6ef5',
  buttonIcon: '',
  buttonText: '',
  buttonBorderRadius: 28,
  positionMode: 'rightBottom' as 'rightBottom' | 'leftBottom' | 'rightTop' | 'leftTop',
  positionRight: 24,
  positionBottom: 24,
  positionLeft: 24,
  positionTop: 24,
  panelWidth: 420,
  panelHeight: 640,
  panelTitle: '在线客服',
  panelColor: '#4c6ef5',
  zIndex: 9999,
});

onMounted(async () => {
  // 并行加载域名配置和访客接入配置
  await Promise.all([
    loadDomainOptions(),
    loadVisitorAccessConfig(),
  ]);
  loadingMode.value = false;
});

async function loadDomainOptions() {
  try {
    const domainRes = await defHttp.get(
      { url: '/cs/domain/get' },
      { successMessageMode: 'none', isTransformResponse: false },
    );
    const domainData = domainRes?.result || domainRes;
    if (domainData?.domains) {
      const lines = domainData.domains.split('\n').map((s: string) => s.trim()).filter((s: string) => s);
      const normalized = lines.map((d: string) => {
        if (!/^https?:\/\//i.test(d)) return 'https://' + d;
        return d;
      });
      const allOptions = [getOriginUrl(), ...normalized];
      domainOptions.value = [...new Set(allOptions)];
    } else {
      domainOptions.value = [getOriginUrl()];
    }
  } catch {
    domainOptions.value = [getOriginUrl()];
  }
}

async function loadVisitorAccessConfig() {
  try {
    const res = await defHttp.get(
      { url: '/cs/agent/global/visitor-access' },
      { successMessageMode: 'none', isTransformResponse: false },
    );
    if (res?.success) {
      const config = res.result || {};
      visitorTokenRequired.value = config.tokenRequired !== 'false';
      secretKey.value = config.secretKey || '';
    } else {
      visitorTokenRequired.value = true;
      secretKey.value = '';
    }
  } catch (e) {
    console.error('加载访客接入配置失败', e);
  }
}

async function generateVisitorSecretKey() {
  visitorSecretGenerating.value = true;
  try {
    const res = await defHttp.get(
      { url: '/cs/agent/global/visitor-access/generate-key' },
      { successMessageMode: 'none', isTransformResponse: false },
    );
    if (res?.success && res.result) {
      secretKey.value = res.result;
      message.success('密钥已生成，请点击保存');
    }
  } catch (e) {
    message.error('生成密钥失败');
  } finally {
    visitorSecretGenerating.value = false;
  }
}

async function saveVisitorSecretConfig() {
  visitorSecretSaving.value = true;
  try {
    const payload = {
      tokenRequired: visitorTokenRequired.value ? 'true' : 'false',
      secretKey: secretKey.value || '',
    };
    await defHttp.put({ url: '/cs/agent/global/visitor-access', data: payload });
    message.success('保存成功');
    await loadVisitorAccessConfig();
  } catch (e) {
    console.error('保存访客接入配置失败', e);
    message.error('保存失败');
  } finally {
    visitorSecretSaving.value = false;
  }
}

async function onTokenSwitchChange(checked: boolean) {
  tokenSwitchSaving.value = true;
  const prevValue = !checked;
  try {
    const payload = {
      tokenRequired: checked ? 'true' : 'false',
      secretKey: secretKey.value || '',
    };
    await defHttp.put({ url: '/cs/agent/global/visitor-access', data: payload });
    message.success(checked ? 'Token验证已启用' : 'Token验证已关闭');
  } catch (e) {
    console.error('切换Token模式失败', e);
    message.error('切换失败');
    visitorTokenRequired.value = prevValue;
  } finally {
    tokenSwitchSaving.value = false;
  }
}

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
function resolveUrl(url: string) {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url;
  return getFileAccessHttpUrl(url);
}

const accessUrl = computed(() => {
  const base = baseUrl.value.replace(/\/$/, '');
  if (!tokenMode.value) {
    const params = new URLSearchParams();
    if (secretKey.value) params.set('key', secretKey.value);
    if (userName.value) params.set('userName', userName.value);
    if (source.value) params.set('source', source.value);
    if (agentId.value) params.set('agentId', agentId.value);
    const qs = params.toString();
    return `${base}/cs/userChat${qs ? '?' + qs : ''}`;
  }
  if (!token.value) {
    let url = `${base}/cs/userChat?token=短时token&externalUserId=${externalUserId.value}&userName=${userName.value}&source=${source.value}`;
    if (agentId.value) url += `&agentId=${agentId.value}`;
    return url;
  }
  const params = new URLSearchParams({
    token: token.value,
    externalUserId: externalUserId.value,
    userName: userName.value,
    source: source.value,
  });
  if (agentId.value) params.set('agentId', agentId.value);
  return `${base}/cs/userChat?${params.toString()}`;
});

const tokenDocCurlMarkdown = computed(() => {
  return `
\`\`\`bash
curl -X POST "${baseUrl.value}/jeecgboot/airag/cs/visitor/token" \\
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
    String url = "${baseUrl.value}/jeecgboot/airag/cs/visitor/token";
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
$ch = curl_init("${baseUrl.value}/jeecgboot/airag/cs/visitor/token");
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
  const resp = await fetch("${baseUrl.value}/jeecgboot/airag/cs/visitor/token", {
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
  const agentParam = agentId.value ? `&agentId=${encodeURIComponent(agentId.value)}` : '';
  if (!tokenMode.value) {
    const params: string[] = [];
    if (secretKey.value) params.push(`key=${encodeURIComponent(secretKey.value)}`);
    if (userName.value) params.push(`userName=${encodeURIComponent(userName.value)}`);
    if (source.value) params.push(`source=${encodeURIComponent(source.value)}`);
    if (agentId.value) params.push(`agentId=${encodeURIComponent(agentId.value)}`);
    const freeUrl = params.length
      ? `${baseUrl.value}/cs/userChat?${params.join('&')}`
      : `${baseUrl.value}/cs/userChat`;
    const keyNote = secretKey.value
      ? '免Token模式下需携带 `key` 参数（接入密钥）。系统自动使用设备码标识用户。'
      : '免Token模式下，访客直接通过URL访问，无需任何凭证。系统自动使用设备码标识用户。';
    const jsExample = secretKey.value
      ? `window.location.href = "${baseUrl.value}/cs/userChat?key=" + encodeURIComponent("你的接入密钥");`
      : `window.location.href = "${baseUrl.value}/cs/userChat";`;
    return `
${keyNote}${agentId.value ? ' 填写了 `agentId` 参数，访客将直接接入指定客服。' : ''}

\`\`\`text
${freeUrl}
\`\`\`

${secretKey.value ? '必传参数：`key`（接入密钥）。可选参数' : '可选参数'}：\`userName\`（昵称）、\`source\`（来源标识）、\`agentId\`（指定客服）。

\`\`\`js
// 直接跳转即可，无需获取Token
${jsExample}
\`\`\`
`;
  }
  const previewToken = token.value || '短时token';
  const accessPreviewUrl = `${baseUrl.value}/cs/userChat?token=${encodeURIComponent(previewToken)}&externalUserId=${encodeURIComponent(externalUserId.value)}&userName=${encodeURIComponent(userName.value)}&source=${encodeURIComponent(source.value)}${agentParam}`;
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
    const url = "${baseUrl.value}/cs/userChat?token=" + encodeURIComponent(t)
      + "&externalUserId=${externalUserId.value}&userName=${userName.value}&source=${source.value}${agentParam}";
    window.location.href = url;
  });
\`\`\`
`;
});

const accessDocIframeMarkdown = computed(() => {
  const agentParam = agentId.value ? `&agentId=${encodeURIComponent(agentId.value)}` : '';
  if (!tokenMode.value) {
    const params: string[] = [];
    if (secretKey.value) params.push(`key=${encodeURIComponent(secretKey.value)}`);
    if (agentId.value) params.push(`agentId=${encodeURIComponent(agentId.value)}`);
    const freeUrl = params.length
      ? `${baseUrl.value}/cs/userChat?${params.join('&')}`
      : `${baseUrl.value}/cs/userChat`;
    const note = secretKey.value ? '免Token模式下需携带 `key` 参数：' : '免Token模式下直接嵌入即可：';
    return `
${note}

\`\`\`html
<iframe src="${freeUrl}" style="width:420px;height:640px;border:0;"></iframe>
\`\`\`
`;
  }
  const previewToken = token.value || '短时token';
  const accessPreviewUrl = `${baseUrl.value}/cs/userChat?token=${encodeURIComponent(previewToken)}&externalUserId=${encodeURIComponent(externalUserId.value)}&userName=${encodeURIComponent(userName.value)}&source=${encodeURIComponent(source.value)}${agentParam}`;
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
    const url = "${baseUrl.value}/cs/userChat?token=" + encodeURIComponent(t)
      + "&externalUserId=${externalUserId.value}&userName=${userName.value}&source=${source.value}${agentParam}";
    document.getElementById("cs-iframe").src = url;
  });
\`\`\`
`;
});

// ============ 挂件嵌入代码生成 ============
function buildWidgetOptionsCode() {
  const lines: string[] = [];
  if (wc.buttonSize !== 56) lines.push(`  buttonSize: ${wc.buttonSize},`);
  if (wc.buttonColor !== '#4c6ef5') lines.push(`  buttonColor: "${wc.buttonColor}",`);
  if (wc.buttonIcon) lines.push(`  buttonIcon: "${resolveUrl(wc.buttonIcon)}",`);
  if (wc.buttonText) lines.push(`  buttonText: "${wc.buttonText}",`);
  if (wc.buttonBorderRadius !== 28) lines.push(`  buttonBorderRadius: ${wc.buttonBorderRadius},`);
  if (wc.panelWidth !== 420) lines.push(`  width: ${wc.panelWidth},`);
  if (wc.panelHeight !== 640) lines.push(`  height: ${wc.panelHeight},`);
  if (wc.panelTitle !== '在线客服') lines.push(`  title: "${wc.panelTitle}",`);
  if (wc.panelColor !== '#4c6ef5') lines.push(`  panelColor: "${wc.panelColor}",`);
  if (wc.zIndex !== 9999) lines.push(`  zIndex: ${wc.zIndex},`);
  // 位置
  const pos: string[] = [];
  if (wc.positionMode.includes('right') && wc.positionRight !== 24) pos.push(`right: ${wc.positionRight}`);
  if (wc.positionMode.includes('Bottom') && wc.positionBottom !== 24) pos.push(`bottom: ${wc.positionBottom}`);
  if (wc.positionMode.includes('left')) pos.push(`left: ${wc.positionLeft}`);
  if (wc.positionMode.includes('Top')) pos.push(`top: ${wc.positionTop}`);
  if (pos.length) lines.push(`  position: { ${pos.join(', ')} },`);
  return lines.length ? '\n' + lines.join('\n') : '';
}

const widgetEmbedCode = computed(() => {
  const scriptCloseTag = '</' + 'script>';
  const opts = buildWidgetOptionsCode();
  const agentLine = agentId.value ? `\n  agentId: "${agentId.value}",` : '';
  if (!tokenMode.value) {
    const keyLine = secretKey.value ? `\n  key: "${secretKey.value}",` : '';
    return `<script src="${baseUrl.value}/cs-widget.js">${scriptCloseTag}
<script>
JeecgCsWidget.init({
  baseUrl: "${baseUrl.value}",${keyLine}${agentLine}
  userName: "${userName.value}",
  source: "${source.value}",${opts}
});
${scriptCloseTag}`;
  }
  return `<script src="${baseUrl.value}/cs-widget.js">${scriptCloseTag}
<script>
JeecgCsWidget.init({
  baseUrl: "${baseUrl.value}",
  externalUserId: "${externalUserId.value}",
  userName: "${userName.value}",
  source: "${source.value}",${agentLine}${opts}
  getToken: function() {
    return fetch("/your-backend/visitor-token", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        externalUserId: "${externalUserId.value}",
        userName: "${userName.value}",
        source: "${source.value}"
      })
    }).then(function(r){ return r.json(); })
      .then(function(d){ return d.token || (d.result && d.result.token) || ""; });
  }
});
${scriptCloseTag}`;
});

function copyWidgetCode() {
  navigator.clipboard.writeText(widgetEmbedCode.value).then(() => {
    message.success('挂件代码已复制到剪贴板');
  }).catch(() => {
    message.error('复制失败，请手动复制');
  });
}

function downloadWidgetHtml() {
  const html = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>客服挂件演示</title>
  <style>
    body { margin: 0; min-height: 100vh; background: #f7f8fa; display: flex; align-items: center; justify-content: center; font-family: sans-serif; color: #666; }
  </style>
</head>
<body>
  <h2>右下角会出现客服聊天挂件</h2>
  ${widgetEmbedCode.value}
</body>
</html>`;
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'cs-widget-demo.html';
  a.click();
  URL.revokeObjectURL(url);
}

const accessDocUrlHtml = computed(() => md.render(accessDocUrlMarkdown.value));
const accessDocIframeHtml = computed(() => md.render(accessDocIframeMarkdown.value));

const faqDocMarkdown = computed(() => {
  return `
### 没有密钥，前端怎么获取 token？
未配置密钥时可以直接调用 \`/airag/cs/visitor/token\` 获取短时 token。

### 配了密钥会不会泄露？
密钥只放后端，前端只拿 token，不接触密钥。建议配合频率控制。

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

可在本页上方「安全配置」中切换。`;
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
      baseUrl: baseUrl.value,
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
    if (agentId.value) opts.agentId = agentId.value;
    // 应用当前挂件配置
    if (wc.buttonSize !== 56) opts.buttonSize = wc.buttonSize;
    if (wc.buttonColor !== '#4c6ef5') opts.buttonColor = wc.buttonColor;
    if (wc.buttonIcon) opts.buttonIcon = resolveUrl(wc.buttonIcon);
    if (wc.buttonText) opts.buttonText = wc.buttonText;
    if (wc.buttonBorderRadius !== 28) opts.buttonBorderRadius = wc.buttonBorderRadius;
    if (wc.panelWidth !== 420) opts.width = wc.panelWidth;
    if (wc.panelHeight !== 640) opts.height = wc.panelHeight;
    if (wc.panelTitle !== '在线客服') opts.title = wc.panelTitle;
    if (wc.panelColor !== '#4c6ef5') opts.panelColor = wc.panelColor;
    if (wc.zIndex !== 9999) opts.zIndex = wc.zIndex;
    const position: any = {};
    if (wc.positionMode.includes('right') && wc.positionRight !== 24) position.right = wc.positionRight;
    if (wc.positionMode.includes('Bottom') && wc.positionBottom !== 24) position.bottom = wc.positionBottom;
    if (wc.positionMode.includes('left')) position.left = wc.positionLeft;
    if (wc.positionMode.includes('Top')) position.top = wc.positionTop;
    if (Object.keys(position).length) opts.position = position;

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

.security-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
}

.security-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.security-label {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  display: flex;
  align-items: center;
}

.security-desc {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 4px;
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

/* 挂件配置器 */
.widget-configurator {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.wc-section {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 12px 16px;
  background: #fafafa;
}
.wc-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}
.wc-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
}
.wc-field {
  display: flex;
  align-items: center;
  gap: 6px;
}
.wc-label {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  min-width: 70px;
}
.wc-unit {
  font-size: 11px;
  color: #999;
}
.wc-color {
  width: 36px;
  height: 26px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  padding: 0;
}
.wc-preview-window {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}
.wc-preview-titlebar {
  display: flex;
  align-items: center;
  padding: 6px 10px;
  background: #e8e8e8;
  gap: 4px;
}
.wc-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.wc-preview-content {
  position: relative;
  height: 200px;
  background: #fafafa;
  overflow: hidden;
}
.wc-preview-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 12px;
  color: #ccc;
}
.wc-preview-btn {
  position: absolute;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,.15);
  transition: all .3s;
  overflow: hidden;
}
.wc-code {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px 14px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
