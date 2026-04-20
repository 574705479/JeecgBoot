package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.airag.cs.entity.CsBrandConfig;

/**
 * 客服系统品牌配置 Mapper
 *
 * @author jeecg
 * @date 2026-01-20
 */
public interface CsBrandConfigMapper extends BaseMapper<CsBrandConfig> {

    /**
     * 检查 fid 是否真实出现在 brand 字段中（精确匹配，避免 SQL 通配符注入）。
     * 仅查 del_flag=0 AND status=1 的有效记录。
     *
     * @param fid 文件 ID（不含 cse:// 前缀）
     * @return >0 表示存在
     */
    @Select("SELECT COUNT(1) FROM cs_brand_config "
            + "WHERE del_flag = 0 AND status = 1 "
            + "AND ( logo_url = CONCAT('cse://', #{fid}) "
            + "   OR favicon_url = CONCAT('cse://', #{fid}) "
            + "   OR login_bg_url = CONCAT('cse://', #{fid}) )")
    int existsByFid(@Param("fid") String fid);
}
