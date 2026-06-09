package com.example.aftersight.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class TrendVO {
    private List<LocalDate> dates;
    private List<Integer> totalOrders;
    private List<Integer> aiCompleted;
    private List<Integer> manualPending;
}
