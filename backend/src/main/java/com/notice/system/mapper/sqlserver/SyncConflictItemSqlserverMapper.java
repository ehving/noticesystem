package com.notice.system.mapper.sqlserver;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.notice.system.entity.SyncConflictItem;
import com.notice.system.mapper.base.SyncConflictItemBaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
@DS("sqlserver")
public interface SyncConflictItemSqlserverMapper extends SyncConflictItemBaseMapper {

    @Override
    @Select("SELECT * FROM sync_conflict_item WHERE conflict_id = #{conflictId}")
    List<SyncConflictItem> listByConflictId(@Param("conflictId") String conflictId);
}

