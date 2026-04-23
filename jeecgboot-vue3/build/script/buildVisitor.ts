/**
 * buildVisitor.ts
 *
 * 在主项目 build 完成之后调用：
 *   1. 进入兄弟目录 jeecgboot-vue3-visitor 跑 `pnpm build:nocheck`，生成访客端独立子项目产物
 *   2. 清空 jeecgboot-vue3/dist/cs/userChat（避免上一次构建残留）
 *   3. 把 jeecgboot-vue3-visitor/dist 整体拷到 jeecgboot-vue3/dist/cs/userChat
 *
 * 这样部署到 nginx 后：
 *   - 访问 /cs/userChat?key=xxx → nginx 命中真实文件 dist/cs/userChat/index.html → 返回访客端 SPA
 *   - 主项目其他路由不受影响（mainOut.ts 已删除 /cs/userChat 路由，避免 vue-router 抢路径）
 *
 * 触发方式：
 *   - `pnpm build:with-visitor`（package.json 中定义）
 *   - 单独调试：`esno ./build/script/buildVisitor.ts`
 *
 * 失败行为：
 *   - 如果找不到 visitor 项目目录，立即 exit 1，避免上线时悄悄少了一份产物
 *   - 如果 visitor build 失败，原样抛出 child_process 的 exit code
 */
const fs = require('fs-extra');
const path = require('path');
const { execSync } = require('child_process');
const colors = require('picocolors');

const MAIN_ROOT = path.resolve(__dirname, '../..');
const VISITOR_ROOT = path.resolve(MAIN_ROOT, '../jeecgboot-vue3-visitor');
const VISITOR_DIST = path.join(VISITOR_ROOT, 'dist');
const TARGET_DIR = path.join(MAIN_ROOT, 'dist', 'cs', 'userChat');

function log(msg: string, color: 'cyan' | 'green' | 'yellow' | 'red' = 'cyan') {
  console.log(`${colors[color](`[buildVisitor] ${msg}`)}`);
}

async function main() {
  if (!fs.existsSync(VISITOR_ROOT)) {
    log(`访客端项目目录不存在: ${VISITOR_ROOT}`, 'red');
    log('请确认 jeecgboot-vue3-visitor 与 jeecgboot-vue3 在同一父目录下', 'red');
    process.exit(1);
  }

  log(`step 1/3 → 在 ${VISITOR_ROOT} 执行 pnpm build:nocheck`);
  try {
    execSync('pnpm build:nocheck', {
      cwd: VISITOR_ROOT,
      stdio: 'inherit',
      shell: process.platform === 'win32' ? 'powershell.exe' : '/bin/sh',
    });
  } catch (err: any) {
    log(`访客端 build 失败 (exit ${err?.status ?? 'unknown'})`, 'red');
    process.exit(typeof err?.status === 'number' ? err.status : 1);
  }

  if (!fs.existsSync(VISITOR_DIST)) {
    log(`未找到 visitor build 产物: ${VISITOR_DIST}`, 'red');
    process.exit(1);
  }

  log(`step 2/3 → 清空目标目录 ${TARGET_DIR}`);
  await fs.remove(TARGET_DIR);
  await fs.ensureDir(TARGET_DIR);

  log(`step 3/3 → 拷贝 ${VISITOR_DIST} → ${TARGET_DIR}`);
  await fs.copy(VISITOR_DIST, TARGET_DIR, { overwrite: true });

  log(`访客端产物合并完成 ✓`, 'green');
  log(`部署后访问 /cs/userChat 即返回新版访客端 SPA`, 'green');
}

main().catch((err) => {
  log(`执行异常: ${err?.message ?? err}`, 'red');
  process.exit(1);
});
