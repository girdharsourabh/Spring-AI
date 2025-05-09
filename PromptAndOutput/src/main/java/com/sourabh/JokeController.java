package com.sourabh;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JokeController {


    private final ChatClient chatClient;

    public JokeController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Value("classpath:joke-template.st")
    private Resource jokeTemplate;

    @GetMapping("/joke")
    public JokeResponse getJoke(@RequestParam String subject){

       return chatClient.prompt().advisors(new SimpleLoggerAdvisor()).user(
                promptUserSpec -> promptUserSpec.text(jokeTemplate)
                        .param("subject", subject))
                .call()
                .entity(JokeResponse.class);

    }
}
