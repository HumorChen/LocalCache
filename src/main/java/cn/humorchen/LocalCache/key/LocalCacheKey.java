package cn.humorchen.LocalCache.key;

import cn.humorchen.LocalCache.annotations.LocalCache;
import cn.hutool.core.util.HashUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author: humorchen
 * date: 2023/11/21
 * description: 本地缓存注解式使用时，对被代理方法参数的包装Key对象
 **/
@Data
@Slf4j
public class LocalCacheKey {
    /**
     * 方法全限定名
     */
    private String methodKey;
    /**
     * 被代理方法用到的注解
     */
    private LocalCache localCache;
    /**
     * 被代理的对象
     */
    private Object target;
    /**
     * 被代理方法
     */
    private Method method;
    /**
     * 执行的方法参数
     */
    private Object[] args;

    public LocalCacheKey() {

    }

    public LocalCacheKey(String methodKey, LocalCache localCache, Object target, Method method, Object[] args) {
        this.methodKey = methodKey;
        this.localCache = localCache;
        this.target = target;
        this.method = method;
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof LocalCacheKey)) {
            return false;
        }
        LocalCacheKey annotationKey = (LocalCacheKey) o;
        // 不比对被代理对象，被代理的方法是同一个类的同一个方法，不关心是实例
        // 比对方法
        if (!Objects.equals(annotationKey.getMethodKey(), this.getMethodKey())) {
            return false;
        }
        // 比对参数
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                // 优先对象自己的equals，如果没有自己的equals导致不相同则用json序列化再判断一下是不是相同
                if (!(Objects.equals(args[i], annotationKey.args[i]) || Objects.equals(JSONObject.toJSONString(args[i]), JSONObject.toJSON(annotationKey.args[i])))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        // 加入method
        StringBuilder stringBuilder = new StringBuilder(methodKey);
        // 融入参数hash打乱同method不同参数的hash
        if (args != null) {
            for (Object arg : args) {
                if (arg != null) {
                    stringBuilder.append(arg.hashCode());
                } else {
                    stringBuilder.append("null".hashCode());
                }
            }
        }
        return HashUtil.javaDefaultHash(stringBuilder.toString());
    }


}
