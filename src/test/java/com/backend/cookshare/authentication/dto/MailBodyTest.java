package com.backend.cookshare.authentication.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MailBodyTest {

    @Test
    void testBuilderCreatesMailBodyCorrectly() {
        MailBody mail = MailBody.builder()
                .to("test@example.com")
                .subject("Hello")
                .text("This is a test email")
                .build();

        assertEquals("test@example.com", mail.to());
        assertEquals("Hello", mail.subject());
        assertEquals("This is a test email", mail.text());
    }

    @Test
    void testRecordEqualityAndHashCode() {
        MailBody mail1 = MailBody.builder()
                .to("user@example.com")
                .subject("Subject")
                .text("Body text")
                .build();

        MailBody mail2 = MailBody.builder()
                .to("user@example.com")
                .subject("Subject")
                .text("Body text")
                .build();

        assertEquals(mail1, mail2);
        assertEquals(mail1.hashCode(), mail2.hashCode());
    }

    @Test
    void testToStringNotNull() {
        MailBody mail = MailBody.builder()
                .to("a@b.com")
                .subject("Test")
                .text("Hello")
                .build();

        assertNotNull(mail.toString());
        assertTrue(mail.toString().contains("a@b.com"));
        assertTrue(mail.toString().contains("Test"));
        assertTrue(mail.toString().contains("Hello"));
    }
}
