package com.example.aftersight.mapper;

import com.example.aftersight.vo.StoreRankingVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DashboardMapper {
    List<StoreRankingVO> getrank();
}
