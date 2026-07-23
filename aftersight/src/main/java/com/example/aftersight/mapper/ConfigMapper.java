package com.example.aftersight.mapper;

import com.example.aftersight.entity.SystemConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ConfigMapper {

    @Select("SELECT * FROM system_config ORDER BY config_group, sort_order")
    List<SystemConfig> selectAll();

    @Select("SELECT * FROM system_config WHERE config_group = #{group} ORDER BY sort_order")
    List<SystemConfig> selectByGroup(String group);

    @Select("SELECT * FROM system_config WHERE id = #{id}")
    SystemConfig selectById(Long id);

    @Update("UPDATE system_config SET config_value = #{configValue} WHERE id = #{id}")
    int updateValue(@Param("id") Long id, @Param("configValue") String configValue);

    @Update("UPDATE system_config SET config_value = #{configValue} WHERE config_key = #{configKey}")
    int updateByKey(@Param("configKey") String configKey, @Param("configValue") String configValue);
}
