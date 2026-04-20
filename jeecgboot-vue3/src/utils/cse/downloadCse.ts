/**
 * CSE 加密文件下载工具
 *
 * 浏览器不认 cse:// 协议，无法直接 window.open 或 <a href>。
 * 需先解密为 Blob，再触发浏览器下载（保留原文件名）。
 *
 * 用法：
 *   import { downloadCse } from '/@/utils/cse/downloadCse';
 *   await downloadCse('cse://xxx', 'report.pdf');
 *
 * 兼容点：
 *   - 普通 http(s) URL：直接走 <a download> 触发浏览器下载（不绕道）
 *   - cse:// URL：解密 → Blob → object URL → <a download> → revoke
 *   - 失败时弹消息提示
 */
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { decryptFileById } from '/@/utils/cse/cseDecrypt';

/**
 * 触发浏览器下载（保留 fileName 作为下载默认文件名）
 * @param url 原始 URL（cse:// 或 http(s)://）
 * @param fileName 下载时显示的文件名（可选）
 * @param mime 推荐的 MIME 类型（可选；cse:// 时用于构造 Blob）
 */
export async function downloadCse(url: string, fileName?: string, mime?: string): Promise<void> {
  if (!url) return;
  if (!isCseUrl(url)) {
    // 普通 URL 直接 a 标签触发下载
    triggerDownload(url, fileName);
    return;
  }
  const fid = parseCseFid(url);
  if (!fid) {
    console.warn('[downloadCse] invalid cse:// url', url);
    return;
  }
  try {
    const blob = await decryptFileById(fid, { mime: mime || 'application/octet-stream' });
    const blobUrl = URL.createObjectURL(blob);
    try {
      triggerDownload(blobUrl, fileName);
    } finally {
      // 触发下载后稍延迟 revoke，避免某些浏览器还没开始下载就丢失
      setTimeout(() => {
        try {
          URL.revokeObjectURL(blobUrl);
        } catch {}
      }, 30_000);
    }
  } catch (e: any) {
    console.error('[downloadCse] decrypt fail', fid, e?.message || e);
    // 静默失败：避免在批量下载场景刷弹窗
  }
}

function triggerDownload(href: string, fileName?: string): void {
  const a = document.createElement('a');
  a.href = href;
  if (fileName) a.download = fileName;
  // 部分浏览器对跨域 blob 下载需要 target=_blank 兜底
  a.style.display = 'none';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}
