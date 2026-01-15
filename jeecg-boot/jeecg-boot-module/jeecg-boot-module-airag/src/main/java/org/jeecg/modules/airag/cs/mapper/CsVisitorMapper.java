package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.airag.cs.entity.CsVisitor;

/**
 * 访客信息Mapper
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Mapper
public interface CsVisitorMapper extends BaseMapper<CsVisitor> {

    /**
     * 根据appId和userId查询访客
     */
    @Select("SELECT * FROM cs_visitor WHERE app_id = #{appId} AND user_id = #{userId}")
    CsVisitor selectByAppAndUser(@Param("appId") String appId, @Param("userId") String userId);

    /**
     * 根据userId查询访客（不指定appId，取最新创建的一条）
     * 用于新版客服系统（不再依赖appId）
     */
    @Select("SELECT * FROM cs_visitor WHERE user_id = #{userId} ORDER BY create_time DESC LIMIT 1")
    CsVisitor selectByUserId(@Param("userId") String userId);

    /**
     * 更新访客访问次数和最后访问时间
     */
    @Update("UPDATE cs_visitor SET visit_count = visit_count + 1, last_visit_time = NOW() WHERE id = #{id}")
    int updateVisitInfo(@Param("id") String id);

    /**
     * 更新会话数
     */
    @Update("UPDATE cs_visitor SET conversation_count = conversation_count + 1 WHERE id = #{id}")
    int incrementConversationCount(@Param("id") String id);

    /**
     * 切换星标状态
     */
    @Update("UPDATE cs_visitor SET star = IF(star = 0, 1, 0), update_time = NOW() WHERE id = #{id}")
    int toggleStar(@Param("id") String id);

    /**
     * 分页查询访客列表
     */
    @Select("<script>" +
            "SELECT v.*, " +
            "(SELECT COUNT(*) FROM cs_conversation c WHERE c.user_id = v.user_id AND c.app_id = v.app_id) as conversation_count " +
            "FROM cs_visitor v " +
            "WHERE 1=1 " +
            "<if test='appId != null and appId != \"\"'> AND v.app_id = #{appId} </if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (v.nickname LIKE CONCAT('%',#{keyword},'%') OR v.real_name LIKE CONCAT('%',#{keyword},'%') OR v.phone LIKE CONCAT('%',#{keyword},'%') OR v.user_id LIKE CONCAT('%',#{keyword},'%')) </if>" +
            "<if test='level != null'> AND v.level = #{level} </if>" +
            "<if test='star != null'> AND v.star = #{star} </if>" +
            "ORDER BY v.last_visit_time DESC" +
            "</script>")
    IPage<CsVisitor> selectVisitorPage(Page<CsVisitor> page, 
                                        @Param("appId") String appId,
                                        @Param("keyword") String keyword,
                                        @Param("level") Integer level,
                                        @Param("star") Integer star);
}
