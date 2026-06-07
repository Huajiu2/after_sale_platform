package com.example.aftersight.service.impl;

import com.example.aftersight.mapper.DashboardMapper;
import com.example.aftersight.service.DashboardService;
import com.example.aftersight.vo.StoreRankingVO;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String RANKING_KEY_PREFIX = "rank:store:monthly:";


    @Resource
    private ObjectMapper objectMapper;
    @Autowired
    private DashboardMapper dashboardMapper;

    @Override
    public Map<String, Object> getrank() {
        //设置redis的key
        String month=LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key=RANKING_KEY_PREFIX+month;
        //尝试从 Redis ZSet 取排行榜
        Set<String> ranking = stringRedisTemplate.opsForZSet().reverseRange(key, 0, 9);
        if (ranking!=null&&!ranking.isEmpty()){
            //从redis中查询到了数据
            List<StoreRankingVO> list = ranking.stream()
                    .map(json -> objectMapper.readValue(json, StoreRankingVO.class))
                    .toList();
            Map<String, Object> map = new HashMap<>();
            map.put("rankMonth", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")));
            map.put("list",list);
            return map;
        }
        //如果没有在redis中查询到,存放到redis中
        List<StoreRankingVO> rank = dashboardMapper.getrank();
        rank.stream().forEach(vo->
        {
            String s = objectMapper.writeValueAsString(vo);
            stringRedisTemplate.opsForZSet().add(key,s,vo.getOrderCount());
        });
        Map<String, Object> map = new HashMap<>();
        map.put("rankMonth", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")));
        map.put("list",rank);
        return map;
    }
}
