package com.example.aftersight.service.impl;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.dto.DocParseMessageDTO;
import com.example.aftersight.entity.KnowledgeDoc;
import com.example.aftersight.mapper.KnowledgeMapper;
import com.example.aftersight.service.KnowledgeService;
import com.example.aftersight.vo.DocChunkVO;
import com.example.aftersight.vo.KnowledgeDocVO;
import com.example.aftersight.vo.KnowledgeUploadVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    @Resource
    private KnowledgeMapper knowledgeMapper;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Value("${app.knowledge.upload-dir}")
    private String uploadPath;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static String categoryDesc(String category) {
        switch (category) {
            case "platform_general":
                return "平台通用规则";
            case "digital":
                return "数码售后规则";
            case "fresh":
                return "生鲜售后规则";
            case "apparel":
                return "服饰售后规则";
            case "home_appliance":
                return "家居家电售后规则";
            case "beauty":
                return "美妆护肤售后规则";
            case "medical":
                return "食品保健&医疗器械专项售后规则";
            case "history_case":
                return "历史判例";
            default:
                return category;
        }
    }

    private static String statusDesc(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:
                return "待解析";
            case 1:
                return "解析中";
            case 2:
                return "已向量化";
            case 3:
                return "失败";
            default:
                return "未知";
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

    @Override
    public Result upload(MultipartFile file, String category) throws IOException {
        //校验文件大小
        if (file.getSize() > 20 * 1024 * 1024) {
            return Result.fail(403, "文件大小不能超过20MB");
        }
        //校验文件类型
        String filename = file.getOriginalFilename();
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!Set.of("md", "pdf", "doc", "docx", "txt").contains(ext)) {
            return Result.fail(400, "仅支持 MarkDown / PDF / Word / TXT 格式");
        }
        //文件上传到服务器
        // 1. 将配置的上传路径字符串转为 NIO Path 对象
        Path path = Path.of(uploadPath);
        // 2. 判断目录是否存在，不存在则递归创建多级目录（a/b/c 一并生成）
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        // 3. 生成唯一文件名：UUID + 原文件后缀，避免同名覆盖
        // 4. 将上传文件流写入目标路径
        Path target = path.resolve(file.getOriginalFilename());
        // 自动关闭InputStream
        try (InputStream is = file.getInputStream()) {
            // REPLACE_EXISTING：文件存在则覆盖，避免报错
            Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
        }

        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 5. 插入数据库
        KnowledgeDoc kd = new KnowledgeDoc();
        kd.setDocCode(knowledgeMapper.nextDocCode());
        kd.setDocName(filename);
        kd.setCategory(category);
        kd.setFileType(ext);
        kd.setFileSize(file.getSize());
        kd.setVectorizeStatus(0);  // 待解析
        kd.setUploadedBy("管理员");
        kd.setUploadedAt(LocalDateTime.now());
        knowledgeMapper.insert(kd);

        // 6. 投递 MQ 异步解析
        DocParseMessageDTO msg = new DocParseMessageDTO();
        msg.setDocId(kd.getId());
        msg.setDocCode(kd.getDocCode());
        msg.setFilePath(target.toString());
        msg.setCategory(category);
        rabbitTemplate.convertAndSend("exchange.knowledge", "doc.parse", msg);

        // 7. 返回结果
        KnowledgeUploadVO vo = new KnowledgeUploadVO();
        vo.setDocId(kd.getId());
        vo.setDocCode(kd.getDocCode());
        vo.setDocName(filename);
        vo.setVectorizeStatus(0);
        vo.setVectorizeStatusDesc("待解析");
        return Result.success("文档上传成功，已进入异步解析向量化队列", vo);
    }

    @Override
    public Result<HashMap<String, Object>> getDocChunks(Long docId, Integer page, Integer size) {
        KnowledgeDoc doc = knowledgeMapper.selectById(docId);
        if (doc == null) return Result.fail(404, "文档不存在");

        String fileName = doc.getDocName();
        int offset = (page - 1) * size;

        String pgUrl = "jdbc:postgresql://127.0.0.1:5432/after_sale_platform";
        String pgUser = "postgres";
        String pgPwd = "kaduoxi2";

        try (Connection conn = DriverManager.getConnection(pgUrl, pgUser, pgPwd)) {

            // 查询总数
            PreparedStatement countStmt = conn.prepareStatement(
                    "SELECT COUNT(*) FROM rag_vectors WHERE metadata->>'file_name' = ?");
            countStmt.setString(1, fileName);
            ResultSet countRs = countStmt.executeQuery();
            countRs.next();
            int total = countRs.getInt(1);
            countRs.close();
            countStmt.close();

            // 查询分页数据
            PreparedStatement dataStmt = conn.prepareStatement(
                    "SELECT text FROM rag_vectors WHERE metadata->>'file_name' = ? " +
                            "ORDER BY embedding_id LIMIT ? OFFSET ?");
            dataStmt.setString(1, fileName);
            dataStmt.setInt(2, size);
            dataStmt.setInt(3, offset);
            ResultSet rs = dataStmt.executeQuery();

            List<DocChunkVO> list = new ArrayList<>();
            int idx = offset;
            while (rs.next()) {
                DocChunkVO vo = new DocChunkVO();
                vo.setChunkId((long) ++idx);
                vo.setChunkIndex(idx - 1);
                vo.setChunkText(rs.getString("text"));
                vo.setTokenCount(0);
                list.add(vo);
            }
            rs.close();
            dataStmt.close();

            HashMap<String, Object> result = new HashMap<>();
            result.put("docId", docId);
            result.put("docName", fileName);
            result.put("totalChunks", total);
            result.put("records", list);
            return Result.success(result);

        } catch (SQLException e) {
            log.error("查询切片失败: docId={}", docId, e);
            return Result.fail(500, "查询切片失败");
        }
    }

    @Override
    public Result<KnowledgeDocVO> getDocById(Long id) {
        KnowledgeDoc doc = knowledgeMapper.selectById(id);
        if (doc == null) return Result.fail(404, "文档不存在");
        return Result.success(toVO(doc));
    }

    @Override
    public Result deleteDoc(Long docId) {
        KnowledgeDoc doc = knowledgeMapper.selectById(docId);
        if (doc == null) return Result.fail(404, "文档不存在");

        // 1. 删除 pgvector 中的向量
        String pgUrl = "jdbc:postgresql://127.0.0.1:5432/after_sale_platform";
        try (Connection conn = DriverManager.getConnection(pgUrl, "postgres", "kaduoxi2")) {
            PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM rag_vectors WHERE metadata->>'file_name' = ?");
            stmt.setString(1, doc.getDocName());
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            log.error("删除向量失败: docId={}", docId, e);
        }

        // 2. 删除 MySQL 记录
        knowledgeMapper.deleteById(docId);

        return Result.success("文档已删除");
    }
}
