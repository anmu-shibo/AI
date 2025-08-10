package com.example.shiboaiagent.rag;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取PDF文件的reader，用于读取单个PDF文件中的每一页为一个Document对象
 */
@Component
@Slf4j
public class PagePdfDocumentReader {


    private final ResourcePatternResolver resourcePatternResolver;

    public PagePdfDocumentReader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    List<Document> loadPdfDocument() {
        List<Document> documents = new ArrayList<>();

        try {
            //获取所有的pdf文件
            Resource[] resources = resourcePatternResolver.getResources("classpath*:document/*.pdf");
            log.info("Found {} PDF files in classpath*:document/*.pdf", resources.length);
            //循环遍历每一个文件
            for (Resource resource : resources) {
                log.info("Processing file: {}", resource.getFilename());
                PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfTopTextLinesToDelete(0)
                                .build())
                        .withPagesPerDocument(1)
                        .build();
                ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(resource, config);
                //将所有获得文件都传入documents中
                List<Document> docs = pdfReader.read();
                log.info("Extracted {} pages from {}", docs.size(), resource.getFilename());
                documents.addAll(docs);
            }
        } catch (IOException e) {
            log.error("Error reading PDF document", e);
        }
        log.info("Total documents extracted: {}", documents.size());
        return documents;
    }
}