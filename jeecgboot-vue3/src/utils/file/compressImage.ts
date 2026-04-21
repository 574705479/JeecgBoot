/**
 * 客户端图片压缩工具（纯前端 Canvas，零 npm 依赖）。
 *
 * 治本目标：在客服 / 访客上传图片前先在浏览器里把长边压到 1920px、
 * 质量 0.85，避免后端 CSE 加密通道再为几 MB 的手机原图反复消耗带宽。
 *
 * 严格语义（与计划 R5/R6 对齐）：
 *  - 仅图片走压缩；非图片直接返回原 file
 *  - GIF / HEIC / HEIF / SVG 跳过（动图 / 解码不出来 / 矢量本身就小）
 *  - 文件 ≤ 500KB 直接返回原 file（压缩收益小、且会破坏后端秒传 md5）
 *  - 长边 ≤ 1920px 且文件 ≤ 1MB 时也跳过
 *  - 输出格式：原文件含 alpha → PNG（无损保 alpha）；否则 JPEG，质量 0.85
 *  - 任何环节抛错 / 压缩后反而更大 → fallback 原 file，绝不阻断发送
 *
 * 与后端配合：调用方在上传请求附加 header `X-No-Strip-Metadata: 1`，
 * 后端 CseUploader 跳过 ImageIO 二次重新编码，避免画质雪崩（双重压缩）。
 */

const MAX_DIMENSION = 1920;
const SIZE_THRESHOLD = 500 * 1024; // 500KB 以下不压
const SIZE_PASS_THROUGH = 1 * 1024 * 1024; // 1MB 以下且尺寸够小直接放过
const JPEG_QUALITY = 0.85;

/** 跳过类型：动图 / 编解码不通用的格式 */
const SKIP_MIMES = new Set([
  'image/gif',
  'image/heic',
  'image/heif',
  'image/svg+xml',
  'image/avif', // AVIF Canvas encode 兼容性不稳，留给后端
]);

/** 判定是否是图片 */
function isImageFile(file: File): boolean {
  return !!file && file.type && file.type.startsWith('image/');
}

/**
 * 检测 PNG 是否包含 alpha 通道（粗略判定：tRNS chunk 或 colour type 6/4）。
 * 不是 PNG 直接返回 false。失败时返回 true（保守：假设有 alpha 走 PNG）。
 */
async function pngHasAlpha(file: File): Promise<boolean> {
  if (file.type !== 'image/png') return false;
  try {
    const head = new Uint8Array(await file.slice(0, 64).arrayBuffer());
    // PNG signature: 89 50 4E 47 0D 0A 1A 0A，IHDR 在 16~24，colour type 在 25
    if (head[0] !== 0x89 || head[1] !== 0x50 || head[2] !== 0x4e || head[3] !== 0x47) return false;
    const colourType = head[25];
    // 4=灰度+alpha, 6=RGBA
    return colourType === 4 || colourType === 6;
  } catch {
    return true;
  }
}

/** 算出按 MAX_DIMENSION 等比缩放后的目标尺寸 */
function calcTargetSize(srcW: number, srcH: number): { w: number; h: number } {
  const longEdge = Math.max(srcW, srcH);
  if (longEdge <= MAX_DIMENSION) return { w: srcW, h: srcH };
  const ratio = MAX_DIMENSION / longEdge;
  return { w: Math.round(srcW * ratio), h: Math.round(srcH * ratio) };
}

function canvasToBlob(canvas: HTMLCanvasElement, mime: string, quality: number): Promise<Blob | null> {
  return new Promise((resolve) => {
    try {
      canvas.toBlob((b) => resolve(b), mime, quality);
    } catch {
      resolve(null);
    }
  });
}

/**
 * 压缩单张图片。失败 / 不需要压缩 / 反而变大 → 直接返回原 file。
 */
export async function compressImage(file: File): Promise<File> {
  try {
    if (!isImageFile(file)) return file;
    if (SKIP_MIMES.has(file.type)) return file;
    if (file.size <= SIZE_THRESHOLD) return file;

    // 解码原图
    let bitmap: ImageBitmap | null = null;
    try {
      bitmap = await createImageBitmap(file);
    } catch {
      // 部分浏览器对 webp / heic 解码失败 → fallback 原文件
      return file;
    }

    const { width: srcW, height: srcH } = bitmap;
    const { w, h } = calcTargetSize(srcW, srcH);
    // 尺寸不需要缩 + 文件本身已经不大 → 跳过
    if (w === srcW && h === srcH && file.size <= SIZE_PASS_THROUGH) {
      bitmap.close?.();
      return file;
    }

    const canvas = document.createElement('canvas');
    canvas.width = w;
    canvas.height = h;
    const ctx = canvas.getContext('2d');
    if (!ctx) {
      bitmap.close?.();
      return file;
    }
    ctx.drawImage(bitmap, 0, 0, w, h);
    bitmap.close?.();

    // 输出格式：原图有 alpha 留 PNG，否则 JPEG
    const hasAlpha = await pngHasAlpha(file);
    const outMime = hasAlpha ? 'image/png' : 'image/jpeg';
    const outQuality = hasAlpha ? 1.0 : JPEG_QUALITY;
    const blob = await canvasToBlob(canvas, outMime, outQuality);
    if (!blob || blob.size === 0) return file;

    // 压完反而更大（小文件 + canvas 元数据膨胀）→ 用原 file
    if (blob.size >= file.size) return file;

    // 重命名为对应扩展名，防止后端按扩展名判断 MIME 时不一致
    const base = file.name.replace(/\.[^.]+$/, '');
    const ext = outMime === 'image/png' ? '.png' : '.jpg';
    return new File([blob], base + ext, { type: outMime, lastModified: Date.now() });
  } catch (e) {
    console.warn('[compressImage] fallback to original', e);
    return file;
  }
}
