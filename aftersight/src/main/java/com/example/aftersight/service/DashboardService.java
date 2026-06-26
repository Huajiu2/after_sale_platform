package com.example.aftersight.service;

import com.example.aftersight.common.Result;
import com.example.aftersight.vo.StatsVO;
import com.example.aftersight.vo.TrendVO;
import com.example.aftersight.vo.TypeRatioVO;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getrank();

    Result<StatsVO> getStats() throws JsonProcessingException;

    Result<TrendVO> getTrend();

    Result<TypeRatioVO> getTypeRatio();
}
