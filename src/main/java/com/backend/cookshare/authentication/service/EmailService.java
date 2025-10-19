package com.backend.cookshare.authentication.service;

import com.backend.cookshare.authentication.dto.MailBody;
import org.thymeleaf.context.Context;

public interface EmailService {
    void sendSimpleMessage(MailBody mailBody);

    void sendHtmlMessage(String to, String subject, String templateName, Context context);
}
