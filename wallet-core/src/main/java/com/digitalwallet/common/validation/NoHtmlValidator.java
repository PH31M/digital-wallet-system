package com.digitalwallet.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class NoHtmlValidator implements ConstraintValidator<NoHtml, String> {

    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || !HTML_TAG.matcher(value).find();
    }
}
