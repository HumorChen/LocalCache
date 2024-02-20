package cn.humorchen.localcache.aspect;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.humorchen.localcache.LocalCache;
import cn.humorchen.localcache.LocalCacheLogger;
import cn.humorchen.localcache.LocalCacheUtil;
import cn.humorchen.localcache.bean.LocalCacheKey;
import cn.humorchen.localcache.bean.LocalCacheValue;
import cn.humorchen.localcache.config.LocalCacheGlobalConfig;
import cn.humorchen.localcache.constant.LocalCacheConstant;
import cn.humorchen.localcache.enums.CopyResultStrategy;
import cn.humorchen.localcache.interfaces.ILocalCachePutValueTrigger;
import cn.humorchen.localcache.listener.LocalCacheDefaultLogRemovalListener;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author  humorchen
 * date: 2023/11/21
 * description: 本地缓存的实现类切面
 **/
@Aspect
@Component
@Slf4j
public class LocalCacheAspect implements DisposableBean {
    /**
     * 线程编号
     */
    private final AtomicInteger threadNum = new AtomicInteger(0);
    /**
     * 共用线程池
     */
    private static ThreadPoolExecutor executor = null;
    /**
     * cache容器
     */
    public static final Map<String, Cache<LocalCacheKey, LocalCacheValue>> cacheMap = new ConcurrentHashMap<>();
    /**
     * 初始化锁
     */
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final LocalCacheGlobalConfig config;
    /**
     * 键移除监听器
     */
    private RemovalListener<Object, Object> removalListener;
    /**
     * 引用队列
     */
    public static ReferenceQueue<Object> valueReferenceQueue = new ReferenceQueue<>();
    /**
     * put value的钩子
     */
    private List<ILocalCachePutValueTrigger> localCachePutValueTriggers;

    public LocalCacheAspect(LocalCacheGlobalConfig config, List<ILocalCachePutValueTrigger> localCachePutValueTriggers) {
        this.config = config;
        this.localCachePutValueTriggers = localCachePutValueTriggers;
        try {
            LocalCacheLogger.info(null, "本地缓存启动，配置为：{}", JSONObject.toJSONString(config));
            // 初始化公共线程池
            if (executor == null) {
                executor = new ThreadPoolExecutor(config.getThreadPoolCoreThreadSize(), config.getThreadPoolMaxThreadSize(), 10, TimeUnit.MINUTES, new ArrayBlockingQueue<>(config.getThreadPoolQueueSize()), r -> {
                    String threadName = generateThreadName();
                    log.info(threadName + " 线程启动");
                    return new Thread(r, threadName);
                }, new ThreadPoolExecutor.AbortPolicy());
            }
            // key 移除监听器
            if (removalListener == null) {
                try {
                    String keyRemovalListener = config.getKeyRemovalListener();
                    if (StrUtil.isNotBlank(keyRemovalListener)) {
                        Class listenerCls = Class.forName(keyRemovalListener);
                        Object object = listenerCls.newInstance();
                        if (object instanceof RemovalListener) {
                            removalListener = (RemovalListener<Object, Object>) object;
                        }
                    }
                } catch (Exception e) {
                    log.error("本地缓存Key Removal Listener创建失败，启用默认Listener");
                }
                if (removalListener == null) {
                    log.info("本地缓存Key Removal Listener启用默认监听器LocalCacheDefaultLogRemovalListener");
                    removalListener = new LocalCacheDefaultLogRemovalListener();
                }
            }
            // 初始化日志配置
            LocalCacheLogger.setEnableLog(config.getEnableLog());
            if (config.getOneLogMaxLength() != null && config.getOneLogMaxLength() > 0) {
                LocalCacheLogger.setOneLogMaxLength(config.getOneLogMaxLength());
            }
        } catch (Exception e) {
            log.error("初始化报错", e);
            throw e;
        }
    }

    /**
     * Invoked by the containing {@code BeanFactory} on destruction of a bean.
     *
     * @throws Exception in case of shutdown errors. Exceptions will get logged
     *                   but not rethrown to allow other beans to release their resources as well.
     */
    @Override
    public void destroy() throws Exception {
        // 线程池拒绝新任务并执行完现有任务
        executor.shutdown();
    }

    /**
     * 获取方法缓存共用的线程池
     *
     * @return
     */
    public static Executor getMethodCacheExecutor() {
        return executor;
    }
    /**
     * 生成线程名
     *
     * @return
     */
    private String generateThreadName() {
        return "【" + config.getThreadPoolThreadNamePrefix() + "】thread" + threadNum.incrementAndGet();
    }
    /**
     * 切入点
     */
    @Pointcut(value = "@annotation(cn.humorchen.localcache.LocalCache)")
    public void pointcut() {

    }

