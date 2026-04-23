/**
 * 极简图片预览（替代主项目 /@/components/Preview）
 *
 * 主项目 createImgPreview 基于自研 Image 组件 + 复杂的 mountedComponent 逻辑，
 * 访客端只需：全屏遮罩 + 当前图 + 左右切换 + ESC 关闭。
 *
 * 不引入任何额外依赖，纯 DOM 实现，挂到 body 上。
 */
export interface CreateImgPreviewOptions {
  imageList: string[];
  index?: number;
  /** 关闭时回调 */
  onClose?: () => void;
}

export function createImgPreview(opts: CreateImgPreviewOptions) {
  const list = (opts.imageList || []).filter(Boolean);
  if (!list.length) return;
  let cur = Math.max(0, Math.min(opts.index || 0, list.length - 1));

  const wrap = document.createElement('div');
  wrap.className = 'visitor-img-preview';
  wrap.innerHTML = `
    <div class="vip-mask"></div>
    <button class="vip-btn vip-prev" aria-label="prev">‹</button>
    <button class="vip-btn vip-next" aria-label="next">›</button>
    <button class="vip-close" aria-label="close">×</button>
    <div class="vip-stage"><img class="vip-img" alt="preview" /></div>
    <div class="vip-counter"></div>
  `;
  injectStyle();
  document.body.appendChild(wrap);

  const imgEl = wrap.querySelector('.vip-img') as HTMLImageElement;
  const counterEl = wrap.querySelector('.vip-counter') as HTMLElement;
  const prevBtn = wrap.querySelector('.vip-prev') as HTMLElement;
  const nextBtn = wrap.querySelector('.vip-next') as HTMLElement;
  const closeBtn = wrap.querySelector('.vip-close') as HTMLElement;
  const maskEl = wrap.querySelector('.vip-mask') as HTMLElement;

  function render() {
    imgEl.src = list[cur];
    counterEl.textContent = list.length > 1 ? `${cur + 1} / ${list.length}` : '';
    (prevBtn as any).style.display = list.length > 1 ? '' : 'none';
    (nextBtn as any).style.display = list.length > 1 ? '' : 'none';
  }
  render();

  function close() {
    document.removeEventListener('keydown', onKey);
    wrap.remove();
    opts.onClose?.();
  }
  function next() { cur = (cur + 1) % list.length; render(); }
  function prev() { cur = (cur - 1 + list.length) % list.length; render(); }
  function onKey(e: KeyboardEvent) {
    if (e.key === 'Escape') close();
    else if (e.key === 'ArrowRight') next();
    else if (e.key === 'ArrowLeft') prev();
  }

  prevBtn.addEventListener('click', prev);
  nextBtn.addEventListener('click', next);
  closeBtn.addEventListener('click', close);
  maskEl.addEventListener('click', close);
  document.addEventListener('keydown', onKey);
}

let _styleInjected = false;
function injectStyle() {
  if (_styleInjected) return;
  _styleInjected = true;
  const css = `
.visitor-img-preview {
  position: fixed; inset: 0; z-index: 99999;
  display: flex; align-items: center; justify-content: center;
}
.visitor-img-preview .vip-mask {
  position: absolute; inset: 0; background: rgba(0,0,0,.85);
}
.visitor-img-preview .vip-stage {
  position: relative; max-width: 95vw; max-height: 90vh;
  display: flex; align-items: center; justify-content: center;
}
.visitor-img-preview .vip-img {
  max-width: 95vw; max-height: 90vh;
  object-fit: contain; user-select: none;
  background: #222; border-radius: 4px;
  box-shadow: 0 6px 32px rgba(0,0,0,.5);
}
.visitor-img-preview .vip-btn,
.visitor-img-preview .vip-close {
  position: absolute; z-index: 1;
  border: none; background: rgba(0,0,0,.4); color: #fff;
  width: 40px; height: 40px; border-radius: 50%;
  font-size: 24px; line-height: 1; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .2s;
}
.visitor-img-preview .vip-btn:hover,
.visitor-img-preview .vip-close:hover { background: rgba(0,0,0,.7); }
.visitor-img-preview .vip-prev { left: 24px; top: 50%; transform: translateY(-50%); }
.visitor-img-preview .vip-next { right: 24px; top: 50%; transform: translateY(-50%); }
.visitor-img-preview .vip-close { right: 16px; top: 16px; font-size: 28px; }
.visitor-img-preview .vip-counter {
  position: absolute; bottom: 24px; left: 50%; transform: translateX(-50%);
  color: #fff; font-size: 13px; opacity: .85;
  background: rgba(0,0,0,.4); padding: 4px 12px; border-radius: 12px;
}
`;
  const style = document.createElement('style');
  style.textContent = css;
  document.head.appendChild(style);
}
