package com.example.aftersight.vo;

import java.math.BigDecimal;

//“AI 审核依据”的返回对象，用来告诉前端：AI 为什么得出这个审核结论，它参考了哪些知识库规则或历史判例
public class RagEvidenceVO {
    private Integer rank;
    private BigDecimal similarity;
    private String ruleContent;
    private String sourceDoc;
    private Long chunkId;
}