package com.example.aftersight.controller;

import com.example.aftersight.common.Result;
import com.example.aftersight.service.DashboardService;
import com.example.aftersight.vo.StoreRankingVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("store-ranking")
    public Result<Map> getrank(){
        Map<String, Object> map = dashboardService.getrank();
        return Result.success(map);
    }


}
