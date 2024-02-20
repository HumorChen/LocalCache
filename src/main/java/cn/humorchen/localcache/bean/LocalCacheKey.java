package cn.humorchen.localcache.bean;

import cn.hutool.core.util.HashUtil;
import cn.hutool.crypto.digest.MD5;
import cn.humorchen.localcache.LocalCache;
import cn.humorchen.localcache.interfaces.ILocalCacheEquals;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author  humorchen
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
    @JSONField(serialize = false)
    private LocalCache localCache;
    /**
     * 被代理的对象
     */
    @JSONField(serialize = false)
    private Object target;
    /**
     * 被代理方法
     */
    @JSONField(serialize = false)
    private Method method;
    /**
     * 执行的方法参数
     */
    private Object[] args;
    /**
     * args的json的md5
     */
    private String[] argsMd5;
    /**
     * Object[] args的json字符串长度
     * 长度一致不代表相等
     */
    private int argsJSONLength;


    public LocalCacheKey() {

    }

    public LocalCacheKey(String methodKey, LocalCache localCache, Object target, Method method, Object[] args) {
        this.methodKey = methodKey;
        this.localCache = localCache;
        this.target = target;
        this.method = method;
        this.args = args;

        if (args != null && args.length > 0) {
            int argsJSONLength = 0;
            String[] argsMd5 = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                String jsonString = JSONObject.toJSONString(arg);
                argsJSONLength += jsonString.length();
                argsMd5[i] = MD5.create().digestHex(jsonString);
            }
            this.argsJSONLength = argsJSONLength;
            this.argsMd5 = argsMd5;
        }
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
            // 逐个对比参数
            for (int i = 0; i < args.length; i++) {
                // 优先对象自己的equals，如果没有自己的equals导致不相同则用md5值再判断一下是不是相同
                Object arg = args[i];
                Object objArg = annotationKey.args[i];
                // 接口判定
                if (arg instanceof ILocalCacheEquals && objArg instanceof ILocalCacheEquals) {
                    boolean localCacheEquals = ((ILocalCacheEquals) arg).equals((ILocalCacheEquals) objArg);
                    if (!localCacheEquals) {
                        return false;
                    }
                } else {
                    // equals方法和md5判定
                    boolean equalsOrMd5 = Objects.equals(arg, objArg) || Objects.equals(this.argsMd5[i], annotationKey.argsMd5[i]);
                    if (!equalsOrMd5) {
                        return false;
                    }
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
