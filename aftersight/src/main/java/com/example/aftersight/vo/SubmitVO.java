package com.example.aftersight.vo;

import lombok.Data;

@Data
public class SubmitVO {
    private String ticketNo;
    private Integer ticketStatus;
    private String estimatedTime;
    private String message;
}
