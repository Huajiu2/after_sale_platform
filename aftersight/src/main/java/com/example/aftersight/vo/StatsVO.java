package com.example.aftersight.vo;

import lombok.Data;

@Data
public class StatsVO {
    private Integer todayNewOrders;
    private Integer todayAiCompleted;
    private Integer pendingManual;
    private Double aiPassRate;
    private Integer todayDlqCount;
    private Double todayNewOrdersTrend;
    private Double todayAiCompletedTrend;
    private Double pendingManualTrend;
    private Double aiPassRateTrend;
    private Double todayDlqCountTrend;
    private String statisticsTime;
}
