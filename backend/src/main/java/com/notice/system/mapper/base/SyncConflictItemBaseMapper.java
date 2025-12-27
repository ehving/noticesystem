package com.notice.system.mapper.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.SyncConflictItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SyncConflictItemBaseMapper extends BaseMapper<SyncConflictItem> {

    List<SyncConflictItem> listByConflictId(@Param("conflictId") String conflictId);
}

