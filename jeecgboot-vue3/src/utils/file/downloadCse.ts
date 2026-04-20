/**
 * CSE 加密文件统一下载工具。
 *
 * 旧代码常用 `window.open(url)` 直链下载，但 cse://{fid} 不是浏览器可识别的协议，
 * 直接 open 会得到 about:blank 或 ERR_UNKNOWN_URL_SCHEME。本工具会：
 *   1. 识别 cse:// 前缀，调用 decryptFileById 拿到明文 Blob
 *   2. 通过 <a download> 触发浏览器原生下载，保留原始文件名
 *   3. 普通 http/https 仍然按原行为 window.open 或 a[download]
 *
 * 使用：
 *   import { downloadByUrl } from '/@/utils/file/downloadCse';
 *   await downloadByUrl(url, '可选的文件名');
 */
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { decryptFileById } from '/@/utils/cse/cseDecrypt';

/**
 * 通用下载入口，自动区分 cse:// 与 http(s)://。
 *
 * @param url      文件地址（可能是 cse://{fid} 或 http(s):// 直链）
 * @param fileName 可选的导出文件名；不传时尝试从 URL 末段截取
 */
export async function downloadByUrl(url: string, fileName?: string): Promise<void> {
  if (!url) return;
  if (isCseUrl(url)) {
    const fid = parseCseFid(url);
    if (!fid) throw new Error('CSE URL 缺少 fid');
    const blob = await decryptFileById(fid);
    triggerBlobDownload(blob, fileName || `${fid}.bin`);
    return;
  }
  triggerLinkDownload(url, fileName);
}

/**
 * 直接给定 cse:// fid 时使用。某些场景（评论区、附件列表）已经握有 fid，
 * 不需要重新拼接 cse:// 字符串。
 */
export async function downloadByFid(fid: string, fileName?: string): Promise<void> {
  if (!fid) return;
  const blob = await decryptFileById(fid);
  triggerBlobDownload(blob, fileName || `${fid}.bin`);
}

function triggerBlobDownload(blob: Blob, fileName: string) {
  const blobUrl = URL.createObjectURL(blob);
  try {
    triggerLinkDownload(blobUrl, fileName);
  } finally {
    setTimeout(() => URL.revokeObjectURL(blobUrl), 60 * 1000);
  }
}

function triggerLinkDownload(href: string, fileName?: string) {
  const a = document.createElement('a');
  a.style.display = 'none';
  a.href = href;
  if (fileName) a.download = fileName;
  a.target = '_self';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
}

export default downloadByUrl;
