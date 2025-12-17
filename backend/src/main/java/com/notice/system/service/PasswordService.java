package com.notice.system.service;

/**
 * 密码相关统一入口：
 * - 强度校验
 * - 入库前处理（目前明文）
 * - 登录/改密时校验（目前明文 equals）
 *
 * 以后要升级加密，只改 impl 即可。
 */
public interface PasswordService {

    /** 是否弱密码（规则复用 PasswordPolicy） */
    boolean isWeak(String rawPassword);

    /** 若弱密码则抛异常/或由 Controller 自行返回 fail */
    void assertStrong(String rawPassword);

    /** 入库前处理：当前=原样返回（明文）；以后可改成 encode */
    String prepareForStore(String rawPassword);

    /** 校验：当前=明文 equals；以后可改成 matches */
    boolean matches(String rawPassword, String storedPassword);
}

