package com.example.aftersight.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SubmitDTO {
    private String orderNo;
    private Integer afterSaleType;
    private String applyReason;
    private MultipartFile[] evidenceFiles;
}
