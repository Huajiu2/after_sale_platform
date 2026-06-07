package com.example.aftersight.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息表 order_info
 */
@Data
public class OrderInfo {

    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 所属店铺ID */
    private Long storeId;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String userName;

    /** 用户手机号 */
    private String userPhone;

    /** 商品名称 */
    private String productName;

    /** 商品规格 */
    private String productSpec;

    /** 商品图片URL */
    private String productImage;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 订单状态：1待发货 2已发货 3已签收 4已完成 */
    private Integer orderStatus;

    /** 下单时间 */
    private LocalDateTime orderTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
