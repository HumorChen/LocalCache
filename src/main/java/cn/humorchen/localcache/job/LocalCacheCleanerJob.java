package cn.humorchen.localcache.job;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.humorchen.localcache.*;
import cn.humorchen.localcache.aspect.LocalCacheAspect;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import cn.humorchen.localcache.cleaner.ILocalCacheCleaner;
import cn.humorchen.localcache.cleaner.LocalCacheCleanerFactory;
import cn.humorchen.localcache.config.LocalCacheGlobalConfig;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: 本地缓存清理器
 **/
@Component
@Slf4j
public class LocalCacheCleanerJob {
    @Autowired
    private LocalCacheGlobalConfig config;
    @Autowired
    private LocalCacheSizeUtil localCacheSizeUtil;

    /**
     * 执行清理的锁
     */
    private ReentrantLock cleanLock = new ReentrantLock();


    /**
     * 清理 方法缓存的 LocalCacheValue
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cleanMethodCacheLocalCacheValue() {
        cleanLock.lock();
        try {
            int handled = 0;
            Map<String, Cache<LocalCacheKey, LocalCacheValue>> cacheMap = LocalCacheAspect.cacheMap;
            ReferenceQueue<Object> valueReferenceQueue = LocalCacheAspect.valueReferenceQueue;
            Reference<?> poll;
            while ((poll = valueReferenceQueue.poll()) != null) {
                if (poll instanceof LocalCacheValue) {
                    LocalCacheValue localCacheValue = (LocalCacheValue) poll;
                    LocalCacheKey localCacheKey = localCacheValue.getKey();
                    String methodKey = localCacheKey.getMethodKey();
                    LocalCache localCache = localCacheKey.getLocalCache();
                    Cache<LocalCacheKey, LocalCacheValue> cache = cacheMap.get(methodKey);
                    // 使缓存失效
                    cache.invalidate(localCacheKey);
                    handled++;
                    LocalCacheLogger.info(localCache, "本地缓存：{} 值已被gc回收，缓存值失效", localCacheKey.getMethodKey());
                }
            }
            if (handled > 0) {
                LocalCacheLogger.debug(null, "本地缓存 清理被GC释放掉的缓存 执行结束,总计处理被GC回收对象 {} 个", handled);
            }
        } catch (Exception e) {
            log.error("清理本地缓存报错(cleanMethodCacheLocalCacheValue)", e);
        } finally {
            cleanLock.unlock();
        }
    }

    /**
     * 方法缓存容量限制的定时清理
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void cleanMethodCacheForMemoryLimit() {
        LocalCacheLogger.debug(null, "本地缓存 方法缓存-内存回收 执行开始");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Map<String, Cache<LocalCacheKey, LocalCacheValue>> cacheMap = LocalCacheAspect.cacheMap;
        cacheMap.keySet().forEach(this::cleanMethodCacheForMemoryLimit);
        stopWatch.stop();
        LocalCacheLogger.debug(null, "本地缓存 方法缓存-内存回收 执行结束 耗时 {} ms", stopWatch.getLastTaskTimeMillis());
    }

    /**
     * 清理内存
     *
     * @param methodKey
     */
    public void cleanMethodCacheForMemoryLimit(String methodKey) {
        cleanLock.lock();
        try {
            long oneCacheUsedMemoryMaxSize = config.getOneCacheUsedMemoryMaxSizeByte() != null && config.getOneCacheUsedMemoryMaxSizeByte() >= 0 ? config.getOneCacheUsedMemoryMaxSizeByte() : 0;
            Map<String, Cache<LocalCacheKey, LocalCacheValue>> cacheMap = LocalCacheAspect.cacheMap;
            Cache<LocalCacheKey, LocalCacheValue> cache = cacheMap.get(methodKey);
            ConcurrentMap<@NonNull LocalCacheKey, @NonNull LocalCacheValue> map = cache.asMap();
            if (MapUtil.isNotEmpty(map)) {
                LocalCacheKey localCacheKey = map.keySet().stream().findFirst().orElse(null);
                if (localCacheKey == null) {
                    LocalCacheLogger.error(null, "清理失败，获取到的key为空");
                    return;
                }
                LocalCache localCache = localCacheKey.getLocalCache();
                long usedMemoryMaxSizeByte = localCacheSizeUtil.parseSizeStr(localCache.usedMemoryMaxSize());
                long usedMemoryMaxSize = usedMemoryMaxSizeByte >= 0 ? usedMemoryMaxSizeByte : 0;
                // 判定是否要执行清理
                // 都是无限制，无需清理
                if (Math.max(oneCacheUsedMemoryMaxSize, usedMemoryMaxSize) == 0) {
                    return;
                }
                // 配置取小
                long limit = usedMemoryMaxSize > 0 ? (oneCacheUsedMemoryMaxSize > 0 ? Math.min(usedMemoryMaxSize, oneCacheUsedMemoryMaxSize) : usedMemoryMaxSize) : oneCacheUsedMemoryMaxSize;

                long cacheByteSize = localCacheSizeUtil.getMethodCacheByteSize(LocalCacheContext.getCacheName(cache));
                long expectCleanSize = cacheByteSize - limit;
                if (expectCleanSize > 0) {
                    // 需要清理
                    CleanStrategyEnum cleanStrategyEnum = localCache.cleanStrategy() != null ? localCache.cleanStrategy() : (StrUtil.isNotBlank(config.getCleanStrategy()) ? CleanStrategyEnum.of(config.getCleanStrategy()) : null);
                    ILocalCacheCleaner cleaner = LocalCacheCleanerFactory.getCleaner(cleanStrategyEnum);
                    long cleaned = cleaner.clean(cache, cacheByteSize, expectCleanSize);
                    LocalCacheLogger.info(null, "本地缓存执行清理（cleanMethodCacheForMemoryLimit） 清理缓存：{}  清理了{} ", methodKey, DataSizeUtil.format(cleaned));
                } else {
                    LocalCacheLogger.debug(null, "本地缓存执行清理（cleanMethodCacheForMemoryLimit） 缓存：{} 无需清理，当前内存占用 {} ，限制大小为 {}", localCacheKey.getMethodKey(), DataSizeUtil.format(cacheByteSize), DataSizeUtil.format(limit));
                }
            }

        } catch (Exception e) {
            log.error("清理本地缓存报错(cleanMethodCacheForMemoryLimit)", e);
        } finally {
            cleanLock.unlock();
        }
    }


