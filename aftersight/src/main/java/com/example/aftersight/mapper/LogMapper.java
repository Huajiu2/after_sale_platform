package com.example.aftersight.mapper;

import com.example.aftersight.entity.OperationLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LogMapper {

    List<OperationLog> selectList(@Param("bizType") String bizType, @Param("bizId") String bizId);
}
