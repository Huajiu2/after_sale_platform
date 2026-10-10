package com.example.aftersight.dto;

import lombok.Data;

//动态SQL,条件查询
@Data
public class AfterSaleQueryDTO {
    private String orderNo;        // 订单号，精确匹配
    private String userPhone;      // 手机号，前缀模糊
    private Long storeId;          // 店铺ID，精确
    private Integer afterSaleType; // 售后类型：1仅退款 2退货退款 3投诉
    private Integer ticketStatus;  // 工单状态：0待AI 1办结 2待人工 3驳回
    private String startTime;      // 创建起 yyyy-MM-dd
    private String endTime;        // 创建止 yyyy-MM-dd
}