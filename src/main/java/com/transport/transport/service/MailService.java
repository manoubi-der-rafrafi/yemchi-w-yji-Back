package com.transport.transport.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class MailService {

  private static final Logger logger = LoggerFactory.getLogger(MailService.class);
  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
  private static final String RESEND_API_URL = "https://api.resend.com/emails";

  private final OkHttpClient httpClient;
  private final ObjectMapper objectMapper;

  @Value("${resend.api.key}")
  private String apiKey;

  @Value("${resend.from}")
  private String fromEmail;

  public MailService() {
    this.httpClient = new OkHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  public static class MailDeliveryException extends RuntimeException {
    private final int statusCode;
    private final String providerResponseBody;

    public MailDeliveryException(int statusCode, String providerResponseBody) {
      super("Erreur Resend: HTTP " + statusCode + " - " + providerResponseBody);
      this.statusCode = statusCode;
      this.providerResponseBody = providerResponseBody;
    }

    public int getStatusCode() {
      return statusCode;
    }

    public String getProviderResponseBody() {
      return providerResponseBody;
    }
  }

  /**
   * Envoie un email HTML avec un bouton de verification via Resend.
   */
  public void sendVerificationEmail(String toEmail, String verificationUrl, String customMessage) {
    String safeMessage = (customMessage == null || customMessage.isBlank())
        ? "Veuillez confirmer votre adresse email en cliquant sur le bouton ci-dessous :"
        : customMessage;

    String html = """
        <div style="font-family: Arial, sans-serif; line-height: 1.6;">
          <p>Bonjour,</p>
          <p>%s</p>
          <p style="text-align: center;">
            <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 20px; text-decoration: none; border-radius: 6px;">
              Verifier mon email
            </a>
          </p>
          
          <p>Ce lien expire dans quelques minutes.</p>
        </div>
        """.formatted(safeMessage, verificationUrl);

    sendEmail(toEmail, "Verifiez votre adresse email", html, true);
  }

  /**
   * Envoi generique d'un email (HTML ou texte) via l'API HTTP Resend.
   */
  public void sendEmail(String toEmail, String subject, String body, boolean isHtml) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("resend.api.key est manquant");
    }
    if (fromEmail == null || fromEmail.isBlank()) {
      throw new IllegalStateException("resend.from est manquant");
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("from", fromEmail);
    payload.put("to", toEmail);
    payload.put("subject", subject);
    payload.put(isHtml ? "html" : "text", body);

    if ("onboarding@resend.dev".equalsIgnoreCase(fromEmail)) {
      logger.warn("MailService utilise encore onboarding@resend.dev. Resend peut refuser l'envoi vers des adresses externes.");
    }

    try {
      String json = objectMapper.writeValueAsString(payload);
      RequestBody requestBody = RequestBody.create(json, JSON);

      Request request = new Request.Builder()
          .url(RESEND_API_URL)
          .addHeader("Authorization", "Bearer " + apiKey)
          .addHeader("Content-Type", "application/json")
          .post(requestBody)
          .build();

      try (Response response = httpClient.newCall(request).execute()) {
        if (!response.isSuccessful()) {
          String errorBody = response.body() != null ? response.body().string() : "";
          throw new MailDeliveryException(response.code(), errorBody);
        }
      }
    } catch (MailDeliveryException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
    }
  }

  /**
   * Envoie un email texte simple (corps en texte brut).
   */
  public void sendTextEmail(String toEmail, String subject, String body) {
    sendEmail(toEmail, subject, body, false);
  }
}
