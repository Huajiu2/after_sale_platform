package com.example.aftersight.dto;

import lombok.Data;

@Data
public class DocParseMessageDTO {
    private Long docId;
    private String docCode;
    private String filePath;
    private String category;
}