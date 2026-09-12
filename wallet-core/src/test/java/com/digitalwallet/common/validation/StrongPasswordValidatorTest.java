package com.digitalwallet.common.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exhaustive coverage for the @StrongPassword rule backing DWS-40's
 * "weak password -> 400 WEAK_PASSWORD" requirement. Each assertion isolates
 * exactly one missing character class so a regression in any single check
 * (uppercase/lowercase/digit/special) is caught individually rather than
 * masked by an overly "weak" all-lowercase test string.
 */
class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @Test
    void allCriteriaMet_isValid() {
        assertThat(validator.isValid("Str0ng@Pass", null)).isTrue();
    }

    @Test
    void minimalPasswordWithOneOfEachClass_isValid() {
        assertThat(validator.isValid("Aa1!aaaa", null)).isTrue();
    }

    @Test
    void tooShortPassword_isInvalidEvenWhenAllCharacterClassesArePresent() {
        assertThat(validator.isValid("Aa1!", null)).isFalse();
    }

    @Test
    void missingUppercase_isInvalid() {
        assertThat(validator.isValid("str0ng@pass", null)).isFalse();
    }

    @Test
    void missingLowercase_isInvalid() {
        assertThat(validator.isValid("STR0NG@PASS", null)).isFalse();
    }

    @Test
    void missingDigit_isInvalid() {
        assertThat(validator.isValid("Strong@Pass", null)).isFalse();
    }

    @Test
    void missingSpecialCharacter_isInvalid() {
        assertThat(validator.isValid("Str0ngPass1", null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { "!", "@", "#", "$", "%", "^", "&", "*", "(", ")", ",", ".", "?", "\"", ":", "{", "}",
            "|", "<", ">" })
    void anyRecognizedSpecialCharacter_satisfiesTheSpecialCharacterClass(String specialChar) {
        assertThat(validator.isValid("Str0ngPass" + specialChar, null)).isTrue();
    }

    @Test
    void nullPassword_isValid_delegatesToNotBlankAnnotation() {
        // @StrongPassword only checks composition; presence is @NotBlank's job.
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @Test
    void blankPassword_isValid_delegatesToNotBlankAnnotation() {
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    void emptyPassword_isValid_delegatesToNotBlankAnnotation() {
        assertThat(validator.isValid("", null)).isTrue();
    }
}
