package com.digitalwallet.exception.handler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.digitalwallet.common.request.RequestIds;
import com.digitalwallet.common.response.ApiResponse;
import com.digitalwallet.exception.BusinessException;
import com.digitalwallet.exception.EmailAlreadyExistsException;
import com.digitalwallet.exception.ErrorCode;
import com.digitalwallet.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Detailed coverage for DWS-67: every branch of GlobalExceptionHandler,
 * the WEAK_PASSWORD resolution heuristic, and WARN/ERROR log-level routing.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private ListAppender<ILoggingEvent> logAppender;
    private Logger handlerLogger;

    @BeforeEach
    void attachLogAppender() {
        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        handlerLogger.detachAppender(logAppender);
    }

    // ---------- MethodArgumentNotValidException / validation ----------

    @Test
    void handleValidation_sizeViolationOnPasswordField_mapsToWeakPassword() {
        MethodArgumentNotValidException ex = validationExceptionFor(
                "password", new String[] { "Size.registerRequest.password", "Size.password", "Size.String", "Size" },
                "Password must be at least 8 characters");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getError().getCode()).isEqualTo("WEAK_PASSWORD");
        assertThat(response.getBody().getError().getField()).isEqualTo("password");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Password must be at least 8 characters");
    }

    @Test
    void handleValidation_strongPasswordViolationOnPasswordField_mapsToWeakPassword() {
        MethodArgumentNotValidException ex = validationExceptionFor(
                "password",
                new String[] { "StrongPassword.registerRequest.password", "StrongPassword.password",
                        "StrongPassword.String", "StrongPassword" },
                "Password must contain uppercase, lowercase, number and special character");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request());

        assertThat(response.getBody().getError().getCode()).isEqualTo("WEAK_PASSWORD");
    }

    @Test
    void handleValidation_notBlankViolationOnPasswordField_doesNotMapToWeakPassword() {
        // A missing password should be a generic validation failure, not WEAK_PASSWORD -
        // guards against the resolver being over-eager just because field == "password".
        MethodArgumentNotValidException ex = validationExceptionFor(
                "password", new String[] { "NotBlank.registerRequest.password", "NotBlank.password",
                        "NotBlank.String", "NotBlank" },
                "Password is required");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request());

        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getError().getField()).isEqualTo("password");
    }

    @Test
    void handleValidation_violationOnNonPasswordField_mapsToValidationFailed() {
        MethodArgumentNotValidException ex = validationExceptionFor(
                "email", new String[] { "Email.registerRequest.email", "Email.email", "Email.String", "Email" },
                "Email is invalid");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getError().getField()).isEqualTo("email");
    }

    @Test
    void handleValidation_fieldErrorCodesNull_doesNotThrowAndDefaultsToValidationFailed() {
        MethodArgumentNotValidException ex = validationExceptionFor("password", null, "Password is required");

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request());

        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
    }

    @Test
    void handleValidation_noFieldErrorPresent_returnsGenericMessageAndNullField() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        MethodParameter parameter = dummyMethodParameter();
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex, request());

        assertThat(response.getBody().getError().getCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().getError().getField()).isNull();
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Invalid request data");
    }

    @Test
    void handleValidation_logsAtWarnLevel_notError() {
        MethodArgumentNotValidException ex = validationExceptionFor(
                "email", new String[] { "Email" }, "Email is invalid");

        handler.handleValidation(ex, request());

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    }

    // ---------- BusinessException ----------

    @Test
    void handleBusiness_emailAlreadyExists_returns400WithErrorCode() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new EmailAlreadyExistsException("Email already exists"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getError().getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(response.getBody().getError().getMessage()).isEqualTo("Email already exists");
    }

    @Test
    void handleBusiness_accountLocked_returns403() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.ACCOUNT_LOCKED), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError().getCode()).isEqualTo("ACCOUNT_LOCKED");
    }

    @Test
    void handleBusiness_invalidCredentials_returns401() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new BusinessException(ErrorCode.INVALID_CREDENTIALS), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void handleBusiness_resourceNotFound_returns404() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new ResourceNotFoundException("Wallet not found"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    void handleBusiness_logsAtWarnLevel_notError() {
        handler.handleBusiness(new EmailAlreadyExistsException("Email already exists"), request());

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    }

    @Test
    void handleBusiness_responseCarriesRequestId() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new EmailAlreadyExistsException("Email already exists"), request());

        assertThat(response.getBody().getRequestId()).isNotBlank();
        assertThat(UUID.fromString(response.getBody().getRequestId())).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleBusiness_responseCarriesExistingRequestId() {
        MockHttpServletRequest request = request();
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(RequestIds.ATTRIBUTE, requestId);

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusiness(
                new EmailAlreadyExistsException("Email already exists"), request);

        assertThat(response.getBody().getRequestId()).isEqualTo(requestId);
    }

    // ---------- AccessDeniedException ----------

    @Test
    void handleAccessDenied_returns403WithAccessDeniedCode() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(
                new AccessDeniedException("Forbidden"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError().getCode()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void handleAccessDenied_logsAtWarnLevel_notError() {
        handler.handleAccessDenied(new AccessDeniedException("Forbidden"), request());

        assertThat(logAppender.list).hasSize(1);
        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.WARN);
    }

    // ---------- Fallback / 500 ----------

    @Test
    void handleUnhandled_returns500WithGenericMessage_hidesInternalDetails() {
        RuntimeException internalError = new RuntimeException("Column 'balance' does not exist - leaking SQL detail");

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnhandled(internalError, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
        // The raw exception message must never leak to the client.
        assertThat(response.getBody().getError().getMessage()).doesNotContain("Column 'balance'");
    }

    @Test
    void handleUnhandled_logsAtErrorLevel_withStackTraceAndUri() {
        MockHttpServletRequest req = request();
        req.setRequestURI("/api/wallets/transfer");

        handler.handleUnhandled(new IllegalStateException("boom"), req);

        assertThat(logAppender.list).hasSize(1);
        ILoggingEvent event = logAppender.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getFormattedMessage()).contains("/api/wallets/transfer");
    }

    // ---------- helpers ----------

    private MockHttpServletRequest request() {
        return new MockHttpServletRequest();
    }

    private MethodArgumentNotValidException validationExceptionFor(String field, String[] codes, String message) {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", field, null, false, codes, null, message));
        try {
            return new MethodArgumentNotValidException(dummyMethodParameter(), bindingResult);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void dummyTarget(String password) {
    }
}
