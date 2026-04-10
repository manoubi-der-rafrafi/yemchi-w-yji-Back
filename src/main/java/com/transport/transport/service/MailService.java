package com.transport.transport.service;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

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

  public static class MailTransportException extends RuntimeException {
    public MailTransportException(String message, Throwable cause) {
      super(message, cause);
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
          <body style="margin: 0; padding: 0; background-color: #eef2f7; font-family: Arial, Helvetica, sans-serif; color: #0f172a;">
            <div style="display: none; max-height: 0; overflow: hidden; opacity: 0; mso-hide: all;">
              Confirmez votre adresse email pour activer votre compte.
            </div>
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="background-color: #eef2f7; margin: 0; padding: 32px 12px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width: 640px;">
                    <tr>
                      <td style="padding-bottom: 16px; text-align: center; font-size: 12px; color: #64748b; letter-spacing: 0.12em; text-transform: uppercase;">
                        Yemchi W Yji
                      </td>
                    </tr>
                    <tr>
                      <td style="background-color: #0f172a; background-image: linear-gradient(135deg, #0f172a 0%%, #1e293b 100%%); border-radius: 24px 24px 0 0; padding: 0 32px 32px 32px; color: #ffffff;">
                        <div style="height: 6px; background: linear-gradient(90deg, #1d4ed8 0%%, #2563eb 48%%, #c58a1a 100%%); border-radius: 24px 24px 0 0;"></div>
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                          <tr>
                            <td align="left" style="padding-top: 24px; padding-bottom: 24px;">
                              <span style="display: inline-block; width: 72px; height: 6px; border-radius: 999px; background-color: #c58a1a;"></span>
                            </td>
                            <td align="right" style="padding-top: 24px; padding-bottom: 24px;">
                              <span style="display: inline-block; padding: 8px 14px; border-radius: 999px; background-color: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.14); font-size: 11px; font-weight: 700; letter-spacing: 0.08em; color: #cbd5e1;">
                                VERIFICATION
                              </span>
                            </td>
                          </tr>
                        </table>
                        <p style="margin: 0 0 12px 0; font-size: 13px; font-weight: 700; letter-spacing: 0.12em; text-transform: uppercase; color: #94a3b8;">
                          YEMCHI W YJI
                        </p>
                        <h1 style="margin: 0 0 14px 0; font-size: 34px; line-height: 1.15; font-weight: 800; color: #f8fafc;">
                          Confirmez votre adresse email
                        </h1>
                        <p style="margin: 0; font-size: 17px; line-height: 1.7; color: #cbd5e1;">
                          Un dernier clic suffit pour finaliser l'activation de votre compte.
                        </p>
                      </td>
                    </tr>
                    <tr>
                      <td style="background-color: #ffffff; border-radius: 0 0 24px 24px; padding: 32px; box-shadow: 0 24px 50px rgba(15, 23, 42, 0.08); border: 1px solid #dbe3ee;">
                        <p style="margin: 0 0 16px 0; font-size: 16px; line-height: 1.7; color: #0f172a;">
                          Bonjour,
                        </p>
                        <p style="margin: 0 0 28px 0; font-size: 16px; line-height: 1.8; color: #475569;">
                          %s
                        </p>
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin: 0 auto 28px auto;">
                          <tr>
                            <td align="center" bgcolor="#1d4ed8" style="border-radius: 12px; box-shadow: 0 12px 24px rgba(29, 78, 216, 0.18);">
                              <a href="%s" style="display: inline-block; padding: 15px 30px; font-size: 16px; font-weight: 700; color: #ffffff; text-decoration: none; border-radius: 12px;">
                                Verifier mon email
                              </a>
                            </td>
                          </tr>
                        </table>
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="margin-bottom: 4px; background-color: #f8fafc; border: 1px solid #d9e2ec; border-left: 4px solid #c58a1a; border-radius: 16px;">
                          <tr>
                            <td style="padding: 18px 20px;">
                              <p style="margin: 0 0 8px 0; font-size: 14px; font-weight: 700; color: #0f172a;">
                                Informations utiles
                              </p>
                              <p style="margin: 0; font-size: 14px; line-height: 1.7; color: #475569;">
                                Ce lien expire dans quelques minutes. Si vous n'etes pas a l'origine de cette demande, vous pouvez ignorer cet email en toute securite.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding: 18px 10px 0 10px; text-align: center; font-size: 12px; line-height: 1.6; color: #94a3b8;">
                        Cet email a ete envoye automatiquement. Merci de ne pas y repondre.
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.formatted(escapedMessage, escapedUrl);
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
      logger.info("MailService sendEmail start to={} subject={} from={} html={}",
          toEmail, subject, fromEmail, isHtml);
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
          logger.warn("MailService sendEmail provider failure status={} to={} subject={} body={}",
              response.code(), toEmail, subject, errorBody);
          throw new MailDeliveryException(response.code(), errorBody);
        }
        logger.info("MailService sendEmail success to={} subject={}", toEmail, subject);
      }
    } catch (MailDeliveryException e) {
      throw e;
    } catch (IOException e) {
      logger.error("MailService sendEmail network failure to={} subject={}", toEmail, subject, e);
      throw new MailTransportException("Erreur reseau lors de l'envoi de l'email", e);
    } catch (Exception e) {
      logger.error("MailService sendEmail unexpected failure to={} subject={}", toEmail, subject, e);
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
