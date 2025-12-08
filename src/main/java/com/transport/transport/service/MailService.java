package com.transport.transport.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

  private final JavaMailSender mailSender;

  public MailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  /**
   * Envoie un email HTML avec un bouton de vérification.
   */
  public void sendVerificationEmail(String toEmail, String verificationUrl) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(toEmail);
      helper.setSubject("Vérifiez votre adresse email");

      String html = """
          <div style="font-family: Arial, sans-serif; line-height: 1.6;">
            <p>Bonjour,</p>
            <p>Veuillez confirmer votre adresse email en cliquant sur le bouton ci-dessous :</p>
            <p style="text-align: center;">
              <a href="%s" style="background-color: #2563eb; color: white; padding: 12px 20px; text-decoration: none; border-radius: 6px;">
                Vérifier mon email
              </a>
            </p>
            <p>Si le bouton ne fonctionne pas, copiez-collez ce lien dans votre navigateur :</p>
            <p><a href="%s">%s</a></p>
            <p>Ce lien expire dans quelques minutes.</p>
          </div>
          """.formatted(verificationUrl, verificationUrl, verificationUrl);

      helper.setText(html, true);
      mailSender.send(message);
    } catch (Exception e) {
      throw new RuntimeException("Erreur lors de l'envoi de l'email de vérification", e);
    }
  }
}
