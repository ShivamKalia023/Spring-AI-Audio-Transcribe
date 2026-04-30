package com.audio.transcribe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import okhttp3.OkHttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
public class DeepgramConfig {

    @Value("${spring.deepgram.api-key}")
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
}