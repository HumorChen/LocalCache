package cn.humorchen.localcache;

import cn.hutool.core.util.StrUtil;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author  humorchen
 * date: 2023/12/27
 * description: 本地缓存上下文
 **/
public class LocalCacheContext {
    /**
     * 缓存对象map
     */
    private static final Map<String, Cache> CACHE_MAP = new ConcurrentHashMap<>();
    /**
     * 保存一个注解缓存的包装key对象，方便打印相关信息
     */
    protected static final Map<String, LocalCache> LOCAL_CACHE_MAP = new ConcurrentHashMap<>();
    /**
     * 获取名字
     */
    protected static final Map<Cache, String> CACHE_NAME_MAP = new HashMap<>();

    /**
     * 把缓存注册到监控器监控
     *
     * @param cacheName
     * @param cache
     */

    public static void register(String cacheName, Cache cache, LocalCache localCache) {
        if (CACHE_MAP.containsKey(cacheName) && cache != CACHE_MAP.get(cacheName)) {
            throw new IllegalArgumentException("本地缓存-缓存名重复：" + cacheName);
        }
        putCache(cacheName, cache, localCache);
    }

    /**
     * 放入cache
     *
     * @param cacheName
     * @param cache
     */
    public static void putCache(String cacheName, Cache<Object, Object> cache, LocalCache localCache) {
        CACHE_MAP.put(cacheName, cache);
        CACHE_NAME_MAP.put(cache, cacheName);
        if (localCache != null) {
            LOCAL_CACHE_MAP.put(cacheName, localCache);
        }
    }

    /**
     * 获取所有缓存
     *
     * @return
     */
    public static Map<String, Cache> getCacheMap() {
        return CACHE_MAP;
    }

    /**
     * 获取
     *
     * @param cacheName
     * @return
     */
    public static Cache<Object, Object> getCache(String cacheName) {
        return CACHE_MAP.get(cacheName);
    }

    /**
     * 获取
     *
     * @param cacheName
     * @return
     */
    public static Cache<LocalCacheKey, LocalCacheValue> getMethodCache(String cacheName) {
        return (Cache<LocalCacheKey, LocalCacheValue>) CACHE_MAP.get(cacheName);
    }
    /**
     * 获取
     *
     * @param cache
     * @return
     */
    public static String getCacheName(Cache cache) {
        return CACHE_NAME_MAP.get(cache);
    }

    /**
     * 获取方法缓存的注解
     *
     * @param cacheName
     * @return
     */
    public static LocalCache getLocalCacheAnnotation(String cacheName) {
        return LOCAL_CACHE_MAP.get(cacheName);
    }
    /**
     * 清理方法缓存
     *
     * @param cacheName
     * @return 删除了多少个缓存
     */
    public static long cleanMethodCache(String cacheName) {
        long size = 0;
        if (StrUtil.isNotBlank(cacheName)) {
            Cache<Object, Object> objectObjectCache = CACHE_MAP.get(cacheName);
            if (objectObjectCache != null) {
                size = objectObjectCache.estimatedSize();
                objectObjectCache.cleanUp();
            }
        }
        return size;
    }
}
