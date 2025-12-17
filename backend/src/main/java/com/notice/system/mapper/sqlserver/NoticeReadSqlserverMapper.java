package com.notice.system.mapper.sqlserver;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.NoticeRead;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("sqlserver")
public interface NoticeReadSqlserverMapper extends BaseMapper<NoticeRead> {
}