    /**
     * key是否需要写入到缓存
     *
     * @param key
     * @param localCache
     * @return
     */
    private boolean isKeyNeedWriteToCache(LocalCacheKey key, LocalCache localCache) {
        int globalMax = config.getOneCacheMaxKeyLength() == null ? 0 : config.getOneCacheMaxKeyLength();
        int annotationMax = localCache != null ? localCache.maxKeyLength() : 0;
        Assert.isTrue(globalMax >= 0, () -> new IllegalArgumentException("oneCacheMaxKeyLength配置错误，不得为负数"));
        Assert.isTrue(annotationMax >= 0, () -> new IllegalArgumentException("maxKeyLength配置错误，不得为负数"));
        if (globalMax > 0 && key.getArgsJSONLength() > globalMax) {
            return false;
        }
        if (annotationMax > 0 && key.getArgsJSONLength() > annotationMax) {
            return false;
        }
        return true;
    }

    /**
     * value是否需要写入到缓存
     *
     * @param localCache
     * @param localCacheValue
     * @return
     */
    private boolean isValueNeedWriteToCache(LocalCache localCache, LocalCacheValue localCacheValue) {
        int globalMax = config.getOneCacheMaxValueLength() == null ? 0 : config.getOneCacheMaxValueLength();
        int annotationMax = localCache != null ? localCache.maxValueLength() : 0;
        Assert.isTrue(globalMax >= 0, () -> new IllegalArgumentException("oneCacheMaxValueLength配置错误，不得为负数"));
        Assert.isTrue(annotationMax >= 0, () -> new IllegalArgumentException("maxValueLength配置错误，不得为负数"));
        if (globalMax > 0 && localCacheValue.getJsonLength() > globalMax) {
            return false;
        }
        if (annotationMax > 0 && localCacheValue.getJsonLength() > annotationMax) {
            return false;
        }
        return true;
    }

