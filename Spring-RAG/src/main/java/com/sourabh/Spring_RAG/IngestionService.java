package com.sourabh.Spring_RAG;

import org.apache.tomcat.util.descriptor.web.Injectable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;



@Component
public class IngestionService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(Injectable.class);

    private final VectorStore vectorStore;

    @Value("classpath:/docs/article_thebeatoct2024.pdf")
    private Resource marketPdf;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        var pdfReader = new ParagraphPdfDocumentReader(marketPdf);
        TextSplitter textSplitter = new TokenTextSplitter();
        //vectorStore.accept(pdfReader.get());
        logger.info("Vector sotre loaded with data");
    }
}