    /**
     * 方法缓存容量限制的定时清理
     */
    @Scheduled(fixedDelay = 60 * 1000, initialDelay = 10)
    public void cleanAllMethodCacheForMemoryLimit() {
        long allMethodCacheUsedMemoryMaxSize = config.getAllMethodCacheUsedMemoryMaxSizeByte() != null && config.getAllMethodCacheUsedMemoryMaxSizeByte() > 0 ? config.getAllMethodCacheUsedMemoryMaxSizeByte() : 0;
        if (allMethodCacheUsedMemoryMaxSize > 0) {
            cleanLock.lock();
            try {
                LocalCacheLogger.debug(null, "本地缓存 方法缓存-全局内存占用限制回收内存 执行开始");
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                Map<String, Cache<LocalCacheKey, LocalCacheValue>> cacheMap = LocalCacheAspect.cacheMap;
                if (MapUtil.isNotEmpty(cacheMap)) {
                    long cacheMapByteSize = cacheMap.keySet().stream().mapToLong(cacheName -> localCacheSizeUtil.getMethodCacheByteSize(cacheName)).sum();
                    long expectCleanSize = cacheMapByteSize - allMethodCacheUsedMemoryMaxSize;
                    if (expectCleanSize > 0) {
                        CleanStrategyEnum cleanStrategy = CleanStrategyEnum.of(config.getCleanStrategy());
                        ILocalCacheCleaner cleaner = LocalCacheCleanerFactory.getCleaner(cleanStrategy);
                        long cleaned = cleaner.clean(cacheMap, cacheMapByteSize, expectCleanSize);
                        // 把容量大小的缓存给清理掉，避免重复清理
                        LocalCacheContext.cleanMethodCache(LocalCacheSizeUtil.CACHE_SIZE_CACHE_NAME);
                        LocalCacheLogger.info(null, "本地缓存执行清理（cleanAllMethodCacheForMemoryLimit）清理了{} ", DataSizeUtil.format(cleaned));
                    }
                }
                stopWatch.stop();
                LocalCacheLogger.debug(null, "本地缓存 方法缓存-全局内存占用限制回收内存 执行结束, 耗时 {} ms", stopWatch.getLastTaskTimeMillis());

            } catch (Exception e) {
                log.error("清理本地缓存报错(cleanMethodCacheForMemoryLimit)", e);
            } finally {
                cleanLock.unlock();
            }
        }
    }


}
