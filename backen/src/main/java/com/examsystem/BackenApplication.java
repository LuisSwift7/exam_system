package com.examsystem;

import com.examsystem.controller.student.ExamWebSocketHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.examsystem.mapper")
public class BackenApplication {
  public static void main(String[] args) {
    SpringApplication.run(BackenApplication.class, args);
  }

  @Bean
  public CommandLineRunner startWebSocketHeartbeat(ExamWebSocketHandler webSocketHandler) {
    return args -> {
      webSocketHandler.startHeartbeatCheck();
      System.out.println("WebSocket heartbeat check started");
    };
  }
}

