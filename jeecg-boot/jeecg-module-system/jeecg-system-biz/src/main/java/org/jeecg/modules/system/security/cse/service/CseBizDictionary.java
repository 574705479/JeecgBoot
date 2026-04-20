package org.jeecg.modules.system.security.cse.service;

import java.util.List;

/**
 * CSE 业务路径硬编码字典。
 *
 * 设计取舍：bizPath 是各 controller 里硬编码的（avatar/cs-visitor/airag/comment/...），
 * 运维不可能凭空"加一个新业务"就让它生效——必须先发版，后端代码端要有人对接。
 * 把字典放代码里反而更安全：review 严格 + 不会出现"运维瞎加路径但代码端没人接收"。
 *
 * forceLocked=true 含义：前端复选框 disabled、不允许取消；后端 save 时强制把它 union
 * 进对应列表（即使前端漏传也兜底），且**禁止出现在另一个列表里**（双向护栏）。
 *
 * 字典 12 项 = 9 ENCRYPT + 3 PUBLIC，与 application-prod.yml 兜底配置完全对齐。
 *
 * 历史死配置（已剔除，不在字典中）：rich/、leave-msg/、sys-comment/、brand/、chat/
 */
public final class CseBizDictionary {

    private CseBizDictionary() {}

    public enum Category {
        /** 加密业务（白名单）：命中即走 CSE 加密链路，返回 cse://{fid} */
        ENCRYPT,
        /** 公开业务（黑名单）：命中即明文上传（黑名单优先级高于白名单） */
        PUBLIC
    }

    public record BizDef(
            String path,
            String name,
            String description,
            Category category,
            boolean forceLocked
    ) {}

    public static final List<BizDef> ALL = List.of(
            // ── ENCRYPT (9 项) ────────────────────────────────────────────────────
            new BizDef("avatar/",      "用户/客服头像",
                    "登录用户头像、客服头像（跨租户共享）。删除会导致头像可被未登录用户读取，系统强制锁定。",
                    Category.ENCRYPT, true),
            new BizDef("cs-brand/",    "客服品牌 LOGO/背景",
                    "聊天窗口 LOGO/背景图，加密存储 + 通过 /cs/brand/file/{fid} 匿名代理端点解密下发。" +
                            "fid 必须真实出现在 brand 字段中才放行（CsBrandFidWhitelist 校验），可防 fid 遍历。系统强制锁定。",
                    Category.ENCRYPT, true),
            new BizDef("cs-visitor/",  "客服-访客上传",
                    "访客在客服窗口发的图/视频/PDF 等附件",
                    Category.ENCRYPT, false),
            new BizDef("airag/",       "客服-客服侧上传",
                    "客服在工作台上传的资源（聊天附件、内部素材等）",
                    Category.ENCRYPT, false),
            new BizDef("comment/",     "评论附件",
                    "评论模块的图/附件（useComment.ts 上传通道）",
                    Category.ENCRYPT, false),
            new BizDef("jeditor/",     "Tinymce 富文本图",
                    "Tinymce 富文本编辑器内插的图（Editor.vue / ImgUpload.vue）",
                    Category.ENCRYPT, false),
            new BizDef("markdown/",    "Markdown 富文本图",
                    "Markdown 编辑器内插的图（Markdown.vue）",
                    Category.ENCRYPT, false),
            new BizDef("import/",      "数据导入文件",
                    "导入 Excel/CSV 等业务数据文件（ImportFileServiceImpl）",
                    Category.ENCRYPT, false),
            new BizDef("temp/",        "临时上传（默认通道）",
                    "JUpload/JImageUpload 未指定 bizPath 时的默认值，覆盖通知附件、动态表单等所有兜底场景",
                    Category.ENCRYPT, false),

            // ── PUBLIC (3 项) ─────────────────────────────────────────────────────
            new BizDef("public/",      "公开示例资源",
                    "公开示例图，无需登录即可访问",
                    Category.PUBLIC, true),
            new BizDef("captcha/",     "图形验证码",
                    "登录验证码图，登录前必须能匿名访问，系统强制锁定",
                    Category.PUBLIC, true),
            new BizDef("appVersion/",  "APP 升级包",
                    "Android/iOS 升级 APK，必须允许用户公开下载（无解密代理端点），系统强制锁定",
                    Category.PUBLIC, true)
    );

    /** 取指定分类下的所有路径（含强制锁项），用于服务端 save 时 union 兜底 */
    public static java.util.Set<String> forceLockedPaths(Category category) {
        java.util.Set<String> r = new java.util.LinkedHashSet<>();
        for (BizDef d : ALL) {
            if (d.category() == category && d.forceLocked()) {
                r.add(d.path());
            }
        }
        return r;
    }

    /** 双向护栏使用：取另一类别下的所有强制锁路径，禁止它们出现在本类别 */
    public static java.util.Set<String> opposingForceLockedPaths(Category category) {
        Category opp = (category == Category.ENCRYPT) ? Category.PUBLIC : Category.ENCRYPT;
        return forceLockedPaths(opp);
    }
}
