/**
 * Phase 3 Sprint 2：消息解密 Web Worker。
 *
 * <p>主线程批量传 ciphertext[]，worker 用 csEncrypt 的实现做 AES-CBC 解密后回传 plaintext[]。
 * 仅在消息数 N>=30 时启用，避免主线程同步阻塞超过 30ms 影响首屏体感。
 * N<30 时通信开销 ~1-2ms 不划算，调用方走同步路径。</p>
 *
 * <h3>消息协议</h3>
 * 主线程 → Worker:
 * <pre>{
 *   id: number,            // 请求批次 id，用于响应匹配
 *   ciphertexts: string[]  // 待解密的密文数组（content 字段，可能是 transport→storage 双层）
 * }</pre>
 *
 * Worker → 主线程:
 * <pre>{
 *   id: number,
 *   plaintexts: string[],  // 同长度，与请求一一对应
 *   error?: string         // 整批失败时携带错误信息（不会发生在单条解密失败，那种情况 fallback 到原文）
 * }</pre>
 */
import { decryptMessage } from '/@/utils/cs/csEncrypt';

interface DecryptRequest {
  id: number;
  ciphertexts: string[];
}

interface DecryptResponse {
  id: number;
  plaintexts: string[];
  error?: string;
}

self.addEventListener('message', (event: MessageEvent<DecryptRequest>) => {
  const { id, ciphertexts } = event.data || ({} as DecryptRequest);
  if (!Array.isArray(ciphertexts)) {
    const errResp: DecryptResponse = { id, plaintexts: [], error: 'ciphertexts not array' };
    (self as any).postMessage(errResp);
    return;
  }
  try {
    const plaintexts = ciphertexts.map((c) => {
      try { return decryptMessage(c); } catch { return c; }
    });
    const resp: DecryptResponse = { id, plaintexts };
    (self as any).postMessage(resp);
  } catch (e: any) {
    const errResp: DecryptResponse = { id, plaintexts: [], error: e?.message || String(e) };
    (self as any).postMessage(errResp);
  }
});

export {};
