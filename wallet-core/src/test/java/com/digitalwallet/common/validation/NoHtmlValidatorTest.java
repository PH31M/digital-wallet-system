package com.digitalwallet.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NoHtmlValidatorTest {

    private final NoHtmlValidator validator = new NoHtmlValidator();

    @Test
    void plainText_isValid() {
        assertThat(validator.isValid("Nguyen Van A", null)).isTrue();
    }

    @Test
    void nullValue_isValid_delegatesToOtherAnnotations() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void emptyString_isValid() {
        assertThat(validator.isValid("", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert(1)</script>",
            "Nguyen <b>Van</b> A",
            "<img src=x onerror=alert(1)>",
            "text with <a href='x'>link</a> inside"
    })
    void textContainingHtmlTags_isInvalid(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }

    @Test
    void lessThanWithoutClosingTag_isValid() {
        assertThat(validator.isValid("5 < 10", null)).isTrue();
    }
}
