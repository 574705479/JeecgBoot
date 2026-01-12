<template>
  <BasicModal 
    @register="registerModal" 
    :title="title" 
    :width="700" 
    :canFullscreen="false"
    @ok="handleSubmit"
  >
    <a-spin :spinning="loading">
      <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical">
        <a-alert 
          message="接入配置说明" 
          description="配置后，第三方系统可通过签名验证安全地接入此AI应用。留空则允许无签名访问。"
          type="info" 
          show-icon 
          style="margin-bottom: 16px"
        />

        <a-form-item label="密钥" name="secretKey">
          <a-input-group compact>
            <a-input 
              v-model:value="formState.secretKey" 
              placeholder="点击生成密钥" 
              style="width: calc(100% - 100px)"
              readonly
            />
            <a-button type="primary" @click="generateNewKey">
              {{ formState.secretKey ? '重新生成' : '生成密钥' }}
            </a-button>
          </a-input-group>
        </a-form-item>

        <a-form-item label="域名白名单" name="domainWhitelist">
          <a-textarea 
            v-model:value="formState.domainWhitelist" 
            placeholder="允许访问的域名，多个用逗号分隔，支持*.example.com通配符&#10;例如: example.com, *.myapp.com"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="签名有效期（分钟）" name="tokenExpireMinutes">
              <a-input-number 
                v-model:value="formState.tokenExpireMinutes" 
                :min="1" 
                :max="60" 
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="启用状态" name="enabled">
              <a-switch v-model:checked="formState.enabled" checked-children="启用" un-checked-children="禁用" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="备注" name="remark">
          <a-textarea 
            v-model:value="formState.remark" 
            placeholder="可选：备注信息"
            :auto-size="{ minRows: 2, maxRows: 3 }"
          />
        </a-form-item>

        <a-divider>接入示例</a-divider>

        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="iframe" tab="iframe嵌入">
            <div class="code-block">
              <div class="code-header">
                <span>HTML</span>
                <a-button type="link" size="small" @click="copyCode('iframe')">
                  <Icon icon="ant-design:copy-outlined" /> 复制
                </a-button>
              </div>
              <pre>{{ iframeCode }}</pre>
            </div>
          </a-tab-pane>
          <a-tab-pane key="script" tab="Script嵌入">
            <div class="code-block">
              <div class="code-header">
                <span>HTML</span>
                <a-button type="link" size="small" @click="copyCode('script')">
                  <Icon icon="ant-design:copy-outlined" /> 复制
                </a-button>
              </div>
              <pre>{{ scriptCode }}</pre>
            </div>
          </a-tab-pane>
          <a-tab-pane key="token" tab="Token生成示例">
            <div class="code-block">
              <div class="code-header">
                <span>JavaScript (Node.js)</span>
                <a-button type="link" size="small" @click="copyCode('token')">
                  <Icon icon="ant-design:copy-outlined" /> 复制
                </a-button>
              </div>
              <pre>{{ tokenCode }}</pre>
            </div>
          </a-tab-pane>
          <a-tab-pane key="test" tab="测试模板">
            <div class="test-template-info">
              <a-alert 
                message="下载测试模板" 
                description="下载完整的HTML测试页面，可以直接在浏览器中打开进行本地测试。模板已预填当前应用ID和服务器地址。"
                type="success" 
                show-icon 
                style="margin-bottom: 16px"
              />
              <a-button type="primary" @click="downloadTestTemplate">
                <Icon icon="ant-design:download-outlined" /> 下载测试模板 (chat-test.html)
              </a-button>
            </div>
          </a-tab-pane>
        </a-tabs>
      </a-form>
    </a-spin>
  </BasicModal>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue';
import { BasicModal, useModalInner } from '/@/components/Modal';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import { copyTextToClipboard } from '/@/hooks/web/useCopyToClipboard';
import Icon from '/@/components/Icon';

const { createMessage } = useMessage();

const emit = defineEmits(['register', 'success']);

const title = ref('接入设置');
const loading = ref(false);
const formRef = ref();
const activeTab = ref('iframe');
const appId = ref('');
const secretId = ref('');

// 表单状态
const formState = reactive({
  secretKey: '',
  domainWhitelist: '',
  tokenExpireMinutes: 5,
  enabled: true,
  remark: '',
});

