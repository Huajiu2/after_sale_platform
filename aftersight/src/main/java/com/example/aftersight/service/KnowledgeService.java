package com.example.aftersight.service;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import java.util.HashMap;
import com.example.aftersight.vo.KnowledgeDocVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface KnowledgeService {
    PageResult<KnowledgeDocVO> getKnowledgeList(Integer page, Integer size, String docName);

    Result upload(MultipartFile file, String category) throws IOException;

    Result<KnowledgeDocVO> getDocById(Long id);

    Result<HashMap<String, Object>> getDocChunks(Long docId, Integer page, Integer size);

    Result deleteDoc(Long docId);

    Result reVectorize(Long docId);
}
