/**
 * ╔════════════════════════════════════════════════════════════════╗
 * ║ DO NOT EDIT INDEPENDENTLY                                       ║
 * ║ 1:1 复制自 jeecgboot-vue3/src/views/super/airag/cs/utils/        ║
 * ║ csNotificationSound.ts                                          ║
 * ╚════════════════════════════════════════════════════════════════╝
 *
 * 客服/访客提示音共用：两声短蜂鸣；增益乘数上限与工作台滑块 200% 一致
 */

export const CS_NOTIFY_MAX_GAIN = 2;

export function playCsNotificationSound(audioCtx: AudioContext, gainMultiplier: number): void {
  const m = Math.max(0, Math.min(CS_NOTIFY_MAX_GAIN, gainMultiplier));
  if (m <= 0) return;
  const t = audioCtx.currentTime;
  const peak = 0.8 * m;
  const tail = 0.05 * m;
  const osc1 = audioCtx.createOscillator();
  const gain1 = audioCtx.createGain();
  osc1.connect(gain1);
  gain1.connect(audioCtx.destination);
  osc1.frequency.value = 880;
  gain1.gain.setValueAtTime(peak, t);
  gain1.gain.exponentialRampToValueAtTime(tail, t + 0.15);
  osc1.start(t);
  osc1.stop(t + 0.15);
  const osc2 = audioCtx.createOscillator();
  const gain2 = audioCtx.createGain();
  osc2.connect(gain2);
  gain2.connect(audioCtx.destination);
  osc2.frequency.value = 1318.5;
  gain2.gain.setValueAtTime(peak, t + 0.18);
  gain2.gain.exponentialRampToValueAtTime(tail, t + 0.4);
  osc2.start(t + 0.18);
  osc2.stop(t + 0.4);
}