// 表单规则
const rules = {
  tokenExpireMinutes: [
    { required: true, message: '请输入签名有效期' },
    { type: 'number', min: 1, max: 60, message: '有效期需在1-60分钟之间' },
  ],
};

// 注册弹窗
const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
  setModalProps({ confirmLoading: false });
  appId.value = data.appId;
  
  // 重置表单
  Object.assign(formState, {
    secretKey: '',
    domainWhitelist: '',
    tokenExpireMinutes: 5,
    enabled: true,
    remark: '',
  });
  secretId.value = '';
  
  // 加载已有配置
  await loadSecretConfig();
});

// 加载密钥配置
async function loadSecretConfig() {
  loading.value = true;
  try {
    const res = await defHttp.get({
      url: '/airag/appSecret/queryByAppId',
      params: { appId: appId.value }
    }, { isTransformResponse: false });
    
    if (res.success && res.result) {
      const config = res.result;
      secretId.value = config.id;
      formState.secretKey = config.secretKey || '';
      formState.domainWhitelist = config.domainWhitelist || '';
      formState.tokenExpireMinutes = config.tokenExpireMinutes || 5;
      formState.enabled = config.enabled === 1;
      formState.remark = config.remark || '';
    }
  } catch (error) {
    console.error('加载接入配置失败', error);
  } finally {
    loading.value = false;
  }
}

// 生成新密钥
async function generateNewKey() {
  try {
    const res = await defHttp.get({
      url: '/airag/appSecret/generateKey'
    }, { isTransformResponse: false });
    
    if (res.success && res.result) {
      formState.secretKey = res.result;
      createMessage.success('密钥已生成');
    }
  } catch (error) {
    createMessage.error('生成密钥失败');
  }
}

// 提交表单
async function handleSubmit() {
  try {
    await formRef.value?.validate();
    
    setModalProps({ confirmLoading: true });
    
    const params = {
      id: secretId.value || undefined,
      appId: appId.value,
      secretKey: formState.secretKey,
      domainWhitelist: formState.domainWhitelist,
      tokenExpireMinutes: formState.tokenExpireMinutes,
      enabled: formState.enabled ? 1 : 0,
      remark: formState.remark,
    };
    
    const url = secretId.value ? '/airag/appSecret/edit' : '/airag/appSecret/add';
    const method = secretId.value ? 'put' : 'post';
    
    await defHttp[method]({ url, params });
    
    createMessage.success('保存成功');
    closeModal();
    emit('success');
  } catch (error) {
    console.error('保存接入配置失败', error);
    createMessage.error('保存失败');
  } finally {
    setModalProps({ confirmLoading: false });
  }
}

// 获取当前域名
const currentDomain = computed(() => {
  return window.location.origin;
});

// iframe代码
const iframeCode = computed(() => {
  const params = new URLSearchParams();
  params.append('externalUserId', 'YOUR_USER_ID');
  params.append('externalUserName', '用户名');
  params.append('sessionMode', 'persist');
  if (formState.secretKey) {
    params.append('token', 'YOUR_TOKEN');
    params.append('timestamp', 'YOUR_TIMESTAMP');
  }
  
  return `<iframe 
  src="${currentDomain.value}/ai/app/chat/${appId.value}?${params.toString()}"
  style="width: 100%; height: 600px; border: none;">
</iframe>`;
});

// script代码
const scriptCode = computed(() => {
  let code = `<script src="${currentDomain.value}/chat/chat.js" id="e7e007dd52f67fe36365eff636bbffbd"><\/script>
<script>
  createAiChat({
    appId: "${appId.value}",
    externalUserId: "YOUR_USER_ID",
    externalUserName: "用户名",
    sessionMode: "persist",`;
  
  if (formState.secretKey) {
    code += `
    token: "YOUR_TOKEN",
    timestamp: Date.now(),`;
  }
  
  code += `
    iconPosition: "bottom-right",
    showWelcomePage: true
  })
<\/script>`;
  
  return code;
});

