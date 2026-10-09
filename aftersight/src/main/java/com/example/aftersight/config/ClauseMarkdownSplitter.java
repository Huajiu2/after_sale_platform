package com.example.aftersight.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * 按 Markdown 三级标题（### 条款）切分规则文档，每个条款独立成一个切片。
 * 切片文本 = 【父级##大类】###条款标题 + 正文；无 ### 的裸 ## 块整体成一片。
 */
//自定义文档切片器
public class ClauseMarkdownSplitter implements DocumentSplitter {

    @Override
    public List<TextSegment> split(Document document) {
        String[] lines = document.text().split("\n", -1);
        List<TextSegment> segments = new ArrayList<>();

        String h2 = null;   // 最近一个 ## 大类
        String h3 = null;   // 当前 ### 条款标题
        StringBuilder body = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("### ")) {
                flush(segments, h2, h3, body, document);
                h3 = line.substring(4).trim();
                body.setLength(0);
            } else if (line.startsWith("## ")) {
                flush(segments, h2, h3, body, document);
                h2 = line.substring(3).trim();
                h3 = null;
                body.setLength(0);
            } else if (line.startsWith("# ")) {
                // 一级文档标题跳过：来源已由 file_name 元数据承担
                continue;
            } else if (h2 != null) {
                body.append(line).append("\n");
            }
        }
        flush(segments, h2, h3, body, document);
        return segments;
    }

    private void flush(List<TextSegment> out, String h2, String h3,
                       StringBuilder body, Document doc) {
        String text = body.toString().trim();
        body.setLength(0);
        if (h2 == null || text.isEmpty()) {
            return;
        }
        String title = (h3 != null) ? "【" + h2 + "】" + h3 : "【" + h2 + "】";
        out.add(TextSegment.from(title + "\n" + text, doc.metadata()));
    }
}
