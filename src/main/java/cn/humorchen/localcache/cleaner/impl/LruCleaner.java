package cn.humorchen.localcache.cleaner.impl;

import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import cn.humorchen.localcache.cleaner.ILocalCacheCleaner;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: LRU算法清理
 **/
@Component
@Slf4j
public class LruCleaner implements ILocalCacheCleaner {
    /**
     * 清理策略
     *
     * @return
     */
    @Override
    public CleanStrategyEnum getCleanStrategyEnum() {
        return CleanStrategyEnum.LRU;
    }


    /**
     * 清理一次
     *
     * @param cache
     * @return
     */
    @Override
    public long cleanOnce(Cache<LocalCacheKey, LocalCacheValue> cache) {
        // 由于刚超出内存限制将立即触发进行清理，因此超出的内存数不会很多，不用全排序（n*log n）后删除,也不维护LRU结构，而采用删最大值方法（删一次为n），每次过一遍，减少遍历次数
        LocalCacheValue localCacheValue = cache.asMap().values().stream().max((v1, v2) -> (int) (v2.getLastAccessTimestamp() - v1.getLastAccessTimestamp())).orElse(null);
        if (localCacheValue != null) {
            LocalCacheKey key = localCacheValue.getKey();
            cleanKey(cache, key);
            int jsonLength = localCacheValue.getJsonLength();
            return jsonLength * 2L;
        }
        return 0L;
    }


}
