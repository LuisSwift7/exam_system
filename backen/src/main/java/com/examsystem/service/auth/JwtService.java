package com.examsystem.service.auth;

import com.examsystem.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JwtService {
  private final JwtProperties jwtProperties;

  public String issueToken(Long userId, String username, String roleCode) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(jwtProperties.getExpireSeconds());
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .addClaims(Map.of(
            "username", username,
            "role", roleCode
        ))
        .signWith(signKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parse(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(signKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private SecretKey signKey() {
    String secret = jwtProperties.getSecret();
    byte[] keyBytes;
    if (secret == null) {
      keyBytes = new byte[0];
    } else if (looksLikeBase64(secret)) {
      keyBytes = Decoders.BASE64.decode(secret);
    } else {
      keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private boolean looksLikeBase64(String value) {
    if (value == null) return false;
    if (value.length() < 32) return false;
    return value.matches("^[A-Za-z0-9+/=]+$");
  }
}

