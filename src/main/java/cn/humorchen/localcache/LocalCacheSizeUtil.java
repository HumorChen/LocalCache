package cn.humorchen.localcache;

import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * @author  humorchen
 * date: 2023/12/29
 * description: 本地缓存容量计算工具
 **/
@Component
@Slf4j
public class LocalCacheSizeUtil {
    public static final String CACHE_SIZE_CACHE_NAME = "LocalCacheSizeUtilCache";
    public static final String SIZE_PARSE_CACHE_NAME = "LocalCacheSizeUtilSizeParseCache";
    /**
     * 计算缓存容量
     * 100ms后刷新的，保障100ms以内最多计算一次
     *
     * @param methodCacheName
     * @return
     */
    @LocalCache(cacheName = CACHE_SIZE_CACHE_NAME, initCapacity = 8, maxCapacity = 256, expireAfterWrite = 1000 * 60 * 60 * 24, refreshAfterWrite = 100, timeUnit = TimeUnit.MILLISECONDS, skipGlobalMemoryLimit = true)
    public long getMethodCacheByteSize(String methodCacheName) {
        return getMethodCacheByteSize(LocalCacheContext.getMethodCache(methodCacheName));
    }

    /**
     * str的容量大小转化为long字节
     * 例如 "1KB" 转化为 1024
     *
     * @param str
     * @return
     */
    @LocalCache(cacheName = SIZE_PARSE_CACHE_NAME, initCapacity = 16, maxCapacity = 1024, expireAfterWrite = 60 * 60 * 24 * 365, skipGlobalMemoryLimit = true)
    public long parseSizeStr(String str) {
        if (StrUtil.isNotBlank(str)) {
            return DataSizeUtil.parse(str);
        }
        return 0;
    }
    /**
     * 获取方法缓存大小
     *
     * @param cache
     * @return
     */
    public static long getMethodCacheByteSize(Cache<LocalCacheKey, LocalCacheValue> cache) {
        long byteSum = 0;
        if (cache != null) {
            ConcurrentMap<@NonNull LocalCacheKey, @NonNull LocalCacheValue> map = cache.asMap();
            // Key args长度、value长度
            byteSum += map.keySet().stream().mapToInt(LocalCacheKey::getArgsJSONLength).sum();
            byteSum += map.values().stream().mapToInt(LocalCacheValue::getJsonLength).sum();
        }
        // java 1字符等于2字节
        return byteSum * 2L;
    }

    /**
     * 获取方法缓存大小，非方法缓存的不算
     *
     * @param cache
     * @return
     */
    public static long getCacheByteSize(Cache<Object, Object> cache) {
        if (cache != null) {
            ConcurrentMap<@NonNull Object, @NonNull Object> map = cache.asMap();
            if (MapUtil.isNotEmpty(map)) {
                Collection<@NonNull Object> values = map.values();
                Object object = values.stream().findFirst().orElse(null);
                if (object instanceof LocalCacheValue) {
                    return values.stream().mapToInt(o -> ((LocalCacheValue) o).getJsonLength()).sum() * 2L;
                } else {
                    return values.stream().mapToInt(o -> JSONObject.toJSONString(o).length()).sum() * 2L;
                }
            }
        }
        return 0;
    }
}
