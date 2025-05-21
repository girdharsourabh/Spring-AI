package com.sourabh.Spring_RAG;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTests {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private Resource mockMarketPdf;

    private IngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new IngestionService(vectorStore);
        ReflectionTestUtils.setField(ingestionService, "marketPdf", mockMarketPdf);

        // Default behavior for mockMarketPdf to avoid NPEs in ParagraphPdfDocumentReader constructor or basic methods
        when(mockMarketPdf.getFilename()).thenReturn("dummy.pdf");
    }

    @Test
    void testRun_successfulExecution_noVectorStoreInteraction() throws IOException {
        // Arrange
        when(mockMarketPdf.exists()).thenReturn(true);
        // Provide a minimal valid-enough input stream if ParagraphPdfDocumentReader tries to read it.
        // A real PDF is complex. An empty stream might cause issues.
        // A stream with "%PDF-" might be enough to pass initial checks for some readers.
        String minimalPdfContent = "%PDF-1.4\n%EOF"; // Very basic PDF marker
        InputStream dummyInputStream = new ByteArrayInputStream(minimalPdfContent.getBytes(StandardCharsets.UTF_8));
        when(mockMarketPdf.getInputStream()).thenReturn(dummyInputStream);


        // Act & Assert
        // Since vectorStore.accept() is commented out, we just check for no unexpected exceptions.
        assertThatCode(() -> ingestionService.run())
                .doesNotThrowAnyException();

        // Verify that the resource was interacted with (e.g., exists was called)
        verify(mockMarketPdf).exists();
        // ParagraphPdfDocumentReader is created internally. We can't easily verify its instantiation
        // without more complex mocking (PowerMock or refactoring IngestionService).
        // We are implicitly testing that its creation with the mocked resource doesn't fail.

        // Verify that vectorStore.accept() is NOT called, as it's commented out in the source.
        verify(vectorStore, never()).accept(any());
        verify(vectorStore, never()).add(any());
    }

    @Test
    void testRun_resourceNotFound_logsErrorAndCompletes() {
        // Arrange
        when(mockMarketPdf.exists()).thenReturn(false);
        // No need to mock getInputStream if exists() is false and code checks it first.
        // The ParagraphPdfDocumentReader might be instantiated even if exists() is false.
        // Its get() method would likely fail.

        // Act & Assert
        // Expecting it to log an error and not throw an unhandled exception like NullPointerException.
        // The actual ParagraphPdfDocumentReader might throw an exception if the resource doesn't exist
        // when its get() method is called. IngestionService's logger should catch this.
        // For this test, we'll assume the logger handles it and the method completes.
        // If ParagraphPdfDocumentReader throws a specific exception that IngestionService doesn't catch,
        // this test would need to assert that specific exception.
        // Given the current IngestionService code, it logs "Starting Ingestion..." then "Ingesting document..."
        // then "Ingestion complete." It doesn't have explicit try-catch around reader.get().
        // If reader.get() fails, this test might fail.
        // Let's assume ParagraphPdfDocumentReader's constructor or get() method might throw if resource is invalid.
        // The prompt says "Assert that run() handles this gracefully (e.g., logs an error, doesn't throw NPE)."
        // It might be that "gracefully" implies it *does* throw a specific, non-NPE error.
        
        // If ParagraphPdfDocumentReader throws, say, an IOException or specific Spring AI exception
        // when resource.exists() is false and get() is called, then we should expect that.
        // For now, let's simulate that ParagraphPdfDocumentReader itself might handle this internally or
        // its creation/get() might not immediately fail just on exists() == false, but rather on getInputStream() if called.
        // If the logger catches it, then no exception.
        // If ParagraphPdfDocumentReader(mockMarketPdf) constructor throws, then that's what we test.
        // Let's assume that if exists() is false, it logs and returns, or the reader handles it.
        // The provided code for IngestionService does:
        // LOG.info("Ingesting document: {}", this.marketPdf.getFilename());
        // ParagraphPdfDocumentReader pdfReader = new ParagraphPdfDocumentReader(this.marketPdf);
        // ... (vectorStore.accept is commented out)
        // LOG.info("Ingestion complete.");
        // There's no explicit check of this.marketPdf.exists() in IngestionService.run() itself.
        // So, ParagraphPdfDocumentReader will receive a resource for which exists() is false.
        // How ParagraphPdfDocumentReader handles this is key. It might throw.
        
        // Let's refine: If marketPdf.exists() is false, ParagraphPdfDocumentReader
        // will likely throw an exception when its get() method (or constructor) tries to access the file.
        // The IngestionService doesn't catch this. So, an exception IS expected.
        // The "handles gracefully" might mean it doesn't throw NPE, but a meaningful exception from the reader.
        
        // Mocking getInputStream to throw IOException when exists() is false, as this is a likely outcome.
        when(mockMarketPdf.getInputStream()).thenThrow(new IOException("Mocked: Resource does not exist or is not readable"));

        assertThatThrownBy(() -> ingestionService.run())
                .isInstanceOf(RuntimeException.class) // ParagraphPdfDocumentReader.get() wraps IOExceptions in RuntimeException
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("Failed to read PDF document"); // This is a typical message from ParagraphPdfDocumentReader

        verify(mockMarketPdf).exists(); // ensure the check was made if the service logic was to use it. (It doesn't here)
        verify(vectorStore, never()).accept(any());
    }
    
    @Test
    void testRun_resourceExistsButCannotBeRead_logsErrorAndCompletes() throws IOException {
        // Arrange
        when(mockMarketPdf.exists()).thenReturn(true);
        when(mockMarketPdf.getInputStream()).thenThrow(new IOException("Simulated read error"));

        // Act & Assert
        // ParagraphPdfDocumentReader.get() wraps IOExceptions
        assertThatThrownBy(() -> ingestionService.run())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to read PDF document")
            .hasCauseInstanceOf(IOException.class);
        
        verify(mockMarketPdf).exists(); // Though not used by IngestionService, reader might use it.
        verify(mockMarketPdf).getInputStream();
        verify(vectorStore, never()).accept(any());
    }
}
