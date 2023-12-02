package cn.humorchen.LocalCache;

import cn.humorchen.LocalCache.annotations.LocalCache;
import cn.hutool.core.lang.Assert;
import com.github.benmanes.caffeine.cache.*;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.lang.annotation.Annotation;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: humorchen
 * date: 2023/11/21
 * description: Caffeine本地缓存创建工具，建议使用该工具进行缓存对象构建
 * 使用该工具类，内部提供默认参数，可内存安全、线程安全的创建caffeine
 **/
@Setter
@Accessors(chain = true)
public class LocalCacheUtil<K, V> {
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
    /**
     * 缓存写入后n秒过期 默认值
     */
    private final int DEFAULT_EXPIRE_AFTER_WRITE = 6;
    /**
     * 缓存写入3秒后一旦有访问，就异步更新这个缓存值 默认值
     */
    private final int DEFAULT_REFRESH_AFTER_WRITE = 3;
    /**
     * 缓存的时间单位
     */
    private final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    /**
     * Caffeine缓存名称，用于做线程名，打印等
     */
    private String cacheName;
    /**
     * 线程计数
     */
    private AtomicInteger threadNum = new AtomicInteger(0);

    /**
     * 线程池初始线程数
     */
    private int thread = DEFAULT_THREAD;
    /**
     * 线程池最大线程数
     */
    private int maxThread = DEFAULT_MAX_THREAD;
    /**
     * 线程池额外线程数存活时间
     */
    private int threadKeepAlive = DEFAULT_THREAD_KEEP_ALIVE;
    /**
     * 线程池额外线程数存活时间单位
     */
    private TimeUnit threadKeepAliveTimeunit = DEFAULT_THREAD_KEEP_ALIVE_TIMEUNIT;
    /**
     * 线程池拒绝策略
     */
    private final RejectedExecutionHandler threadExecutorAbortPolicy = DEFAULT_THREAD_EXECUTOR_ABORT_POLICY;
    /**
     * 初始化缓存容量
     */
    private int initCapacity = DEFAULT_INIT_CAPACITY;
    /**
     * 缓存最大容量
     */
    private int maxCapacity = DEFAULT_MAX_CAPACITY;
    /**
     * 线程池等待队列的容量
     */

    private int blockingQueueSize = DEFAULT_BLOCKING_QUEUE_SIZE;
    /**
     * 缓存写入后n秒过期
     */
    private Integer expireAfterWrite;
    /**
     * 缓存访问后n秒过期
     */
    private Integer expireAfterAccess;
    /**
     * 缓存写入3秒后一旦有访问，就异步更新这个缓存值
     */
    private Integer refreshAfterWrite;
    /**
     * 缓存时间单位
     */
    private TimeUnit timeUnit = DEFAULT_TIME_UNIT;
    /**
     * 移除key的监听器
     */
    private RemovalListener<K, V> removalListener;
    /**
     * 调度器
     */
    private Scheduler scheduler;
    /**
     * 当前构建的caffeine对象
     */
    private Caffeine<K, V> sourceCaffeineBuilder;
    /**
     * 线程池
     */
    private Executor executor;
    /**
     * 是否启用日志打印，默认打开
     */
    private boolean enableLog = true;

    private LocalCacheUtil(String cacheName) {
        this.cacheName = cacheName;
    }

    /**
     * 新建一个caffeine缓存构建器
     *
     * @param cacheName 缓存名称（用于线程名，日志打印）
     * @return
     */
    public static LocalCacheUtil<Object, Object> newCacheBuilder(String cacheName) {
        LocalCacheUtil<Object, Object> builder = new LocalCacheUtil<>(cacheName);
        builder.sourceCaffeineBuilder = Caffeine.newBuilder();
        return builder;
    }

    /**
     * 获取原生caffeine builder修改非常用配置
     *
     * @return
     */
    public Caffeine<K, V> getRawCaffeineBuilder() {
        return this.sourceCaffeineBuilder;
    }

    /**
     * 简单缓存
     *
     * @param initCapacity
     * @param maxCapacity
     * @param expireSecond
     * @return
     */
    public LocalCacheUtil<K, V> basicCacheConfig(int initCapacity, int maxCapacity, int expireSecond) {
        setInitCapacity(initCapacity);
        setMaxCapacity(maxCapacity);
        setExpireAfterWrite(expireSecond);
        return this;
    }

    /**
     * 简单缓存
     *
     * @param initCapacity
     * @param maxCapacity
     * @param expireSecond
     * @param refreshAfterWrite 多久更新缓存数据，build的时候要传入加载方法
     * @return
     */
    public LocalCacheUtil<K, V> basicAutoRefreshCacheConfig(int initCapacity, int maxCapacity, int expireSecond, int refreshAfterWrite) {
        setInitCapacity(initCapacity);
        setMaxCapacity(maxCapacity);
        setExpireAfterWrite(expireSecond);
        setRefreshAfterWrite(refreshAfterWrite);
        return this;
    }

