/**
 * 访客端 auth stub
 *
 * 主项目 utils/auth 在登录态从 sessionStorage / localStorage 取 X-Access-Token，
 * 但访客端没有「登录用户」，仅有 X-Visitor-Session / X-Visitor-Token / X-Device-Id。
 * 这里提供与主项目同名的 getToken / getTenantId，但永远返回空串，
 * 让 cseAuthContext.getCseAuthToken() 自动 fallback 到 visitorCredential / deviceCredential。
 *
 * 不要在访客端调用 setToken 等写入操作。
 */
export function getToken(): string {
  return '';
}

export function getRefreshToken(): string {
  return '';
}

export function getTenantId(): string {
  return '0';
}

export function setToken(_token: string): void {
  // no-op：访客端不接受登录 token
}

export function clearAuthCache(_immediate?: boolean): void {
  // no-op
}
