package com.luminous.config;

import com.google.genai.Client;
import com.luminous.service.ai.AiTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.embedding.GoogleGenAiEmbeddingConnectionDetails;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingModel;
import org.springframework.ai.google.genai.text.GoogleGenAiTextEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Bean
    @ConditionalOnExpression("'${app.ai.enabled:true}' == 'true' and '${app.ai.gemini.api-key:}' != ''")
    public ChatClient aiChatClient(
            @Value("${app.ai.gemini.api-key}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-3.5-flash-lite}") String model,
            @Value("${app.ai.gemini.max-tokens:800}") int maxTokens,
            AiTools aiTools) {
        Client client = Client.builder().apiKey(apiKey).build();
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .maxOutputTokens(maxTokens)
                .build();
        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .options(options)
                .build();
        return ChatClient.builder(chatModel)
                .defaultOptions(GoogleGenAiChatOptions.builder().model(model).maxOutputTokens(maxTokens))
                .defaultTools(aiTools)
                .build();
    }

    @Bean
    @ConditionalOnExpression("'${app.ai.enabled:true}' == 'true' and '${app.ai.gemini.api-key:}' != ''")
    public EmbeddingModel aiEmbeddingModel(
            @Value("${app.ai.gemini.api-key}") String apiKey,
            @Value("${app.ai.embedding.model:gemini-embedding-001}") String model,
            @Value("${app.ai.embedding.dimensions:768}") int dimensions) {
        Client client = Client.builder().apiKey(apiKey).build();
        GoogleGenAiEmbeddingConnectionDetails details = GoogleGenAiEmbeddingConnectionDetails.builder()
                .apiKey(apiKey)
                .genAiClient(client)
                .build();
        GoogleGenAiTextEmbeddingOptions options = GoogleGenAiTextEmbeddingOptions.builder()
                .model(model)
                .dimensions(dimensions)
                .build();
        return new GoogleGenAiTextEmbeddingModel(details, options);
    }
}
