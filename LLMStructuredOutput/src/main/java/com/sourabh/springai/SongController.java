package com.sourabh.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class SongController {

    private final ChatClient chatClient;

    public SongController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/songs")
    public List<String> getSongsByArtist(@RequestParam (value ="artist", defaultValue = "Sonu Nigam") String artist){
        var message = "Please give me list of top 10 songs for the artist {artist}. If you dont know the answer, just say I dont know" +
                "{format}";

        ListOutputConverter listOutputParser = new ListOutputConverter(new DefaultConversionService());

        PromptTemplate promptTemplate = new PromptTemplate(message, Map.of("artist", artist, "format", listOutputParser.getFormat()));
        Prompt prompt = promptTemplate.create();

        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();

        return listOutputParser.convert(response.getResult().getOutput().getText());

    }
}
