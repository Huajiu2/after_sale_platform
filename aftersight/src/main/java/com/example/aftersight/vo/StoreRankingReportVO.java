package com.example.aftersight.vo;

import lombok.Data;

@Data
public class StoreRankingReportVO {
    private Integer rank;
    private String storeName;
    private Integer orderCount;
    private Double aiProcessRate;
    private Double rejectedRate;
    private Double trend;
}
