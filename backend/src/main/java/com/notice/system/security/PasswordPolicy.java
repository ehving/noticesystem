package com.notice.system.security;

public final class PasswordPolicy {

    private PasswordPolicy() {}

    /**
     * 你现在的作业要求：不升级加密体系，这里只做“弱密码校验规则”
     */
    public static boolean isWeak(String pwd) {
        if (pwd == null) return true;
        String s = pwd.trim();
        if (s.length() < 6) return true;

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return !(hasLetter && hasDigit);
    }
}

