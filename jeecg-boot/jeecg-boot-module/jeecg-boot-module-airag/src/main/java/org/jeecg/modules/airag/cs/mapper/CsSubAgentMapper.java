package org.jeecg.modules.airag.cs.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * 子客服管理Mapper
 * 直接操作 sys_user、sys_user_role 等系统表
 *
 * @author jeecg
 * @date 2026-02-06
 */
@Mapper
public interface CsSubAgentMapper {

    /**
     * 查询用户名是否已存在
     */
    @Select("SELECT COUNT(1) FROM sys_user WHERE username = #{username} AND del_flag = '0'")
    int countByUsername(@Param("username") String username);

    /**
     * 插入系统用户
     */
    @Insert("INSERT INTO sys_user (id, username, realname, password, salt, phone, email, status, del_flag, create_time, activiti_sync) " +
            "VALUES (#{id}, #{username}, #{realname}, #{password}, #{salt}, #{phone}, #{email}, 1, '0', NOW(), 1)")
    void insertSysUser(@Param("id") String id,
                        @Param("username") String username,
                        @Param("realname") String realname,
                        @Param("password") String password,
                        @Param("salt") String salt,
                        @Param("phone") String phone,
                        @Param("email") String email);

    /**
     * 根据角色编码获取角色ID
     */
    @Select("SELECT id FROM sys_role WHERE role_code = #{roleCode} LIMIT 1")
    String getRoleIdByCode(@Param("roleCode") String roleCode);

    /**
     * 插入用户角色关联
     */
    @Insert("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (#{id}, #{userId}, #{roleId})")
    void insertSysUserRole(@Param("id") String id,
                            @Param("userId") String userId,
                            @Param("roleId") String roleId);

    /**
     * 根据用户ID获取用户名
     */
    @Select("SELECT username FROM sys_user WHERE id = #{userId}")
    String getUsernameByUserId(@Param("userId") String userId);

    /**
     * 更新系统用户真实姓名
     */
    @Update("UPDATE sys_user SET realname = #{realname}, update_time = NOW() WHERE id = #{userId}")
    void updateSysUserRealname(@Param("userId") String userId, @Param("realname") String realname);

    /**
     * 更新系统用户手机号
     */
    @Update("UPDATE sys_user SET phone = #{phone}, update_time = NOW() WHERE id = #{userId}")
    void updateSysUserPhone(@Param("userId") String userId, @Param("phone") String phone);

    /**
     * 更新系统用户邮箱
     */
    @Update("UPDATE sys_user SET email = #{email}, update_time = NOW() WHERE id = #{userId}")
    void updateSysUserEmail(@Param("userId") String userId, @Param("email") String email);

    /**
     * 更新系统用户密码
     */
    @Update("UPDATE sys_user SET password = #{password}, salt = #{salt}, update_time = NOW() WHERE id = #{userId}")
    void updateSysUserPassword(@Param("userId") String userId, @Param("password") String password, @Param("salt") String salt);

    /**
     * 删除用户角色关联
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    void deleteSysUserRoleByUserId(@Param("userId") String userId);

    /**
     * 逻辑删除系统用户
     */
    @Update("UPDATE sys_user SET del_flag = '1', update_time = NOW() WHERE id = #{userId}")
    void logicDeleteSysUser(@Param("userId") String userId);

    /**
     * 检查用户是否为客服角色（管理员客服或子客服）
     */
    @Select("SELECT COUNT(1) FROM sys_user u " +
            "JOIN sys_user_role ur ON u.id = ur.user_id " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE u.username = #{username} AND u.del_flag = '0' " +
            "AND r.role_code IN ('cs_admin_agent', 'cs_sub_agent')")
    int isAgentUser(@Param("username") String username);

    /**
     * 获取在线客服相关菜单树（用于子客服菜单分配）
     * 排除：团队管理(cs_sub_agent)、客服管理(cs_agent) - 子客服不应看到这两个菜单
     */
    @Select("SELECT id, parent_id AS parentId, name, url, component, icon, sort_no AS sortNo, menu_type AS menuType " +
            "FROM sys_permission " +
            "WHERE del_flag = 0 " +
            "AND id NOT IN ('cs_sub_agent', 'cs_agent', 'cs_agent_ip_whitelist', 'cs_agent_login_log') " +
            "AND (url LIKE '/cs/%' OR url LIKE '/security/%' OR component LIKE 'super/airag/cs/%' OR id IN " +
            "(SELECT parent_id FROM sys_permission WHERE (url LIKE '/cs/%' OR url LIKE '/security/%' OR component LIKE 'super/airag/cs/%') AND del_flag = 0)) " +
            "ORDER BY sort_no ASC")
    List<Map<String, Object>> getCsMenuTree();
}
