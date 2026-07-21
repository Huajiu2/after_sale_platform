package com.example.aftersight.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class KnowledgeDoc {
    private Long id;
    private String docCode;
    private String docName;
    private String category;
    private String fileType;
    private Long fileSize;
    private Integer chunkCount;
    private Integer vectorizeStatus;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
