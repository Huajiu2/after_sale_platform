package com.example.aftersight.vo;

import lombok.Data;

@Data
public class DocChunkVO {
    private Long chunkId;
    private Integer chunkIndex;
    private String chunkText;
    private Integer tokenCount;
}
