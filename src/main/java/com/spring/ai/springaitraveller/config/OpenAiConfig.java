package com.spring.ai.springaitraveller.config;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties
@Import(OpenAiAutoConfiguration.class)
public class OpenAiConfig extends OpenAiAutoConfiguration {
}
