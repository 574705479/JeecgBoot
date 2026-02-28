-- 将 depends_on 从旧格式 ["svc1","svc2"] 转换为新格式 [{"service":"svc1"},{"service":"svc2"}]
UPDATE docker_service ds
SET ds.depends_on = (
    SELECT JSON_ARRAYAGG(JSON_OBJECT('service', jt.svc))
    FROM JSON_TABLE(ds.depends_on, '$[*]' COLUMNS (svc VARCHAR(200) PATH '$')) AS jt
)
WHERE ds.depends_on IS NOT NULL
  AND JSON_LENGTH(ds.depends_on) > 0
  AND JSON_TYPE(JSON_EXTRACT(ds.depends_on, '$[0]')) = 'STRING';
