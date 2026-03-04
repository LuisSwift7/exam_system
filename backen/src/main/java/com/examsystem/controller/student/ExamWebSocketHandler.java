package com.examsystem.controller.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ExamWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从路径中获取recordId
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        Long recordId = Long.parseLong(parts[parts.length - 2]);
        
        sessions.put(recordId, session);
        System.out.println("WebSocket connected: " + recordId);
        
        // 发送连接成功消息
        sendMessage(recordId, Map.of("type", "connectionSuccess", "message", "WebSocket连接成功"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 从路径中获取recordId
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        Long recordId = Long.parseLong(parts[parts.length - 2]);
        
        // 解析消息
        Map<?, ?> data = objectMapper.readValue(message.getPayload(), Map.class);
        String type = (String) data.get("type");
        
        // 处理不同类型的消息
        switch (type) {
            case "heartbeat":
                // 响应心跳
                sendMessage(recordId, Map.of("type", "heartbeatResponse"));
                break;
            default:
                System.out.println("Unknown message type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从路径中获取recordId
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        Long recordId = Long.parseLong(parts[parts.length - 2]);
        
        sessions.remove(recordId);
        System.out.println("WebSocket disconnected: " + recordId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket error: " + exception.getMessage());
    }

    /**
     * 发送消息给指定recordId的客户端
     */
    public void sendMessage(Long recordId, Object message) {
        WebSocketSession session = sessions.get(recordId);
        if (session != null && session.isOpen()) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(jsonMessage));
            } catch (IOException e) {
                System.err.println("Failed to send message: " + e.getMessage());
            }
        }
    }

    /**
     * 向所有连接的客户端广播消息
     */
    public void broadcastMessage(Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);
            
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to broadcast message: " + e.getMessage());
        }
    }

    /**
     * 启动心跳检测
     */
    public void startHeartbeatCheck() {
        executorService.scheduleAtFixedRate(() -> {
            for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
                WebSocketSession session = entry.getValue();
                if (!session.isOpen()) {
                    sessions.remove(entry.getKey());
                    System.out.println("Removed closed session: " + entry.getKey());
                }
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    /**
     * 停止心跳检测
     */
    public void stopHeartbeatCheck() {
        executorService.shutdown();
    }
}
