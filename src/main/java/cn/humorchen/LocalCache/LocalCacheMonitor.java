package cn.humorchen.LocalCache;

import cn.humorchen.LocalCache.annotations.LocalCache;
import cn.humorchen.LocalCache.config.LocalCacheGlobalConfig;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: humorchen
 * date: 2023/11/23
 * description: 本地缓存监视器
 * 打印缓存的命中率、访问数、淘汰数等信息
 **/
@Component
@Slf4j
@Getter
public class LocalCacheMonitor {
    /**
     * 缓存对象map
     */
    private static final Map<String, Cache<Object, Object>> CACHE_MAP = new ConcurrentHashMap<>();
    /**
     * 保存一个注解缓存的包装key对象，方便打印相关信息
     */
    private static final Map<String, LocalCache> LOCAL_CACHE_MAP = new ConcurrentHashMap<>();
    /**
     * 纳秒到微秒的倍数
     */
    private static final BigDecimal NANOS_TO_MILLS = BigDecimal.valueOf(1000000);

    @Autowired
    private LocalCacheGlobalConfig config;
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
        CACHE_MAP.put(cacheName, cache);
        LOCAL_CACHE_MAP.put(cacheName, localCache);
    }

    /**
     * 监控打印是否启用
     *
     * @return
     */
    private boolean isMonitorEnabled() {
        return config.isMonitor();
    }

    /**
     * 纳秒到毫秒，保留三位小数
     *
     * @param nanos
     * @return
     */
    public static BigDecimal nanosToMills(double nanos) {
        return BigDecimal.valueOf(nanos).divide(NANOS_TO_MILLS, 3, RoundingMode.HALF_UP);
    }
    /**
     * 打印缓存情况
     *
     * @param cacheName
     * @param cache
     */
    private void printCacheStatus(String cacheName, Cache<Object, Object> cache) {
        try {
            CacheStats stats = cache.stats();
            double loadPenaltyInNanoS = stats.averageLoadPenalty();
            String loadPenaltyInMills = nanosToMills(loadPenaltyInNanoS).toString();
            BigDecimal hitRate = BigDecimal.valueOf(stats.hitRate() * 100).setScale(2, RoundingMode.HALF_UP);
            LocalCache localCache = LOCAL_CACHE_MAP.get(cacheName);
            log.info("【本地缓存状态】key：{} 总请求数：{} 次 ，缓存命中率：{}%，平均加载耗时：{} ms，淘汰key次数：{} 次，缓存命中次数：{} 次，缓存未命中次数：{}次 缓存配置：{}", cacheName, stats.requestCount(), hitRate, loadPenaltyInMills, stats.evictionCount(), stats.hitCount(), stats.missCount(), JSONObject.toJSONString(localCache));
        } catch (Exception e) {
            log.error("【本地缓存状态】key：" + cacheName + "打印缓存状态报错", e);
        }
    }

    /**
     * 打印一次整个本地缓存的情况
     */
    private void printMonitor() {
        log.info("-----------------------本地缓存状态打印-----------------------");
        if (config.isDisabled()) {
            log.info("本地缓存已被全局禁用");
        } else {
            CACHE_MAP.forEach((cacheName, cache) -> {
                printCacheStatus(cacheName, cache);
            });
        }

        log.info("-----------------------本地缓存状态打印结束-----------------------");
    }

    /**
     * 定时任务打印
     */
    @Scheduled(fixedDelay = 60000)
    public void printScheduled() {
        if (isMonitorEnabled()) {
            printMonitor();
        }
    }
}
