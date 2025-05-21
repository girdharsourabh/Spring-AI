package com.sourabh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JokeControllerTests {

    @Mock
    private ChatClient chatClient; // JokeController takes ChatClient directly

    // Mocks for the fluent API chain
    @Mock
    private ChatClient.PromptSpec mockPromptSpec;
    @Mock
    private ChatClient.CallSpec mockCallSpec;
    @Mock
    private ChatClient.ResponseSpec mockResponseSpec;

    @InjectMocks
    private JokeController jokeController;

    @BeforeEach
    void setUp() {
        // Common mocking for the general structure of the fluent API
        // Specific return values will be set in each test method
        when(chatClient.prompt()).thenReturn(mockPromptSpec);
        when(mockPromptSpec.user(anyString())).thenReturn(mockCallSpec);
        when(mockCallSpec.call()).thenReturn(mockResponseSpec);
    }

    @Test
    void testJoke_success() {
        // Arrange
        String expectedJoke = "Mocked AI Joke";
        when(mockResponseSpec.content()).thenReturn(expectedJoke);

        // Act
        Map<String, String> result = jokeController.joke();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(Map.of("joke", expectedJoke));

        // Verify interactions
        verify(chatClient).prompt();
        verify(mockPromptSpec).user("Tell me a joke"); // As per JokeController implementation
        verify(mockCallSpec).call();
        verify(mockResponseSpec).content();
    }

    @Test
    void testJoke_nullResponseFromChatClient() {
        // Arrange
        when(mockResponseSpec.content()).thenReturn(null);

        // Act
        Map<String, String> result = jokeController.joke();

        // Assert
        assertThat(result).isNotNull();
        // Depending on requirements, it could be an empty map or map with null value.
        // The prompt implies Map.of("joke", null)
        assertThat(result.get("joke")).isNull();
        assertThat(result).containsKey("joke"); // Confirms the key "joke" exists
        assertThat(result).hasSize(1);


        // Verify interactions
        verify(chatClient).prompt();
        verify(mockPromptSpec).user("Tell me a joke");
        verify(mockCallSpec).call();
        verify(mockResponseSpec).content();
    }

    @Test
    void testJoke_chatClientThrowsException() {
        // Arrange
        RuntimeException expectedException = new RuntimeException("ChatClient communication error");
        // Let's make the call() method throw the exception
        when(mockCallSpec.call()).thenThrow(expectedException);

        // Act & Assert
        assertThatThrownBy(() -> jokeController.joke())
                .isInstanceOf(RuntimeException.class)
                .isEqualTo(expectedException); // Verifies it's the same exception instance

        // Verify interactions up to the point of failure
        verify(chatClient).prompt();
        verify(mockPromptSpec).user("Tell me a joke");
        verify(mockCallSpec).call();
        // mockResponseSpec.content() should not be called in this scenario
        verify(mockResponseSpec, never()).content();
    }
}
