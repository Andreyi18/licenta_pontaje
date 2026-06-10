package ro.upt.pontaje.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ro.upt.pontaje.exception.BadRequestException;

import java.util.List;

/**
 * Serviciu pentru trimiterea de email-uri (rapoarte cu atașament PDF).
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Value("${app.reports.default-recipient:}")
    private String defaultRecipient;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Verifică dacă serviciul de email este configurat (are credențiale).
     */
    public boolean isConfigured() {
        return fromAddress != null && !fromAddress.isBlank();
    }

    /**
     * Destinatarul implicit configurat pentru rapoarte (poate fi gol).
     */
    public String getDefaultRecipient() {
        return defaultRecipient == null ? "" : defaultRecipient;
    }

    /**
     * Trimite un email cu un atașament PDF.
     *
     * @param to             destinatari principali (obligatoriu cel puțin unul)
     * @param cc             destinatari în copie (poate fi null/gol)
     * @param subject        subiectul email-ului
     * @param body           corpul email-ului (text simplu)
     * @param attachment     conținutul PDF
     * @param attachmentName numele fișierului atașat
     */
    public void sendReportWithAttachment(List<String> to, List<String> cc, String subject,
                                         String body, byte[] attachment, String attachmentName) {
        if (!isConfigured()) {
            throw new BadRequestException(
                    "Serviciul de email nu este configurat. Setați MAIL_USERNAME și MAIL_PASSWORD în mediul aplicației.");
        }
        if (to == null || to.isEmpty()) {
            throw new BadRequestException("Trebuie specificat cel puțin un destinatar.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(to.toArray(new String[0]));
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }
            helper.setSubject(subject);
            helper.setText(body, false);

            if (attachment != null && attachment.length > 0) {
                helper.addAttachment(attachmentName, new ByteArrayResource(attachment));
            }

            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException e) {
            throw new BadRequestException("Trimiterea email-ului a eșuat: " + e.getMessage());
        }
    }
}
