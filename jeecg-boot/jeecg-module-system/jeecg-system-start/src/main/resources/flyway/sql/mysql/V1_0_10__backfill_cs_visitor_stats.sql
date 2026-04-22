-- ============================================================================
-- 回填 cs_visitor 访问统计字段（修复历史 firstVisitTime 为空、visitCount/conversationCount 不准的数据）
--
-- 背景：
--   旧版本 createConversation 流程未触发 cs_visitor 写入，导致：
--     1) 已有客服手动备注的 visitor 行：first_visit_time / last_visit_time 缺失
--     2) 完全没有 visitor 记录的访客：cs_conversation 有数据，但 cs_visitor 无对应行
--
-- 修复策略：
--   1) 已有 visitor 行 → 仅在字段为空时回填，不覆盖客服手动写入的值
--   2) 完全缺失的访客 → 用 cs_conversation 聚合数据补建
-- ============================================================================

-- 1) 给已有 visitor 行回填首次/最后访问时间，并校正访问/会话计数
UPDATE cs_visitor v
JOIN (
    SELECT user_id,
           MIN(create_time) AS first_t,
           MAX(COALESCE(last_message_time, create_time)) AS last_t,
           COUNT(*) AS conv_cnt
    FROM cs_conversation
    WHERE user_id IS NOT NULL AND user_id <> ''
    GROUP BY user_id
) c ON c.user_id = v.user_id
SET v.first_visit_time   = COALESCE(v.first_visit_time, c.first_t),
    v.last_visit_time    = COALESCE(v.last_visit_time, c.last_t),
    v.conversation_count = c.conv_cnt,
    v.visit_count        = GREATEST(COALESCE(v.visit_count, 0), c.conv_cnt);

-- 2) 给从未建过 visitor 但有会话历史的访客补建记录
INSERT INTO cs_visitor (id, app_id, user_id, nickname,
                        first_visit_time, last_visit_time,
                        visit_count, conversation_count,
                        level, star, gender, create_time)
SELECT REPLACE(UUID(), '-', ''),
       MAX(c.app_id),
       c.user_id,
       MAX(c.user_name),
       MIN(c.create_time),
       MAX(COALESCE(c.last_message_time, c.create_time)),
       COUNT(*),
       COUNT(*),
       1, 0, 0, NOW()
FROM cs_conversation c
LEFT JOIN cs_visitor v ON v.user_id = c.user_id
WHERE v.id IS NULL
  AND c.user_id IS NOT NULL AND c.user_id <> ''
GROUP BY c.user_id;
