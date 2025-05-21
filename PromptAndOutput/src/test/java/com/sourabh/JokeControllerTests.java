package com.sourabh;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimplePrompttemplateAdvisor;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JokeControllerTests {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.PromptSpec promptSpec;

    @Mock
    private ChatClient.RequestSpec requestSpec; // Returned by advisors()

    @Mock
    private ChatClient.CallSpec callSpec; // Returned after user() configuration

    @Mock
    private ChatClient.ResponseSpec responseSpec; // Returned by call()

    @Mock
    private Resource mockJokeTemplateResource; // Mock for the jokeTemplate field

    @InjectMocks
    private JokeController jokeController;
    
    @Captor
    private ArgumentCaptor<Consumer<ChatClient.UserSpec>> userSpecConsumerCaptor;

    @Captor
    private ArgumentCaptor<SimplePrompttemplateAdvisor> advisorCaptor;

    // To hold the mock UserSpec for verification in the success test
    private ChatClient.UserSpec mockUserSpec;


    @BeforeEach
    void setUp() throws IOException {
        // Mock ChatClient.Builder
        when(chatClientBuilder.build()).thenReturn(chatClient);

        // Setup the full fluent chain for ChatClient
        when(chatClient.prompt()).thenReturn(promptSpec);
        
        when(promptSpec.advisors(any(SimplePrompttemplateAdvisor.class))).thenReturn(requestSpec);

        // Mocking the user() method which accepts a Consumer<UserSpec>.
        // It will capture the consumer and then return callSpec to continue the chain.
        when(requestSpec.user(userSpecConsumerCaptor.capture())).thenReturn(callSpec);
        
        // Mocking the call() method
        when(callSpec.call()).thenReturn(responseSpec);

        ReflectionTestUtils.setField(jokeController, "jokeTemplate", mockJokeTemplateResource);

        String dummyTemplateContent = "A funny joke about {subject}.";
        InputStream inputStream = new ByteArrayInputStream(dummyTemplateContent.getBytes(StandardCharsets.UTF_8));
        when(mockJokeTemplateResource.getInputStream()).thenReturn(inputStream);
        when(mockJokeTemplateResource.exists()).thenReturn(true);
        when(mockJokeTemplateResource.isReadable()).thenReturn(true);
        when(mockJokeTemplateResource.getFilename()).thenReturn("test-joke-template.st");

        // Prepare the mockUserSpec for use in the success test verification
        // Using RETURNS_DEEP_STUBS allows chaining like .text(...).param(...)
        mockUserSpec = mock(ChatClient.UserSpec.class, RETURNS_DEEP_STUBS);
        // When the captured consumer is executed with this mockUserSpec, these ensure chaining works:
        when(mockUserSpec.text(any(Resource.class))).thenReturn(mockUserSpec);
        when(mockUserSpec.param(anyString(), any())).thenReturn(mockUserSpec);

    }

    @Test
    void getJoke_success_returnsJokeResponse() {
        // Arrange
        String subject = "cats";
        JokeResponse expectedJokeResponse = new JokeResponse(subject, "Cats are purrfectly hilarious!");
        
        when(responseSpec.entity(JokeResponse.class)).thenReturn(expectedJokeResponse);

        // Act
        JokeResponse actualJokeResponse = jokeController.getJoke(subject);

        // Assert
        assertThat(actualJokeResponse).isNotNull();
        assertThat(actualJokeResponse.subject()).isEqualTo(subject);
        assertThat(actualJokeResponse.joke()).isEqualTo("Cats are purrfectly hilarious!");

        // Verify the chain
        verify(chatClientBuilder).build();
        verify(chatClient).prompt();
        verify(promptSpec).advisors(advisorCaptor.capture());
        
        SimplePrompttemplateAdvisor capturedAdvisor = advisorCaptor.getValue();
        assertThat(ReflectionTestUtils.getField(capturedAdvisor, "promptResource")).isEqualTo(mockJokeTemplateResource);

        // Execute the captured consumer with the mockUserSpec prepared in setUp
        Consumer<ChatClient.UserSpec> capturedConsumer = userSpecConsumerCaptor.getValue();
        capturedConsumer.accept(mockUserSpec);

        // Verify that the text() and param() methods were called on mockUserSpec as expected
        verify(mockUserSpec).text(mockJokeTemplateResource);
        verify(mockUserSpec).param("subject", subject);

        verify(callSpec).call();
        verify(responseSpec).entity(JokeResponse.class);
    }

    @Test
    void getJoke_chatClientError_throwsRuntimeException() {
        // Arrange
        String subject = "dogs";
        
        when(callSpec.call()).thenThrow(new RuntimeException("ChatClient communication error"));

        // Act & Assert
        assertThatThrownBy(() -> jokeController.getJoke(subject))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ChatClient communication error");

        verify(chatClientBuilder).build();
        verify(chatClient).prompt();
        verify(promptSpec).advisors(any(SimplePrompttemplateAdvisor.class));
        // The consumer is captured by the mock setup for requestSpec.user()
        // We can verify it was captured, and then call it to simulate the controller's action.
        Consumer<ChatClient.UserSpec> capturedConsumer = userSpecConsumerCaptor.getValue();
        // It's important that this consumer is called to fully simulate the path to callSpec.call()
        capturedConsumer.accept(mockUserSpec); 
        
        verify(callSpec).call(); // This is where the exception is thrown
        verifyNoInteractions(responseSpec); 
    }
    
    @Test
    void getJoke_resourceLoadError_throwsRuntimeException() throws IOException {
        // Arrange
        String subject = "birds";
        // This setup makes the SimplePrompttemplateAdvisor throw an error during its construction or when it first reads the template
        when(mockJokeTemplateResource.getInputStream()).thenThrow(new IOException("Failed to load template"));

        // Act & Assert
        assertThatThrownBy(() -> jokeController.getJoke(subject))
                .isInstanceOf(RuntimeException.class) 
                .hasCauseInstanceOf(IOException.class) 
                .hasMessageContaining("Error creating SimplePrompttemplateAdvisor");

        verify(chatClientBuilder).build();
        verify(chatClient).prompt();
        // advisors() might not be called if the advisor instantiation fails before that.
        // The controller code is `new SimplePrompttemplateAdvisor(this.jokeTemplate)`
        // This happens *before* it's passed to `advisors()`. So, `promptSpec.advisors` might not be reached.
        // However, the resource is accessed inside the lambda passed to user(), after advisors().
        // Let's re-check JokeController logic:
        // new SimplePrompttemplateAdvisor(this.jokeTemplate) is created and passed to advisors().
        // The advisor itself reads the template lazily or on construction.
        // If it's on construction, the error is before advisors(). If lazy, it's later.
        // SimplePrompttemplateAdvisor reads the template in its constructor.
        // So, the error happens when `new SimplePrompttemplateAdvisor(this.jokeTemplate)` is executed.
        // This means `promptSpec.advisors()` will not be called.
        verifyNoInteractions(promptSpec, requestSpec, callSpec, responseSpec);
    }
}
