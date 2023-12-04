# LocalCache
 一个线程&内存安全、自动异步加载缓存、命中率等状态监控的本地缓存，基于caffeine二次封装
 > 支持注解使用普通的方法缓存和高效的自更新方法缓存（缓存后每n秒自动异步执行一次方法更新缓存值）
> 
 > 支持自动每分钟打印缓存监控，掌握缓存命中、耗时等指标数据
> 
 > 支持查看每次是否命中缓存，每次执行参数、返回值等
> 
 > 默认设置初始容量、最大容量、线程池等参数，线程安全、预防内存溢出
>  支持全局配置启用/关闭缓存，启用/关闭 单个/全局日志打印，启用/关闭状态信息打印

## 注解式
### 普通缓存示范 
```java
@LocalCache(expireAfterWrite = 5)
```
写入的缓存5秒后过期。5秒内对于相同参数调用该方法不再执行方法逻辑，直接返回缓存中的返回对象
### 异步自动更新缓存示范
```java
@LocalCache(expireAfterWrite = 10,refreshAfterWrite = 3)
```

写入的缓存10秒过期，3秒后若有请求访问该key，这次直接返回缓存内的值，然后触发异步加载这个key（key对象里提前保存好了方法所有参数，调用方法执行） ,将缓存值刷新为最新值。
是否为相同key的自定义判定可以通过覆写方法参数对象类的equals方法和hash方法实现，判定规则为equals方法为true或json序列化结果相同，优先equals
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

## 参数默认值
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

## 监控打印
> 监控打印日志默认打开

默认每分钟打印1次，打印结果示范如下
```text
2023-12-04 10:01:30.621 INFO [] 1 --- [ scheduling-1] c.s.r.c.localcache.LocalCacheMonitor : -----------------------本地缓存状态打印-----------------------
2023-12-04 10:01:30.622 INFO [] 1 --- [ scheduling-1] c.s.r.c.localcache.LocalCacheMonitor : 【本地缓存状态】key：METHOD-CACHE-cn.xxx.recovery.manage.service.impl.xxxServiceImpl#getHttpParam(java.lang.String) 总请求数：1318400 次 ，缓存命中率：99.99%，平均加载耗时：125 ms，淘汰key次数：144 次，缓存命中次数：1318255 次，缓存未命中次数：145次 缓存配置：{"initCapacity":1,"enableLog":true,"maxCapacity":4,"expireAfterWrite":10,"refreshAfterWrite":5,"timeUnit":"SECONDS"}
2023-12-04 10:01:30.622 INFO [] 1 --- [ scheduling-1] c.s.r.c.localcache.LocalCacheMonitor : -----------------------本地缓存状态打印结束-----------------------
```

本项目将持续更新新功能与优化，目标是做一个好用的springboot项目里的缓存工具，有改进建议和功能需求欢迎提出，逐个处理。
