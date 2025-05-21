package com.sourabh;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {

    @Test
    void testMainOutput() {
        // Redirect System.out to capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // Call the main method
        App.main(null);

        // Restore System.out
        System.setOut(originalOut);

        // Assert the captured output
        // Adding a newline character as System.out.println adds one.
        assertEquals("Hello World!" + System.lineSeparator(), outContent.toString());
    }
}
