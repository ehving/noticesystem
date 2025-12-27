package com.notice.system.sync;

import com.notice.system.entityEnum.SyncLogStatus;

public final class SyncStatusDecider {

    private SyncStatusDecider() {}

    /**
     * 异常 -> FAILED / ERROR
     * FAILED：可重试的瞬态错误
     * ERROR：不可重试的结构/规则错误
     */
    public static SyncLogStatus decideOnException(Throwable ex) {
        if (ex == null) return SyncLogStatus.FAILED;

        String msg = String.valueOf(ex.getMessage()).toLowerCase();

        // ===== 可重试（FAILED）=====
        if (containsAny(msg,
                "timeout", "timed out",
                "connection", "could not open connection",
                "deadlock", "lock wait",
                "temporarily", "too many connections",
                "server is down", "broken pipe")) {
            return SyncLogStatus.FAILED;
        }

        // ===== 不可重试（ERROR）=====
        if (containsAny(msg,
                "foreign key", "violates foreign key",
                "not-null", "cannot be null",
                "data too long", "value too long",
                "bad sql grammar", "syntax error",
                "type mismatch", "cannot cast",
                "column", "unknown column")) {
            return SyncLogStatus.ERROR;
        }

        // 默认保守：给 FAILED，让它走重试上限再转 ERROR（更稳）
        return SyncLogStatus.FAILED;
    }

    private static boolean containsAny(String s, String... keys) {
        if (s == null) return false;
        for (String k : keys) {
            if (k != null && !k.isBlank() && s.contains(k)) return true;
        }
        return false;
    }
}


