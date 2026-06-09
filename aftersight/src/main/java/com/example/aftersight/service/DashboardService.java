package com.example.aftersight.service;

import com.example.aftersight.common.Result;
import com.example.aftersight.vo.StatsVO;
import com.example.aftersight.vo.TrendVO;
import com.example.aftersight.vo.TypeRatioVO;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getrank();

    Result<StatsVO> getStats();

    Result<TrendVO> getTrend();

    Result<TypeRatioVO> getTypeRatio();
}
