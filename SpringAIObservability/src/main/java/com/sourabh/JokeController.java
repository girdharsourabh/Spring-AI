package com.sourabh;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class JokeController {

    public JokeController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    private final ChatClient chatClient;

    @GetMapping("joke")
    Map<String, String> joke(){
        var reply = chatClient.prompt().user("Tell me a joke").call().content();
        return Map.of("joke", reply);
    }
}
