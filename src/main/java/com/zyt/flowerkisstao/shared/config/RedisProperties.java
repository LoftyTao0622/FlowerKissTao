package com.zyt.flowerkisstao.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.redis")
public class RedisProperties {

    private boolean enabled = true;
    private boolean cacheEnabled = true;
    private String keyPrefix = "flowerkisstao:v1";
    private long ttlJitterSeconds = 15;
    private boolean rateLimitEnabled = true;
    private int loginLimit = 10;
    private int registerLimit = 5;
    private int searchLimit = 60;
    private int recommendationLimit = 10;
    private int paymentLimit = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public long getTtlJitterSeconds() {
        return ttlJitterSeconds;
    }

    public void setTtlJitterSeconds(long ttlJitterSeconds) {
        this.ttlJitterSeconds = ttlJitterSeconds;
    }

    public boolean isRateLimitEnabled() { return rateLimitEnabled; }
    public void setRateLimitEnabled(boolean rateLimitEnabled) { this.rateLimitEnabled = rateLimitEnabled; }
    public int getLoginLimit() { return loginLimit; }
    public void setLoginLimit(int loginLimit) { this.loginLimit = loginLimit; }
    public int getRegisterLimit() { return registerLimit; }
    public void setRegisterLimit(int registerLimit) { this.registerLimit = registerLimit; }
    public int getSearchLimit() { return searchLimit; }
    public void setSearchLimit(int searchLimit) { this.searchLimit = searchLimit; }
    public int getRecommendationLimit() { return recommendationLimit; }
    public void setRecommendationLimit(int recommendationLimit) { this.recommendationLimit = recommendationLimit; }
    public int getPaymentLimit() { return paymentLimit; }
    public void setPaymentLimit(int paymentLimit) { this.paymentLimit = paymentLimit; }
}
