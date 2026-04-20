/**
 * CSE 端到端文件解密
 *
 * 协议（与后端 FileEncryptionService 严格对齐）：
 *  - DEK 二次包装：HKDF-SHA256(IKM=token+"|"+kid, salt=fileId, info="cse:dek-wrap", L=32)
 *  - 派生 IV：file_iv 字节反转后 ^ 0x5A
 *  - DEK 解出后用 AES-256-GCM 直接解密文件密文（AAD=fileId）
 *
 * 优先 SubtleCrypto；不可用时（Electron file:// 等）回退到 @noble/ciphers。
 */
import { defHttp } from '/@/utils/http/axios';
// Phase 3.2b/3.2c：登录与访客一律走 cseAuthContext，
//   - HKDF IKM 用 getCseAuthToken（保证与后端 SecureFileController 的 sealToken 同源）
//   - 访问 /sys/secure/file/* 时附加 getCseExtraHeaders（X-Visitor-Session 等）
import { getCseAuthToken, getCseExtraHeaders } from '/@/utils/cse/cseAuthContext';

const SECURE_BASE = '/sys/secure/file';
const HKDF_INFO = 'cse:dek-wrap';
const GCM_TAG_LEN = 16;

interface KeyMeta {
  algo: string;
  ivB64: string;
  kid: string;
  dekSealedB64: string;
}

/** 浏览器是否支持 SubtleCrypto */
function hasSubtle(): boolean {
  try {
    return !!(globalThis.crypto && globalThis.crypto.subtle);
  } catch {
    return false;
  }
}

let _nobleCiphers: any = null;
let _nobleHkdf: any = null;
async function loadNoble() {
  if (!_nobleCiphers) {
    _nobleCiphers = await import('@noble/ciphers/aes.js');
  }
  if (!_nobleHkdf) {
    const hkdfMod: any = await import('@noble/hashes/hkdf.js');
    const sha2Mod: any = await import('@noble/hashes/sha2.js');
    _nobleHkdf = { hkdf: hkdfMod.hkdf, sha256: sha2Mod.sha256 };
  }
  return { ciphers: _nobleCiphers, kdf: _nobleHkdf };
}

