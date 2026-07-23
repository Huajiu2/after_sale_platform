package com.example.aftersight.mapper;

import com.example.aftersight.entity.StoreInfo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BasicMapper {


    List<StoreInfo> selectStores(@Param("keyword") String keyword);
}
