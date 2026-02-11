package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.api.ISysBaseAPI;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.PasswordUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.mapper.CsSubAgentMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.*;

/**
 * 子客服管理Controller
 * 管理员客服可以创建、编辑、删除子客服
 *
 * @author jeecg
 * @date 2026-02-06
 */
@Slf4j
@Tag(name = "子客服管理")
@RestController
@RequestMapping("/cs/sub-agent")
public class CsSubAgentController {

    @Autowired
    private ICsAgentService csAgentService;

    @Autowired
    private CsSubAgentMapper csSubAgentMapper;

    @Autowired
    private ISysBaseAPI sysBaseAPI;

    /** 子客服默认角色编码 */
    private static final String SUB_AGENT_ROLE_CODE = "cs_sub_agent";

    /**
     * 判断当前登录用户是否为系统超级管理员
     */
    private boolean isSystemAdmin() {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        return loginUser != null && "admin".equals(loginUser.getUsername());
    }

    /**
     * 获取当前管理员客服信息
     */
    private CsAgent getCurrentSupervisor() {
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        if (loginUser == null) {
            return null;
        }
        CsAgent agent = csAgentService.getByUserId(loginUser.getId());
        if (agent == null || !agent.checkSupervisor()) {
            return null;
        }
        return agent;
    }

