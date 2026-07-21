package com.example.aftersight.config;

import com.example.aftersight.entity.KnowledgeDoc;
import com.example.aftersight.mapper.KnowledgeMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.segment.TextSegmentTransformer;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
public class RagConfig {

    @Value("${app.rag.load-on-startup:false}")
    private Boolean loadOnStartup;

    @Resource
    private KnowledgeMapper knowledgeMapper;

    @Bean
    public PgVectorEmbeddingStore pgVectorStore(){
        DataSource ds = DataSourceBuilder.create()
                .url("jdbc:postgresql://127.0.0.1:5432/after_sale_platform")
                .username("postgres")
                .password("kaduoxi2")
                .driverClassName("org.postgresql.Driver")
                .build();

        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(ds)
                .table("rag_vectors")
                .dimension(1024)
                .createTable(true)
                .dropTableFirst(false)
                .build();
    }

    @Bean
    public DocumentByParagraphSplitter paragraphSplitter(){
        return new DocumentByParagraphSplitter(1600,200);
    }

    //转换器：读取文本分片元数据里的 file_name，在正文前面拼接 【文件名】，方便 RAG 检索结果区分来源文档。
    @Bean
    public TextSegmentTransformer textSegmentTransformer(){
        return textSegment -> {
            String fileName = textSegment.metadata().getString("file_name");
            return TextSegment.from(
                    "【"+fileName+"】"+ textSegment.text(),
                    textSegment.metadata()
            );
        };
    }

    //把前面 4 个组件（切片器、转换器、向量模型、向量库）组装成一条流水线
    @Bean
    public EmbeddingStoreIngestor embeddingStoreIngestor(
            DocumentByParagraphSplitter paragraphSplitter,
            TextSegmentTransformer textSegmentTransformer,
            PgVectorEmbeddingStore pgVectorStore,
            EmbeddingModel embeddingModel){
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(paragraphSplitter)
                .textSegmentTransformer(textSegmentTransformer)
                .embeddingModel(embeddingModel)
                .embeddingStore(pgVectorStore)
                .build();

    }

    //容器启动完成后自动执行。用来加载 rules/ 目录下的 md 文档进向量库
//    @Bean
//    public CommandLineRunner loadDocuments(
//            EmbeddingStoreIngestor embeddingStoreIngestor,
//            DocumentByParagraphSplitter paragraphSplitter){
//        return args -> {
//            if(!loadOnStartup){
//                log.info("RAG 文档加载已关闭，跳过");
//                return;
//            }
//            Path rulesDir = Path.of("src/main/resources/rules");
//            if (!Files.exists(rulesDir)){
//                log.warn("规则目录不存在: {}", rulesDir);
//                return;
//            }
//            List<Document> documents = FileSystemDocumentLoader.loadDocuments(rulesDir);
//            embeddingStoreIngestor.ingest(documents);
//            log.info("RAG 文档加载完成，共 {} 个文档", documents.size());
//
//            for (Document doc : documents) {
//                KnowledgeDoc kd = new KnowledgeDoc();
//                kd.setDocCode(knowledgeMapper.nextDocCode());
//                kd.setDocName(doc.metadata().getString("file_name"));
//                kd.setCategory(mapCategory(doc.metadata().getString("file_name")));
//                kd.setFileType("md");
//                // 文件大小（字节）
//                try {
//                    kd.setFileSize(Files.size(rulesDir.resolve(doc.metadata().getString("file_name"))));
//                } catch (Exception ignored) {}
//                // 切片数量
//                kd.setChunkCount(paragraphSplitter.split(doc).size());
//                kd.setVectorizeStatus(2);
//                kd.setUploadedBy("系统");
//                kd.setUploadedAt(LocalDateTime.now());
//                knowledgeMapper.insert(kd);
//            }
//        };
//    }

    private String mapCategory(String fileName) {
        if (fileName.contains("数码") || fileName.contains("3C")) return "digital";
        if (fileName.contains("服饰") || fileName.contains("鞋包")) return "apparel";
        if (fileName.contains("生鲜")) return "fresh";
        if (fileName.contains("美妆") || fileName.contains("护肤")) return "beauty";
        if (fileName.contains("家居家电")) return "home_appliance";
        return "platform_general";
    }




    //检索器只负责查询，不做入库，后续 AI 审核时传入消费者的申请原因就能查到最相似的规则
    @Bean
    public ContentRetriever contentRetriever(
            PgVectorEmbeddingStore pgVectorStore,
            EmbeddingModel embeddingModel
    ){
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(pgVectorStore)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.75)
                .build();
    }

}
