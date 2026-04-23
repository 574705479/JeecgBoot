/**
 * 1:1 复制自 jeecgboot-vue3/src/utils/file/downloadCse.ts
 *
 * CSE 加密文件统一下载工具。
 */
import { isCseUrl, parseCseFid } from '/@/utils/cse/cseUrl';
import { decryptFileById } from '/@/utils/cse/cseDecrypt';

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
