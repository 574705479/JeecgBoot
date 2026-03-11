<template>
  <div class="cs-login-page" :style="loginBgStyle">
    <!-- 登录卡片 -->
    <div v-show="type === 'login'" class="cs-login-card">
      <!-- Logo + 品牌 -->
      <div class="cs-login-header">
        <div class="cs-avatar-ring">
          <img :src="logoDisplayUrl || logoUrl" :alt="appTitle" class="cs-logo-img" />
        </div>
        <h1 class="cs-app-title">{{ appTitle }}</h1>
        <p class="cs-app-subtitle" v-if="appSubtitle">{{ appSubtitle }}</p>
      </div>

      <!-- 表单区 -->
      <a-form ref="loginRef" :model="formData" v-if="activeIndex === 'accountLogin'" @keyup.enter.native="loginHandleClick" class="cs-login-form">
        <div class="cs-input-group">
          <div class="cs-input-wrapper">
            <span class="cs-input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
            </span>
            <a-input class="fix-auto-fill cs-input" :placeholder="t('sys.login.userName')" v-model:value="formData.username" :bordered="false" />
          </div>
          <div class="cs-input-wrapper">
            <span class="cs-input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/></svg>
            </span>
            <a-input class="fix-auto-fill cs-input" type="password" :placeholder="t('sys.login.password')" v-model:value="formData.password" :bordered="false" />
          </div>
          <div class="cs-input-wrapper cs-captcha-row">
            <span class="cs-input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
            </span>
            <a-input class="fix-auto-fill cs-input cs-captcha-input" type="text" :placeholder="t('sys.login.inputCode')" v-model:value="formData.inputCode" :bordered="false" />
            <div class="cs-captcha-img" @click="handleChangeCheckCode">
              <img v-if="randCodeData.requestCodeSuccess" :src="randCodeData.randCodeImage" />
              <img v-else :src="codeImg" />
            </div>
          </div>
          <div class="cs-input-wrapper" v-if="showDepart">
            <span class="cs-input-icon">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/><polyline points="9 22 9 12 15 12 15 22"/></svg>
            </span>
            <div class="cs-dept-select">
              <a-select allow-clear style="width: 100%" :bordered="false" v-model:value="formData.loginOrgCode" :placeholder="t('sys.login.loginOrgCode')">
                <template v-for="depart in departList" :key="depart.orgCode">
                  <a-select-option :value="depart.orgCode">{{ getShortDeptName(depart.label) }}</a-select-option>
                </template>
              </a-select>
            </div>
          </div>
        </div>

        <!-- 选项行 -->
        <div class="cs-options-row">
          <a-checkbox v-model:checked="rememberMe" class="cs-checkbox">{{ t('sys.login.rememberMe') }}</a-checkbox>
        </div>

        <!-- 在线状态选择 -->
        <div class="cs-status-row">
          <div class="cs-status-toggle" :class="{ 'is-online': csOnlineLogin }" @click="toggleCsLoginStatus">
            <span class="cs-status-dot"></span>
            <span class="cs-status-label">{{ csOnlineLogin ? '在线登录' : '隐身登录' }}</span>
          </div>
          <span class="cs-status-hint">{{ csOnlineLogin ? '登录后自动在线接待访客' : '登录后不接收新会话分配' }}</span>
        </div>

        <!-- 登录按钮 -->
        <a-button :loading="loginLoading" class="cs-login-btn" type="primary" block size="large" @click="loginHandleClick">
          {{ t('sys.login.loginButton') }}
        </a-button>
      </a-form>

      <!-- 底部信息 -->
      <div class="cs-login-footer">
        <span class="cs-footer-text">Powered by {{ appTitle }}</span>
      </div>
    </div>

    <div v-if="type === 'forgot' && !hideExtraLogin" :class="`${prefixCls}-form`">
      <MiniForgotpad ref="forgotRef" @go-back="goBack" @success="handleSuccess" />
    </div>
    <div v-if="type === 'register' && !hideExtraLogin" :class="`${prefixCls}-form`">
      <MiniRegister ref="registerRef" @go-back="goBack" @success="handleSuccess" />
    </div>
    <div v-if="type === 'codeLogin' && !hideExtraLogin" :class="`${prefixCls}-form`">
      <MiniCodelogin ref="codeRef" @go-back="goBack" @success="handleSuccess" />
    </div>
    <ThirdModal ref="thirdModalRef"></ThirdModal>
    <CaptchaModal @register="captchaRegisterModal" @ok="getLoginCode" />
  </div>
