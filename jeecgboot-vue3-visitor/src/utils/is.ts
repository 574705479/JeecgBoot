/**
 * 极简类型判定（访客端用）—— 主项目 utils/is.ts 含 30+ 工具函数，访客端只用 isArray。
 */
export function isArray(val: any): val is any[] {
  return Array.isArray(val);
}

export function isString(val: any): val is string {
  return typeof val === 'string';
}

export function isFunction(val: any): val is Function {
  return typeof val === 'function';
}

export function isObject(val: any): val is object {
  return val !== null && typeof val === 'object';
}
