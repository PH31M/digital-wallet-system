package com.digitalwallet.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailTemplateService {

    private static final String TEMPLATE_ROOT = "templates/email/";

    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public String render(String templateName, Map<String, ?> variables) {
        String html = templateCache.computeIfAbsent(templateName, this::loadTemplate);
        for (Map.Entry<String, ?> entry : variables.entrySet()) {
            html = html.replace("{{" + entry.getKey() + "}}", escape(entry.getValue()));
        }
        return html;
    }

    private String loadTemplate(String templateName) {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_ROOT + templateName);
        try (var inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to load email template: " + templateName, ex);
        }
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}