</template>
<script lang="ts" setup name="login-mini">
  import { getCaptcha, getCodeInfo } from '/@/api/sys/user';
  import { computed, onMounted, reactive, ref, toRaw, unref, watch } from 'vue';
  import codeImg from '/@/assets/images/checkcode.png';
  import { Rule } from '/@/components/Form';
  import { useUserStore } from '/@/store/modules/user';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useI18n } from '/@/hooks/web/useI18n';
  import { SmsEnum } from '/@/views/sys/login/useLogin';
  import ThirdModal from '/@/views/sys/login/ThirdModal.vue';
  import MiniForgotpad from './MiniForgotpad.vue';
  import MiniRegister from './MiniRegister.vue';
  import MiniCodelogin from './MiniCodelogin.vue';
  import { getBrandSetting } from '/@/settings/brandSetting';
  import { resolveBrandUrl } from '/@/utils/brand';
  import { AppLocalePicker, AppDarkModeToggle } from '/@/components/Application';
  import { useLocaleStore } from '/@/store/modules/locale';
  import { createLocalStorage } from '/@/utils/cache';
  import { useDesign } from "/@/hooks/web/useDesign";
  import { useAppInject } from "/@/hooks/web/useAppInject";
  import { GithubFilled, WechatFilled, DingtalkCircleFilled, createFromIconfontCN } from '@ant-design/icons-vue';
  import CaptchaModal from '@/components/jeecg/captcha/CaptchaModal.vue';
  import { useModal } from "@/components/Modal";
  import { ExceptionEnum } from "@/enums/exceptionEnum";
  import { encryptAESCBC } from '/@/utils/cipher';
  import { defHttp } from "@/utils/http/axios";

  const IconFont = createFromIconfontCN({
    scriptUrl: '//at.alicdn.com/t/font_2316098_umqusozousr.js',
  });
  const { prefixCls } = useDesign('mini-login');
  const { appTitle, appSubtitle, logoUrl, loginBgUrl } = getBrandSetting();
  const hideExtraLogin = true;
  const logoDisplayUrl = computed(() => resolveBrandUrl(logoUrl));
  const loginBgStyle = computed(() => {
    if (!loginBgUrl) return {};
    const bgUrl = resolveBrandUrl(loginBgUrl);
    return {
      backgroundImage: `url(${bgUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    };
  });
  const { notification, createMessage } = useMessage();
  const userStore = useUserStore();
  const { t } = useI18n();
  const $ls = createLocalStorage();
  const localeStore = useLocaleStore();
  const showLocale = localeStore.getShowPicker;
  const randCodeData = reactive<any>({
    randCodeImage: '',
    requestCodeSuccess: false,
    checkKey: null,
  });
  // 记住用户名
  const rememberMe = ref<boolean>(false);
  const REMEMBER_USERNAME_KEY = 'LOGIN_REMEMBER_USERNAME';
  // 客服在线状态登录
  const CS_ONLINE_LOGIN_KEY = 'CS_ONLINE_LOGIN';
  const csOnlineLogin = ref<boolean>(localStorage.getItem(CS_ONLINE_LOGIN_KEY) !== 'false');
  function onCsOnlineLoginChange(e: any) {
    const checked = e?.target?.checked ?? e;
    localStorage.setItem(CS_ONLINE_LOGIN_KEY, String(checked));
  }
  function toggleCsLoginStatus() {
    csOnlineLogin.value = !csOnlineLogin.value;
    localStorage.setItem(CS_ONLINE_LOGIN_KEY, String(csOnlineLogin.value));
  }
  //手机号登录还是账号登录
  const activeIndex = ref<string>('accountLogin');
  const type = ref<string>('login');
  //账号登录表单字段
  const formData = reactive<any>({
    inputCode: '',
    username: '',
    password: '',
    loginOrgCode: '',
  });
  //手机登录表单字段
  const phoneFormData = reactive<any>({
    mobile: '',
    smscode: '',
    loginOrgCode: '',
  });
  const loginRef = ref();
  //第三方登录弹窗
  const thirdModalRef = ref();
  //扫码登录
  const codeRef = ref();
  //是否显示获取验证码
  const showInterval = ref<boolean>(true);
  //60s
  const timeRuning = ref<number>(60);
  //定时器
  const timer = ref<any>(null);
  //忘记密码
  const forgotRef = ref();
  //注册
  const registerRef = ref();
  const loginLoading = ref<boolean>(false);
  const { getIsMobile } = useAppInject();
  const [captchaRegisterModal, { openModal: openCaptchaModal }] = useModal();
  defineProps({
    sessionTimeout: {
      type: Boolean,
    },
  });
 //**********************查询部门逻辑begin**********************************************
  //用户部门
  const departList = ref([]);
  //部门显示
  const showDepart = computed(()=>{
    return departList.value.length > 1
  })
  //获取部门缩写
  const getShortDeptName = computed(()=>{
    return (deptName) => {
      if (!deptName) return '';
      if (deptName.length > 18) {
        return '...' + deptName.substring(deptName.length-18, deptName.length) ;
      }
      return deptName;
    };
  })
  //监听验证码和输入框的修改
  watch(
      () => [formData.inputCode, phoneFormData.smscode],
      () => {
        if ((formData.inputCode && formData.inputCode.length == 4)
            || (phoneFormData.smscode && phoneFormData.smscode.length == 6)) {
            checkAccount()
        }
      },
  );
  /**
   * 监听账号变化，清除部门信息
   */
  watch(
      () => [formData.username,phoneFormData.mobile,activeIndex.value],
      () => {
        formData.loginOrgCode = null;
        phoneFormData.loginOrgCode = null;
        departList.value = [];
        if ((formData.inputCode && formData.inputCode.length == 4)
            || (phoneFormData.smscode && phoneFormData.smscode.length == 6)) {
          checkAccount()
        }
      }
  );

  //初始化数据
  let deptTimer;
  function checkAccount() {
    deptTimer && clearTimeout(deptTimer);
    deptTimer = setTimeout(async () => {
      let loginType = activeIndex.value === 'accountLogin' ? 'account' : 'phone';
      // 验证条件提取
      const isValidAccount = loginType === 'account' && formData.username && formData.password;
      const isValidPhone = loginType == 'phone' && phoneFormData.mobile && phoneFormData.smscode;
      let finalFormData = loginType == 'phone' ? {...phoneFormData} : {...formData};
      if (!isValidAccount && !isValidPhone) {
        return;
      }
      //查询部门信息前，优先进行账户校验
      if (departList.value && departList.value.length == 0) {
        let params = {...finalFormData, loginType: activeIndex.value === 'accountLogin' ? 'account' : 'phone'};
        if (loginType == 'account') {
          params['password'] = encryptAESCBC(formData.password);
          params['checkKey'] = randCodeData.checkKey;
        }
        const res = await defHttp.post({
          url: '/sys/loginGetUserDeparts',
          params: {...params}
        }, {isTransformResponse: false});
        if (res.success && res.result) {
          let {departs,currentOrgCode} = res.result;
          // 判断当前部门是否在所属的部门列表中
          if (departs && departs.length > 0) {
            // 代码逻辑说明: JHHB-790 用户部门变更，会出现这个情况（因为之前设置的这里只切换部门，过滤了公司和岗位信息）
            const hasCurrentDepart = departs.some(item => item.orgCode == currentOrgCode);
            formData.loginOrgCode = hasCurrentDepart?currentOrgCode:null;
            phoneFormData.loginOrgCode = hasCurrentDepart?currentOrgCode:null;
            departList.value = departs.map((item) => {
              return {
                label: item.departName,
                value: item.orgCode,
                orgCode: item.orgCode,
                departName: item.departName,
              };
            });
          }
        } else {
          //createMessage.warn(res.message);
        }
      }
    },500)
  }
 //**********************查询部门逻辑end*************************************************
  /**
   * 获取验证码
   */
  function handleChangeCheckCode() {
    formData.inputCode = '';
    // 代码逻辑说明: [QQYUN-10775]验证码可以复用 #7674------------
    randCodeData.checkKey = new Date().getTime() + Math.random().toString(36).slice(-4); // 1629428467008;
    getCodeInfo(randCodeData.checkKey).then((res) => {
      randCodeData.randCodeImage = res;
      randCodeData.requestCodeSuccess = true;
    });
  }

  /**
   * 切换登录方式
   */
  function loginClick(type) {
    activeIndex.value = type;
  }

  /**
   * 账号或者手机登录
   */
  async function loginHandleClick() {
    if (unref(activeIndex) === 'accountLogin') {
      accountLogin();
    } else {
      //手机号登录
      phoneLogin();
    }
  }

  async function accountLogin() {
    if (!formData.username) {
      createMessage.warn(t('sys.login.accountPlaceholder'));
      return;
    }
    if (!formData.password) {
      createMessage.warn(t('sys.login.passwordPlaceholder'));
      return;
    }
    try {
      loginLoading.value = true;

      // 密码使用AES加密传输
      const encryptedPassword = encryptAESCBC(formData.password);
      const { userInfo } = await userStore.login(
        toRaw({
          password: encryptedPassword,
          username: formData.username,
          loginOrgCode: formData.loginOrgCode,
          captcha: formData.inputCode,
          checkKey: randCodeData.checkKey,
          mode: 'none', //不要默认的错误提示
        })
      );
      if (userInfo) {
        notification.success({
          message: t('sys.login.loginSuccessTitle'),
          description: `${t('sys.login.loginSuccessDesc')}: ${userInfo.realname}`,
          duration: 3,
        });
        // 登录成功后处理记住用户名
        if (rememberMe.value && formData.username) {
          $ls.set(REMEMBER_USERNAME_KEY, formData.username)
        } else {
          $ls.remove(REMEMBER_USERNAME_KEY)
        }
      }
    } catch (error) {
      notification.error({
        message: t('sys.api.errorTip'),
        description: error.message || t('sys.login.networkExceptionMsg'),
        duration: 3,
      });
      handleChangeCheckCode();
    } finally {
      loginLoading.value = false;
    }
  }

  /**
   * 手机号登录
   */
  async function phoneLogin() {
    if (!phoneFormData.mobile) {
      createMessage.warn(t('sys.login.mobilePlaceholder'));
      return;
    }
    if (!phoneFormData.smscode) {
      createMessage.warn(t('sys.login.smsPlaceholder'));
      return;
    }
    try {
      loginLoading.value = true;
      const { userInfo }: any = await userStore.phoneLogin({
        mobile: phoneFormData.mobile,
        captcha: phoneFormData.smscode,
        loginOrgCode: phoneFormData.loginOrgCode,
        mode: 'none', //不要默认的错误提示
      });
      if (userInfo) {
        notification.success({
          message: t('sys.login.loginSuccessTitle'),
          description: `${t('sys.login.loginSuccessDesc')}: ${userInfo.realname}`,
          duration: 3,
        });
      }
    } catch (error) {
      notification.error({
        message: t('sys.api.errorTip'),
        description: error.message || t('sys.login.networkExceptionMsg'),
        duration: 3,
      });
    } finally {
      loginLoading.value = false;
    }
  }

  /**
   * 获取手机验证码
   */
  async function getLoginCode() {
    if (!phoneFormData.mobile) {
      createMessage.warn(t('sys.login.mobilePlaceholder'));
      return;
    }
    // 代码逻辑说明: 【issues/8567】严重：修改密码存在水平越权问题：登录应该用登录模板不应该用忘记密码的模板---
    const result = await getCaptcha({ mobile: phoneFormData.mobile, smsmode: SmsEnum.LOGIN }).catch((res) =>{
      if(res.code === ExceptionEnum.PHONE_SMS_FAIL_CODE){
        openCaptchaModal(true, {});
      }
    });
    if (result) {
      const TIME_COUNT = 60;
      if (!unref(timer)) {
        timeRuning.value = TIME_COUNT;
        showInterval.value = false;
        timer.value = setInterval(() => {
          if (unref(timeRuning) > 0 && unref(timeRuning) <= TIME_COUNT) {
            timeRuning.value = timeRuning.value - 1;
          } else {
            showInterval.value = true;
            clearInterval(unref(timer));
            timer.value = null;
          }
        }, 1000);
      }
    }
  }

  /**
   * 第三方登录
   * @param type
   */
  function onThirdLogin(type) {
    thirdModalRef.value.onThirdLogin(type);
  }

  /**
   * 忘记密码
   */
  function forgetHandelClick() {
    type.value = 'forgot';
    setTimeout(() => {
      forgotRef.value.initForm();
    }, 300);
  }

  /**
   * 返回登录页面
   */
  function goBack() {
    activeIndex.value = 'accountLogin';
    type.value = 'login';
  }

  /**
   * 忘记密码/注册账号回调事件
   * @param value
   */
  function handleSuccess(value) {
    Object.assign(formData, value);
    Object.assign(phoneFormData, { mobile: "", smscode: "" });
    type.value = 'login';
    activeIndex.value = 'accountLogin';
    handleChangeCheckCode();
  }

  /**
   * 注册
   */
  function registerHandleClick() {
    type.value = 'register';
    setTimeout(() => {
      registerRef.value.initForm();
    }, 300);
  }

  /**
   * 注册
   */
  function codeHandleClick() {
    type.value = 'codeLogin';
    setTimeout(() => {
      codeRef.value.initFrom();
    }, 300);
  }

  onMounted(() => {
    if (hideExtraLogin) {
      activeIndex.value = 'accountLogin';
      type.value = 'login';
    }
    //加载验证码
    handleChangeCheckCode();
    // 恢复已记住的用户名
    const saved = $ls.get(REMEMBER_USERNAME_KEY);
    if (saved) {
      formData.username = saved;
      rememberMe.value = true;
    }
  });
</script>

<style lang="less" scoped>
/* ============ 聊天软件风格登录页 ============ */

.cs-login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 60%);
    animation: cs-bg-rotate 20s linear infinite;
  }
}

@keyframes cs-bg-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.cs-login-card {
  position: relative;
  z-index: 2;
  width: 400px;
  max-width: 92vw;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px 36px 30px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(255,255,255,0.1);
  animation: cs-card-in 0.5s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes cs-card-in {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ---- Header 区域 ---- */
.cs-login-header {
  text-align: center;
  margin-bottom: 28px;
}

.cs-avatar-ring {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  padding: 3px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  animation: cs-ring-pulse 3s ease-in-out infinite;
}

@keyframes cs-ring-pulse {
  0%, 100% { box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4); }
  50% { box-shadow: 0 4px 30px rgba(102, 126, 234, 0.6); }
}

.cs-logo-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  background: #fff;
  display: block;
}

.cs-app-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 4px;
  letter-spacing: 0.5px;
}

.cs-app-subtitle {
  font-size: 13px;
  color: #888;
  margin: 0;
}

/* ---- 表单区 ---- */
.cs-login-form {
  :deep(.ant-form-item) {
    margin-bottom: 0;
  }
}

.cs-input-group {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.cs-input-wrapper {
  display: flex;
  align-items: center;
  background: #f4f6fb;
  border-radius: 12px;
  padding: 0 14px;
  height: 48px;
  border: 2px solid transparent;
  transition: all 0.25s ease;

  &:hover {
    background: #eef1f8;
  }

  &:focus-within {
    background: #fff;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
  }
}

.cs-input-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aab0c6;
  margin-right: 10px;
  transition: color 0.25s;

  .cs-input-wrapper:focus-within & {
    color: #667eea;
  }
}

.cs-input {
  flex: 1;
  font-size: 14px;
  height: 44px;
  background: transparent !important;

  :deep(input) {
    background: transparent !important;
    font-size: 14px;
    color: #333;
  }
}

/* 验证码行 */
.cs-captcha-row {
  position: relative;
}

.cs-captcha-input {
  flex: 1;
  margin-right: 100px;
}

.cs-captcha-img {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 95px;
  height: 34px;
  cursor: pointer;
  border-radius: 6px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

/* 部门选择 */
.cs-dept-select {
  flex: 1;

  :deep(.ant-select-selection-placeholder) {
    font-size: 14px;
    color: #aab0c6;
  }
}

/* ---- 选项行 ---- */
.cs-options-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 14px;
  padding: 0 2px;
}

.cs-checkbox {
  font-size: 13px;
  color: #666;

  :deep(.ant-checkbox-inner) {
    border-radius: 4px;
  }

  :deep(.ant-checkbox-checked .ant-checkbox-inner) {
    background-color: #667eea;
    border-color: #667eea;
  }
}

/* ---- 在线状态行 ---- */
.cs-status-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 14px;
  background: #f8f9fd;
  border-radius: 10px;
}

.cs-status-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
  padding: 4px 12px;
  border-radius: 20px;
  background: #e8e8ee;
  transition: all 0.3s ease;
  flex-shrink: 0;

  &.is-online {
    background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);

    .cs-status-dot {
      background: #fff;
      box-shadow: 0 0 6px rgba(255,255,255,0.8);
    }

    .cs-status-label {
      color: #fff;
      font-weight: 600;
    }
  }
}

.cs-status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #aaa;
  transition: all 0.3s;
}

.cs-status-label {
  font-size: 12px;
  color: #888;
  font-weight: 500;
  transition: all 0.3s;
}

.cs-status-hint {
  font-size: 11px;
  color: #aab0c6;
  line-height: 1.4;
}

/* ---- 登录按钮 ---- */
.cs-login-btn {
  margin-top: 22px;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
  transition: all 0.3s ease;

  &:hover, &:focus {
    transform: translateY(-1px);
    box-shadow: 0 8px 25px rgba(102, 126, 234, 0.5);
    background: linear-gradient(135deg, #5a6fd6 0%, #6a4396 100%);
  }

  &:active {
    transform: translateY(0);
    box-shadow: 0 4px 15px rgba(102, 126, 234, 0.35);
  }
}

/* ---- 底部 ---- */
.cs-login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid #eef0f5;
}

.cs-footer-text {
  font-size: 11px;
  color: #c0c4d0;
  letter-spacing: 0.3px;
}

/* ---- 自适应 ---- */
@media (max-width: 480px) {
  .cs-login-card {
    border-radius: 18px;
    padding: 32px 24px 24px;
  }

  .cs-avatar-ring {
    width: 64px;
    height: 64px;
  }

  .cs-app-title {
    font-size: 18px;
  }

  .cs-input-wrapper {
    height: 44px;
  }

  .cs-login-btn {
    height: 44px;
    font-size: 15px;
  }
}
</style>

<style lang="less">
/* 暗色模式兼容 */
html[data-theme='dark'] {
  .cs-login-page {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
  }

  .cs-login-card {
    background: rgba(30, 36, 54, 0.95);
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4);
  }

  .cs-app-title {
    color: #e0e0e0;
  }

  .cs-input-wrapper {
    background: #252d3f;

    &:hover {
      background: #2a3348;
    }

    &:focus-within {
      background: #1e2438;
      border-color: #667eea;
    }
  }

  .cs-input :deep(input),
  input.fix-auto-fill,
  .fix-auto-fill input {
    color: #e0e0e0 !important;
    -webkit-text-fill-color: #e0e0e0 !important;
  }

  .cs-status-row {
    background: #252d3f;
  }

  .cs-status-toggle:not(.is-online) {
    background: #333c52;

    .cs-status-label {
      color: #aab0c6;
    }
  }

  .cs-checkbox {
    color: #aab0c6;
  }

  .cs-login-footer {
    border-top-color: #2a3348;
  }

  .cs-footer-text {
    color: #555e72;
  }
}
</style>
