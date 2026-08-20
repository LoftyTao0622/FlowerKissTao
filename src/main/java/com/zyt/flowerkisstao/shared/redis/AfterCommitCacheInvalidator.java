package com.zyt.flowerkisstao.shared.redis;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AfterCommitCacheInvalidator {

    private final RedisCacheService cacheService;

    public AfterCommitCacheInvalidator(RedisCacheService cacheService) {
        this.cacheService = cacheService;
    }

    public void delete(String... keys) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            cacheService.delete(keys);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheService.delete(keys);
            }
        });
    }
}
