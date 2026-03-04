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

  public String issueAccessToken(Long userId, String username, String roleCode) {
    Date now = new Date();
    // Access token expires in 15 days
    Date exp = new Date(now.getTime() + 15 * 24 * 60 * 60 * 1000);
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .setIssuedAt(now)
        .setExpiration(exp)
        .addClaims(Map.of(
            "username", username,
            "role", roleCode,
            "tokenType", "access"
        ))
        .signWith(signKey(), SignatureAlgorithm.HS256)
        .compact();
  }

  public String issueRefreshToken(Long userId, String username, String roleCode) {
    Date now = new Date();
    // Refresh token expires in 30 minutes
    Date exp = new Date(now.getTime() + 30 * 60 * 1000);
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .setIssuedAt(now)
        .setExpiration(exp)
        .addClaims(Map.of(
            "username", username,
            "role", roleCode,
            "tokenType", "refresh"
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

