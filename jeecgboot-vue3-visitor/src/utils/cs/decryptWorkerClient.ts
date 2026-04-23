/**
 * Phase 3 Sprint 2：消息解密 Web Worker 客户端封装。
 *
 * <p>对外暴露一个简单的 {@link decryptMessagesBatch} API，内部按以下策略选择执行路径：</p>
 * <ul>
 *   <li>N &lt; THRESHOLD：主线程同步解密（worker 通信开销 ~1-2ms，小批量不划算）</li>
 *   <li>N &gt;= THRESHOLD：lazy 创建 worker，postMessage 批量解密</li>
 *   <li>worker 创建失败 / 不支持：fallback 到主线程同步解密</li>
 * </ul>
 *
 * <p>Worker 用 idle 销毁策略：连续 60s 没用就 terminate，回收内存。</p>
 *
 * @author jeecg
 */
import { decryptMessage } from '/@/utils/cs/csEncrypt';
// Vite 的 ?worker 后缀会把目标文件按 worker 形式打包，import 默认导出是 worker 构造器
import DecryptWorker from '/@/utils/cs/decryptWorker?worker';

/** 消息数 < 该阈值时走主线程同步解密；>= 时走 worker */
export const DECRYPT_WORKER_THRESHOLD = 30;

/** worker 闲置 60s 后销毁，下次需要时重新创建 */
const IDLE_TERMINATE_MS = 60_000;

interface PendingTask {
  resolve: (plaintexts: string[]) => void;
  reject: (err: Error) => void;
}

let workerInstance: Worker | null = null;
let workerSupported: boolean = typeof Worker !== 'undefined';
let nextTaskId = 1;
const pending = new Map<number, PendingTask>();
let idleTimer: ReturnType<typeof setTimeout> | null = null;

function ensureWorker(): Worker | null {
  if (!workerSupported) return null;
  if (workerInstance) return workerInstance;
  try {
    workerInstance = new DecryptWorker();
    workerInstance.addEventListener('message', (e: MessageEvent) => {
      const { id, plaintexts, error } = e.data || {};
      const task = pending.get(id);
      if (!task) return;
      pending.delete(id);
      if (error) {
        task.reject(new Error(error));
      } else {
        task.resolve(Array.isArray(plaintexts) ? plaintexts : []);
      }
      scheduleIdleTerminate();
    });
    workerInstance.addEventListener('error', (e: ErrorEvent) => {
      console.warn('[decryptWorker] error event', e.message);
      // worker 整体崩溃：拒绝所有 pending，置空实例下次重建
      const err = new Error(e.message || 'worker error');
      pending.forEach((t) => t.reject(err));
      pending.clear();
      try { workerInstance?.terminate(); } catch {}
      workerInstance = null;
    });
  } catch (e) {
    console.warn('[decryptWorker] init failed, fallback to main thread', e);
    workerSupported = false;
    workerInstance = null;
  }
  return workerInstance;
}

function scheduleIdleTerminate() {
  if (idleTimer) clearTimeout(idleTimer);
  idleTimer = setTimeout(() => {
    if (pending.size === 0 && workerInstance) {
      try { workerInstance.terminate(); } catch {}
      workerInstance = null;
    }
  }, IDLE_TERMINATE_MS);
}

/**
 * 批量解密消息 content 字段。
 *
 * @param ciphertexts 密文数组（可能是 transport→storage 双层加密）
 * @returns plaintext 数组（与输入同长度，单条解密失败回退原文）
 */
export async function decryptMessagesBatch(ciphertexts: string[]): Promise<string[]> {
  if (!Array.isArray(ciphertexts) || ciphertexts.length === 0) {
    return [];
  }
  // 小批量直接主线程，避免通信开销
  if (ciphertexts.length < DECRYPT_WORKER_THRESHOLD) {
    return ciphertexts.map((c) => {
      try { return decryptMessage(c); } catch { return c; }
    });
  }
  const worker = ensureWorker();
  if (!worker) {
    // worker 不可用 fallback
    return ciphertexts.map((c) => {
      try { return decryptMessage(c); } catch { return c; }
    });
  }
  const id = nextTaskId++;
  return new Promise<string[]>((resolve, reject) => {
    pending.set(id, { resolve, reject });
    try {
      worker.postMessage({ id, ciphertexts });
    } catch (e: any) {
      pending.delete(id);
      reject(e instanceof Error ? e : new Error(String(e)));
    }
  });
}

/** 主动销毁 worker（用于页面卸载） */
export function disposeDecryptWorker() {
  if (idleTimer) { clearTimeout(idleTimer); idleTimer = null; }
  if (workerInstance) {
    try { workerInstance.terminate(); } catch {}
    workerInstance = null;
  }
  pending.clear();
}
