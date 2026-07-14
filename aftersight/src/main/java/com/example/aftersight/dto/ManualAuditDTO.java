package com.example.aftersight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ManualAuditDTO {

    /** 工单号 */
    @NotBlank(message = "工单号不能为空")
    private String ticketNo;

    /** 审核结果：1同意售后 2驳回售后 */
    @NotNull(message = "审核结果不能为空")
    private Integer manualResult;

    /** 人工备注 */
    private String manualRemark;
}
