package com.notice.system.converter;

import com.notice.system.entity.Dept;
import com.notice.system.entity.NoticeRead;
import com.notice.system.entity.User;
import com.notice.system.vo.noticeread.NoticeReadUserVo;

public class NoticeReadConverter {

    private NoticeReadConverter() {
    }

    public static NoticeReadUserVo toUserReadVo(NoticeRead read,
                                                User user,
                                                Dept dept) {
        if (read == null) {
            return null;
        }
        NoticeReadUserVo vo = new NoticeReadUserVo();

        vo.setUserId(read.getUserId());
        vo.setReadTime(read.getReadTime());
        vo.setDeviceType(read.getDeviceType());

        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setDeptId(user.getDeptId());
        }

        if (dept != null) {
            vo.setDeptName(dept.getName());
        }

        return vo;
    }
}

