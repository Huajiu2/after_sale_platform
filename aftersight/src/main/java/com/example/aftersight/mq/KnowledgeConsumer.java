package com.example.aftersight.mq;

import com.example.aftersight.dto.DocParseMessageDTO;
import com.example.aftersight.entity.KnowledgeDoc;
import com.example.aftersight.mapper.KnowledgeMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KnowledgeConsumer {


    @Resource
    private EmbeddingStoreIngestor embeddingStoreIngestor;

    @Resource
    private KnowledgeMapper knowledgeMapper;

    @Resource
    private DocumentByParagraphSplitter paragraphSplitter;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.doc.parse", durable = "true"),
            exchange = @Exchange(value = "exchange.knowledge", type = ExchangeTypes.DIRECT),
            key = "doc.parse"
    ))
    public void handleDocParse(DocParseMessageDTO message) {
        // TODO
        Document doc = FileSystemDocumentLoader.loadDocument(message.getFilePath());
        try {
            embeddingStoreIngestor.ingest(doc);

            KnowledgeDoc kd = new KnowledgeDoc();
            kd.setId(message.getDocId());
            kd.setVectorizeStatus(2);
            kd.setChunkCount(paragraphSplitter.split(doc).size());
            knowledgeMapper.updateStatus(kd);
        } catch (Exception e) {
            log.error("文档解析失败: docId={}", message.getDocId(), e);
            KnowledgeDoc kd = new KnowledgeDoc();
            kd.setId(message.getDocId());
            kd.setVectorizeStatus(3);  // 失败
            knowledgeMapper.updateStatus(kd);        }
    }
}