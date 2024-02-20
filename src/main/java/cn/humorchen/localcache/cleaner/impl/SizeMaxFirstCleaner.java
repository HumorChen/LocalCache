package cn.humorchen.localcache.cleaner.impl;

import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import cn.humorchen.localcache.cleaner.ILocalCacheCleaner;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: 内存大的优先清理
 **/
@Component
@Slf4j
public class SizeMaxFirstCleaner implements ILocalCacheCleaner {
    /**
     * 清理策略
     *
     * @return
     */
    @Override
    public CleanStrategyEnum getCleanStrategyEnum() {
        return CleanStrategyEnum.SIZE_MAX_FIRST;
    }


    /**
     * 清理一次
     *
     * @param cache
     * @return
     */
    @Override
    public long cleanOnce(Cache<LocalCacheKey, LocalCacheValue> cache) {
        if (cache != null) {
            LocalCacheValue localCacheValue = cache.asMap().values().stream().max(Comparator.comparingInt(LocalCacheValue::getJsonLength)).orElse(null);
            if (localCacheValue != null) {
                LocalCacheKey key = localCacheValue.getKey();
                cleanKey(cache, key);
                int jsonLength = localCacheValue.getJsonLength();
                return jsonLength * 2L;
            }
        }
        return 0L;
    }

}
