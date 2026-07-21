package com.example.aftersight.vo;

import lombok.Data;

@Data
public class KnowledgeUploadVO {
    private Long docId;
    private String docCode;
    private String docName;
    private Integer vectorizeStatus;
    private String vectorizeStatusDesc;
}