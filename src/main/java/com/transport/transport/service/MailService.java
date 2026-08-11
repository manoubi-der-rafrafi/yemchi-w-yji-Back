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
            <title>Vérification de votre email</title>
          </head>
          <body style="margin:0;padding:0;background-color:#f5f5f5;font-family:Arial,Helvetica,sans-serif;color:#1a1a1a;">
            <!-- Preview text -->
            <div style="display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;">
              Confirmez votre adresse email pour activer votre compte Yemchi W Yji.
            </div>

            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                   style="background-color:#f5f5f5;padding:32px 12px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                         style="max-width:600px;">

                    <!-- ── Header: gradient + logo ── -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#FFA726 0%%,#FF5722 100%%);
                                 border-radius:20px 20px 0 0;padding:36px 32px 28px 32px;text-align:center;">
                        <div style="display:inline-block;width:80px;height:80px;border-radius:50%%;
                                    background:rgba(255,255,255,0.15);
                                    border:2px solid rgba(255,255,255,0.3);
                                    line-height:80px;margin-bottom:16px;">
                          <span style="font-size:34px;">✉️</span>
                        </div>
                        <h1 style="margin:0 0 8px 0;font-size:26px;font-weight:800;color:#ffffff;
                                   text-shadow:0 2px 8px rgba(0,0,0,0.15);">
                          Yemchi W Yji
                        </h1>
                        <p style="margin:0;font-size:13px;font-weight:600;letter-spacing:0.1em;
                                  text-transform:uppercase;color:rgba(255,255,255,0.85);">
                          Vérification de votre email
                        </p>
                      </td>
                    </tr>

                    <!-- ── Body ── -->
                    <tr>
                      <td style="background:#ffffff;padding:36px 32px;
                                 border-left:1px solid #e8e8e8;border-right:1px solid #e8e8e8;">

                        <p style="margin:0 0 18px 0;font-size:17px;font-weight:700;color:#1a1a1a;">
                          Bonjour,
                        </p>
                        <p style="margin:0 0 28px 0;font-size:15px;line-height:1.8;color:#444444;">
                          %s
                        </p>

                        <!-- CTA button -->
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0"
                               style="margin:0 auto 28px auto;">
                          <tr>
                            <td align="center"
                                style="background:linear-gradient(135deg,#FFA726,#FF5722);
                                       border-radius:12px;
                                       box-shadow:0 6px 20px rgba(255,87,34,0.35);">
                              <a href="%s"
                                 style="display:inline-block;padding:15px 36px;font-size:16px;
                                        font-weight:700;color:#ffffff;text-decoration:none;
                                        border-radius:12px;letter-spacing:0.02em;">
                                Vérifier mon email
                              </a>
                            </td>
                          </tr>
                        </table>

                        <!-- Info box -->
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                               style="background:#fff8f0;border:1px solid #ffe0b2;
                                      border-left:4px solid #FFA726;border-radius:12px;
                                      margin-bottom:8px;">
                          <tr>
                            <td style="padding:16px 20px;">
                              <p style="margin:0 0 6px 0;font-size:13px;font-weight:700;color:#e65100;">
                                ℹ️ Informations utiles
                              </p>
                              <p style="margin:0;font-size:13px;line-height:1.7;color:#5d4037;">
                                Ce lien expire dans quelques minutes. Si vous n'êtes pas à l'origine
                                de cette demande, vous pouvez ignorer cet email en toute sécurité.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- ── Footer ── -->
                    <tr>
                      <td style="background:#f9fafb;border-radius:0 0 20px 20px;
                                 border:1px solid #e8e8e8;border-top:none;
                                 padding:20px 32px;text-align:center;">
                        <p style="margin:0 0 6px 0;font-size:13px;font-weight:700;color:#374151;">
                          Yemchi W Yji
                        </p>
                        <p style="margin:0;font-size:12px;line-height:1.6;color:#9ca3af;">
                          Cet email a été envoyé automatiquement. Merci de ne pas y répondre.<br>
                          © 2026 Yemchi W Yji — Tous droits réservés.
                        </p>
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

  /**
   * Builds a professional HTML email for password reset.
   */
  public String buildResetPasswordEmailHtml(String resetUrl) {
    String escapedUrl = escapeHtml(resetUrl);
    return """
        <!DOCTYPE html>
        <html lang="fr">
          <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Réinitialisation de votre mot de passe</title>
          </head>
          <body style="margin:0;padding:0;background-color:#f5f5f5;font-family:Arial,Helvetica,sans-serif;color:#1a1a1a;">
            <!-- Preview text -->
            <div style="display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;">
              Réinitialisez votre mot de passe Yemchi W Yji — lien valable 30 minutes.
            </div>

            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                   style="background-color:#f5f5f5;padding:32px 12px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                         style="max-width:600px;">

                    <!-- ── Header: gradient + logo ── -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#FFA726 0%%,#FF5722 100%%);
                                 border-radius:20px 20px 0 0;padding:36px 32px 28px 32px;text-align:center;">
                        <!-- Decorative top ring -->
                        <div style="display:inline-block;width:80px;height:80px;border-radius:50%%;
                                    background:rgba(255,255,255,0.15);
                                    border:2px solid rgba(255,255,255,0.3);
                                    line-height:80px;margin-bottom:16px;">
                          <!-- Lock icon built from text -->
                          <span style="font-size:34px;">🔒</span>
                        </div>
                        <h1 style="margin:0 0 8px 0;font-size:26px;font-weight:800;color:#ffffff;
                                   text-shadow:0 2px 8px rgba(0,0,0,0.15);">
                          Yemchi W Yji
                        </h1>
                        <p style="margin:0;font-size:13px;font-weight:600;letter-spacing:0.1em;
                                  text-transform:uppercase;color:rgba(255,255,255,0.85);">
                          Réinitialisation du mot de passe
                        </p>
                      </td>
                    </tr>

                    <!-- ── Body ── -->
                    <tr>
                      <td style="background:#ffffff;padding:36px 32px;
                                 border-left:1px solid #e8e8e8;border-right:1px solid #e8e8e8;">

                        <p style="margin:0 0 18px 0;font-size:17px;font-weight:700;color:#1a1a1a;">
                          Bonjour,
                        </p>
                        <p style="margin:0 0 24px 0;font-size:15px;line-height:1.8;color:#444444;">
                          Vous avez demandé la réinitialisation de votre mot de passe sur
                          <strong>Yemchi W Yji</strong>. Cliquez sur le bouton ci-dessous
                          pour choisir un nouveau mot de passe.
                        </p>

                        <!-- CTA button -->
                        <table role="presentation" cellspacing="0" cellpadding="0" border="0"
                               style="margin:0 auto 28px auto;">
                          <tr>
                            <td align="center"
                                style="background:linear-gradient(135deg,#FFA726,#FF5722);
                                       border-radius:12px;
                                       box-shadow:0 6px 20px rgba(255,87,34,0.35);">
                              <a href="%s"
                                 style="display:inline-block;padding:15px 36px;font-size:16px;
                                        font-weight:700;color:#ffffff;text-decoration:none;
                                        border-radius:12px;letter-spacing:0.02em;">
                                Réinitialiser mon mot de passe
                              </a>
                            </td>
                          </tr>
                        </table>

                        <!-- Info box -->
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                               style="background:#fff8f0;border:1px solid #ffe0b2;
                                      border-left:4px solid #FFA726;border-radius:12px;
                                      margin-bottom:24px;">
                          <tr>
                            <td style="padding:16px 20px;">
                              <p style="margin:0 0 6px 0;font-size:13px;font-weight:700;color:#e65100;">
                                ⏱ Ce lien expire dans 30 minutes
                              </p>
                              <p style="margin:0;font-size:13px;line-height:1.7;color:#5d4037;">
                                Pour des raisons de sécurité, ce lien ne peut être utilisé qu'une seule fois.
                                Après expiration, vous devrez refaire une demande de réinitialisation.
                              </p>
                            </td>
                          </tr>
                        </table>

                        <!-- Security notice -->
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0"
                               style="background:#f9fafb;border:1px solid #e5e7eb;
                                      border-radius:12px;margin-bottom:8px;">
                          <tr>
                            <td style="padding:14px 18px;">
                              <p style="margin:0;font-size:13px;line-height:1.7;color:#6b7280;">
                                🔐 <strong>Vous n'avez pas demandé cette réinitialisation ?</strong><br>
                                Ignorez simplement cet email. Votre mot de passe actuel reste inchangé.
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- ── Footer ── -->
                    <tr>
                      <td style="background:#f9fafb;border-radius:0 0 20px 20px;
                                 border:1px solid #e8e8e8;border-top:none;
                                 padding:20px 32px;text-align:center;">
                        <p style="margin:0 0 6px 0;font-size:13px;font-weight:700;color:#374151;">
                          Yemchi W Yji
                        </p>
                        <p style="margin:0;font-size:12px;line-height:1.6;color:#9ca3af;">
                          Cet email a été envoyé automatiquement. Merci de ne pas y répondre.<br>
                          © 2026 Yemchi W Yji — Tous droits réservés.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.formatted(escapedUrl);
  }


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
