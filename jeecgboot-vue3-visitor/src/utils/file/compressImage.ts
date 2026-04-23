/**
 * 1:1 复制自 jeecgboot-vue3/src/utils/file/compressImage.ts
 *
 * 客户端图片压缩工具（纯前端 Canvas，零 npm 依赖）。
 *
 * 严格语义：
 *  - 仅图片走压缩；非图片直接返回原 file
 *  - GIF / HEIC / HEIF / SVG 跳过
 *  - 文件 ≤ 500KB 直接返回原 file
 *  - 长边 ≤ 1920px 且文件 ≤ 1MB 时也跳过
 *  - 输出格式：原文件含 alpha → PNG；否则 JPEG，质量 0.85
 *  - 任何环节抛错 / 压缩后反而更大 → fallback 原 file
 *
 * 与后端配合：调用方在上传请求附加 header `X-No-Strip-Metadata: 1`。
 */

const MAX_DIMENSION = 1920;
const SIZE_THRESHOLD = 500 * 1024;
const SIZE_PASS_THROUGH = 1 * 1024 * 1024;
const JPEG_QUALITY = 0.85;

const SKIP_MIMES = new Set([
  'image/gif',
  'image/heic',
  'image/heif',
  'image/svg+xml',
  'image/avif',
]);

function isImageFile(file: File): boolean {
  return !!file && file.type && file.type.startsWith('image/');
}

async function pngHasAlpha(file: File): Promise<boolean> {
  if (file.type !== 'image/png') return false;
  try {
    const head = new Uint8Array(await file.slice(0, 64).arrayBuffer());
    if (head[0] !== 0x89 || head[1] !== 0x50 || head[2] !== 0x4e || head[3] !== 0x47) return false;
    const colourType = head[25];
    return colourType === 4 || colourType === 6;
  } catch {
    return true;
  }
}

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

export async function compressImage(file: File): Promise<File> {
  try {
    if (!isImageFile(file)) return file;
    if (SKIP_MIMES.has(file.type)) return file;
    if (file.size <= SIZE_THRESHOLD) return file;

    let bitmap: ImageBitmap | null = null;
    try {
      bitmap = await createImageBitmap(file);
    } catch {
      return file;
    }

    const { width: srcW, height: srcH } = bitmap;
    const { w, h } = calcTargetSize(srcW, srcH);
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

    const hasAlpha = await pngHasAlpha(file);
    const outMime = hasAlpha ? 'image/png' : 'image/jpeg';
    const outQuality = hasAlpha ? 1.0 : JPEG_QUALITY;
    const blob = await canvasToBlob(canvas, outMime, outQuality);
    if (!blob || blob.size === 0) return file;

    if (blob.size >= file.size) return file;

    const base = file.name.replace(/\.[^.]+$/, '');
    const ext = outMime === 'image/png' ? '.png' : '.jpg';
    return new File([blob], base + ext, { type: outMime, lastModified: Date.now() });
  } catch (e) {
    console.warn('[compressImage] fallback to original', e);
    return file;
  }
}
