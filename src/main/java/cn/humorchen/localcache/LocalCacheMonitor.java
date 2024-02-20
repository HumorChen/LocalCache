package cn.humorchen.localcache;

import cn.hutool.core.io.unit.DataSizeUtil;
import cn.humorchen.localcache.config.LocalCacheGlobalConfig;
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

import static cn.humorchen.localcache.LocalCacheContext.*;
/**
 * @author  humorchen
 * date: 2023/11/23
 * description: 本地缓存监视器
 * 打印缓存的命中率、访问数、淘汰数等信息
 **/
@Component
@Slf4j
@Getter
public class LocalCacheMonitor {

    /**
     * 纳秒到微秒的倍数
     */
    private static final BigDecimal NANOS_TO_MILLS = BigDecimal.valueOf(1000000);

    @Autowired
    private LocalCacheGlobalConfig config;



    /**
     * 监控打印是否启用
     *
     * @return
     */
    private boolean isMonitorEnabled() {
        return config.getMonitor();
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
            long estimatedSize = cache.estimatedSize();
            long cacheByteSize = LocalCacheSizeUtil.getCacheByteSize(cache);
            long avgValueByteSize = estimatedSize > 0 ? cacheByteSize / estimatedSize : 0;
            long requestCount = stats.requestCount();
            long hitCount = stats.hitCount();
            long missCount = stats.missCount();
            long evictionCount = stats.evictionCount();
            LocalCache localCache = LOCAL_CACHE_MAP.get(cacheName);
            log.info("【本地缓存状态】key：{} ,缓存命中率 {}% , 当前缓存值{} 个,总占用内存 {} ,平均单个内存 {}, 总请求数 {} 次 ,平均加载耗时 {} ms , 缓存命中次数 {} 次 , 缓存未命中次数 {} 次 , 淘汰key次数 {} 次 , 缓存配置：{}",
                    cacheName, hitRate, estimatedSize, DataSizeUtil.format(cacheByteSize), DataSizeUtil.format(avgValueByteSize), requestCount, loadPenaltyInMills, hitCount, missCount, evictionCount, JSONObject.toJSONString(localCache));
        } catch (Exception e) {
            log.error("【本地缓存状态】key：" + cacheName + "打印缓存状态报错", e);
        }
    }

    /**
     * 打印一次整个本地缓存的情况
     */
    private void printMonitor() {
        log.info("-----------------------本地缓存状态打印-----------------------");
        if (config.getDisabled()) {
            log.info("本地缓存已被全局禁用");
        } else {
            LocalCacheLogger.info(null, "项目全局配置：{}", JSONObject.toJSONString(config));
            LocalCacheContext.getCacheMap().forEach(this::printCacheStatus);
        }

        log.info("-----------------------本地缓存状态打印结束-----------------------");
    }

    /**
     * 定时任务打印
     */
    @Scheduled(fixedDelay = 60 * 1000)
    public void printScheduled() {
        if (isMonitorEnabled()) {
            printMonitor();
        }
    }
}
