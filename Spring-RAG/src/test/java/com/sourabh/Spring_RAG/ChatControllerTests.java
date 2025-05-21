package com.sourabh.Spring_RAG;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.PgVectorStore; // Assuming this is the correct VectorStore implementation used

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatControllerTests {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    // Mocks for the fluent API chain
    @Mock
    private ChatClient.PromptSpec mockPromptSpec;
    @Mock
    private ChatClient.CallSpec mockCallSpec;
    @Mock
    private ChatClient.ResponseSpec mockResponseSpec;

    @Mock
    private PgVectorStore vectorStore; // As per ChatController constructor

    @InjectMocks
    private ChatController chatController;

    @BeforeEach
    void setUp() {
        // Mock the ChatClient.Builder fluent API and build() method
        // This setup assumes ChatController receives ChatClient.Builder and calls .build()
        when(chatClientBuilder.defaultAdvisors(any(QuestionAnswerAdvisor.class))).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        // If @InjectMocks is used, and ChatController's constructor takes ChatClient.Builder,
        // Mockito will use the chatClientBuilder mock. The controller will then call .build()
        // internally, which we've mocked to return our chatClient mock.
        // So, no manual instantiation of chatController is typically needed here.
        // If manual instantiation was needed:
        // chatController = new ChatController(chatClientBuilder, vectorStore);
    }

    @Test
    void testChat_success() {
        // Arrange
        String testMessage = "Tell me a joke";
        String expectedResponse = "Mocked AI Response";

        // Mock the fluent API chain for chatClient
        when(chatClient.prompt()).thenReturn(mockPromptSpec);
        when(mockPromptSpec.user(anyString())).thenReturn(mockCallSpec);
        when(mockCallSpec.call()).thenReturn(mockResponseSpec);
        when(mockResponseSpec.content()).thenReturn(expectedResponse);

        // Act
        String actualResponse = chatController.chat(testMessage);

        // Assert
        assertThat(actualResponse).isEqualTo(expectedResponse);

        // Verify interactions
        verify(chatClient).prompt();
        verify(mockPromptSpec).user(testMessage); // Verify with the exact message
        verify(mockCallSpec).call();
        verify(mockResponseSpec).content();
    }

    @Test
    void testChat_clientError() {
        // Arrange
        String testMessage = "This will cause an error";
        RuntimeException expectedException = new RuntimeException("ChatClient failed");

        // Mock the fluent API chain to throw an exception at the call() stage
        when(chatClient.prompt()).thenReturn(mockPromptSpec);
        when(mockPromptSpec.user(anyString())).thenReturn(mockCallSpec);
        when(mockCallSpec.call()).thenThrow(expectedException);

        // Act & Assert
        assertThatThrownBy(() -> chatController.chat(testMessage))
                .isInstanceOf(RuntimeException.class)
                .isEqualTo(expectedException); // Check if it's the same exception instance

        // Verify interactions up to the point of failure
        verify(chatClient).prompt();
        verify(mockPromptSpec).user(testMessage);
        verify(mockCallSpec).call();
        // mockResponseSpec.content() should not be called
        verify(mockResponseSpec, org.mockito.Mockito.never()).content();
    }
}
