package com.vela.gramstore.listener;

import com.vela.gramstore.entity.OTPResendEvent;
import com.vela.gramstore.entity.UserRegisteredEvent;
import com.vela.gramstore.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OTPEventListener {

    private final EmailService emailService;

    @Autowired
    public OTPEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegister(UserRegisteredEvent event) {
        emailService.sendVerificationEmail(event.email(), event.otp());
    }

    @Async("emailExecutor")
    @EventListener
    public void handleResendOTP(OTPResendEvent event) {
        emailService.sendVerificationEmail(event.email(), event.otp());
    }
}
