declare type Recordable<T = any> = Record<string, T>;
declare type Nullable<T> = T | null;
declare type Fn<T = any, R = T> = (...arg: T[]) => R;

declare interface Window {
  __APP_BRAND__?: Record<string, string>;
  _CONFIG?: Record<string, any>;
}
