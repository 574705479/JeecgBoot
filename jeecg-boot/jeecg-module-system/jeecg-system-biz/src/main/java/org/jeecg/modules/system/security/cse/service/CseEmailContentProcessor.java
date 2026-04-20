package org.jeecg.modules.system.security.cse.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件 HTML 中 cse:// 图片的服务端预处理：
 *  - 把 {@code <img src="cse://{fid}">} 替换为 {@code <img src="cid:cse_{n}">}
 *  - 用 {@link MimeMessageHelper#addInline(String, jakarta.mail.util.DataSource)} 内嵌解密后的图片字节
 *  - 走 multipart/related，体积比 base64 内嵌小约 25%，且兼容主流邮件客户端
 *
 * 兼容性要点：
 *  - Outlook (Windows/Mac/Web) ✓
 *  - Gmail (Web/iOS/Android) ✓
 *  - Foxmail / 网易 / QQ 邮箱 ✓
 *  - 失败渠道：极少数老式 Webmail 不支持 cid: 引用，此时图片不显示但不影响正文
 *
 * 用法：
 * <pre>
 *   String processed = cseEmailContentProcessor.process(content, helper);
 *   helper.setText(processed, true);
 * </pre>
 */
@Slf4j
@Service
public class CseEmailContentProcessor {

    private static final Pattern IMG_CSE_SRC = Pattern.compile(
            "<img\\b([^>]*?)\\bsrc=\"(cse://[^\"]+)\"([^>]*)>",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern CSE_FID_FROM_URL = Pattern.compile("^cse://([^?#]+)");

    @Autowired
    private CseAwareImageReader cseAwareImageReader;

    /**
     * 扫描 HTML，把 cse:// 图替换为 cid: 引用，同时把解密字节注册到 helper 作为内联附件。
     *
     * @param html  原始 HTML（可能含 0..N 个 <img src="cse://...">）
     * @param helper 必须以 multipart 模式构造（new MimeMessageHelper(message, true)）
     * @return 处理后的 HTML（cid 引用版本）
     */
    public String process(String html, MimeMessageHelper helper) {
        if (html == null || html.isEmpty() || helper == null) return html;
        if (html.indexOf("cse://") < 0) return html;

        Matcher m = IMG_CSE_SRC.matcher(html);
        StringBuffer out = new StringBuffer();
        Map<String, String> seen = new LinkedHashMap<>(); // cseUrl -> cid
        while (m.find()) {
            String beforeAttr = m.group(1);
            String cseUrl = m.group(2);
            String afterAttr = m.group(3);
            String cid = seen.get(cseUrl);
            if (cid == null) {
                cid = "cse_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                Matcher fidM = CSE_FID_FROM_URL.matcher(cseUrl);
                if (!fidM.find()) {
                    log.warn("[CseEmailContentProcessor] 解析 fid 失败 url={}", cseUrl);
                    m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                    continue;
                }
                String fid = fidM.group(1);
                byte[] bytes = cseAwareImageReader.readBytesByFid(fid);
                if (bytes == null || bytes.length == 0) {
                    log.warn("[CseEmailContentProcessor] cse 解密失败 fid={}", fid);
                    m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                    continue;
                }
                try {
                    String mime = guessMime(bytes);
                    helper.addInline(cid, new ByteArrayDataSource(bytes, mime));
                    seen.put(cseUrl, cid);
                } catch (MessagingException e) {
                    log.warn("[CseEmailContentProcessor] addInline 失败 fid={} err={}", fid, e.getMessage());
                    m.appendReplacement(out, Matcher.quoteReplacement(m.group(0)));
                    continue;
                }
            }
            String replacement = "<img" + beforeAttr + "src=\"cid:" + cid + "\"" + afterAttr + ">";
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** 简单 MIME 嗅探（只覆盖常见图片格式），失败回退 image/png */
    private String guessMime(byte[] bytes) {
        if (bytes.length >= 4) {
            // PNG
            if (bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return "image/png";
            // JPEG
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return "image/jpeg";
            // GIF
            if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') return "image/gif";
            // WebP（RIFF....WEBP）
            if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes.length >= 12
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        }
        return "image/png";
    }
}
