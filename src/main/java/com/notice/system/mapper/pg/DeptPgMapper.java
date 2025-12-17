package com.notice.system.mapper.pg;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("pg")
public interface DeptPgMapper extends BaseMapper<Dept> {
}
