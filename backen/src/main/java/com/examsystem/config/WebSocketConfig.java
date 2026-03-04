package com.examsystem.config;

import com.examsystem.controller.student.ExamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ExamWebSocketHandler examWebSocketHandler;

    public WebSocketConfig(ExamWebSocketHandler examWebSocketHandler) {
        this.examWebSocketHandler = examWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(examWebSocketHandler, "/api/student/exam-taking/{recordId}/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:5174");
    }
}
