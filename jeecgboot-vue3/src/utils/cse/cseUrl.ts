/**
 * CSE 文件 URL 工具：识别 cse:// 协议并提取 fileId
 */

export const CSE_PREFIX = 'cse://';

export function isCseUrl(url: any): boolean {
  return typeof url === 'string' && url.indexOf(CSE_PREFIX) === 0;
}

/**
 * 从 cse://{fid} 中解析 fid
 */
export function parseCseFid(url: string): string | null {
  if (!isCseUrl(url)) {
    return null;
  }
  const tail = url.substring(CSE_PREFIX.length).trim();
  if (!tail) {
    return null;
  }
  // 兼容形如 cse://fid?ts=xxx 的情况
  const q = tail.indexOf('?');
  return q >= 0 ? tail.substring(0, q) : tail;
}

/**
 * 把字符串里夹杂的 cse:// 转成可识别的 data-fid 属性形式
 * 给富文本 / Markdown 内容预处理时用
 */
export function replaceCseImgWithPlaceholder(html: string, placeholder = '/resource/img/loading.svg'): string {
  if (!html) return html;
  return html.replace(/<img\b([^>]*?)\bsrc=["'](cse:\/\/[^"']+)["']([^>]*)>/gi, (_m, before, src, after) => {
    const fid = parseCseFid(src);
    if (!fid) return _m;
    return `<img${before}data-fid="${fid}" src="${placeholder}"${after}>`;
  });
}
