package com.sourabh.Spring_RAG;

import org.springframework.ai.chat.client.ChatClient;

import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final PgVectorStore vectorStore;

    public ChatController(ChatClient.Builder chatClientBuilder, PgVectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
    .build();
    }

    @GetMapping("/getAnswerFromRAG")
    public String chat(){
        return chatClient.prompt()
                .user("How did the federal reserve interest fate cut impacted various asset classes  according to analysis")
                .call().content();
    }
}
