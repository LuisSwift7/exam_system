package com.examsystem.config;

import com.examsystem.common.ApiResponse;
import com.examsystem.security.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthFilter jwtAuthFilter;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors()
        .and()
        .csrf().disable()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authorizeRequests()
        .antMatchers("/api/auth/**").permitAll()
        .antMatchers("/api/images/**").permitAll()
        .antMatchers("/api/stems/**").permitAll()
        .antMatchers("/api/student/exam-taking/**").permitAll()
        .anyRequest().authenticated()
        .and()
        .exceptionHandling()
        .authenticationEntryPoint((request, response, authException) -> writeUnauthorized(response))
        .and()
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  private void writeUnauthorized(HttpServletResponse response) {
    try {
      response.setStatus(200);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.fail(3001, "未登录或令牌无效")));
    } catch (Exception ignored) {
    }
  }
}

