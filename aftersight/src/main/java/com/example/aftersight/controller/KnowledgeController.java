package com.example.aftersight.controller;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.service.KnowledgeService;
import com.example.aftersight.vo.KnowledgeDocVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;

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

    /**
     * 3.3 文档切片详情
     */
    @GetMapping("/chunks/{docId}")
    public Result<HashMap<String, Object>> chunks(
            @PathVariable Long docId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size) {
        return knowledgeService.getDocChunks(docId, page, size);
    }

    /**
     * 查询文档详情（轮询用）
     */
    @GetMapping("/{docId}")
    public Result<KnowledgeDocVO> getDoc(@PathVariable Long docId) {
        return knowledgeService.getDocById(docId);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) throws IOException {
        return knowledgeService.upload(file, category);
    }

    /**
     * 3.4 重新向量化文档
     */
    @PostMapping("/re-vectorize/{docId}")
    public Result reVectorize(@PathVariable Long docId) {
        return knowledgeService.reVectorize(docId);
    }

    /**
     * 3.5 删除文档
     */
    @DeleteMapping("/{docId}")
    public Result delete(@PathVariable Long docId) {
        return knowledgeService.deleteDoc(docId);
    }
}
