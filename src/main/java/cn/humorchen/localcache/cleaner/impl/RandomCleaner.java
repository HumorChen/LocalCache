package cn.humorchen.localcache.cleaner.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.RandomUtil;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import cn.humorchen.localcache.cleaner.ILocalCacheCleaner;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.ArrayList;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description:
 **/
public class RandomCleaner implements ILocalCacheCleaner {
    /**
     * 清理策略
     *
     * @return
     */
    @Override
    public CleanStrategyEnum getCleanStrategyEnum() {
        return CleanStrategyEnum.RANDOM;
    }

    /**
     * 清理一次
     *
     * @param cache
     * @return
     */
    @Override
    public long cleanOnce(Cache<LocalCacheKey, LocalCacheValue> cache) {
        ArrayList<LocalCacheValue> localCacheValues = new ArrayList<>(cache.asMap().values());
        LocalCacheValue localCacheValue = localCacheValues.get(RandomUtil.randomInt(localCacheValues.size()));
        if (localCacheValue != null) {
            LocalCacheKey key = localCacheValue.getKey();
            cleanKey(cache, key);
            int jsonLength = localCacheValue.getJsonLength();
            return jsonLength * 2L;
        }
        return 0L;
    }

    /**
     * 清理缓存
     *
     * @param cache
     * @param cacheByteSize
     * @param expectCleanSize
     * @return 实际清理的大小
     */
    @Override
    public long clean(Cache<LocalCacheKey, LocalCacheValue> cache, long cacheByteSize, long expectCleanSize) {
        ArrayList<LocalCacheValue> localCacheValues = new ArrayList<>(cache.asMap().values());
        long cleanSize = expectCleanSize;
        while (cleanSize > 0 && CollectionUtil.isNotEmpty(localCacheValues)) {
            LocalCacheValue localCacheValue = localCacheValues.get(RandomUtil.randomInt(localCacheValues.size()));
            if (localCacheValue != null) {
                LocalCacheKey key = localCacheValue.getKey();
                cleanKey(cache, key);
                int jsonLength = localCacheValue.getJsonLength();
                cleanSize = cleanSize - jsonLength * 2L;
            } else {
                break;
            }
        }
        return Math.abs(cleanSize) + expectCleanSize;
    }
}
