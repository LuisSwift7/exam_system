package com.examsystem.service.auth;

import com.examsystem.common.BizException;
import com.examsystem.config.CaptchaProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CaptchaService {
  private final CaptchaProperties captchaProperties;

  private final ConcurrentHashMap<String, CaptchaItem> store = new ConcurrentHashMap<>();
  private final Random random = new Random();

  public CaptchaResult generate() {
    cleanupExpired();

    String code = randomCode(captchaProperties.getLength());
    String captchaId = "cpt_" + UUID.randomUUID().toString().replace("-", "");

    BufferedImage image = render(code);
    String base64 = encodePng(image);

    CaptchaItem item = new CaptchaItem(sha256(code.toLowerCase(Locale.ROOT)), Instant.now().plusSeconds(captchaProperties.getExpireSeconds()).toEpochMilli());
    store.put(captchaId, item);
    return new CaptchaResult(captchaId, "data:image/png;base64," + base64);
  }

  public void verify(String captchaId, String captchaCode) {
    cleanupExpired();
    if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
      throw new BizException(2002, "验证码错误或过期");
    }
    CaptchaItem item = store.remove(captchaId);
    if (item == null) {
      throw new BizException(2002, "验证码错误或过期");
    }
    if (item.getExpireAtMillis() <= Instant.now().toEpochMilli()) {
      throw new BizException(2002, "验证码错误或过期");
    }
    String inputHash = sha256(captchaCode.toLowerCase(Locale.ROOT));
    if (!inputHash.equals(item.getAnswerHash())) {
      throw new BizException(2002, "验证码错误或过期");
    }
  }

  private void cleanupExpired() {
    long now = Instant.now().toEpochMilli();
    store.entrySet().removeIf(e -> e.getValue().getExpireAtMillis() <= now);
  }

  private String randomCode(int len) {
    String chars = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < Math.max(4, len); i++) {
      sb.append(chars.charAt(random.nextInt(chars.length())));
    }
    return sb.toString();
  }

  private BufferedImage render(String text) {
    int width = 140;
    int height = 44;
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(new Color(245, 246, 248));
    g.fillRect(0, 0, width, height);

    g.setColor(new Color(210, 214, 221));
    for (int i = 0; i < 8; i++) {
      int x1 = random.nextInt(width);
      int y1 = random.nextInt(height);
      int x2 = random.nextInt(width);
      int y2 = random.nextInt(height);
      g.drawLine(x1, y1, x2, y2);
    }

    g.setFont(new Font("Arial", Font.BOLD, 26));
    for (int i = 0; i < text.length(); i++) {
      g.setColor(new Color(40 + random.nextInt(90), 40 + random.nextInt(90), 40 + random.nextInt(90)));
      int x = 18 + i * 26;
      int y = 30 + random.nextInt(6);
      double angle = (random.nextDouble() - 0.5) * 0.5;
      g.rotate(angle, x, y);
      g.drawString(String.valueOf(text.charAt(i)), x, y);
      g.rotate(-angle, x, y);
    }
    g.dispose();
    return img;
  }

  private String encodePng(BufferedImage image) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(image, "png", baos);
      return Base64.getEncoder().encodeToString(baos.toByteArray());
    } catch (Exception e) {
      throw new BizException(1001, "验证码服务不可用");
    }
  }

  private String sha256(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] bytes = md.digest(value.getBytes());
      return Base64.getEncoder().encodeToString(bytes);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Data
  @AllArgsConstructor
  private static class CaptchaItem {
    private String answerHash;
    private long expireAtMillis;
  }

  @Data
  @AllArgsConstructor
  public static class CaptchaResult {
    private String captchaId;
    private String imageBase64;
  }
}