    /**
     * 简单数据的缓存，待自动刷新
     *
     * @param initCapacity
     * @param maxCapacity
     * @param expireSecond
     * @param refreshAfterWrite
     * @return
     */
    public LocalCacheUtil<K, V> basicAutoRefreshCacheConfigWithSingleExecutor(int initCapacity, int maxCapacity, int expireSecond, int refreshAfterWrite) {
        LocalCacheUtil<K, V> util = this.basicAutoRefreshCacheConfig(initCapacity, maxCapacity, expireSecond, refreshAfterWrite);
        util.setThread(1);
        util.setMaxThread(4);
        util.setBlockingQueueSize(16);
        return util;
    }

    /**
     * 构建不带加载器的cache
     * @return
     * @param <K1>
     * @param <V1>
     */
    public <K1 extends K, V1 extends V> Cache<K1, V1> build() {
        return this.build(null);
    }
    /**
     * 构建最终的缓存对象
     *
     * @return Cache
     */
    public <K1 extends K, V1 extends V> Cache<K1, V1> build(CacheLoader<K1, V1> cacheLoader) {
        Caffeine<K, V> caffeine = this.sourceCaffeineBuilder.initialCapacity(initCapacity).maximumSize(maxCapacity);
        // 必须设置过期时间
        Assert.isFalse(expireAfterAccess == null && expireAfterWrite == null, () -> new IllegalArgumentException("expireAfterAccess和expireAfterWrite必须设置一个"));
        int expireSecond = 1;
        // 写入后多久过期
        if (expireAfterWrite != null) {
            caffeine.expireAfterWrite(expireAfterWrite, timeUnit);
            expireSecond = expireAfterWrite;
        }
        // 最后一次访问多久后过期
        if (expireAfterAccess != null) {
            caffeine.expireAfterAccess(expireAfterAccess, timeUnit);
            expireSecond = expireAfterAccess;
        }
        // 填充默认过期时间值并检查值
        if (cacheLoader != null && refreshAfterWrite == null) {
            refreshAfterWrite = Math.max(expireSecond / 2, 1);
            Assert.isTrue(refreshAfterWrite < expireSecond, () -> new IllegalArgumentException("使用默认方案时过期时间请大于1秒"));
            setRefreshAfterWrite(refreshAfterWrite);
        }
        // 自动刷新缓存最新值
        if (refreshAfterWrite != null) {
            Assert.notNull(cacheLoader, () -> new IllegalArgumentException("使用自动刷新必须提供加载器cacheLoader"));
            caffeine.refreshAfterWrite(refreshAfterWrite, timeUnit);
        }
        // 调度器
        if (scheduler != null) {
            caffeine.scheduler(scheduler);
        }
        // 移除缓存监听器
        if (removalListener != null) {
            caffeine.removalListener(removalListener);
        }
        // 线程池
        if (executor != null) {
            caffeine.executor(executor);
        } else {
            ThreadFactory threadFactory = (runnable) -> {
                String cacheName = "【本地缓存线程】" + this.cacheName + "-" + threadNum.incrementAndGet();
                return new Thread(runnable, cacheName);
            };
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(thread, maxThread, threadKeepAlive, threadKeepAliveTimeunit, new LinkedBlockingDeque<>(blockingQueueSize), threadFactory, threadExecutorAbortPolicy);
            caffeine.executor(threadPoolExecutor);
        }
        // 启用记录缓存命中等状态计数信息
        caffeine.recordStats();
        Cache cache = cacheLoader == null ? caffeine.build() : caffeine.build(cacheLoader);
        // 注册到监控
        LocalCacheMonitor.register(this.cacheName, cache, config2LocalCache());
        return cache;
    }


    /**
     * 当前配置转cache对象
     *
     * @return
     */
    private LocalCache config2LocalCache() {
        return new LocalCache() {

            /**
             * Returns the annotation type of this annotation.
             *
             * @return the annotation type of this annotation
             */
            @Override
            public Class<? extends Annotation> annotationType() {
                return LocalCache.class;
            }

            /**
             * @return
             */
            @Override
            public int initCapacity() {
                return initCapacity;
            }

            /**
             * @return
             */
            @Override
            public int maxCapacity() {
                return maxCapacity;
            }

            /**
             * @return
             */
            @Override
            public int expireAfterWrite() {
                return expireAfterWrite;
            }

            /**
             * @return
             */
            @Override
            public int refreshAfterWrite() {
                return refreshAfterWrite;
            }

            /**
             * @return
             */
            @Override
            public TimeUnit timeUnit() {
                return timeUnit;
            }

            /**
             * @return
             */
            @Override
            public boolean enableLog() {
                return enableLog;
            }
        };
    }
}
