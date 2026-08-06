package com.zyt.flowerkisstao.shared.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绑定 application.yml 中的 app.jwt.* 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** 签名密钥，HS256 要求原文至少 32 字节 */
    private String secret;

    /** 访问令牌有效期，单位分钟 */
    private long expireMinutes = 120;

    /** 承载 token 的请求头 */
    private String header = "Authorization";

    /** token 前缀，注意尾部空格 */
    private String prefix = "Bearer ";
}
