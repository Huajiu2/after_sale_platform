package com.example.aftersight.mapper;

import com.example.aftersight.entity.StoreInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TestMapper {

    @Select("select * from store_info")
    List<StoreInfo> tquery();
}
