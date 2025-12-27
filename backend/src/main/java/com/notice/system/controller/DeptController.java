package com.notice.system.controller;

import com.notice.system.converter.DeptConverter;
import com.notice.system.entity.Dept;
import com.notice.system.common.Result;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.service.AuthService;
import com.notice.system.service.DeptService;
import com.notice.system.vo.dept.DeptOptionVo;
import com.notice.system.vo.dept.DeptTreeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 部门前台接口：
 *  - 只提供和页面展示相关的查询，不允许写操作
 */
@RestController
@RequestMapping("/api/dept")
@RequiredArgsConstructor
public class DeptController {

    private final AuthService authService;
    private final DeptService deptService;

    /**
     * 部门下拉选项：
     *  - 列出所有启用状态部门,供用户选择部门用
     */
    @GetMapping("/options")
    public Result<List<DeptOptionVo>> listOptions() {
        // 必须登录，但不限制角色
        authService.requireLoginUser();

        List<Dept> enabledList = deptService.listEnabled();
        List<DeptOptionVo> voList = DeptConverter.toOptionList(enabledList);
        return Result.success(voList);
    }





    /**
     * 部门树：
     *  - 使用已启用部门构建树形结构
     */
    @GetMapping("/tree")
    public Result<List<DeptTreeVo>> getTree() {
        authService.requireLoginUser();

        List<Dept> enabled = deptService.listEnabled();
        List<DeptTreeVo> tree = deptService.buildTree(enabled);
        return Result.success(tree);
    }


}

