/**
 * @description: base64 to blob
 */
export function dataURLtoBlob(base64Buf: string): Blob {
  const arr = base64Buf.split(',');
  const typeItem = arr[0];
  const mime = typeItem.match(/:(.*?);/)![1];
  const bstr = atob(arr[1]);
  let n = bstr.length;
  const u8arr = new Uint8Array(n);
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n);
  }
  return new Blob([u8arr], { type: mime });
}

/**
 * img url to base64
 * @param url
 */
export async function urlToBase64(url: string, mineType?: string): Promise<string> {
  // CSE 加密图：浏览器 <img> 不能加载 cse://，先解密为 blob URL，再走 canvas 流程
  try {
    const { isCseUrl, parseCseFid } = await import('/@/utils/cse/cseUrl');
    if (isCseUrl(url)) {
      const fid = parseCseFid(url);
      if (!fid) throw new Error('invalid cse url');
      const { decryptFileToObjectUrl } = await import('/@/utils/cse/cseDecrypt');
      const blobUrl = await decryptFileToObjectUrl(fid, { mime: 'image/*' });
      try {
        return await canvasToBase64(blobUrl, mineType);
      } finally {
        try { URL.revokeObjectURL(blobUrl); } catch {}
      }
    }
  } catch (e) {
    // 解密失败：回退到原始路径，让 canvas 自行报错
    console.warn('[urlToBase64] cse decrypt fail, fallback', e);
  }
  return canvasToBase64(url, mineType);
}

function canvasToBase64(src: string, mineType?: string): Promise<string> {
  return new Promise((resolve, reject) => {
    let canvas = document.createElement('CANVAS') as Nullable<HTMLCanvasElement>;
    const ctx = canvas!.getContext('2d');

    const img = new Image();
    img.crossOrigin = '';
    img.onload = function () {
      if (!canvas || !ctx) {
        return reject();
      }
      canvas.height = img.height;
      canvas.width = img.width;
      ctx.drawImage(img, 0, 0);
      const dataURL = canvas.toDataURL(mineType || 'image/png');
      canvas = null;
      resolve(dataURL);
    };
    img.onerror = (e) => reject(e);
    img.src = src;
  });
}
