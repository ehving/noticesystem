package com.notice.system.mapper.sqlserver;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Notice;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

/**
 * SQL Server 的 Notice 表 Mapper
 */
@Mapper
@DS("sqlserver")   //  指定走 sqlserver 数据源
public interface NoticeSqlserverMapper extends BaseMapper<Notice> {
}
