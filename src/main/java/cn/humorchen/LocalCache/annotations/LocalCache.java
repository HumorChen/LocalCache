package cn.humorchen.LocalCache.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author: humorchen
 * date: 2023/11/21
 * description: 本地异步热缓存的注解，基于安全的caffeine二次封装 <br>
 * 支持注解使用普通的方法缓存和高效的自更新方法缓存（缓存后每n秒自动异步执行一次方法更新缓存值）  <br>
 * 支持自动每分钟打印缓存监控，掌握缓存命中、耗时等指标数据  <br>
 * 支持查看每次是否命中缓存，每次执行参数、返回值等  <br>
 * 支持设置初始容量、最大容量、线程安全、不会内存溢出 <br>
 * 普通缓存示范 <br>
 * <code>@LocalCache(expireAfterWrite = 5)<code/> <br>
 * 异步自更新缓存示范
 * <code>@LocalCache(expireAfterWrite = 10,refreshAfterWrite = 3)<code/> <br>
 * 写入的缓存10秒过期，3秒后若有请求访问该key，这次直接返回缓存内的值，然后触发异步加载这个key（key对象里提前保存好了方法所有参数，调用方法执行） <br>
 * 是否为相同key的自定义判定可以通过覆写方法参数对象类的equals方法和hash方法实现，判定规则为equals方法为true或json序列化结果相同，优先equals <br>
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface LocalCache {
    /*
    缓存初始化容量
     */
    int initCapacity() default 128;

    /*
    缓存最大容量
     */
    int maxCapacity() default 4096;

    /*
    缓存过期秒数
     */
    int expireAfterWrite() default 3;

    /*
    缓存刷新秒数
    缓存写入成功refreshAfterWrite秒后如果该缓存被访问了，则异步加载一次更新缓存值，让值为最新的，下次更新需要再等refreshAfterWrite
    若不需要异步自动刷新则不设置该参数即可（-1代表关闭）
    配置时建议expireSecond >= refreshAfterWrite * 2
     */
    int refreshAfterWrite() default -1;

    /*
    时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /*
    打印缓存变更日志方便查看缓存情况
     */
    boolean enableLog() default false;
}
