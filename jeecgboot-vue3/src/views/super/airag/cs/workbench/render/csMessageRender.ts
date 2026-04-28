import MarkdownIt from 'markdown-it';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import { useGlobSetting } from '/@/hooks/setting';

const globSetting = useGlobSetting();

const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str: string, lang: string) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value;
      } catch (__) {}
    }
    return '';
  },
});

const renderCache = new Map<string, string>();
const maxRenderCacheSize = 300;

const CSE_ALLOWED_URI_REGEXP =
  /^(?:(?:(?:f|ht)tps?|mailto|tel|callto|sms|cid|xmpp|cse):|[^a-z]|[a-z+.\-]+(?:[^a-z+.\-:]|$))/i;

function normalizeImgUrls(html: string): string {
  try {
    const origin = new URL(globSetting.domainUrl).origin;
    return html.replace(
      /(<img[^>]*?\ssrc=["'])(\/[^"']+)(["'])/gi,
      (_match, pre, path, suf) => `${pre}${origin}${path}${suf}`,
    );
  } catch {
    return html;
  }
}

export function stripHtmlTags(html: string): string {
  if (!html) return '';
  return html
    .replace(/<[^>]*>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .trim();
}

export function buildMessagePreview(content: string, attachments: any[]): string {
  if (content) {
    const plain = stripHtmlTags(content);
    return plain || content;
  }
  if (!attachments || attachments.length === 0) return '';
  const labels = new Set<string>();
  attachments.forEach((att) => {
    if (att.type === 'image') labels.add('图片');
    else if (att.type === 'video') labels.add('视频');
    else labels.add('文件');
  });
  return `[${Array.from(labels).join('/')}]`;
}

export function linkifyPlainText(text: string): string {
  const urlPattern =
    /(https?:\/\/[^\s<>]*[^\s<>.,;:!?。，；：！？)\]】]|www\.[^\s<>]*[^\s<>.,;:!?。，；：！？)\]】])/gi;
  let lastIndex = 0;
  let result = '';
  const esc = (s: string) =>
    s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  let match: RegExpExecArray | null;
  urlPattern.lastIndex = 0;
  while ((match = urlPattern.exec(text)) !== null) {
    result += esc(text.slice(lastIndex, match.index));
    const url = match[0];
    const href = url.startsWith('www.') ? 'https://' + url : url;
    result += `<a class="auto-link" href="${esc(href)}" target="_blank" rel="noopener noreferrer">${esc(url)}</a>`;
    lastIndex = match.index + url.length;
  }
  result += esc(text.slice(lastIndex));
  return result.replace(/\n/g, '<br>');
}

export function renderStreamingText(content: string): string {
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>');
}

export function sanitizeHtml(html: string): string {
  return DOMPurify.sanitize(html, {
    ADD_TAGS: ['iframe'],
    ADD_ATTR: ['target', 'allowfullscreen', 'frameborder'],
    ALLOW_DATA_ATTR: false,
    ALLOWED_URI_REGEXP: CSE_ALLOWED_URI_REGEXP,
  });
}

export function renderMessage(content: string): string {
  if (!content) return '';
  content = content.replace(/#\s*\{\s*domainURL\s*\}/g, globSetting.domainUrl);
  content = normalizeImgUrls(content);
  const cached = renderCache.get(content);
  if (cached) {
    return cached;
  }
  let rendered = '';
  const isRichHtml = /^\s*<(?:p|div|ul|ol|h[1-6]|table|blockquote)\b/i.test(content.trim());
  if (isRichHtml) {
    rendered = sanitizeHtml(content);
  } else {
    const hasMarkdown = /!\[[^\]]*]\([^)]*\)|\*\*[^*]+\*\*|```|^\s*#/m.test(content);
    if (hasMarkdown) {
      rendered = sanitizeHtml(md.render(content));
    } else {
      const hasInlineHtml = /<([a-z][\s\S]*?)>/i.test(content);
      if (hasInlineHtml) {
        rendered = sanitizeHtml(md.render(content));
      } else {
        rendered = linkifyPlainText(content);
      }
    }
  }
  if (renderCache.size >= maxRenderCacheSize) {
    renderCache.clear();
  }
  renderCache.set(content, rendered);
  return rendered;
}

export function renderMarkdown(content: string): string {
  if (!content) return '';
  content = content.replace(/#\s*\{\s*domainURL\s*\}/g, globSetting.domainUrl);
  const cached = renderCache.get(content);
  if (cached) {
    return cached;
  }
  try {
    const rendered = sanitizeHtml(md.render(content));
    if (renderCache.size >= maxRenderCacheSize) {
      renderCache.clear();
    }
    renderCache.set(content, rendered);
    return rendered;
  } catch (e) {
    console.error('Markdown渲染失败', e);
    return renderMessage(content);
  }
}

export function clearRenderCache() {
  renderCache.clear();
}
