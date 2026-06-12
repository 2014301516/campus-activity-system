package com.cas.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 微信小程序登录请求
 */
@Data
public class WxLoginDTO {

    @NotBlank(message = "code不能为空")
    private String code;
}
