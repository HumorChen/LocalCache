package cn.humorchen.LocalCache.listener;

import cn.humorchen.LocalCache.key.LocalCacheKey;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author: humorchen
 * date: 2023/11/22
 * description: 默认key移除日志监听器，仅当注解上设置了启用日志时会默认加入该监听器
 **/
@Slf4j
public class LocalCacheDefaultLogRemovalListener implements RemovalListener<Object, Object> {


    /**
     * Notifies the listener that a removal occurred at some point in the past.
     * <p>
     * This does not always signify that the key is now absent from the cache, as it may have already
     * been re-added.
     *
     * @param key   the key represented by this entry, or {@code null} if collected
     * @param value the value represented by this entry, or {@code null} if collected
     * @param cause the reason for which the entry was removed
     */
    @Override
    public void onRemoval(@Nullable Object key, @Nullable Object value, @NonNull RemovalCause cause) {
        if (key instanceof LocalCacheKey) {
            LocalCacheKey k = (LocalCacheKey) key;
            log.info("本地缓存：{}  参数：{} 被移除，移除原因：{}，当前方法值为：{}", k.getMethodKey(), JSONObject.toJSONString(k.getArgs()), cause, JSONObject.toJSONString(value));
        }
    }
}