    /**
     * 分页列表查询 - 查询当前管理员客服的子客服列表
     * admin 可查看所有子客服
     */
    @Operation(summary = "子客服列表")
    @GetMapping("/list")
    public Result<IPage<CsAgent>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "nickname", required = false) String nickname) {
        boolean admin = isSystemAdmin();
        CsAgent supervisor = getCurrentSupervisor();
        if (supervisor == null && !admin) {
            return Result.error("无权限，仅管理员客服可操作");
        }
        LambdaQueryWrapper<CsAgent> qw = new LambdaQueryWrapper<>();
        if (admin) {
            // admin: 查看所有子客服（parentAgentId 不为空的记录）
            qw.isNotNull(CsAgent::getParentAgentId).ne(CsAgent::getParentAgentId, "");
        } else {
            // 管理员客服: 只看自己创建的子客服
            qw.eq(CsAgent::getParentAgentId, supervisor.getId());
        }
        if (oConvertUtils.isNotEmpty(nickname)) {
            qw.like(CsAgent::getNickname, nickname);
        }
        qw.orderByDesc(CsAgent::getCreateTime);
        Page<CsAgent> page = new Page<>(pageNo, pageSize);
        IPage<CsAgent> pageList = csAgentService.page(page, qw);
        // 填充登录账号（从sys_user获取）
        for (CsAgent agent : pageList.getRecords()) {
            if (oConvertUtils.isNotEmpty(agent.getUserId())) {
                String username = csSubAgentMapper.getUsernameByUserId(agent.getUserId());
                if (username != null) {
                    agent.setUsername(username);
                }
            }
        }
        return Result.OK(pageList);
    }

    /**
     * 添加子客服
     * admin 可通过 parentAgentId 指定归属管理员客服
     */
    @Operation(summary = "添加子客服")
    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> add(@RequestBody SubAgentForm form) {
        boolean admin = isSystemAdmin();
        CsAgent supervisor = getCurrentSupervisor();
        if (supervisor == null && !admin) {
            return Result.error("无权限，仅管理员客服可操作");
        }

        // 确定子客服归属的 parentAgentId
        String parentAgentId;
        if (admin && supervisor == null) {
            // admin 不是管理员客服，需要通过表单指定归属
            if (oConvertUtils.isNotEmpty(form.getParentAgentId())) {
                CsAgent parentAgent = csAgentService.getById(form.getParentAgentId());
                if (parentAgent == null || !parentAgent.checkSupervisor()) {
                    return Result.error("指定的管理员客服不存在或不是管理员角色");
                }
                parentAgentId = form.getParentAgentId();
            } else {
                // admin 不指定归属管理员时，查找第一个管理员客服作为默认归属
                LambdaQueryWrapper<CsAgent> adminQw = new LambdaQueryWrapper<>();
                adminQw.eq(CsAgent::getRole, CsAgent.ROLE_SUPERVISOR).last("LIMIT 1");
                CsAgent firstAdmin = csAgentService.getOne(adminQw);
                if (firstAdmin == null) {
                    return Result.error("系统中暂无管理员客服，请先创建管理员客服");
                }
                parentAgentId = firstAdmin.getId();
            }
        } else {
            parentAgentId = supervisor.getId();
        }

        // 1. 校验用户名是否已存在
        if (oConvertUtils.isEmpty(form.getUsername())) {
            return Result.error("用户名不能为空");
        }
        if (oConvertUtils.isEmpty(form.getPassword())) {
            return Result.error("密码不能为空");
        }
        int existCount = csSubAgentMapper.countByUsername(form.getUsername());
        if (existCount > 0) {
            return Result.error("用户名已存在: " + form.getUsername());
        }

        // 2. 创建 sys_user 记录
        String sysUserId = UUID.randomUUID().toString().replace("-", "");
        String salt = oConvertUtils.randomGen(8);
        String passwordEncode = PasswordUtil.encrypt(form.getUsername(), form.getPassword(), salt);

        csSubAgentMapper.insertSysUser(sysUserId, form.getUsername(),
                oConvertUtils.isNotEmpty(form.getNickname()) ? form.getNickname() : form.getUsername(),
                passwordEncode, salt, form.getPhone(), form.getEmail());

        // 3. 查询子客服角色ID，分配角色
        String roleId = csSubAgentMapper.getRoleIdByCode(SUB_AGENT_ROLE_CODE);
        if (oConvertUtils.isNotEmpty(roleId)) {
            String userRoleId = UUID.randomUUID().toString().replace("-", "");
            csSubAgentMapper.insertSysUserRole(userRoleId, sysUserId, roleId);
        } else {
            log.warn("[CS-SubAgent] 未找到角色: {}", SUB_AGENT_ROLE_CODE);
        }

        // 4. 创建 cs_agent 记录
        CsAgent subAgent = new CsAgent();
        subAgent.setUserId(sysUserId);
        subAgent.setNickname(oConvertUtils.isNotEmpty(form.getNickname()) ? form.getNickname() : form.getUsername());
        subAgent.setAvatar(form.getAvatar());
        subAgent.setMaxSessions(form.getMaxSessions() != null ? form.getMaxSessions() : 10);
        subAgent.setWelcomeMessage(form.getWelcomeMessage());
        subAgent.setStatus(CsAgent.STATUS_OFFLINE);
        subAgent.setCurrentSessions(0);
        subAgent.setTotalServed(0);
        subAgent.setRole(CsAgent.ROLE_AGENT); // 子客服固定为普通客服
        subAgent.setParentAgentId(parentAgentId);
        subAgent.setAllowedMenus(form.getAllowedMenus());
        subAgent.setCreateBy(((LoginUser) SecurityUtils.getSubject().getPrincipal()).getUsername());
        subAgent.setCreateTime(new Date());
        csAgentService.save(subAgent);

        return Result.OK("添加成功！");
    }

    /**
     * 编辑子客服
     */
    @Operation(summary = "编辑子客服")
    @PutMapping("/edit")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> edit(@RequestBody SubAgentForm form) {
        boolean admin = isSystemAdmin();
        CsAgent supervisor = getCurrentSupervisor();
        if (supervisor == null && !admin) {
            return Result.error("无权限，仅管理员客服可操作");
        }
        if (oConvertUtils.isEmpty(form.getId())) {
            return Result.error("ID不能为空");
        }
        CsAgent subAgent = csAgentService.getById(form.getId());
        if (subAgent == null) {
            return Result.error("子客服不存在");
        }
        // admin 跳过归属校验，管理员客服需校验
        if (!admin && !supervisor.getId().equals(subAgent.getParentAgentId())) {
            return Result.error("无权限编辑该子客服");
        }

        // 更新 cs_agent
        if (oConvertUtils.isNotEmpty(form.getNickname())) {
            subAgent.setNickname(form.getNickname());
            // 同步更新 sys_user 的 realname
            if (oConvertUtils.isNotEmpty(subAgent.getUserId())) {
                csSubAgentMapper.updateSysUserRealname(subAgent.getUserId(), form.getNickname());
            }
        }
        if (form.getMaxSessions() != null) {
            subAgent.setMaxSessions(form.getMaxSessions());
        }
        if (form.getAvatar() != null) {
            subAgent.setAvatar(form.getAvatar());
        }
        if (form.getWelcomeMessage() != null) {
            subAgent.setWelcomeMessage(form.getWelcomeMessage());
        }
        if (form.getAllowedMenus() != null) {
            subAgent.setAllowedMenus(form.getAllowedMenus());
        }
        if (oConvertUtils.isNotEmpty(form.getPhone()) && oConvertUtils.isNotEmpty(subAgent.getUserId())) {
            csSubAgentMapper.updateSysUserPhone(subAgent.getUserId(), form.getPhone());
        }
        if (oConvertUtils.isNotEmpty(form.getEmail()) && oConvertUtils.isNotEmpty(subAgent.getUserId())) {
            csSubAgentMapper.updateSysUserEmail(subAgent.getUserId(), form.getEmail());
        }
        subAgent.setUpdateBy(((LoginUser) SecurityUtils.getSubject().getPrincipal()).getUsername());
        subAgent.setUpdateTime(new Date());
        csAgentService.updateById(subAgent);

        return Result.OK("编辑成功！");
    }

    /**
     * 删除子客服
     */
    @Operation(summary = "删除子客服")
    @DeleteMapping("/delete")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> delete(@RequestParam(name = "id") String id) {
        boolean admin = isSystemAdmin();
        CsAgent supervisor = getCurrentSupervisor();
        if (supervisor == null && !admin) {
            return Result.error("无权限，仅管理员客服可操作");
        }
        CsAgent subAgent = csAgentService.getById(id);
        if (subAgent == null) {
            return Result.error("子客服不存在");
        }
        // admin 跳过归属校验
        if (!admin && !supervisor.getId().equals(subAgent.getParentAgentId())) {
            return Result.error("无权限删除该子客服");
        }

        // 删除 sys_user_role
        if (oConvertUtils.isNotEmpty(subAgent.getUserId())) {
            csSubAgentMapper.deleteSysUserRoleByUserId(subAgent.getUserId());
            // 逻辑删除 sys_user (设置 del_flag=1)
            csSubAgentMapper.logicDeleteSysUser(subAgent.getUserId());
        }
        // 删除 cs_agent
        csAgentService.removeById(id);
        return Result.OK("删除成功！");
    }

    /**
     * 重置子客服密码
     */
    @Operation(summary = "重置子客服密码")
    @PutMapping("/resetPassword")
    public Result<String> resetPassword(@RequestBody Map<String, String> params) {
        boolean admin = isSystemAdmin();
        CsAgent supervisor = getCurrentSupervisor();
        if (supervisor == null && !admin) {
            return Result.error("无权限，仅管理员客服可操作");
        }
        String id = params.get("id");
        String newPassword = params.get("newPassword");
        if (oConvertUtils.isEmpty(id) || oConvertUtils.isEmpty(newPassword)) {
            return Result.error("参数不完整");
        }

        CsAgent subAgent = csAgentService.getById(id);
        if (subAgent == null) {
            return Result.error("子客服不存在");
        }
        // admin 跳过归属校验
        if (!admin && (supervisor == null || !supervisor.getId().equals(subAgent.getParentAgentId()))) {
            return Result.error("无权限操作该子客服");
        }

        // 获取用户名
        String username = csSubAgentMapper.getUsernameByUserId(subAgent.getUserId());
        if (oConvertUtils.isEmpty(username)) {
            return Result.error("未找到关联的系统用户");
        }
        String salt = oConvertUtils.randomGen(8);
        String passwordEncode = PasswordUtil.encrypt(username, newPassword, salt);
        csSubAgentMapper.updateSysUserPassword(subAgent.getUserId(), passwordEncode, salt);
        return Result.OK("密码重置成功！");
    }

    /**
     * 获取子客服详情（含用户名信息）
     */
    @Operation(summary = "子客服详情")
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam(name = "id") String id) {
        boolean admin = isSystemAdmin();
        CsAgent supervisor = getCurrentSupervisor();
        if (supervisor == null && !admin) {
            return Result.error("无权限");
        }
        CsAgent subAgent = csAgentService.getById(id);
        if (subAgent == null) {
            return Result.error("子客服不存在");
        }
        // admin 跳过归属校验
        if (!admin && (supervisor == null || !supervisor.getId().equals(subAgent.getParentAgentId()))) {
            return Result.error("子客服不存在或无权限");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agent", subAgent);
        if (oConvertUtils.isNotEmpty(subAgent.getUserId())) {
            String username = csSubAgentMapper.getUsernameByUserId(subAgent.getUserId());
            result.put("username", username);
            LoginUser sysUser = sysBaseAPI.getUserById(subAgent.getUserId());
            if (sysUser != null) {
                result.put("phone", sysUser.getPhone());
                result.put("email", sysUser.getEmail());
            }
        }
        return Result.OK(result);
    }

    /**
     * 获取可分配的菜单树（在线客服相关菜单）
     */
    @Operation(summary = "获取可分配菜单列表")
    @GetMapping("/menus")
    public Result<List<Map<String, Object>>> getAssignableMenus() {
        boolean admin = isSystemAdmin();
        CsAgent supervisor = getCurrentSupervisor();
        if (supervisor == null && !admin) {
            return Result.error("无权限");
        }
        List<Map<String, Object>> menus = csSubAgentMapper.getCsMenuTree();
        return Result.OK(menus);
    }

    // ==================== 表单对象 ====================

    @Data
    public static class SubAgentForm implements Serializable {
        /** 子客服agent ID(编辑时使用) */
        private String id;
        /** 登录用户名 */
        private String username;
        /** 登录密码 */
        private String password;
        /** 昵称 */
        private String nickname;
        /** 手机号 */
        private String phone;
        /** 邮箱 */
        private String email;
        /** 头像URL */
        private String avatar;
        /** 最大同时接待数 */
        private Integer maxSessions;
        /** 欢迎语 */
        private String welcomeMessage;
        /** 可见菜单列表(JSON数组) */
        private String allowedMenus;
        /** 归属管理员客服ID(admin添加子客服时可指定) */
        private String parentAgentId;
    }
}
