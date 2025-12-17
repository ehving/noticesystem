package com.notice.system.mapper.sqlserver;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Role;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("sqlserver")
public interface RoleSqlserverMapper extends BaseMapper<Role> {
}
