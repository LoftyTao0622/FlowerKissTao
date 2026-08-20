package com.zyt.flowerkisstao.recommendation.application.service.impl;

import com.zyt.flowerkisstao.operation.application.service.OperationLogService;
import com.zyt.flowerkisstao.recommendation.application.service.WeightConfigService;
import com.zyt.flowerkisstao.recommendation.domain.entity.RecWeightConfig;
import com.zyt.flowerkisstao.recommendation.domain.model.WeightSet;
import com.zyt.flowerkisstao.recommendation.infrastructure.mapper.RecWeightConfigMapper;
import com.zyt.flowerkisstao.recommendation.web.dto.WeightConfigDTO;
import com.zyt.flowerkisstao.shared.exception.BizException;
import com.zyt.flowerkisstao.shared.exception.ErrorCode;
import com.zyt.flowerkisstao.shared.security.CurrentUser;
import com.zyt.flowerkisstao.shared.redis.AfterCommitCacheInvalidator;
import com.zyt.flowerkisstao.shared.redis.RedisCacheService;
import com.zyt.flowerkisstao.shared.redis.RedisKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.Duration;

@Service
public class WeightConfigServiceImpl implements WeightConfigService {

    private final RecWeightConfigMapper weightMapper;
    private final OperationLogService operationLogService;
    private final RedisCacheService cacheService;
    private final AfterCommitCacheInvalidator cacheInvalidator;
    private final RedisKey redisKey;

    public WeightConfigServiceImpl(RecWeightConfigMapper weightMapper,
                                   OperationLogService operationLogService,
                                   RedisCacheService cacheService,
                                   AfterCommitCacheInvalidator cacheInvalidator,
                                   RedisKey redisKey) {
        this.weightMapper = weightMapper;
        this.operationLogService = operationLogService;
        this.cacheService = cacheService;
        this.cacheInvalidator = cacheInvalidator;
        this.redisKey = redisKey;
    }

    @Override
    public WeightSet current() {
        WeightSet cached = cacheService.get(redisKey.recommendWeights(), WeightSet.class).orElse(null);
        if (cached != null) {
            return cached;
        }
        RecWeightConfig config = weightMapper.selectById(RecWeightConfig.SINGLETON_ID);
        // 配置行缺失时退回方案默认口径。让推荐"算不出来"比"用默认权重算"糟糕得多——
        // 前者整个功能瘫痪，后者只是没用上管理员的自定义值
        WeightSet weights = config == null ? WeightSet.DEFAULT : WeightSet.from(config);
        cacheService.set(redisKey.recommendWeights(), weights, Duration.ofMinutes(5));
        return weights;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WeightConfigDTO dto) {
        // 数据库的 ck_rec_weight_sum 是最后一道防线，但它只在 MySQL 8.0.16+ 生效，
        // 而且报出来的是一句英文约束名。这里拦一道，给用户看得懂的话
        if (dto.sum() != RecWeightConfig.WEIGHT_SUM) {
            throw new BizException(ErrorCode.REC_WEIGHT_SUM_INVALID,
                    "七项权重之和必须等于 100，当前是 " + dto.sum());
        }

        RecWeightConfig before = weightMapper.selectById(RecWeightConfig.SINGLETON_ID);
        Map<String, Object> beforeSnapshot = snapshot(before);

        RecWeightConfig config = new RecWeightConfig();
        config.setId(RecWeightConfig.SINGLETON_ID);
        config.setWLight(dto.getLight());
        config.setWTemp(dto.getTemp());
        config.setWHumidity(dto.getHumidity());
        config.setWCare(dto.getCare());
        config.setWSpace(dto.getSpace());
        config.setWBudget(dto.getBudget());
        config.setWPreference(dto.getPreference());
        config.setTopN(dto.getTopN());
        config.setUpdatedBy(CurrentUser.requireUserId());

        // 单行配置由 data.sql 种下。真丢了就补一行，不然管理员改不动
        if (weightMapper.updateById(config) == 0) {
            weightMapper.insert(config);
        }

        // 只记录权重白名单字段，不带用户资料或请求 body
        operationLogService.record("recommendation", "CONFIG", "rec_weight_config",
                RecWeightConfig.SINGLETON_ID, beforeSnapshot, snapshot(config));
        cacheInvalidator.delete(redisKey.recommendWeights());
    }

    private static Map<String, Object> snapshot(RecWeightConfig config) {
        if (config == null) {
            return null;
        }
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("light", config.getWLight());
        values.put("temp", config.getWTemp());
        values.put("humidity", config.getWHumidity());
        values.put("care", config.getWCare());
        values.put("space", config.getWSpace());
        values.put("budget", config.getWBudget());
        values.put("preference", config.getWPreference());
        values.put("topN", config.getTopN());
        return values;
    }
}