    /**
     * 切面
     * 要先于该切面执行的方法请设置order小于10
     *
     * @param joinPoint
     * @return
     */
    @Around(value = "pointcut()")
    @Order(LocalCacheConstant.EXECUTE_ORDER)
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 全局禁用了缓存直接返回
        if (config.getDisabled()) {
            return joinPoint.proceed();
        }
        Object ret = null;
        // 拦截之后看有没有创建缓存，没有的话上锁直接执行创建缓存
        try {
            Object[] args = joinPoint.getArgs();
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Object target = joinPoint.getTarget();
            if (method != null & target != null) {
                // 注解对象
                LocalCache localCache = method.getAnnotation(LocalCache.class);
                // 方法全限定名（带参数类）
                String methodKey = LocalCacheUtil.getMethodCacheKey(method, localCache);
                // 缓存key
                LocalCacheKey key = new LocalCacheKey(methodKey, localCache, target, method, args);
                // 是否写进缓存（长度限制）
                boolean writeToCache = isKeyNeedWriteToCache(key, localCache);
                // 获取cache对象
                Cache<LocalCacheKey, LocalCacheValue> cache = getOrInitCache(key);
                // 正常取得无报错
                if (cache != null) {
                    // 是否命中缓存
                    boolean shotCache = false;
                    // 从缓存中获得结果
                    LocalCacheValue localCacheValue = cache.getIfPresent(key);
                    // 缓存命中且缓存的对象没有被回收且缓存结果不为空
                    if (localCacheValue != null && !localCacheValue.isEnqueued() && (ret = localCacheValue.getValue()) != null) {
                        // 缓存命中
                        shotCache = true;
                    } else {
                        // 缓存没有结果
                        // 执行被缓存方法获得结果
                        localCacheValue = this.cacheLoader(key);
                        // 赋值返回
                        ret = localCacheValue.getValue();
                        // 决定是否要写入缓存（长度限制）
                        writeToCache = writeToCache && isValueNeedWriteToCache(localCache, localCacheValue);
                        if (writeToCache) {
                            cache.put(key, localCacheValue);
                            final LocalCacheValue finalValue = localCacheValue;
                            this.localCachePutValueTriggers.forEach(trigger -> trigger.trigger(methodKey, localCache, key, finalValue));
                        }
                    }
                    // 处理结果拷贝策略
                    CopyResultStrategy copyResultStrategy = localCache.copyResultStrategy();
                    if (copyResultStrategy != CopyResultStrategy.NONE && copyResultStrategy != null && ret != null) {
                        ret = copyResultStrategy.getCopier().copy(ret);
                    }

                    LocalCacheLogger.debug(localCache, "本地缓存：{} 参数：{} 处理完毕，{}命中缓存，是否写入缓存{}，返回值大小：{} Byte 返回结果：{}", methodKey, JSONObject.toJSONString(args), shotCache ? "已" : "未", writeToCache, localCacheValue.getJsonLength() * 2, JSONObject.toJSONString(ret));
                } else {
                    // 意外找不到缓存直接走自己的
                    log.error("本地缓存出现意外找不到缓存也初始化失败");
                    ret = joinPoint.proceed();
                }
            }
        } catch (Throwable e) {
            log.error("本地方法缓存切面执行报错", e);
            throw e;
        }
        return ret;
    }

    /**
     * 缓存加载
     *
     * @param k
     * @return
     */
    private LocalCacheValue cacheLoader(LocalCacheKey k) {
        try {
            LocalCacheLogger.debug(k.getLocalCache(), "本地缓存：{} 执行被代理方法加载最新值，执行参数：{}", k.getMethodKey(), JSONObject.toJSONString(k.getArgs()));
            Object value = k.getMethod().invoke(k.getTarget(), k.getArgs());
            return new LocalCacheValue(k, value, valueReferenceQueue);
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("本地缓存：" + k.getMethodKey() + " 动态加载缓存值报错", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取缓存对象，当不存在的时候初始化，是线程安全的
     * @param key
     * @return
     */
    private Cache<LocalCacheKey, LocalCacheValue> getOrInitCache(LocalCacheKey key) {
        Cache<LocalCacheKey, LocalCacheValue> cache = cacheMap.get(key.getMethodKey());
        if (cache == null) {
            cache = initMethodCacheAndGet(key);
        }
        return cache;
    }

    /**
     * 上锁初始化并返回cache对象，是线程安全的
     * @param key
     * @return
     */
    private Cache<LocalCacheKey, LocalCacheValue> initMethodCacheAndGet(LocalCacheKey key) {
        String methodKey = key.getMethodKey();
        LocalCache localCache = key.getLocalCache();
        reentrantLock.lock();
        Cache<LocalCacheKey, LocalCacheValue> cache = null;
        try {
            LocalCacheLogger.info(localCache, "本地缓存：{} 开始初始化", methodKey);
            cache = cacheMap.get(methodKey);
            // 上锁后二次校验
            if (cache == null) {
                // 开始初始化
                // 检查参数
                Assert.isTrue(localCache.initCapacity() >= 1);
                Assert.isTrue(localCache.initCapacity() <= localCache.maxCapacity());
                Assert.isTrue(localCache.expireAfterWrite() >= 1);
                Assert.isTrue(localCache.timeUnit().toMillis(localCache.expireAfterWrite()) >= 50, () -> new IllegalArgumentException("缓存有效时间最小为50ms"));
                Assert.isTrue(localCache.expireAfterWrite() > localCache.refreshAfterWrite(), () -> new IllegalArgumentException("缓存有效时间需大于缓存动态更新时间"));
                if (isAutoAsyncRefresh(localCache)) {
                    Assert.isTrue(localCache.timeUnit().toMillis(localCache.expireAfterWrite() - localCache.refreshAfterWrite()) > 100, () -> new IllegalArgumentException("缓存有效时间与缓存动态更新时间之差需大于100ms"));
                }

                // 移除key日志打印监听器
                RemovalListener<Object, Object> removalListener = LocalCacheLogger.isEnableLog(localCache) ? this.removalListener : null;

                // 执行初始化
                if (isAutoAsyncRefresh(localCache)) {
                    LocalCacheLogger.info(localCache, "本地缓存 {} 自动异步更新缓存池初始化开始", methodKey);
                    cache = LocalCacheUtil.newCacheBuilder(methodKey).basicAutoRefreshCacheConfig(localCache.initCapacity(), localCache.maxCapacity(), localCache.expireAfterWrite(), localCache.refreshAfterWrite()).setExecutor(executor).setRemovalListener(removalListener).setTimeUnit(localCache.timeUnit()).setLocalCache(localCache).build((this::cacheLoader));
                    LocalCacheLogger.info(localCache, "本地缓存: {} 自动异步更新缓存池初始化结束，cache对象是否为空：{}", methodKey, cache == null);
                } else {
                    LocalCacheLogger.info(localCache, "本地缓存 {} 普通本地缓存池初始化开始", methodKey);
                    cache = LocalCacheUtil.newCacheBuilder(methodKey).basicCacheConfig(localCache.initCapacity(), localCache.maxCapacity(), localCache.expireAfterWrite()).setExecutor(executor).setRemovalListener(removalListener).setTimeUnit(localCache.timeUnit()).setLocalCache(localCache).build();
                    LocalCacheLogger.info(localCache, "本地缓存: {} 普通本地缓存池初始化结束，cache对象是否为空：{}", methodKey, cache == null);
                }
                cacheMap.put(methodKey, cache);
            } else {
                LocalCacheLogger.info(localCache, "本地缓存：{} 已被其他线程初始化，本次初始化取消", methodKey);
            }
        } catch (Exception e) {
            log.error("方法本地缓存初始化异常", e);
        } finally {
            reentrantLock.unlock();
        }
        return cache;
    }

    /**
     * 是否为自动刷新
     *
     * @param localCache
     * @return
     */
    public boolean isAutoAsyncRefresh(LocalCache localCache) {
        // refreshAfterWrite为正数则为需要动态异步更新缓存值
        return localCache != null && localCache.refreshAfterWrite() > 0;
    }


}
