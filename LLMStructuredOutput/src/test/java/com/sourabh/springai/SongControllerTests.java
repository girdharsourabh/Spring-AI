package com.sourabh.springai;

import com.sourabh.llmstructuredoutput.Song;
import com.sourabh.llmstructuredoutput.SongController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.core.convert.support.DefaultConversionService; // Keep if ListOutputConverter needs it explicitly

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongControllerTests {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    // Mocks for the fluent API: chatClient.prompt(prompt).call().chatResponse()
    @Mock
    private ChatClient.PromptActions promptActions; // Renamed from PromptSpec for clarity if that's the actual type
    
    @Mock
    private ChatClient.CallActions callActions; // Renamed from CallSpec for clarity

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;
    // Note: Spring AI's Generation.getOutput() returns ChatMessage (e.g., AssistantMessage).
    // ChatCompletionMessage is a concrete implementation often used.
    // The prompt's "ChatResponse.Result.Output" might be a slight abstraction.
    // We will mock generation.getOutput().getContent() for the text.

    @Mock
    private ListOutputConverter listOutputConverter;

    @InjectMocks
    private SongController songController;

    @BeforeEach
    void setUp() {
        // Mock the ChatClient.Builder chain
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Initialize SongController with the mocked ChatClient.Builder and ListOutputConverter
        // This is field injection, but if constructor, it would be:
        songController = new SongController(chatClientBuilder, listOutputConverter);
        
        // Mock the fluent API: chatClient.prompt(prompt).call().chatResponse()
        when(chatClient.prompt(any(Prompt.class))).thenReturn(promptActions);
        when(promptActions.call()).thenReturn(callActions);
        when(callActions.chatResponse()).thenReturn(chatResponse);
        
        // Common setup for the response chain
        when(chatResponse.getResult()).thenReturn(generation);
        // The actual content for generation.getOutput() will be mocked per test method.
    }

    @Test
    void getSongsByArtist_ValidArtist_ReturnsListOfSongs() {
        // Arrange
        String artist = "Taylor Swift";
        List<String> expectedSongTitles = Arrays.asList("Lover", "Cruel Summer");
        // Assuming SongController converts List<String> to List<Song> internally.
        // List<Song> expectedSongs = Arrays.asList(new Song("Lover"), new Song("Cruel Summer"));
        String mockResponseContent = "Lover\nCruel Summer"; // This is what the LLM would return as a string

        // Mocking the final parts of the chain for this specific test
        // Assuming getOutput() returns something that has getContent() like AssistantMessage
        org.springframework.ai.chat.model.AssistantMessage assistantMessage = 
            new org.springframework.ai.chat.model.AssistantMessage(mockResponseContent);
        when(generation.getOutput()).thenReturn(assistantMessage);
        
        when(listOutputConverter.convert(mockResponseContent)).thenReturn(expectedSongTitles);

        // Act
        List<Song> actualSongs = songController.getSongsByArtist(artist);

        // Assert
        assertThat(actualSongs).isNotNull();
        assertThat(actualSongs).hasSize(2);
        assertThat(actualSongs.get(0).title()).isEqualTo("Lover");
        assertThat(actualSongs.get(1).title()).isEqualTo("Cruel Summer");

        verify(chatClient).prompt(any(Prompt.class)); // Verify the start of the chain
        verify(promptActions).call();
        verify(callActions).chatResponse();
        verify(listOutputConverter).convert(mockResponseContent);
    }

    @Test
    void getSongsByArtist_ArtistNotFoundOrEmptyResponse_ReturnsEmptyList() {
        // Arrange
        String artist = "Unknown Artist";
        String mockResponseContent = ""; // Or "I don't know any songs by that artist."

        org.springframework.ai.chat.model.AssistantMessage assistantMessage =
            new org.springframework.ai.chat.model.AssistantMessage(mockResponseContent);
        when(generation.getOutput()).thenReturn(assistantMessage);
        
        when(listOutputConverter.convert(mockResponseContent)).thenReturn(Collections.emptyList());

        // Act
        List<Song> actualSongs = songController.getSongsByArtist(artist);

        // Assert
        assertThat(actualSongs).isNotNull();
        assertThat(actualSongs).isEmpty();

        verify(chatClient).prompt(any(Prompt.class));
        verify(listOutputConverter).convert(mockResponseContent);
    }

    @Test
    void getSongsByArtist_ChatClientError_ThrowsRuntimeException() {
        // Arrange
        String artist = "Error Prone Artist";
        // Mocking the error at the .call() stage, but it could be earlier or later in the chain
        when(chatClient.prompt(any(Prompt.class))).thenReturn(promptActions);
        when(promptActions.call()).thenThrow(new RuntimeException("ChatClient communication error"));

        // Act & Assert
        assertThatThrownBy(() -> songController.getSongsByArtist(artist))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ChatClient communication error");

        verify(chatClient).prompt(any(Prompt.class)); // Verify interaction up to the point of failure
        verify(promptActions).call();
        verifyNoInteractions(callActions, chatResponse, generation, listOutputConverter); // No further interactions
    }

    @Test
    void getSongsByArtist_ListOutputConverterError_ThrowsRuntimeException() {
        // Arrange
        String artist = "Converter Problem Artist";
        String mockResponseContent = "Some response that will break the converter";

        org.springframework.ai.chat.model.AssistantMessage assistantMessage =
            new org.springframework.ai.chat.model.AssistantMessage(mockResponseContent);
        when(generation.getOutput()).thenReturn(assistantMessage);
        
        when(listOutputConverter.convert(mockResponseContent)).thenThrow(new RuntimeException("ListOutputConverter failed"));

        // Act & Assert
        assertThatThrownBy(() -> songController.getSongsByArtist(artist))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ListOutputConverter failed");

        verify(chatClient).prompt(any(Prompt.class));
        verify(listOutputConverter).convert(mockResponseContent);
    }
}
