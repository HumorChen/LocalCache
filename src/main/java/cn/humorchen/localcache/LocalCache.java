package cn.humorchen.localcache;

import cn.humorchen.localcache.enums.CleanStrategyEnum;
import cn.humorchen.localcache.enums.CopyResultStrategy;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * @author  humorchen
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
@Inherited
@Documented
public @interface LocalCache {
    /**
     * 方法缓存的名字
     * 不填写则默认生成（方法+参数全限定路径名）
     *
     * @return
     */
    String cacheName() default "";

    /**
     * 缓存初始化容量
     * @return
     */
    int initCapacity() default 128;

    /**
     * 缓存最大容量
     * @return
     */
    int maxCapacity() default 1024;

    /**
     * 缓存过期秒数
     * @return
     */
    int expireAfterWrite() default 3;

    /**
     * 缓存刷新秒数
     *     缓存写入成功refreshAfterWrite秒后如果该缓存被访问了，则异步加载一次更新缓存值，让值为最新的，下次更新需要再等refreshAfterWrite
     *     若不需要异步自动刷新则不设置该参数即可（-1代表关闭）
     *     配置时建议expireSecond >= refreshAfterWrite * 2
     * @return
     */
    int refreshAfterWrite() default -1;

    /**
     * 时间单位
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 打印缓存变更日志方便查看缓存情况
     * @return
     */
    boolean enableLog() default false;

    /**
     * key最大长度
     * 所有方法参数的json长度和，大于则不加入缓存
     * 0 无限制
     * @return
     */
    int maxKeyLength() default 0;

    /**
     * value最大长度
     * 方法返回值的json长度和，大于则不加入缓存
     * 0 无限制
     * @return
     */
    int maxValueLength() default 0;

    /**
     * 占用内存最大限制
     * 单位：字节
     * java内 1 字符等于 2 字节
     * 默认空字符串代表 无限制
     * 示范值 10MB
     * @see cn.hutool.core.io.unit.DataSize
     *
     * @return
     */
    String usedMemoryMaxSize() default "";


    /**
     * 内存清理策略
     *
     * @return
     * @see CleanStrategyEnum
     */
    CleanStrategyEnum cleanStrategy() default CleanStrategyEnum.NULL;

    /**
     * 是否跳过全局内存限制
     * 若配置了全局内存限制，可设置该项来跳过全局内存限制，不被全局内存限制所回收内存
     *
     * @return
     */
    boolean skipGlobalMemoryLimit() default false;

    /**
     * 是否拷贝返回值
     * 用于解决返回值是引用类型，缓存值被修改的问题
     * 默认不拷贝
     * 警告： 建议只用于普通数据对象拷贝结果，不得用于Atomic等不适合拷贝的对象类型返回值，否则会丢失状态值
     * 不确定可使用CopyResultStrategy.COPY_RESULT_USING_JSON_SERIALIZATION.getCopier().copy(xx)进行拷贝测试
     *
     * @return
     * @see CopyResultStrategy
     */
    CopyResultStrategy copyResultStrategy() default CopyResultStrategy.NONE;
}
