package com.cas.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.cas.common.Result;
import com.cas.entity.Registration;
import com.cas.service.RegistrationService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.List;

/**
 * 报名控制器
 */
@RestController
@RequestMapping("/api")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 报名活动
     */
    @PostMapping("/activity/{activityId}/register")
    public Result<Registration> register(@PathVariable Long activityId) {
        securityUtil.requireStudent();
        Long userId = securityUtil.getCurrentUserId();
        Registration registration = registrationService.register(userId, activityId);
        return Result.success("报名成功", registration);
    }

    /**
     * 取消报名
     */
    @DeleteMapping("/activity/{activityId}/register")
    public Result<?> cancelRegister(@PathVariable Long activityId) {
        securityUtil.requireStudent();
        Long userId = securityUtil.getCurrentUserId();
        registrationService.cancelRegister(userId, activityId);
        return Result.success("已取消报名");
    }

    /**
     * 我的报名列表
     */
    @GetMapping("/my-registrations")
    public Result<List<Registration>> myRegistrations() {
        securityUtil.requireStudent();
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(registrationService.getMyRegistrations(userId));
    }

    /**
     * 活动报名名单
     */
    @GetMapping("/activity/{activityId}/registrations")
    public Result<List<Registration>> activityRegistrations(@PathVariable Long activityId) {
        return Result.success(registrationService.getActivityRegistrations(activityId));
    }

    /** 导出报名名单为 Excel */
    @GetMapping("/activity/{activityId}/registrations/export")
    public void exportExcel(@PathVariable Long activityId, HttpServletResponse response) throws Exception {
        List<Registration> list = registrationService.getActivityRegistrations(activityId);
        String fileName = URLEncoder.encode("报名名单", "UTF-8") + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.addHeaderAlias("userName", "姓名");
        writer.addHeaderAlias("studentId", "学号");
        writer.addHeaderAlias("registeredAt", "报名时间");
        writer.addHeaderAlias("status", "状态");
        writer.setOnlyAlias(true);
        writer.write(list, true);
        writer.flush(response.getOutputStream());
        writer.close();
    }
}
