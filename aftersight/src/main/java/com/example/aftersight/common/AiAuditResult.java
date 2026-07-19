package com.example.aftersight.common;

import lombok.Data;

import java.util.List;

@Data
public class AiAuditResult {
    private String conclusion;
    private Double confidence;
    private String reason;
    private List<String> applicableRules;
    private String suggestedAction;
    private List<String> riskFlags;
}