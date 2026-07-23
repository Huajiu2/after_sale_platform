package com.example.aftersight.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MonthlySummaryVO {
    private String month;
    private Integer totalOrders;
    private Integer aiProcessed;
    private Double aiProcessRate;
    private Integer manualIntervention;
    private Double manualInterventionRate;
    private Integer approvedCount;
    private Double approvedRate;
    private Integer rejectedCount;
    private Double rejectedRate;
    private Integer closedCount;
    private Double closedRate;
}
