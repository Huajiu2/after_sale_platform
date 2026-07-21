package com.example.aftersight.service;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.vo.KnowledgeDocVO;

public interface KnowledgeService {
    PageResult<KnowledgeDocVO> getKnowledgeList(Integer page, Integer size, String docName);
}
