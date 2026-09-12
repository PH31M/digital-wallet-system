package com.digitalwallet.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private Duration window = Duration.ofMinutes(1);
    private int defaultLimit = 120;
    private int authLimit = 20;
    private int moneyMovementLimit = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getAuthLimit() {
        return authLimit;
    }

    public void setAuthLimit(int authLimit) {
        this.authLimit = authLimit;
    }

    public int getMoneyMovementLimit() {
        return moneyMovementLimit;
    }

    public void setMoneyMovementLimit(int moneyMovementLimit) {
        this.moneyMovementLimit = moneyMovementLimit;
    }
}