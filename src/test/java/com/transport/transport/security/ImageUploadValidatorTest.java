package com.transport.transport.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class ImageUploadValidatorTest {
  @Test
  void acceptsJpegWhoseSignatureMatchesItsClaim() {
    var image = new MockMultipartFile(
        "file", "photo.jpg", "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00});
    assertDoesNotThrow(() -> ImageUploadValidator.validate(image, ImageUploadValidator.STANDARD_MAX_BYTES));
  }

  @Test
  void rejectsDisguisedExecutableAndSvg() {
    var fake = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "MZ executable".getBytes());
    var svg = new MockMultipartFile("file", "photo.svg", "image/svg+xml", "<svg/>".getBytes());
    assertThrows(ResponseStatusException.class,
        () -> ImageUploadValidator.validate(fake, ImageUploadValidator.STANDARD_MAX_BYTES));
    assertThrows(ResponseStatusException.class,
        () -> ImageUploadValidator.validate(svg, ImageUploadValidator.STANDARD_MAX_BYTES));
  }

  @Test
  void onlyAllowsKnownCloudinaryTargets() {
    assertEquals("profile", ImageUploadValidator.validateTarget("profile"));
    assertEquals("produits", ImageUploadValidator.validateTarget("produits"));
    assertThrows(ResponseStatusException.class,
        () -> ImageUploadValidator.validateTarget("../private"));
  }
}
