package com.example.aftersight.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

/**
 * 店铺信息表 store_info
 */
@Data
public class StoreInfo {

    @Id
    private Long id;

    /** 店铺编码 */
    private String storeCode;

    /** 店铺名称 */
    private String storeName;

    /** 经营类目：数码/服饰/生鲜/美妆... */
    private String category;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 状态：1启用 0停用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}