package com.example.aftersight.controller;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.service.KnowledgeService;
import com.example.aftersight.vo.KnowledgeDocVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 3.1 文档列表查询（分页）
     */
    @GetMapping("/list")
    public Result<PageResult<KnowledgeDocVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String docName) {
        PageResult<KnowledgeDocVO> result = knowledgeService.getKnowledgeList(page, size, docName);
        return Result.success(result);
    }
}
