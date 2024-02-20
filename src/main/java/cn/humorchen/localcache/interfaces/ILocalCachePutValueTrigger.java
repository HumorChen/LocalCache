package cn.humorchen.localcache.interfaces;

import cn.humorchen.localcache.LocalCache;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;

/**
 * @author  humorchen
 * date: 2023/12/29
 * description: 本地缓存put value钩子
 **/
public interface ILocalCachePutValueTrigger {
    /**
     * put 值 钩子
     * 同步调用钩子函数，请勿加重操作、网络IO操作
     *
     * @param methodCacheKey
     * @param localCache
     * @param localCacheKey
     * @param localCacheValue
     */
    void trigger(String methodCacheKey, LocalCache localCache, LocalCacheKey localCacheKey, LocalCacheValue localCacheValue);
}
