package com.example.aftersight.vo;

import lombok.Data;

@Data
public class ManualAuditResultVO {

    /** 工单号 */
    private String ticketNo;

    /** 工单状态：1AI已办结 3已驳回 */
    private Integer ticketStatus;

    /** 工单状态描述 */
    private String ticketStatusDesc;

    /** 是否已生成优质判例 */
    private Boolean caseGenerated;
}
