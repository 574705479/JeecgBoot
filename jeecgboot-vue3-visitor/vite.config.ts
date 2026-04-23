import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import Components from 'unplugin-vue-components/vite';
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers';
import compression from 'vite-plugin-compression';
import { visualizer } from 'rollup-plugin-visualizer';
import { resolve } from 'node:path';

function pathResolve(dir: string): string {
  return resolve(process.cwd(), dir);
}

function wrapperEnv(env: Record<string, string>) {
  const ret: Record<string, any> = {};
  for (const key in env) {
    let realName: any = env[key].replace(/\\n/g, '\n');
    if (realName === 'true' || realName === 'false') {
      realName = realName === 'true';
    }
    if (key === 'VITE_PROXY' && realName) {
      try {
        realName = JSON.parse(String(realName).replace(/'/g, '"'));
      } catch {
        realName = [];
      }
    }
    ret[key] = realName;
  }
  return ret;
}

function createProxy(list: [string, string][] = []): Record<string, any> {
  const ret: Record<string, any> = {};
  for (const [prefix, target] of list) {
    const isHttps = /^https:\/\//.test(target);
    ret[prefix] = {
      target,
      changeOrigin: true,
      ws: true,
      rewrite: (path: string) => path.replace(new RegExp(`^${prefix}`), ''),
      ...(isHttps ? { secure: false } : {}),
    };
  }
  return ret;
}

// https://vitejs.dev/config/
export default defineConfig(({ mode, command }) => {
  const env = loadEnv(mode, process.cwd());
  const viteEnv = wrapperEnv(env);
  const isBuild = command === 'build';

  return {
    base: viteEnv.VITE_PUBLIC_PATH || '/',
    root: process.cwd(),
    resolve: {
      alias: [
        { find: /^\/@\//, replacement: pathResolve('src') + '/' },
        { find: /^@\//, replacement: pathResolve('src') + '/' },
        { find: /^\/#\//, replacement: pathResolve('types') + '/' },
        { find: /^#\//, replacement: pathResolve('types') + '/' },
      ],
    },
    server: {
      host: true,
      port: 3200,
      open: '/?token=demo',
      proxy: createProxy(viteEnv.VITE_PROXY),
    },
    // preview 仅用于本地验证 prod 包；通过 PREVIEW_BACKEND 注入临时反代到后端，避免污染 .env.production
    preview: {
      host: true,
      port: 3300,
      proxy: process.env.PREVIEW_BACKEND
        ? {
            '/jeecgboot': {
              target: process.env.PREVIEW_BACKEND,
              changeOrigin: true,
              ws: true,
              rewrite: (p: string) => p.replace(/^\/jeecgboot/, ''),
            },
          }
        : undefined,
    },
    css: {
      preprocessorOptions: {
        less: {
          javascriptEnabled: true,
        },
      },
    },
    plugins: [
      vue(),
      Components({
        // 仅按需自动引入 antd 组件 + 样式（运行时按需，不会预先打包整套样式）
        resolvers: [
          AntDesignVueResolver({
            importStyle: 'less', // 与 less.javascriptEnabled 配套，避免全量 reset.css
            resolveIcons: true,
          }),
        ],
        // 不自动收集 src/components 下的本地组件（保持显式 import 语义清晰）
        dirs: [],
        dts: false,
      }),
      isBuild && viteEnv.VITE_BUILD_COMPRESS === 'gzip'
        ? compression({
            ext: '.gz',
            algorithm: 'gzip',
            deleteOriginFile: viteEnv.VITE_BUILD_COMPRESS_DELETE_ORIGIN_FILE === true,
            threshold: 10 * 1024,
          })
        : null,
      isBuild && process.env.REPORT === 'true'
        ? visualizer({ filename: 'stats.html', open: true, gzipSize: true })
        : null,
    ].filter(Boolean) as any,
    build: {
      target: 'es2020',
      outDir: 'dist',
      cssCodeSplit: true,
      sourcemap: false,
      minify: 'esbuild',
      reportCompressedSize: false,
      chunkSizeWarningLimit: 1500,
      rollupOptions: {
        treeshake: true,
        output: {
          manualChunks(id: string) {
            // 按业务领域强制分包：保持首屏只加载必要 chunk
            if (id.includes('node_modules')) {
              if (id.includes('@noble')) return 'crypto-noble';
              if (id.includes('crypto-js') || id.includes('spark-md5')) return 'crypto-vendor';
              if (id.includes('markdown-it') || id.includes('dompurify') || id.includes('@traptitech')) {
                return 'markdown-vendor';
              }
              if (id.includes('@ant-design/icons-vue')) return 'antd-icons';
              if (id.includes('ant-design-vue')) return 'antd-vendor';
              if (id.includes('vue') || id.includes('@vue') || id.includes('pinia')) return 'vue-vendor';
              if (id.includes('axios')) return 'axios-vendor';
              if (id.includes('dayjs')) return 'dayjs-vendor';
            }
            return undefined;
          },
        },
      },
    },
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'axios',
        'crypto-js',
        'spark-md5',
        'dayjs',
        'markdown-it',
        'dompurify',
        '@ant-design/icons-vue',
      ],
    },
  };
});
