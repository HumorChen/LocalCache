# LocalCache
 一个安全好用的springboot项目本地方法缓存，基于最高效的caffeine框架。
 > 支持注解添加方法本地缓存
 >
 > 缓存支持自更新（缓存写入n秒后被调用触发异步执行更新缓存值，每n秒最多执行一次更新）
 >
 > 支持命中缓存返回拷贝的结果对象（可选择多种拷贝器，可自行实现）
 >
 > 支持获取缓存监控，掌握缓存命中、耗时等指标数据，可按分钟打印
 >
 > 支持debug查看每次调用是否命中缓存，每次执行参数、返回值等
 >
 > 初始容量、最大容量、线程池等参数带有默认参数，可全局、局部配置
 > 支持全局配置启用/关闭缓存，启用/关闭 单个/全局日志打印，启用/关闭状态信息打印
 >
 > 支持按Mb等内存大小格式配置限制缓存占用内存大小
 >
 > 支持自行定制缓存值移除监听器、缓存回收器、线程池工厂等
 >
 > 支持配置每行缓存日志打印最大长度，避免打印超长数据日志

## 注解式

方法有无参数、参数个数任意个都支持使用

### 普通缓存示范 
```java
@LocalCache(expireAfterWrite = 5)
public Data getData(String id){
    // your code
}
```
写入的缓存5秒后过期。5秒内对于相同参数调用该方法不再执行方法逻辑，直接返回缓存中的返回对象
### 异步自动更新缓存示范
```java
@LocalCache(expireAfterWrite = 10,refreshAfterWrite = 3)
public Data getData(String id){
    // your code
}
```

写入的缓存10秒过期，3秒后若有请求访问该key，这次直接返回缓存内的值，然后触发异步加载这个key（key对象里提前保存好了方法所有参数，调用方法执行） ,将缓存值刷新为最新值。
是否为相同key的自定义判定可以通过覆写方法参数对象类的equals方法和hash方法实现，判定规则为equals方法为true或json序列化结果相同，优先equals

### 注解可配参数

```java
@LocalCache
```

```java
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

```





## 手动构建控制

> 每个参数都带有安全可靠的默认值，预防内存溢出

### 普通缓存示范 
```java
Cache<Object, Object> cache = LocalCacheUtil.newCacheBuilder("your cache name").setExpireAfterWrite(5).setTimeUnit(TimeUnit.SECONDS).setInitCapacity(128).setMaxCapacity(1024).build();
```
上述代码建立了一个名字为"your cache name"的缓存，缓存在写入5秒后过期，缓存空间初始容量128，最大容量1024

### 异步自动更新缓存示范

```java
Cache<Object, Object> cache = LocalCacheUtil.newCacheBuilder("your cache name").setExpireAfterWrite(600).setRefreshAfterWrite(2).build((key)->{
            return yourLoadMethod(key);
        });
```
上述代码建立了一个名字为"your cache name"的缓存，缓存在写入600秒后过期，若在该key写入缓存2秒后再次被访问则触发异步加载该缓存key最新的值，加载方法为yourLoadMethod(key) 

### 参数默认值
```java
    /**
     * 缓存的时间单位
     */
    private final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    /**
     * 线程池初始线程数 默认值，无任务执行不占用CPU
     */
    private static final int DEFAULT_THREAD = 4;
    /**
     * 线程池最大线程数 默认值
     */
    private final int DEFAULT_MAX_THREAD = 128;
    /**
     * 线程池额外线程数存活时间 默认值
     */
    private final int DEFAULT_THREAD_KEEP_ALIVE = 10;
    /**
     * 线程池额外线程数存活时间单位 默认值
     */
    private final TimeUnit DEFAULT_THREAD_KEEP_ALIVE_TIMEUNIT = TimeUnit.MINUTES;

    /**
     * 线程池拒绝策略 默认值
     */
    private final RejectedExecutionHandler DEFAULT_THREAD_EXECUTOR_ABORT_POLICY = new ThreadPoolExecutor.AbortPolicy();
    /**
     * 初始化缓存容量默认值
     */
    private final int DEFAULT_INIT_CAPACITY = 128;
    /**
     * 缓存最大容量默认值
     */
    private final int DEFAULT_MAX_CAPACITY = 4096;
    /**
     * 线程池等待队列的容量默认值 用的链表不用担心浪费内存
     */
    private final int DEFAULT_BLOCKING_QUEUE_SIZE = 4096;
```

## 全局配置项

```java
LocalCacheGlobalConfig.java
```

