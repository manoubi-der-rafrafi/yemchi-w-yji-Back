package com.transport.transport.service;

import java.util.HashMap;
import java.util.Map;

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
          <p>Si le bouton ne fonctionne pas, copiez-collez ce lien dans votre navigateur :</p>
          <p><a href="%s">%s</a></p>
          <p>Ce lien expire dans quelques minutes.</p>
        </div>
        """.formatted(safeMessage, verificationUrl, verificationUrl, verificationUrl);

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
          throw new RuntimeException("Erreur Resend: HTTP " + response.code() + " - " + errorBody);
        }
      }
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
