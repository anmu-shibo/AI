package com.example.shiboaiagent.rag;



import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 将读取到的document添加到vectorStore(向量数据库中，SpringAI内置的则是SimpleVectorStore)中
 */
@Configuration
@Slf4j
public class PagePdfVectorStoreConfig {

    @Resource
    private PagePdfDocumentReader pagePdfDocumentReader;

    @Bean
    VectorStore pagePdfVectorStore(EmbeddingModel dashScopeEmbeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(dashScopeEmbeddingModel).build();
        List<Document> documents = pagePdfDocumentReader.loadPdfDocument();
        vectorStore.add(documents);
        return vectorStore;
    }
}
