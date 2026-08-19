package com.vela.gramstore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender sender;

    @Autowired
    public EmailService(JavaMailSender sender) {
        this.sender = sender;
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        sender.send(message);
    }

    public void sendVerificationEmail(String to, String otp) {
        send(
                to,
                "Email Verification Code",
                """
                Thanks for creating account on our site.
                Verification Code: %s
                Happy Posting!
                """.formatted(otp)
        );
    }
}
