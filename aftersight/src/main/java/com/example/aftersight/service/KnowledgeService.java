package com.example.aftersight.service;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.vo.KnowledgeDocVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface KnowledgeService {
    PageResult<KnowledgeDocVO> getKnowledgeList(Integer page, Integer size, String docName);

    Result upload(MultipartFile file, String category) throws IOException;
}
