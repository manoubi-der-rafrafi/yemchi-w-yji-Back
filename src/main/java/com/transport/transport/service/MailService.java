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

    String html = buildVerificationEmailHtml(safeMessage, verificationUrl);

    sendEmail(toEmail, "Verifiez votre adresse email", html, true);
  }

  private String buildVerificationEmailHtml(String message, String verificationUrl) {
    String escapedMessage = escapeHtml(message);
    String escapedUrl = escapeHtml(verificationUrl);

    return """
        <!DOCTYPE html>
        <html lang="fr">
          <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Verification de votre email</title>
          </head>
          <body style="margin: 0; padding: 0; background-color: #f4f7fb; font-family: Arial, Helvetica, sans-serif; color: #14213d;">
            <div style="display: none; max-height: 0; overflow: hidden; opacity: 0; mso-hide: all;">
              Confirmez votre adresse email pour activer votre compte.
            </div>
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #f4f7fb; margin: 0; padding: 24px 12px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 640px;">
                    <tr>
                      <td style="padding-bottom: 16px; text-align: center; font-size: 13px; color: #5c677d; letter-spacing: 0.08em; text-transform: uppercase;">
                        Verification de compte
                      </td>
                    </tr>
                    <tr>
                      <td style="background: linear-gradient(135deg, #0f172a 0%%, #1d4ed8 100%%); border-radius: 24px 24px 0 0; padding: 32px 32px 24px 32px; color: #ffffff;">
                        <div style="display: inline-block; background-color: rgba(255,255,255,0.14); border: 1px solid rgba(255,255,255,0.18); border-radius: 999px; padding: 8px 14px; font-size: 12px; font-weight: 700; letter-spacing: 0.04em;">
                          SECURITE
                        </div>
                        <h1 style="margin: 18px 0 12px 0; font-size: 30px; line-height: 1.2; font-weight: 700;">
                          Confirmez votre adresse email
                        </h1>
                        <p style="margin: 0; font-size: 16px; line-height: 1.7; color: #dbeafe;">
                          Un dernier clic suffit pour finaliser l'activation de votre compte.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background-color: #ffffff; border-radius: 0 0 24px 24px; padding: 32px; box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);">
                        <p style="margin: 0 0 16px 0; font-size: 16px; line-height: 1.7;">
                          Bonjour,
                        </p>
                        <p style="margin: 0 0 24px 0; font-size: 16px; line-height: 1.7; color: #334155;">
                          %s
                        </p>
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 0 auto 24px auto;">
                          <tr>
                            <td align="center" bgcolor="#2563eb" style="border-radius: 12px;">
                              <a href="%s" style="display: inline-block; padding: 15px 26px; font-size: 16px; font-weight: 700; color: #ffffff; text-decoration: none; border-radius: 12px;">
                                Verifier mon email
                              </a>
                            </td>
                          </tr>
                        </table>
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-bottom: 24px; background-color: #eff6ff; border: 1px solid #bfdbfe; border-radius: 16px;">
                          <tr>
                            <td style="padding: 18px 20px;">
                              <p style="margin: 0 0 8px 0; font-size: 14px; font-weight: 700; color: #1d4ed8;">
                                Informations utiles
                              </p>
                              <p style="margin: 0; font-size: 14px; line-height: 1.7; color: #334155;">
                                Ce lien expire dans quelques minutes. Si vous n'etes pas a l'origine de cette demande, vous pouvez ignorer cet email en toute securite.
                              </p>
                            </td>
                          </tr>
                        </table>
                        <p style="margin: 0 0 10px 0; font-size: 14px; line-height: 1.6; color: #475569;">
                          Si le bouton ne fonctionne pas, copiez et collez ce lien dans votre navigateur :
                        </p>
                        <p style="margin: 0; word-break: break-word;">
                          <a href="%s" style="font-size: 14px; line-height: 1.7; color: #2563eb; text-decoration: none;">
                            %s
                          </a>
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding: 18px 10px 0 10px; text-align: center; font-size: 12px; line-height: 1.6; color: #64748b;">
                        Cet email a ete envoye automatiquement. Merci de ne pas y repondre.
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.formatted(escapedMessage, escapedUrl, escapedUrl, escapedUrl);
  }

  private String escapeHtml(String value) {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
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
