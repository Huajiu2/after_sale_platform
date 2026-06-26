package com.example.aftersight.controller;

import com.example.aftersight.common.Result;
import com.example.aftersight.service.DashboardService;
import com.example.aftersight.vo.StatsVO;
import com.example.aftersight.vo.StoreRankingVO;
import com.example.aftersight.vo.TrendVO;
import com.example.aftersight.vo.TypeRatioVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 仪表盘统计
     */
    @GetMapping("/stats")
    public Result<StatsVO> getStats() throws JsonProcessingException {
        return dashboardService.getStats();
    }


    /**
     * 店铺售后排行top10
     * @return
     */
    @GetMapping("/store-ranking")
    public Result<Map> getrank(){
        Map<String, Object> map = dashboardService.getrank();
        return Result.success(map);
    }

    /**
     * 近7日趋势图
     */
    @GetMapping("/trend")
    public Result<TrendVO> getTrend(){
         return dashboardService.getTrend();
    }

    /**
     * 售后类型占比
     */
    @GetMapping("/type-ratio")
    public Result<TypeRatioVO> getTypeRatio(){
        return dashboardService.getTypeRatio();
    }
}
