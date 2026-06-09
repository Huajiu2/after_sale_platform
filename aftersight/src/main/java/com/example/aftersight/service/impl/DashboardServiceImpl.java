package com.example.aftersight.service.impl;

import com.example.aftersight.common.Result;
import com.example.aftersight.mapper.DashboardMapper;
import com.example.aftersight.service.DashboardService;
import com.example.aftersight.vo.*;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DashboardServiceImpl implements DashboardService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private static final String RANKING_KEY_PREFIX = "rank:store:monthly:";
    private static final String STATS_KEY_PREFIX="cache:dashboard:stats";


    @Resource
    private ObjectMapper objectMapper;
    @Autowired
    private DashboardMapper dashboardMapper;

    /**
     * 仪表盘统计实现类
     */
    @Override
    public Result<StatsVO> getStats() {
        StatsVO statsVO = new StatsVO();

        String key=STATS_KEY_PREFIX;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json!=null){
            //不为空说明从redis中查询到了数据
            StatsVO vo = objectMapper.readValue(json, StatsVO.class);
            return Result.success(vo);
        }
        // 今日数据
        Integer todayOrders = dashboardMapper.getTodayOrders();
        Integer todayAiComplete = dashboardMapper.getAiComplete();
        Integer pending = dashboardMapper.getPending();
        Integer todayDlq = dashboardMapper.getDlq();

        statsVO.setTodayNewOrders(todayOrders);
        statsVO.setTodayAiCompleted(todayAiComplete);
        statsVO.setPendingManual(pending);
        statsVO.setTodayDlqCount(todayDlq);

        // AI通过率 = 今日AI办结 / (今日总处理数)
        int totalProcessed = todayAiComplete
                + (pending != null ? pending : 0)
                + (dashboardMapper.getYesterdayOrders() != null ? 0 : 0); // 兜底，实际用今日驳回数
        // 简化：通过率 = AI办结 / 今日新增
        double passRate = (todayOrders == null || todayOrders == 0) ? 0.0
                : (double) (todayAiComplete != null ? todayAiComplete : 0) / todayOrders * 100;
        statsVO.setAiPassRate(Math.round(passRate * 100.0) / 100.0);

        // 昨日数据（用于环比）
        Integer yestOrders = dashboardMapper.getYesterdayOrders();
        Integer yestAiComplete = dashboardMapper.getYesterdayAiComplete();
        Integer yestPending = dashboardMapper.getYesterdayPending();
        Integer yestDlq = dashboardMapper.getYesterdayDlq();

        // 计算环比：(今日值 - 昨日值) / 昨日值 × 100
        statsVO.setTodayNewOrdersTrend(calcTrend(todayOrders, yestOrders));
        statsVO.setTodayAiCompletedTrend(calcTrend(todayAiComplete, yestAiComplete));
        statsVO.setPendingManualTrend(calcTrend(pending, yestPending));
        statsVO.setTodayDlqCountTrend(calcTrend(todayDlq, yestDlq));

        // AI通过率环比
        double yestPassRate = (yestOrders == null || yestOrders == 0) ? 0.0
                : (double) (yestAiComplete != null ? yestAiComplete : 0) / yestOrders * 100;
        statsVO.setAiPassRateTrend(Math.round((passRate - yestPassRate) * 100.0) / 100.0);

        // 统计时间
        statsVO.setStatisticsTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        //存入redis中并设置过期时间
        stringRedisTemplate.opsForValue().set(key,objectMapper.writeValueAsString(statsVO));
        stringRedisTemplate.expire(key,60, TimeUnit.SECONDS);

        return Result.success(statsVO);
    }

    /**
     * 计算环比增长率
     */
    private Double calcTrend(Integer today, Integer yesterday) {
        if (today == null || yesterday == null || yesterday == 0) {
            return today != null && today > 0 ? 100.0 : 0.0;
        }
        double trend = (double) (today - yesterday) / yesterday * 100;
        return Math.round(trend * 100.0) / 100.0;
    }

    /**
     * 近 7 日趋势
     */
    @Override
    public Result<TrendVO> getTrend() {

        // 生成完整日期列表
        List<LocalDate> dates = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            dates.add(LocalDate.now().minusDays(i));
        }

        // DB 查询结果转 Map<日期字符串, 计数>
        Map<String, Integer> ordersMap = toCountMap(dashboardMapper.getTrendOrders());
        Map<String, Integer> aiMap = toCountMap(dashboardMapper.getTrendAiCompleted());
        Map<String, Integer> pendingMap = toCountMap(dashboardMapper.getTrendPendingManual());

        // 按日期顺序对齐，缺失补 0
        TrendVO trendVO = new TrendVO();
        trendVO.setDates(dates);
        trendVO.setTotalOrders(dates.stream().map(d -> ordersMap.getOrDefault(d.toString(), 0)).toList());
        trendVO.setAiCompleted(dates.stream().map(d -> aiMap.getOrDefault(d.toString(), 0)).toList());
        trendVO.setManualPending(dates.stream().map(d -> pendingMap.getOrDefault(d.toString(), 0)).toList());

        return Result.success(trendVO);
    }

    /**
     * List&lt;Map(date, cnt)&gt; → Map&lt;date, count&gt;
     */
    private Map<String, Integer> toCountMap(List<Map<String, Object>> raw) {
        Map<String, Integer> map = new HashMap<>();
        if (raw == null) return map;
        raw.forEach(m -> map.put(m.get("date").toString(), ((Number) m.get("cnt")).intValue()));
        return map;
    }


    /**
     * 店铺售后排行top10实现类
     * @return
     */
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
        stringRedisTemplate.expire(key,10,TimeUnit.MINUTES);
        Map<String, Object> map = new HashMap<>();
        map.put("rankMonth", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")));
        map.put("list",rank);
        return map;
    }


    /**
     * 售后类型占比实现类
     */
    @Override
    public Result<TypeRatioVO> getTypeRatio() {
        TypeRatioVO typeRatioVO = new TypeRatioVO();
        Integer count1=dashboardMapper.getTypeCount1();
        Integer count2=dashboardMapper.getTypeCount2();
        Integer count3=dashboardMapper.getTypeCount3();
        Double sum= Double.valueOf(count1+count2+count3);
        Double ratio1=Math.round(count1/sum*100 * 10) / 10.0;
        Double ratio2=Math.round(count2/sum*100 * 10) / 10.0;
        Double ratio3=Math.round(count3/sum*100 * 10) / 10.0;
        typeRatioVO.setRefundOnly(new TypeRatioItem(count1,ratio1));
        typeRatioVO.setRefundReturn(new TypeRatioItem(count2,ratio2));
        typeRatioVO.setComplaint(new TypeRatioItem(count3,ratio3));
        return Result.success(typeRatioVO);
    }
}
