package com.example.aftersight.vo;

import lombok.Data;

@Data
public class DailyRecordVO {
    private String date;
    private Integer totalOrders;
    private Integer aiCompleted;
    private Integer manualCompleted;
    private Integer rejected;
    private Integer closed;
    private Double aiProcessRate;
    private Double rejectedRate;
}
