package cn.humorchen.localcache.bean;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.NonNull;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * @author  humorchen
 * date: 2023/12/28
 * description: 本地缓存，方法返回值
 **/
@Getter
public class LocalCacheValue extends SoftReference<Object> {
    /**
     * 值对应的key引用
     */
    private final LocalCacheKey key;
    /**
     * 返回值的json字符串长度
     * -- GETTER --
     * 获取值的json字符串长度
     *
     * @return
     */
    private final int jsonLength;
    /**
     * 上次访问时间
     */
    private long lastAccessTimestamp;

    public LocalCacheValue(@NonNull LocalCacheKey key, Object object, @NonNull ReferenceQueue<Object> referenceQueue) {
        super(object, referenceQueue);
        this.key = key;
        this.jsonLength = JSONObject.toJSONString(object).length();
        this.lastAccessTimestamp = System.currentTimeMillis();
    }

    /**
     * 获取值
     *
     * @return
     */
    public Object getValue() {
        this.lastAccessTimestamp = System.currentTimeMillis();
        return get();
    }


}
