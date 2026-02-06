package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.airag.cs.entity.CsIpGeoCache;

/**
 * IP地理位置缓存 Mapper
 *
 * @author jeecg
 * @date 2026-02-06
 */
@Mapper
public interface CsIpGeoCacheMapper extends BaseMapper<CsIpGeoCache> {
}
