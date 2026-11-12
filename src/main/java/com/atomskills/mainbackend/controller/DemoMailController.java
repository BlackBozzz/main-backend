package com.atomskills.mainbackend.controller;

import com.argent.servicecommon.mail.ArgentMailMessage;
import com.argent.servicecommon.mail.ArgentQueuedMailResult;
import com.argent.servicecommon.mail.ArgentQueuedMailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Пример отправки через очередь ArgentMail. Только администратор может инициировать рассылку. */
@RestController
@RequestMapping("/api/demo/mail")
@PreAuthorize("@argentAccess.action('admin')")
public class DemoMailController {
    private final ObjectProvider<ArgentQueuedMailService> queuedMail;

    public DemoMailController(ObjectProvider<ArgentQueuedMailService> queuedMail) {
        this.queuedMail = queuedMail;
    }

    public record MailAvailability(boolean enabled) { }
    public record MailRequest(
            @NotBlank String target,
            @NotBlank String recipient,
            @NotBlank @Size(max = 255) String subject,
            @NotBlank String body) { }

    @GetMapping("/status")
    public MailAvailability status() {
        return new MailAvailability(queuedMail.getIfAvailable() != null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ArgentQueuedMailResult enqueue(@Valid @RequestBody MailRequest request) {
        ArgentQueuedMailService mail = queuedMail.getIfAvailable();
        if (mail == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Почта отключена. Настройте ARGENT_MAIL_ENABLED и SMTP-переменные.");
        }
        ArgentMailMessage message = ArgentMailMessage.text(request.subject(), request.body());
        String recipient = request.recipient().trim();
        try {
            return switch (request.target()) {
                case "email" -> mail.queueToEmail(recipient, message);
                case "emails" -> mail.queueToEmails(split(recipient), message);
                case "userId" -> mail.queueToUserId(UUID.fromString(recipient), message);
                case "userIds" -> mail.queueToUserIds(split(recipient).stream().map(UUID::fromString).toList(), message);
                case "role" -> mail.queueToRole(recipient, message);
                case "roles" -> mail.queueToRoles(split(recipient), message);
                case "all" -> mail.queueToAllEnabledUsers(message);
                default -> throw new IllegalArgumentException("Неизвестный режим получателей: " + request.target());
            };
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private static List<String> split(String value) {
        List<String> values = Arrays.stream(value.split("[,;\n]"))
                .map(String::trim).filter(part -> !part.isEmpty()).distinct().toList();
        if (values.isEmpty()) throw new IllegalArgumentException("Укажите хотя бы одного получателя");
        return values;
    }
}