function b64ToBytes(b64: string): Uint8Array {
  const bin = atob(b64);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

function strToBytes(s: string): Uint8Array {
  return new TextEncoder().encode(s);
}

function deriveIv(fileIv: Uint8Array): Uint8Array {
  const out = new Uint8Array(fileIv.length);
  for (let i = 0; i < fileIv.length; i++) {
    out[i] = fileIv[fileIv.length - 1 - i] ^ 0x5a;
  }
  return out;
}

async function hkdfSha256(ikm: Uint8Array, salt: Uint8Array, info: Uint8Array, length: number): Promise<Uint8Array> {
  if (hasSubtle()) {
    const baseKey = await crypto.subtle.importKey('raw', ikm, 'HKDF', false, ['deriveBits']);
    const bits = await crypto.subtle.deriveBits(
      { name: 'HKDF', hash: 'SHA-256', salt, info },
      baseKey,
      length * 8,
    );
    return new Uint8Array(bits);
  }
  const { kdf } = await loadNoble();
  return kdf.hkdf(kdf.sha256, ikm, salt, info, length);
}

async function aesGcmDecrypt(key: Uint8Array, iv: Uint8Array, cipher: Uint8Array, aad?: Uint8Array): Promise<Uint8Array> {
  if (hasSubtle()) {
    const k = await crypto.subtle.importKey('raw', key, 'AES-GCM', false, ['decrypt']);
    const params: AesGcmParams = { name: 'AES-GCM', iv, tagLength: GCM_TAG_LEN * 8 };
    if (aad) params.additionalData = aad;
    const plain = await crypto.subtle.decrypt(params, k, cipher);
    return new Uint8Array(plain);
  }
  const { ciphers } = await loadNoble();
  const gcm = ciphers.gcm(key, iv, aad);
  return gcm.decrypt(cipher);
}

export async function cseSelfTest(): Promise<boolean> {
  try {
    const k = new Uint8Array(32);
    const iv = new Uint8Array(12);
    const data = new TextEncoder().encode('cse-test');
    if (hasSubtle()) {
      const ck = await crypto.subtle.importKey('raw', k, 'AES-GCM', false, ['encrypt', 'decrypt']);
      const ct = await crypto.subtle.encrypt({ name: 'AES-GCM', iv }, ck, data);
      const pt = await crypto.subtle.decrypt({ name: 'AES-GCM', iv }, ck, ct);
      return new TextDecoder().decode(pt) === 'cse-test';
    }
    const { ciphers } = await loadNoble();
    const gcm = ciphers.gcm(k, iv);
    const ct = gcm.encrypt(data);
    const pt = gcm.decrypt(ct);
    return new TextDecoder().decode(pt) === 'cse-test';
  } catch (e) {
    console.error('[CSE] self-test failed', e);
    return false;
  }
}

// === DEK / 文件 缓存（短期） ===
const dekCache = new Map<string, { dek: Uint8Array; iv: Uint8Array; ts: number }>();
const DEK_TTL_MS = 60_000;
const inFlight = new Map<string, Promise<Blob>>();

function getCachedDek(fid: string) {
  const e = dekCache.get(fid);
  if (e && Date.now() - e.ts < DEK_TTL_MS) return e;
  if (e) dekCache.delete(fid);
  return null;
}

async function fetchKeyMeta(fid: string, extraHeaders?: Record<string, string>): Promise<KeyMeta> {
  const headers = { ...(extraHeaders || {}) };
  const res: any = await defHttp.get(
    { url: `${SECURE_BASE}/${fid}/key`, headers },
    { errorMessageMode: 'none' },
  );
  if (!res || !res.algo) {
    throw new Error('no key meta');
  }
  return res as KeyMeta;
}

async function fetchCipher(fid: string, thumb: boolean, extraHeaders?: Record<string, string>): Promise<ArrayBuffer> {
  const url = `${SECURE_BASE}/${fid}${thumb ? '?thumb=1' : ''}`;
  const headers = { ...(extraHeaders || {}) };
  const res: any = await defHttp.get(
    { url, responseType: 'arraybuffer', headers },
    { isReturnNativeResponse: true, errorMessageMode: 'none' },
  );
  return res.data as ArrayBuffer;
}

/**
 * 解密单个 fid，返回 Blob（图片可直接 URL.createObjectURL）
 * @param fid 文件 ID
 * @param opts.thumb 是否取缩略图
 * @param opts.mime 推荐的 MIME（用于构造 Blob）
 */
export async function decryptFileById(
  fid: string,
  opts: { thumb?: boolean; mime?: string } = {},
): Promise<Blob> {
  const key = `${fid}|${opts.thumb ? 't' : 'f'}`;
  const ex = inFlight.get(key);
  if (ex) return ex;

  const p = (async () => {
    // Phase 3.2b T3：登录与访客统一从 cseAuthContext 取 IKM token，
    // 与后端 SecureFileController.resolveVisitorCredential 的 sealToken 严格对齐。
    const token = getCseAuthToken();
    if (!token) {
      throw new Error('no token');
    }
    const extraHeaders = getCseExtraHeaders();
    let cached = getCachedDek(fid);
    if (!cached) {
      const meta = await fetchKeyMeta(fid, extraHeaders);
      const iv = b64ToBytes(meta.ivB64);
      const sealed = b64ToBytes(meta.dekSealedB64);
      const ikm = strToBytes(`${token}|${meta.kid}`);
      const salt = strToBytes(fid);
      const sk = await hkdfSha256(ikm, salt, strToBytes(HKDF_INFO), 32);
      const derivedIv = deriveIv(iv);
      const dek = await aesGcmDecrypt(sk, derivedIv, sealed);
      cached = { dek, iv, ts: Date.now() };
      dekCache.set(fid, cached);
    }

    const cipherBuf = await fetchCipher(fid, !!opts.thumb, extraHeaders);
    // 缩略图加密时 AAD = fid + ':thumb'
    const aad = strToBytes(opts.thumb ? `${fid}:thumb` : fid);
    const plain = await aesGcmDecrypt(cached.dek, cached.iv, new Uint8Array(cipherBuf), aad);
    return new Blob([plain], { type: opts.mime || 'application/octet-stream' });
  })();

  inFlight.set(key, p);
  try {
    return await p;
  } finally {
    inFlight.delete(key);
  }
}

/** 解密为可直接给 <img src> 的 Object URL */
export async function decryptFileToObjectUrl(
  fid: string,
  opts: { thumb?: boolean; mime?: string } = {},
): Promise<string> {
  const blob = await decryptFileById(fid, { mime: opts.mime || 'image/*', thumb: opts.thumb });
  return URL.createObjectURL(blob);
}

/** 清理 DEK 缓存（401 / 退出登录调用） */
export function clearDekCache() {
  dekCache.clear();
  inFlight.clear();
}
