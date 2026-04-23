/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<{}, {}, any>;
  export default component;
}

interface ImportMetaEnv {
  readonly VITE_PUBLIC_PATH: string;
  readonly VITE_GLOB_DOMAIN_URL: string;
  readonly VITE_GLOB_API_URL: string;
  readonly VITE_GLOB_API_URL_PREFIX: string;
  readonly VITE_PROXY?: string;
  readonly VITE_CS_STORAGE_KEY: string;
  readonly VITE_CS_STORAGE_IV: string;
  readonly VITE_CS_TRANSPORT_KEY: string;
  readonly VITE_CS_TRANSPORT_IV: string;
  readonly VITE_BUILD_COMPRESS?: string;
  readonly VITE_BUILD_COMPRESS_DELETE_ORIGIN_FILE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