// token生成代码
const tokenCode = computed(() => {
  return `const crypto = require('crypto');

const appId = '${appId.value}';
const secretKey = '${formState.secretKey || 'YOUR_SECRET_KEY'}';
const timestamp = Date.now();

// token = MD5(appId + secretKey + timestamp)
const token = crypto.createHash('md5')
  .update(appId + secretKey + timestamp)
  .digest('hex');

console.log('token:', token);
console.log('timestamp:', timestamp);`;
});

// 复制代码
function copyCode(type: string) {
  let code = '';
  switch (type) {
    case 'iframe':
      code = iframeCode.value;
      break;
    case 'script':
      code = scriptCode.value;
      break;
    case 'token':
      code = tokenCode.value;
      break;
  }
  
  const success = copyTextToClipboard(code);
  if (success) {
    createMessage.success('复制成功');
  } else {
    createMessage.error('复制失败');
  }
}

// 下载测试模板
function downloadTestTemplate() {
  const testHtml = generateTestHtml();
  const blob = new Blob([testHtml], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `chat-test-${appId.value}.html`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  createMessage.success('测试模板已下载');
}

// 生成测试HTML
function generateTestHtml() {
  return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AI聊天接入测试 - ${appId.value}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container { max-width: 1200px; margin: 0 auto; }
        h1 {
            color: white;
            text-align: center;
            margin-bottom: 30px;
            font-size: 28px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
        }
        .config-panel {
            background: white;
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 24px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        .config-panel h2 {
            color: #333;
            margin-bottom: 20px;
            font-size: 18px;
            border-bottom: 2px solid #667eea;
            padding-bottom: 10px;
        }
        .form-row {
            display: flex;
            flex-wrap: wrap;
            gap: 16px;
            margin-bottom: 16px;
        }
        .form-group {
            flex: 1;
            min-width: 200px;
        }
        .form-group label {
            display: block;
            margin-bottom: 6px;
            color: #555;
            font-size: 14px;
            font-weight: 500;
        }
        .form-group input, .form-group select {
            width: 100%;
            padding: 10px 12px;
            border: 1px solid #ddd;
            border-radius: 6px;
            font-size: 14px;
        }
        .form-group input:focus, .form-group select:focus {
            outline: none;
            border-color: #667eea;
            box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2);
        }
        .btn-group {
            display: flex;
            gap: 12px;
            margin-top: 20px;
            flex-wrap: wrap;
        }
        .btn {
            padding: 12px 24px;
            border: none;
            border-radius: 6px;
            font-size: 14px;
            font-weight: 500;
            cursor: pointer;
            transition: all 0.3s;
        }
        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }
        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
        }
        .btn-success {
            background: #52c41a;
            color: white;
        }
        .btn-success:hover { background: #45a617; }
        .test-panel {
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        .test-panel h3 {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 16px 20px;
            font-size: 16px;
        }
        .test-content {
            padding: 20px;
            min-height: 500px;
            background: #f9f9f9;
        }
        .test-content iframe {
            width: 100%;
            height: 550px;
            border: 1px solid #eee;
            border-radius: 8px;
            background: white;
        }
        .tip {
            background: #e6f7ff;
            border: 1px solid #91d5ff;
            border-radius: 6px;
            padding: 12px 16px;
            margin-bottom: 16px;
            color: #0050b3;
            font-size: 14px;
        }
        .tip::before { content: "💡 "; }
        @media (max-width: 768px) {
            .form-group { min-width: 100%; }
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🤖 AI聊天接入测试</h1>
        
        <div class="config-panel">
            <h2>⚙️ 配置参数</h2>
            <div class="form-row">
                <div class="form-group">
                    <label>服务器地址</label>
                    <input type="text" id="serverUrl" value="${currentDomain.value}" placeholder="http://localhost:3100">
                </div>
                <div class="form-group">
                    <label>应用ID (appId)</label>
                    <input type="text" id="appId" value="${appId.value}" placeholder="请输入应用ID">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>外部用户ID</label>
                    <input type="text" id="externalUserId" value="test_user_001" placeholder="用户唯一标识">
                </div>
                <div class="form-group">
                    <label>外部用户名</label>
                    <input type="text" id="externalUserName" value="测试用户" placeholder="显示的用户名">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>会话模式</label>
                    <select id="sessionMode">
                        <option value="persist">持久会话 (persist)</option>
                        <option value="temp">临时会话 (temp)</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>密钥 (可选)</label>
                    <input type="text" id="secretKey" value="${formState.secretKey || ''}" placeholder="留空则无签名验证">
                </div>
            </div>
            <div class="btn-group">
                <button class="btn btn-primary" onclick="loadIframe()">🚀 iframe方式测试</button>
                <button class="btn btn-success" onclick="loadScript()">📜 Script方式测试</button>
            </div>
        </div>
        
        <div class="test-panel">
            <h3>📺 预览区域</h3>
            <div class="test-content" id="previewArea">
                <div class="tip">请点击上方按钮加载聊天组件进行测试</div>
                <div id="chatContainer"></div>
            </div>
        </div>
    </div>
    
    <script>
        // MD5函数
        function md5(string){function md5cycle(x,k){var a=x[0],b=x[1],c=x[2],d=x[3];a=ff(a,b,c,d,k[0],7,-680876936);d=ff(d,a,b,c,k[1],12,-389564586);c=ff(c,d,a,b,k[2],17,606105819);b=ff(b,c,d,a,k[3],22,-1044525330);a=ff(a,b,c,d,k[4],7,-176418897);d=ff(d,a,b,c,k[5],12,1200080426);c=ff(c,d,a,b,k[6],17,-1473231341);b=ff(b,c,d,a,k[7],22,-45705983);a=ff(a,b,c,d,k[8],7,1770035416);d=ff(d,a,b,c,k[9],12,-1958414417);c=ff(c,d,a,b,k[10],17,-42063);b=ff(b,c,d,a,k[11],22,-1990404162);a=ff(a,b,c,d,k[12],7,1804603682);d=ff(d,a,b,c,k[13],12,-40341101);c=ff(c,d,a,b,k[14],17,-1502002290);b=ff(b,c,d,a,k[15],22,1236535329);a=gg(a,b,c,d,k[1],5,-165796510);d=gg(d,a,b,c,k[6],9,-1069501632);c=gg(c,d,a,b,k[11],14,643717713);b=gg(b,c,d,a,k[0],20,-373897302);a=gg(a,b,c,d,k[5],5,-701558691);d=gg(d,a,b,c,k[10],9,38016083);c=gg(c,d,a,b,k[15],14,-660478335);b=gg(b,c,d,a,k[4],20,-405537848);a=gg(a,b,c,d,k[9],5,568446438);d=gg(d,a,b,c,k[14],9,-1019803690);c=gg(c,d,a,b,k[3],14,-187363961);b=gg(b,c,d,a,k[8],20,1163531501);a=gg(a,b,c,d,k[13],5,-1444681467);d=gg(d,a,b,c,k[2],9,-51403784);c=gg(c,d,a,b,k[7],14,1735328473);b=gg(b,c,d,a,k[12],20,-1926607734);a=hh(a,b,c,d,k[5],4,-378558);d=hh(d,a,b,c,k[8],11,-2022574463);c=hh(c,d,a,b,k[11],16,1839030562);b=hh(b,c,d,a,k[14],23,-35309556);a=hh(a,b,c,d,k[1],4,-1530992060);d=hh(d,a,b,c,k[4],11,1272893353);c=hh(c,d,a,b,k[7],16,-155497632);b=hh(b,c,d,a,k[10],23,-1094730640);a=hh(a,b,c,d,k[13],4,681279174);d=hh(d,a,b,c,k[0],11,-358537222);c=hh(c,d,a,b,k[3],16,-722521979);b=hh(b,c,d,a,k[6],23,76029189);a=hh(a,b,c,d,k[9],4,-640364487);d=hh(d,a,b,c,k[12],11,-421815835);c=hh(c,d,a,b,k[15],16,530742520);b=hh(b,c,d,a,k[2],23,-995338651);a=ii(a,b,c,d,k[0],6,-198630844);d=ii(d,a,b,c,k[7],10,1126891415);c=ii(c,d,a,b,k[14],15,-1416354905);b=ii(b,c,d,a,k[5],21,-57434055);a=ii(a,b,c,d,k[12],6,1700485571);d=ii(d,a,b,c,k[3],10,-1894986606);c=ii(c,d,a,b,k[10],15,-1051523);b=ii(b,c,d,a,k[1],21,-2054922799);a=ii(a,b,c,d,k[8],6,1873313359);d=ii(d,a,b,c,k[15],10,-30611744);c=ii(c,d,a,b,k[6],15,-1560198380);b=ii(b,c,d,a,k[13],21,1309151649);a=ii(a,b,c,d,k[4],6,-145523070);d=ii(d,a,b,c,k[11],10,-1120210379);c=ii(c,d,a,b,k[2],15,718787259);b=ii(b,c,d,a,k[9],21,-343485551);x[0]=add32(a,x[0]);x[1]=add32(b,x[1]);x[2]=add32(c,x[2]);x[3]=add32(d,x[3])}function cmn(q,a,b,x,s,t){a=add32(add32(a,q),add32(x,t));return add32((a<<s)|(a>>>(32-s)),b)}function ff(a,b,c,d,x,s,t){return cmn((b&c)|((~b)&d),a,b,x,s,t)}function gg(a,b,c,d,x,s,t){return cmn((b&d)|(c&(~d)),a,b,x,s,t)}function hh(a,b,c,d,x,s,t){return cmn(b^c^d,a,b,x,s,t)}function ii(a,b,c,d,x,s,t){return cmn(c^(b|(~d)),a,b,x,s,t)}function md51(s){var n=s.length,state=[1732584193,-271733879,-1732584194,271733878],i;for(i=64;i<=s.length;i+=64){md5cycle(state,md5blk(s.substring(i-64,i)))}s=s.substring(i-64);var tail=[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0];for(i=0;i<s.length;i++)tail[i>>2]|=s.charCodeAt(i)<<((i%4)<<3);tail[i>>2]|=0x80<<((i%4)<<3);if(i>55){md5cycle(state,tail);for(i=0;i<16;i++)tail[i]=0}tail[14]=n*8;md5cycle(state,tail);return state}function md5blk(s){var md5blks=[],i;for(i=0;i<64;i+=4){md5blks[i>>2]=s.charCodeAt(i)+(s.charCodeAt(i+1)<<8)+(s.charCodeAt(i+2)<<16)+(s.charCodeAt(i+3)<<24)}return md5blks}var hex_chr='0123456789abcdef'.split('');function rhex(n){var s='',j=0;for(;j<4;j++)s+=hex_chr[(n>>(j*8+4))&0x0F]+hex_chr[(n>>(j*8))&0x0F];return s}function hex(x){for(var i=0;i<x.length;i++)x[i]=rhex(x[i]);return x.join('')}function add32(a,b){return(a+b)&0xFFFFFFFF}return hex(md51(string))}
        
        function getConfig() {
            return {
                serverUrl: document.getElementById('serverUrl').value.replace(/\\/$/, ''),
                appId: document.getElementById('appId').value,
                externalUserId: document.getElementById('externalUserId').value,
                externalUserName: document.getElementById('externalUserName').value,
                sessionMode: document.getElementById('sessionMode').value,
                secretKey: document.getElementById('secretKey').value
            };
        }
        
        function buildChatUrl(config) {
            const params = new URLSearchParams();
            params.append('externalUserId', config.externalUserId);
            params.append('externalUserName', config.externalUserName);
            params.append('sessionMode', config.sessionMode);
            if (config.secretKey) {
                const timestamp = Date.now();
                const token = md5(config.appId + config.secretKey + timestamp);
                params.append('token', token);
                params.append('timestamp', timestamp);
            }
            return config.serverUrl + '/ai/app/chat/' + config.appId + '?' + params.toString();
        }
        
        function loadIframe() {
            const config = getConfig();
            if (!config.appId) { alert('请先输入应用ID'); return; }
            const url = buildChatUrl(config);
            document.getElementById('chatContainer').innerHTML = '<iframe src="' + url + '" style="width: 100%; height: 550px; border: 1px solid #eee; border-radius: 8px;"></iframe>';
        }
        
        function loadScript() {
            const config = getConfig();
            if (!config.appId) { alert('请先输入应用ID'); return; }
            document.getElementById('chatContainer').innerHTML = '';
            
            // 直接调用内嵌的 createAiChat 函数
            const options = {
                appId: config.appId,
                externalUserId: config.externalUserId,
                externalUserName: config.externalUserName,
                sessionMode: config.sessionMode,
                iconPosition: 'bottom-right',
                showWelcomePage: true,
                serverUrl: config.serverUrl
            };
            if (config.secretKey) {
                const timestamp = Date.now();
                options.token = md5(config.appId + config.secretKey + timestamp);
                options.timestamp = timestamp;
            }
            createAiChat(options);
        }
        
        // ========== AI聊天组件 chat.js 内嵌代码 ==========
        (function () {
          let widgetInstance = null;
          const defaultConfig = {
            iconPosition: 'bottom-right',
            iconSize: '45px',
            iconColor: '#155eef',
            appId: '',
            chatWidth: '800px',
            chatHeight: '700px',
            externalUserId: '',
            externalUserName: '',
            sessionMode: 'temp',
            token: '',
            timestamp: '',
            welcomeTitle: '',
            welcomeDesc: '',
            showWelcomePage: false,
            primaryColor: '',
            serverUrl: ''
          };
          
          function createAiChat(config) {
            if (widgetInstance) { widgetInstance.remove(); widgetInstance = null; }
            const finalConfig = { ...defaultConfig, ...config };
            if (!finalConfig.appId) { console.error('appId为空！'); return; }
            
            let body = document.body;
            body.style.margin = "0";
            
            const container = document.createElement('div');
            container.style.cssText = 'position: fixed; z-index: 998; ' + getPositionStyles(finalConfig.iconPosition) + ' cursor: pointer;';
            
            const icon = document.createElement('div');
            icon.style.cssText = 'width: ' + finalConfig.iconSize + '; height: ' + finalConfig.iconSize + '; background-color: ' + finalConfig.iconColor + '; border-radius: 50%; box-shadow: #cccccc 0 4px 8px 0; padding: 10px; display: flex; align-items: center; justify-content: center; color: white; box-sizing: border-box;';
            icon.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024"><path fill="currentColor" d="M573 421c-23.1 0-41 17.9-41 40s17.9 40 41 40c21.1 0 39-17.9 39-40s-17.9-40-39-40m-280 0c-23.1 0-41 17.9-41 40s17.9 40 41 40c21.1 0 39-17.9 39-40s-17.9-40-39-40"></path><path fill="currentColor" d="M894 345c-48.1-66-115.3-110.1-189-130v.1c-17.1-19-36.4-36.5-58-52.1c-163.7-119-393.5-82.7-513 81c-96.3 133-92.2 311.9 6 439l.8 132.6c0 3.2.5 6.4 1.5 9.4c5.3 16.9 23.3 26.2 40.1 20.9L309 806c33.5 11.9 68.1 18.7 102.5 20.6l-.5.4c89.1 64.9 205.9 84.4 313 49l127.1 41.4c3.2 1 6.5 1.6 9.9 1.6c17.7 0 32-14.3 32-32V753c88.1-119.6 90.4-284.9 1-408M323 735l-12-5l-99 31l-1-104l-8-9c-84.6-103.2-90.2-251.9-11-361c96.4-132.2 281.2-161.4 413-66c132.2 96.1 161.5 280.6 66 412c-80.1 109.9-223.5 150.5-348 102m505-17l-8 10l1 104l-98-33l-12 5c-56 20.8-115.7 22.5-171 7l-.2-.1C613.7 788.2 680.7 742.2 729 676c76.4-105.3 88.8-237.6 44.4-350.4l.6.4c23 16.5 44.1 37.1 62 62c72.6 99.6 68.5 235.2-8 330"></path><path fill="currentColor" d="M433 421c-23.1 0-41 17.9-41 40s17.9 40 41 40c21.1 0 39-17.9 39-40s-17.9-40-39-40"></path></svg>';
            
            const iframeContainer = document.createElement('div');
            let chatWidth = finalConfig.chatWidth;
            let chatHeight = finalConfig.chatHeight;
            let right = '10px', bottom = '10px';
            if (isMobileDevice()) { chatWidth = '100%'; chatHeight = '100%'; right = '0'; bottom = '0'; }
            iframeContainer.style.cssText = 'position: fixed; right: ' + right + '; bottom: ' + bottom + '; width: ' + chatWidth + ' !important; height: ' + chatHeight + ' !important; background: white; border-radius: 8px; box-shadow: 0 0 20px #cccccc; display: none; z-index: 10000;';
            
            const iframe = document.createElement('iframe');
            iframe.style.cssText = 'width: 100%; height: 100%; border: none; border-radius: 8px;';
            iframe.id = 'ai-app-chat-document';
            iframe.src = buildIframeSrc(finalConfig);
            
            const closeBtn = document.createElement('div');
            closeBtn.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 1024 1024"><path fill="currentColor" fill-rule="evenodd" d="M799.855 166.312c.023.007.043.018.084.059l57.69 57.69c.041.041.052.06.059.084a.1.1 0 0 1 0 .069c-.007.023-.018.042-.059.083L569.926 512l287.703 287.703c.041.04.052.06.059.083a.12.12 0 0 1 0 .07c-.007.022-.018.042-.059.083l-57.69 57.69c-.041.041-.06.052-.084.059a.1.1 0 0 1-.069 0c-.023-.007-.042-.018-.083-.059L512 569.926L224.297 857.629c-.04.041-.06.052-.083.059a.12.12 0 0 1-.07 0c-.022-.007-.042-.018-.083-.059l-57.69-57.69c-.041-.041-.052-.06-.059-.084a.1.1 0 0 1 0-.069c.007-.023.018-.042.059-.083L454.073 512L166.371 224.297c-.041-.04-.052-.06-.059-.083a.12.12 0 0 1 0-.07c.007-.022.018-.042.059-.083l57.69-57.69c.041-.041.06-.052.084-.059a.1.1 0 0 1 .069 0c.023.007.042.018.083.059L512 454.073l287.703-287.702c.04-.041.06-.052.083-.059a.12.12 0 0 1 .07 0Z"></path></svg>';
            closeBtn.style.cssText = 'position: absolute; margin-top: -9px; right: -6px; cursor: pointer; background: white; width: 25px; height: 25px; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 5px #cccccc;';
            
            iframeContainer.appendChild(closeBtn);
            iframeContainer.appendChild(iframe);
            document.body.appendChild(iframeContainer);
            container.appendChild(icon);
            document.body.appendChild(container);
            
            icon.addEventListener('click', function() { iframeContainer.style.display = 'block'; });
            closeBtn.addEventListener('click', function() { iframeContainer.style.display = 'none'; });
            
            widgetInstance = {
              remove: function() { container.remove(); iframeContainer.remove(); },
              show: function() { iframeContainer.style.display = 'block'; },
              hide: function() { iframeContainer.style.display = 'none'; }
            };
            return widgetInstance;
          }
          
          function buildIframeSrc(config) {
            const baseUrl = config.serverUrl || window.location.origin;
            const url = new URL(baseUrl + '/ai/app/chat/' + config.appId);
            url.searchParams.append('source', 'chatJs');
            if (config.externalUserId) url.searchParams.append('externalUserId', config.externalUserId);
            if (config.externalUserName) url.searchParams.append('externalUserName', encodeURIComponent(config.externalUserName));
            if (config.sessionMode) url.searchParams.append('sessionMode', config.sessionMode);
            if (config.token) url.searchParams.append('token', config.token);
            if (config.timestamp) url.searchParams.append('timestamp', config.timestamp);
            if (config.showWelcomePage) url.searchParams.append('showWelcomePage', 'true');
            return url.toString();
          }
          
          function getPositionStyles(position) {
            const positions = { 'top-left': 'top: 20px; left: 20px;', 'top-right': 'top: 20px; right: 20px;', 'bottom-left': 'bottom: 20px; left: 20px;', 'bottom-right': 'bottom: 20px; right: 20px;' };
            return positions[position] || positions['bottom-right'];
          }
          
          function isMobileDevice() { return /Mobi|Android|iPhone|iPad|iPod/i.test(navigator.userAgent); }
          
          window.createAiChat = createAiChat;
        })();
    <\/script>
</body>
</html>`;
}
</script>

<style scoped lang="less">
.code-block {
  background: #f5f5f5;
  border-radius: 6px;
  overflow: hidden;
  
  .code-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    background: #e8e8e8;
    font-size: 12px;
    color: #666;
  }
  
  pre {
    margin: 0;
    padding: 12px;
    font-size: 12px;
    line-height: 1.6;
    overflow-x: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
