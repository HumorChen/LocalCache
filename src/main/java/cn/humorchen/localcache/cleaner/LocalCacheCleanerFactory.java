package cn.humorchen.localcache.cleaner;

import cn.humorchen.localcache.LocalCacheLogger;
import cn.humorchen.localcache.cleaner.impl.LruCleaner;
import cn.humorchen.localcache.cleaner.impl.RandomCleaner;
import cn.humorchen.localcache.cleaner.impl.SizeMaxFirstCleaner;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: 本地缓存清理器工厂
 * 由于项目可能只用某个清理器，因此就不所有清理器都注入IOC容器了，走静态带缓存工厂
 **/
@Slf4j
public class LocalCacheCleanerFactory {
    private static final Map<CleanStrategyEnum, ILocalCacheCleaner> map = new ConcurrentHashMap<>(4);
    private static final CleanStrategyEnum DEFAULT_CLEAN_STRATEGY = CleanStrategyEnum.LRU;
    /**
     * 获得cleaner
     *
     * @param cleanStrategyEnum
     * @return
     */
    public static ILocalCacheCleaner getCleaner(CleanStrategyEnum cleanStrategyEnum) {
        if (cleanStrategyEnum == null) {
            cleanStrategyEnum = DEFAULT_CLEAN_STRATEGY;
            LocalCacheLogger.warn(null, "本地缓存 内存回收未配置淘汰机制，启用默认 LRU淘汰机制");
        }
        final CleanStrategyEnum finalCleanStrategyEnum = cleanStrategyEnum;
        return map.computeIfAbsent(finalCleanStrategyEnum, c -> {
            switch (c) {
                case LRU: {
                    return new LruCleaner();
                }
                case SIZE_MAX_FIRST: {
                    return new SizeMaxFirstCleaner();
                }
                case RANDOM: {
                    return new RandomCleaner();
                }
            }
            LocalCacheLogger.error(null, "该本地缓存清理策略无法找到对应实现，cleanStrategyEnum：{}", JSONObject.toJSONString(finalCleanStrategyEnum));
            // 默认用LRU
            return new LruCleaner();
        });
    }
}
