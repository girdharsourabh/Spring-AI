package com.sourabh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello world!
 *
 */
@SpringBootApplication
public class SpringPromptApp
{

    private static final Logger logger = LoggerFactory.getLogger(SpringPromptApp.class);

    public static void main( String[] args )
    {
        SpringApplication.run(SpringPromptApp.class, args);
        logger.debug("Debug message - this should not show by default");
        logger.info("Info message - this should show");

    }
}
