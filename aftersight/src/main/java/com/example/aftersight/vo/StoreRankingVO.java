package com.example.aftersight.vo;

import lombok.Data;

@Data
public class StoreRankingVO {
    private Integer rank;
    private Integer storeId;
    private String storeName;
    private Integer orderCount;
}
