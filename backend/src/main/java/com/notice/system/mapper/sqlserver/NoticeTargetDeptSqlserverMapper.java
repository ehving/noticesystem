package com.notice.system.mapper.sqlserver;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.NoticeTargetDept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("sqlserver")
public interface NoticeTargetDeptSqlserverMapper extends BaseMapper<NoticeTargetDept> {
}
