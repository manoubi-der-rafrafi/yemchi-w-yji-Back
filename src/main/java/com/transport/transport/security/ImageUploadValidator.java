package com.transport.transport.security;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public final class ImageUploadValidator {
  public static final long STANDARD_MAX_BYTES = 5L * 1024L * 1024L;
  public static final long ANALYSIS_MAX_BYTES = 8L * 1024L * 1024L;
  private static final Set<String> TARGETS = Set.of("produits", "profile");

  private ImageUploadValidator() {}

  public static void validate(MultipartFile file, long maxBytes) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide");
    }
    if (file.getSize() > maxBytes) {
      throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image trop volumineuse");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Format image requis");
    }

    byte[] header = new byte[16];
    int length;
    try (InputStream input = file.getInputStream()) {
      length = input.read(header);
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image illisible");
    }
    if (!hasAllowedSignature(header, length)) {
      throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Format image non autorise");
    }
  }

  public static String validateTarget(String target) {
    String normalized = target == null ? "" : target.trim().toLowerCase(Locale.ROOT);
    if (!TARGETS.contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination upload invalide");
    }
    return normalized;
  }

  private static boolean hasAllowedSignature(byte[] h, int n) {
    boolean jpeg = n >= 3 && u(h[0]) == 0xff && u(h[1]) == 0xd8 && u(h[2]) == 0xff;
    boolean png = n >= 8 && u(h[0]) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G'
        && u(h[4]) == 0x0d && u(h[5]) == 0x0a && u(h[6]) == 0x1a && u(h[7]) == 0x0a;
    boolean gif = n >= 6 && h[0] == 'G' && h[1] == 'I' && h[2] == 'F'
        && h[3] == '8' && (h[4] == '7' || h[4] == '9') && h[5] == 'a';
    boolean webp = n >= 12 && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
        && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P';
    boolean heif = n >= 12 && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p'
        && ((h[8] == 'h' && h[9] == 'e' && (h[10] == 'i' || h[10] == 'v'))
            || (h[8] == 'm' && h[9] == 'i' && h[10] == 'f' && h[11] == '1'));
    return jpeg || png || gif || webp || heif;
  }

  private static int u(byte value) {
    return value & 0xff;
  }
}
