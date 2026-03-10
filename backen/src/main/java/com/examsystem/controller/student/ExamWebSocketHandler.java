package com.examsystem.controller.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.examsystem.service.exam.ExamTakingService;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.nio.file.*;
import java.util.Base64;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ExamWebSocketHandler extends TextWebSocketHandler {

    private final ExamTakingService examTakingService;
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getUri());
        // 从路径中获取recordId
        String path = session.getUri().getPath();
        System.out.println("WebSocket path: " + path);
        String[] parts = path.split("/");
        System.out.println("WebSocket path parts length: " + parts.length);
        for (int i = 0; i < parts.length; i++) {
            System.out.println("Part " + i + ": " + parts[i]);
        }
        
        // 查找recordId - 它应该在"exam-taking"之后
        Long recordId = null;
        for (int i = 0; i < parts.length; i++) {
            if ("exam-taking".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    recordId = Long.parseLong(parts[i + 1]);
                    break;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid recordId format: " + parts[i + 1]);
                }
            }
        }
        
        if (recordId == null) {
            throw new IllegalArgumentException("Unable to extract recordId from path: " + path);
        }
        
        sessions.put(recordId, session);
        System.out.println("WebSocket connected: " + recordId);
        
        // 发送连接成功消息
        sendMessage(recordId, Map.of("type", "connectionSuccess", "message", "WebSocket连接成功"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            // 从路径中获取recordId
            Long recordId = extractRecordId(session);
            
            System.out.println("Received message from recordId " + recordId + ": " + message.getPayload());
            System.out.println("Message size: " + message.getPayload().length() + " bytes");
            
            // 解析消息
            Map<?, ?> data = objectMapper.readValue(message.getPayload(), Map.class);
            System.out.println("websocket data: " + data);
            String type = (String) data.get("type");
            
            if (type == null) {
                System.err.println("Message type is null");
                return;
            }
            
            // 处理不同类型的消息
            switch (type) {
                case "heartbeat":
                    // 响应心跳
                    System.out.println("Heartbeat received from recordId " + recordId);
                    sendMessage(recordId, Map.of("type", "heartbeatResponse"));
                    break;
                case "test":
                    // 处理测试消息
                    System.out.println("Test message received from recordId " + recordId);
                    sendMessage(recordId, Map.of("type", "testResponse", "message", "Hello from server"));
                    break;
                case "capture":
                    // 处理抓拍图片
                    System.out.println("Capture received from recordId " + recordId);
                    handleCapture(recordId, data);
                    break;
                default:
                    System.out.println("Unknown message type: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error handling text message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 从路径中获取recordId
        Long recordId = extractRecordId(session);
        
        sessions.remove(recordId);
        System.out.println("WebSocket disconnected: " + recordId);
    }
    
    /**
     * 从WebSocket会话中提取recordId
     */
    private Long extractRecordId(WebSocketSession session) {
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        
        // 查找recordId - 它应该在"exam-taking"之后
        Long recordId = null;
        for (int i = 0; i < parts.length; i++) {
            if ("exam-taking".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    recordId = Long.parseLong(parts[i + 1]);
                    break;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid recordId format: " + parts[i + 1]);
                }
            }
        }
        
        if (recordId == null) {
            throw new IllegalArgumentException("Unable to extract recordId from path: " + path);
        }
        
        return recordId;
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
     * 处理抓拍图片
     */
    private void handleCapture(Long recordId, Map<?, ?> data) {
        try {
            String base64Image = (String) data.get("image");
            Long timestamp = (Long) data.get("timestamp");
            
            if (base64Image == null || timestamp == null) {
                System.err.println("Missing required fields for capture");
                return;
            }
            
            // 解码Base64图片
            String base64Data = base64Image.split(",")[1]; // 移除data:image/png;base64,前缀
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            
            // 创建MultipartFile实例
            MultipartFile multipartFile = new Base64MultipartFile(
                "capture_" + timestamp + ".png",
                "image/png",
                imageBytes
            );
            
            // 调用服务保存图片和记录
            examTakingService.saveCapture(multipartFile, recordId);
            
            // 发送确认消息
            sendMessage(recordId, Map.of(
                "type", "captureResponse",
                "message", "图片上传成功",
                "fileName", "capture_" + timestamp + ".png"
            ));
            
        } catch (Exception e) {
            System.err.println("Error handling capture: " + e.getMessage());
            e.printStackTrace();
            
            // 发送错误消息
            sendMessage(recordId, Map.of(
                "type", "captureError",
                "message", "图片上传失败"
            ));
        }
    }
    
    /**
     * Base64转MultipartFile实现类
     */
    private static class Base64MultipartFile implements org.springframework.web.multipart.MultipartFile {
        private final String name;
        private final String contentType;
        private final byte[] content;
        
        public Base64MultipartFile(String name, String contentType, byte[] content) {
            this.name = name;
            this.contentType = contentType;
            this.content = content;
        }
        
        @Override
        public String getName() {
            return name;
        }
        
        @Override
        public String getOriginalFilename() {
            return name;
        }
        
        @Override
        public String getContentType() {
            return contentType;
        }
        
        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }
        
        @Override
        public long getSize() {
            return content.length;
        }
        
        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }
        
        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }
        
        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
    
    /**
     * 停止心跳检测
     */
    public void stopHeartbeatCheck() {
        executorService.shutdown();
    }
}
