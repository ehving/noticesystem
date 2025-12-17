package com.notice.system.converter;

import com.notice.system.entity.Notice;
import com.notice.system.vo.notice.NoticeAdminSaveVo;

import java.time.LocalDateTime;

/**
 * Notice 相关的 VO <-> Entity 转换工具。
 *
 * 约定：
 *  - 不写业务规则（状态默认值、发布时间自动填充等），只做字段拷贝和简单清洗；
 *  - 业务规则仍由 Controller / Service 决定。
 */
public class NoticeConverter {

    /**
     * 管理端：从保存 VO 构造 Notice 实体（用于“新建公告”场景）。
     *
     * @param vo          管理端保存表单
     * @param publisherId 发布人 ID（当前管理员）
     */
    public static Notice toEntityForCreate(NoticeAdminSaveVo vo, String publisherId) {
        if (vo == null) {
            return null;
        }
        Notice notice = new Notice();
        // 新建时 id 通常为空，但保留从 vo 透传的能力，便于你后面支持“前端生成 id”等玩法
        notice.setId(vo.getId());

        notice.setTitle(trim(vo.getTitle()));
        notice.setContent(vo.getContent());
        notice.setLevel(trim(vo.getLevel()));
        notice.setStatus(trim(vo.getStatus()));   // 默认值由外层兜底
        notice.setPublisherId(publisherId);
        notice.setPublishTime(vo.getPublishTime());
        notice.setExpireTime(vo.getExpireTime());
        // viewCount 在外面设置（比如 0L）
        return notice;
    }

    /**
     * 管理端：将 VO 中的变更应用到已有 Notice 实体上（用于“编辑公告”场景）。
     *
     * 只做字段同步，不处理业务规则：
     *  - 比如“改成 PUBLISHED 且 publishTime 为空则补当前时间”仍由外层处理。
     */
    public static void applyAdminUpdate(NoticeAdminSaveVo vo, Notice notice) {
        if (vo == null || notice == null) {
            return;
        }

        if (hasText(vo.getTitle())) {
            notice.setTitle(vo.getTitle().trim());
        }
        if (hasText(vo.getContent())) {
            notice.setContent(vo.getContent());
        }
        if (hasText(vo.getLevel())) {
            notice.setLevel(vo.getLevel().trim());
        }
        if (hasText(vo.getStatus())) {
            notice.setStatus(vo.getStatus().trim());
        }

        // publishTime：只有前端传了非空才覆盖
        if (vo.getPublishTime() != null) {
            notice.setPublishTime(vo.getPublishTime());
        }

        // expireTime：沿用你现在的行为，直接覆盖（为 null 相当于清空）
        notice.setExpireTime(vo.getExpireTime());
    }

    // ==== 工具方法 ====

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}

