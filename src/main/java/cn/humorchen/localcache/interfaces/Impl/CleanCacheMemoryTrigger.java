package cn.humorchen.localcache.interfaces.Impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.humorchen.localcache.LocalCache;
import cn.humorchen.localcache.LocalCacheContext;
import cn.humorchen.localcache.LocalCacheSizeUtil;
import cn.humorchen.localcache.SignalSpeedLimitUtil;
import cn.humorchen.localcache.aspect.LocalCacheAspect;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import cn.humorchen.localcache.cleaner.ILocalCacheCleaner;
import cn.humorchen.localcache.cleaner.LocalCacheCleanerFactory;
import cn.humorchen.localcache.config.LocalCacheGlobalConfig;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import cn.humorchen.localcache.interfaces.ILocalCachePutValueTrigger;
import cn.humorchen.localcache.job.LocalCacheCleanerJob;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author  humorchen
 * date: 2023/12/29
 * description:
 **/
@Component
public class CleanCacheMemoryTrigger implements ILocalCachePutValueTrigger {

    @Autowired
    private LocalCacheSizeUtil localCacheSizeUtil;
    @Autowired
    private LocalCacheCleanerJob localCacheCleanerJob;
    @Autowired
    private LocalCacheGlobalConfig config;


    /**
     * 信号量工具
     */
    private final Map<String, SignalSpeedLimitUtil> methodSignalUtilMap = new ConcurrentHashMap<>();
    private final SignalSpeedLimitUtil globalMethodSignalUtil = new SignalSpeedLimitUtil(100, 1);

    /**
     * put 值 钩子
     * 同步调用钩子函数，请勿加重操作、网络IO操作
     *
     * @param methodCacheKey
     * @param localCache
     * @param localCacheKey
     * @param localCacheValue
     */
    @Override
    public void trigger(String methodCacheKey, LocalCache localCache, LocalCacheKey localCacheKey, LocalCacheValue localCacheValue) {
        // 方法维度
        if (localCache != null && StrUtil.isNotBlank(localCache.usedMemoryMaxSize()) && StrUtil.isNotBlank(methodCacheKey)) {
            long usedMemoryMaxSize = localCacheSizeUtil.parseSizeStr(localCache.usedMemoryMaxSize());
            // 获取大小带缓存
            long methodCacheByteSize = localCacheSizeUtil.getMethodCacheByteSize(methodCacheKey);
            long expectCleanSize = methodCacheByteSize - usedMemoryMaxSize;
            if (expectCleanSize > 0) {
                // 触发异步清理。100ms内只能触发一次
                SignalSpeedLimitUtil signalSpeedLimitUtil = methodSignalUtilMap.computeIfAbsent(methodCacheKey, (k) -> new SignalSpeedLimitUtil(100, 1));
                if (signalSpeedLimitUtil.tryGetSignal()) {
                    LocalCacheAspect.getMethodCacheExecutor().execute(() -> localCacheCleanerJob.cleanMethodCacheForMemoryLimit(methodCacheKey));
                }
            }
        }
        // 全局维度。100ms内只能触发一次
        triggerAsyncCleanIfNecessary();
    }


    /**
     * 全局维度
     */
    public void triggerAsyncCleanIfNecessary() {
        // 获得信号量，100ms只有1个
        if (globalMethodSignalUtil.tryGetSignal()) {
            long allMethodCacheUsedMemoryMaxSize = config.getAllMethodCacheUsedMemoryMaxSizeByte() != null && config.getAllMethodCacheUsedMemoryMaxSizeByte() > 0 ? config.getAllMethodCacheUsedMemoryMaxSizeByte() : 0;
            if (allMethodCacheUsedMemoryMaxSize > 0) {
                Map<String, Cache<LocalCacheKey, LocalCacheValue>> cacheMap = LocalCacheAspect.cacheMap;
                if (MapUtil.isNotEmpty(cacheMap)) {
                    // 获取大小带缓存
                    long cacheMapByteSize = cacheMap.values().stream().mapToLong(cache -> localCacheSizeUtil.getMethodCacheByteSize(LocalCacheContext.getCacheName(cache))).sum();
                    long expectCleanSize = cacheMapByteSize - allMethodCacheUsedMemoryMaxSize;
                    // 需要清理
                    if (expectCleanSize > 0) {
                        CleanStrategyEnum cleanStrategy = CleanStrategyEnum.of(config.getCleanStrategy());
                        ILocalCacheCleaner cleaner = LocalCacheCleanerFactory.getCleaner(cleanStrategy);
                        // 异步清理
                        LocalCacheAspect.getMethodCacheExecutor().execute(() -> cleaner.clean(cacheMap, cacheMapByteSize, expectCleanSize));
                    }
                }
            }
        }
    }
}
