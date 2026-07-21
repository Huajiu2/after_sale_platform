package com.example.aftersight.service.impl;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.entity.KnowledgeDoc;
import com.example.aftersight.mapper.KnowledgeMapper;
import com.example.aftersight.service.KnowledgeService;
import com.example.aftersight.vo.KnowledgeDocVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Resource
    private KnowledgeMapper knowledgeMapper;

    private static String categoryDesc(String category) {
        switch (category) {
            case "platform_general": return "平台通用规则";
            case "digital":           return "数码售后规则";
            case "fresh":             return "生鲜售后规则";
            case "apparel":           return "服饰售后规则";
            case "home_appliance":    return "家居家电售后规则";
            case "beauty":            return "美妆护肤售后规则";
            case "history_case":      return "历史判例";
            default:                  return category;
        }
    }

    private static String statusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:  return "待解析";
            case 1:  return "解析中";
            case 2:  return "已向量化";
            case 3:  return "失败";
            default: return "未知";
        }
    }

    private KnowledgeDocVO toVO(KnowledgeDoc doc) {
        KnowledgeDocVO vo = new KnowledgeDocVO();
        vo.setDocId(doc.getId());
        vo.setDocCode(doc.getDocCode());
        vo.setDocName(doc.getDocName());
        vo.setCategory(doc.getCategory());
        vo.setCategoryDesc(categoryDesc(doc.getCategory()));
        vo.setFileType(doc.getFileType());
        vo.setFileSize(doc.getFileSize());
        vo.setChunkCount(doc.getChunkCount());
        vo.setVectorizeStatus(doc.getVectorizeStatus());
        vo.setVectorizeStatusDesc(statusDesc(doc.getVectorizeStatus()));
        vo.setUploadedBy(doc.getUploadedBy());
        vo.setUploadedAt(doc.getUploadedAt());
        return vo;
    }

    @Override
    public PageResult<KnowledgeDocVO> getKnowledgeList(Integer page, Integer size, String docName) {
        PageHelper.startPage(page, size);
        List<KnowledgeDoc> list = knowledgeMapper.selectList(docName);
        PageInfo<KnowledgeDoc> pageInfo = new PageInfo<>(list);

        List<KnowledgeDocVO> voList = list.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        PageResult<KnowledgeDocVO> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setRecords(voList);
        result.setTotal(pageInfo.getTotal());
        result.setPages(pageInfo.getPages());
        return result;
    }
}
