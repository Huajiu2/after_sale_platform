package com.example.aftersight.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDocVO {
    private Long docId;
    private String docCode;
    private String docName;
    private String category;
    private String categoryDesc;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private Integer vectorizeStatus;
    private String vectorizeStatusDesc;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