```java
/**
 * @author  humorchen
 * date: 2023/11/24
 * description: 本地缓存的所有配置
 **/
@Component
@FieldNameConstants
@Data
@ConfigurationProperties(prefix = "local.cache")
public class LocalCacheGlobalConfig {
    /**
     * 全局开启日志，优先级高于注解的
     */
    private Boolean enableLog = true;
    /**
     * 监控缓存状态并打印
     */
    private Boolean monitor = true;
    /**
     * 是否全局禁用，禁用后即使有缓存注解也会失效，手动编码加的缓存是无法通过该配置禁用的
     */
    private Boolean disabled = false;
    /**
     * 核心线程池 核心线程数
     */
    private Integer threadPoolCoreThreadSize = 8;
    /**
     * 核心线程池 最大线程数
     */
    private Integer threadPoolMaxThreadSize = 64;
    /**
     * 核心线程池 任务队列长度
     */
    private Integer threadPoolQueueSize = 1024;
    /**
     * 核心线程池 线程名前缀
     * 例如 本地缓存  最后生成的 【本地缓存】thread1
     */
    private String threadPoolThreadNamePrefix = "本地缓存";
    /**
     * 本地缓存key被移除的listener
     * 填类的全限定路径
     * 例如 cn.humorchen.localcache.listener.LocalCacheDefaultLogRemovalListener
     * 默认为
     *
     * @see LocalCacheDefaultLogRemovalListener
     */
    private String keyRemovalListener = "";
    /**
     * 单条日志最大打印长度
     * 默认正数最大值
     * 当日志要打印的长度大于这个数值时会截取前n个字符打印
     */
    private Integer oneLogMaxLength;

    /**
     * 单个Key最大长度，超过了是不会写入到缓存的
     * 计算的是方法参数的json字符串长度
     */
    private Integer oneCacheMaxKeyLength;
    /**
     * 单个value最大长度，超过了是不会写入到缓存的
     * 计算的是方法返回值value的json字符串长度
     */
    private Integer oneCacheMaxValueLength;
    /**
     * 单个缓存使用内存最大限制
     * 单位：字节
     * java内 1 字符等于 2 字节
     */
    private Long oneCacheUsedMemoryMaxSizeByte;
    /**
     * 单个缓存使用内存最大限制
     * 例如 12MB，优先级大于上面这个参数
     *
     * @see cn.hutool.core.io.unit.DataSize
     */
    private String oneCacheUsedMemoryMaxSize;
    /**
     * 所有方法缓存使用内存最大限制
     * 单位：字节
     * java内 1 字符等于 2 字节
     */
    private Long allMethodCacheUsedMemoryMaxSizeByte;
    /**
     * 所有方法缓存使用内存最大限制
     * 例如 12MB，优先级大于上面这个参数
     *
     * @see cn.hutool.core.io.unit.DataSize
     */
    private String allMethodCacheUsedMemoryMaxSize;
    /**
     * 内存清理策略
     * 例如 LRU
     * @see CleanStrategyEnum#of(String)
     */
    private String cleanStrategy;

    /**
     * @param oneCacheUsedMemoryMaxSize
     */
    public void setOneCacheUsedMemoryMaxSize(String oneCacheUsedMemoryMaxSize) {
        this.oneCacheUsedMemoryMaxSize = oneCacheUsedMemoryMaxSize;
        if (StrUtil.isNotBlank(oneCacheUsedMemoryMaxSize)) {
            this.oneCacheUsedMemoryMaxSizeByte = DataSizeUtil.parse(oneCacheUsedMemoryMaxSize);
        }
    }

    /**
     * 解析字符串形式的参数
     *
     * @param allMethodCacheUsedMemoryMaxSize
     */
    public void setAllMethodCacheUsedMemoryMaxSize(String allMethodCacheUsedMemoryMaxSize) {
        this.allMethodCacheUsedMemoryMaxSize = allMethodCacheUsedMemoryMaxSize;
        if (StrUtil.isNotBlank(allMethodCacheUsedMemoryMaxSize)) {
            this.allMethodCacheUsedMemoryMaxSizeByte = DataSizeUtil.parse(allMethodCacheUsedMemoryMaxSize);
        }
    }
}
```

## 监控打印

> 监控打印日志默认打开
>
> ```java
> LocalCacheMonitor.java
> ```

默认每分钟打印1次，打印结果示范如下
```shell

2024-02-20 09:19:02.833 INFO [] 1 --- [ scheduling-1] c.s.r.c.localcache.LocalCacheMonitor : -----------------------本地缓存状态打印-----------------------
1
2024-02-20 09:19:02.834 INFO [] 1 --- [ scheduling-1] c.s.r.c.localcache.LocalCacheLogger : 项目全局配置：{"allMethodCacheUsedMemoryMaxSize":"300MB","allMethodCacheUsedMemoryMaxSizeByte":314572800,"cleanStrategy":"LRU","disabled":false,"enableLog":true,"keyRemovalListener":"","monitor":true,"oneLogMaxLength":1000,"threadPoolCoreThreadSize":8,"threadPoolMaxThreadSize":64,"threadPoolQueueSize":1024,"threadPoolThreadNamePrefix":"本地缓存"}
2
2024-02-20 09:19:02.834 INFO [] 1 --- [ scheduling-1] c.s.r.c.localcache.LocalCacheMonitor : 【本地缓存状态】key：METHOD-CACHE-cn.sffix.recovery.manage.service.impl.VivoServiceImpl#getModelIdByNewSkuCode(java.lang.String) ,缓存命中率 97.02% , 当前缓存值307 个,总占用内存 12.33 KB ,平均单个内存 41 B, 总请求数 2260149 次 ,平均加载耗时 15.810 ms , 缓存命中次数 2192805 次 , 缓存未命中次数 67344 次 , 淘汰key次数 67035 次 , 缓存配置：{}

2024-02-20 09:19:02.834 INFO [] 1 --- [ scheduling-1] c.s.r.c.localcache.LocalCacheMonitor : -----------------------本地缓存状态打印结束-----------------------
```



## 核心逻辑代码

本地缓存注解切面

```java
LocalCacheAspect.java
```

本项目将持续更新新功能与优化，目标是做一个好用的springboot项目里的缓存工具，有改进建议和功能需求欢迎提出，欢迎共建。
