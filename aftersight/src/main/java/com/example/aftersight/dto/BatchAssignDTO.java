package com.example.aftersight.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchAssignDTO {
    private List<String> ticketNos;
    private String assignee;
}