package com.digitalwallet.service;

import com.digitalwallet.domain.entity.Transaction;
import com.digitalwallet.domain.entity.User;
import com.digitalwallet.domain.enums.TransactionStatus;
import com.digitalwallet.domain.enums.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private EmailService emailService;

    @Test
    void sendTransactionEmail_delegatesHtmlTemplateToEmailService() {
        EmailNotificationService service = emailNotificationService();
        User user = user();
        Transaction transaction = transaction(TransactionType.TRANSFER, TransactionStatus.COMPLETED);

        service.sendTransactionEmail(user, transaction);

        verify(emailService).sendHtmlEmail(eq(user.getEmail()),
                eq("Thong bao giao dich Digital Wallet"), contains(transaction.getReferenceNumber()));
        verify(emailService).sendHtmlEmail(eq(user.getEmail()),
                eq("Thong bao giao dich Digital Wallet"), contains("Digital Wallet System"));
        verify(emailService).sendHtmlEmail(eq(user.getEmail()),
                eq("Thong bao giao dich Digital Wallet"), contains("Nếu bạn không thực hiện giao dịch này"));
    }

    @Test
    void sendWithdrawalStatusEmail_swallowsMailException() {
        EmailNotificationService service = emailNotificationService();
        User user = user();
        Transaction transaction = transaction(TransactionType.WITHDRAW, TransactionStatus.FAILED);
        doThrow(new MailSendException("mailhog unavailable"))
                .when(emailService).sendHtmlEmail(eq(user.getEmail()), eq("Cap nhat rut tien Digital Wallet"), contains("Rejected"));

        assertThatCode(() -> service.sendWithdrawalStatusEmail(user, transaction, "Rejected"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendSecurityAlertEmail_swallowsMailException() {
        EmailNotificationService service = emailNotificationService();
        User user = user();
        Transaction transaction = transaction(TransactionType.TRANSFER, TransactionStatus.PENDING);
        doThrow(new MailSendException("mailhog unavailable"))
                .when(emailService).sendHtmlEmail(eq(user.getEmail()), eq("Canh bao bao mat Digital Wallet"), contains("risk"));

        assertThatCode(() -> service.sendSecurityAlertEmail(user, transaction, "risk"))
                .doesNotThrowAnyException();
    }

    @Test
    void notificationEmailMethods_useEmailTaskExecutor() throws Exception {
        assertAsyncExecutor("sendTransactionEmail", User.class, Transaction.class);
        assertAsyncExecutor("sendWithdrawalStatusEmail", User.class, Transaction.class, String.class);
        assertAsyncExecutor("sendSecurityAlertEmail", User.class, Transaction.class, String.class);
    }

    @Test
    void templateRenderer_escapesHtmlVariables() {
        EmailTemplateService templateService = new EmailTemplateService();

        String html = templateService.render("security-alert.html", java.util.Map.of(
                "subject", "Canh bao",
                "fullName", "<script>alert(1)</script>",
                "alertMessage", "risk",
                "referenceNumber", "TXN-001"));

        assertThat(html).contains("&lt;script&gt;alert(1)&lt;/script&gt;");
        assertThat(html).doesNotContain("<script>alert(1)</script>");
    }

    private EmailNotificationService emailNotificationService() {
        return new EmailNotificationService(emailService, new EmailTemplateService());
    }

    private void assertAsyncExecutor(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = EmailNotificationService.class.getMethod(methodName, parameterTypes);
        Async async = method.getAnnotation(Async.class);
        assertThat(async).isNotNull();
        assertThat(async.value()).isEqualTo("emailTaskExecutor");
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setFullName("Nguyen Van A");
        return user;
    }

    private Transaction transaction(TransactionType type, TransactionStatus status) {
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setReferenceNumber("TXN-001");
        transaction.setTransactionType(type);
        transaction.setAmount(new BigDecimal("15000000"));
        transaction.setStatus(status);
        return transaction;
    }
}