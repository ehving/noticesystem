package com.notice.system.service.impl;

import com.notice.system.security.PasswordPolicy;
import com.notice.system.service.PasswordService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class PasswordServiceImpl implements PasswordService {

    @Override
    public boolean isWeak(String rawPassword) {
        return PasswordPolicy.isWeak(rawPassword);
    }

    @Override
    public void assertStrong(String rawPassword) {
        if (isWeak(rawPassword)) {
            throw new IllegalArgumentException("密码太弱，至少 6 位，并包含字母和数字");
        }
    }

    @Override
    public String prepareForStore(String rawPassword) {
        // 当前作业阶段：不做加密，明文存储
        return rawPassword == null ? null : rawPassword.trim();
    }

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        // 当前作业阶段：明文比较
        return Objects.equals(
                rawPassword == null ? null : rawPassword.trim(),
                storedPassword
        );
    }
}

