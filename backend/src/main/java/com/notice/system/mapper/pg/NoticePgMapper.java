package com.notice.system.mapper.pg;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notice.system.entity.Notice;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.dynamic.datasource.annotation.DS;


@Mapper
@DS("pg")  // ⭐ 关键：这个 Mapper 固定走 pg 数据源
public interface NoticePgMapper extends BaseMapper<Notice> {

//    @Select("select current_database()")
//    String currentDatabase();
//
//    @Select("select current_schema()")
//    String currentSchema();
//
//    @Select("select count(*) from notice")
//    Long countNotice();
}
