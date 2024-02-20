package cn.humorchen.localcache.cleaner;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.humorchen.localcache.*;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: 本地缓存清理器
 **/
public interface ILocalCacheCleaner {
    /**
     * 清理策略
     *
     * @return
     */
    CleanStrategyEnum getCleanStrategyEnum();


    /**
     * 清理一次
     *
     * @param cache
     * @return
     */
    long cleanOnce(Cache<LocalCacheKey, LocalCacheValue> cache);

    /**
     * 清理缓存
     *
     * @param cache
     * @param cacheByteSize
     * @param expectCleanSize
     * @return 实际清理的大小
     */
    default long clean(Cache<LocalCacheKey, LocalCacheValue> cache, long cacheByteSize, long expectCleanSize) {
        if (cache != null && expectCleanSize > 0) {
            ConcurrentMap<@NonNull LocalCacheKey, @NonNull LocalCacheValue> map = cache.asMap();
            if (MapUtil.isNotEmpty(map)) {
                long cleanSize = expectCleanSize;
                while (cleanSize > 0) {
                    long cleanOnce = cleanOnce(cache);
                    cleanSize = cleanSize - Math.abs(cleanOnce);
                }
                return Math.abs(cleanSize) + expectCleanSize;
            }
        }
        return 0;
    }

    /**
     * 清理缓存
     *
     * @param cacheMap
     * @param cacheByteSize
     * @param expectCleanSize
     */
    default long clean(Map<String, Cache<LocalCacheKey, LocalCacheValue>> cacheMap, long cacheByteSize, long expectCleanSize) {
        if (MapUtil.isNotEmpty(cacheMap) && expectCleanSize > 0) {
            long cleanSize = expectCleanSize;
            List<Cache<LocalCacheKey, LocalCacheValue>> cacheList = new ArrayList<>(cacheMap.values());
            // 去除掉不允许清理的
            cacheList.removeIf(cache -> {
                LocalCache localCacheAnnotation = LocalCacheContext.getLocalCacheAnnotation(LocalCacheContext.getCacheName(cache));
                return localCacheAnnotation != null && localCacheAnnotation.skipGlobalMemoryLimit();
            });
            if (CollectionUtil.isNotEmpty(cacheList)) {
                int cacheSize = cacheList.size();
                // 循环兜底
                long maxTry = 1000;
                while (cleanSize > 0 && maxTry > 0) {
                    // 每次随机抽一个cache来清理一个缓存
                    int randomInt = RandomUtil.randomInt(cacheSize);
                    Cache<LocalCacheKey, LocalCacheValue> cache = cacheList.get(randomInt);
                    ConcurrentMap<@NonNull LocalCacheKey, @NonNull LocalCacheValue> map = cache.asMap();
                    if (MapUtil.isNotEmpty(map)) {
                        // 对抽中的cache执行一次清理
                        long actualClean = clean(cache, LocalCacheSizeUtil.getMethodCacheByteSize(cache), 1);
                        cleanSize = cleanSize - Math.abs(actualClean);
                        if (actualClean == 0) {
                            // 没有减少cleanSize
                            maxTry--;
                        }
                    }
                }
                return Math.abs(cleanSize) + expectCleanSize;
            }
        }
        return 0;
    }

    /**
     * 淘汰Key
     *
     * @param cache
     * @param localCacheKey
     */
    default void cleanKey(Cache<LocalCacheKey, LocalCacheValue> cache, LocalCacheKey localCacheKey) {
        if (cache != null && localCacheKey != null) {
            cache.invalidate(localCacheKey);
            LocalCacheLogger.debug(null, "本地方法缓存内存回收 淘汰Key：{}", JSONObject.toJSONString(localCacheKey));
        }
    }

}
