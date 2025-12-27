package com.notice.system.converter;

import com.notice.system.entity.SyncConflict;
import com.notice.system.entity.SyncConflictItem;
import com.notice.system.mapper.dto.SyncConflictWithItemRow;
import com.notice.system.entityEnum.ConflictStatus;
import com.notice.system.entityEnum.ConflictType;
import com.notice.system.entityEnum.DatabaseType;
import com.notice.system.entityEnum.SyncEntityType;
import com.notice.system.vo.conflict.*;

import java.util.*;
import java.util.stream.Collectors;

public class SyncConflictConverter {

    private SyncConflictConverter() {}

    public static List<SyncConflictWithItemsVo> toListVosFromRows(List<SyncConflictWithItemRow> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        // 按 conflictId 聚合
        Map<String, SyncConflictWithItemsVo> map = new LinkedHashMap<>();
        for (SyncConflictWithItemRow r : rows) {
            if (r == null || r.getConflictId() == null) continue;

            SyncConflictWithItemsVo vo = map.computeIfAbsent(r.getConflictId(), id -> {
                SyncConflictWithItemsVo x = new SyncConflictWithItemsVo();
                x.setId(id);
                x.setEntityType(parseEnum(SyncEntityType.class, r.getEntityType()));
                x.setEntityId(r.getEntityId());
                x.setStatus(parseEnum(ConflictStatus.class, r.getStatus()));
                x.setConflictType(parseEnum(ConflictType.class, r.getConflictType()));
                x.setFirstSeenAt(r.getFirstSeenAt());
                x.setLastSeenAt(r.getLastSeenAt());
                x.setLastCheckedAt(r.getLastCheckedAt());
                x.setLastNotifiedAt(r.getLastNotifiedAt());
                x.setNotifyCount(r.getNotifyCount());
                x.setResolutionSourceDb(parseEnum(DatabaseType.class, r.getResolutionSourceDb()));
                x.setResolutionNote(r.getResolutionNote());
                x.setResolvedAt(r.getResolvedAt());
                return x;
            });

            // item 可能为空（left join）
            if (r.getItemId() != null) {
                SyncConflictItemVo itemVo = new SyncConflictItemVo();
                itemVo.setId(r.getItemId());
                itemVo.setDbType(parseEnum(DatabaseType.class, r.getDbType()));
                itemVo.setExistsFlag(r.getExistsFlag());
                itemVo.setRowHash(r.getRowHash());
                itemVo.setRowVersion(r.getRowVersion());
                itemVo.setRowUpdateTime(r.getRowUpdateTime());
                itemVo.setLastCheckedAt(r.getItemLastCheckedAt());
                vo.getItems().add(itemVo);
            }
        }

        // items 排序（MYSQL/PG/SQLSERVER 固定顺序更好展示）
        for (SyncConflictWithItemsVo vo : map.values()) {
            vo.setItems(sortItems(vo.getItems()));
        }

        return new ArrayList<>(map.values());
    }

    public static SyncConflictDetailVo toDetailVo(SyncConflict conflict, List<SyncConflictItem> items) {
        if (conflict == null) return null;

        SyncConflictDetailVo vo = new SyncConflictDetailVo();
        vo.setId(conflict.getId());
        vo.setEntityType(conflict.getEntityType());
        vo.setEntityId(conflict.getEntityId());
        vo.setStatus(conflict.getStatus());
        vo.setConflictType(conflict.getConflictType());

        vo.setFirstSeenAt(conflict.getFirstSeenAt());
        vo.setLastSeenAt(conflict.getLastSeenAt());
        vo.setLastCheckedAt(conflict.getLastCheckedAt());

        vo.setLastNotifiedAt(conflict.getLastNotifiedAt());
        vo.setNotifyCount(conflict.getNotifyCount());

        vo.setResolutionSourceDb(conflict.getResolutionSourceDb());
        vo.setResolutionNote(conflict.getResolutionNote());
        vo.setResolvedAt(conflict.getResolvedAt());

        vo.setCreateTime(conflict.getCreateTime());
        vo.setUpdateTime(conflict.getUpdateTime());

        if (items != null) {
            List<SyncConflictItemVo> itemVos = items.stream()
                    .filter(Objects::nonNull)
                    .map(SyncConflictConverter::toItemVo)
                    .collect(Collectors.toList());
            vo.setItems(sortItems(itemVos));
        }

        return vo;
    }

    public static SyncConflictItemVo toItemVo(SyncConflictItem it) {
        if (it == null) return null;
        SyncConflictItemVo vo = new SyncConflictItemVo();
        vo.setId(it.getId());
        vo.setDbType(it.getDbType());
        vo.setExistsFlag(it.getExistsFlag());
        vo.setRowHash(it.getRowHash());
        vo.setRowVersion(it.getRowVersion());
        vo.setRowUpdateTime(it.getRowUpdateTime());
        vo.setLastCheckedAt(it.getLastCheckedAt());
        return vo;
    }

    private static List<SyncConflictItemVo> sortItems(List<SyncConflictItemVo> items) {
        if (items == null) return Collections.emptyList();
        Map<DatabaseType, Integer> order = Map.of(
                DatabaseType.MYSQL, 1,
                DatabaseType.PG, 2,
                DatabaseType.SQLSERVER, 3
        );
        items.sort(Comparator.comparingInt(x -> order.getOrDefault(x.getDbType(), 99)));
        return items;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> clazz, String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return Enum.valueOf(clazz, val.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }
}


