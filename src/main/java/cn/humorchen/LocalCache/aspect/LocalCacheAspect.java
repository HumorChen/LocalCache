package cn.humorchen.LocalCache.aspect;

import cn.humorchen.LocalCache.key.LocalCacheKey;
import cn.humorchen.LocalCache.LocalCacheMonitor;
import cn.humorchen.LocalCache.LocalCacheUtil;
import cn.humorchen.LocalCache.annotations.LocalCache;
import cn.humorchen.LocalCache.config.LocalCacheGlobalConfig;
import cn.humorchen.LocalCache.listener.LocalCacheDefaultLogRemovalListener;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalListener;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: humorchen
 * date: 2023/11/21
 * description: 本地缓存的实现类切面
 **/
@Aspect
@Component
@Slf4j
public class LocalCacheAspect {
    /**
     * 线程编号
     */
    private final AtomicInteger threadNum = new AtomicInteger(0);
    /**
     * 共用线程池
     */
    private final Executor executor = new ThreadPoolExecutor(32, 128, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<>(4096), r -> new Thread(r, "【本地缓存】thread" + threadNum.incrementAndGet()), new ThreadPoolExecutor.CallerRunsPolicy());
    /**
     * cache容器
     */
    private final Map<String, Cache<LocalCacheKey, Object>> cacheMap = new ConcurrentHashMap<>();
    /**
     * 初始化锁
     */
    private final ReentrantLock reentrantLock = new ReentrantLock();
    @Autowired
    private LocalCacheGlobalConfig config;

    /**
     * 切入点
     */
    @Pointcut(value = "@annotation(cn.humorchen.LocalCache.annotations.LocalCache)")
    public void pointcut() {

    }

    /**
     * 是否打开了日志
     *
     * @param localCache
     * @return
     */
    private boolean isLogEnable(LocalCache localCache) {
        return config.isEnableLog() || localCache.enableLog();
    }
    /**
     * 切面
     *
     * @param joinPoint
     * @return
     */
    @Around(value = "pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 全局禁用了缓存直接返回
        if (config.isDisabled()) {
            return joinPoint.proceed();
        }
        Object ret = null;
        // 拦截之后看有没有创建缓存，没有的话上锁直接执行创建缓存
        try {
            Object[] args = joinPoint.getArgs();
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Object target = joinPoint.getTarget();
            if (method != null & target != null) {
                LocalCache localCache = method.getAnnotation(LocalCache.class);
                String methodKey = getMethodCacheKey(method);
                LocalCacheKey key = new LocalCacheKey(methodKey, localCache, target, method, args);
                Cache<LocalCacheKey, Object> cache = getOrInitCache(key);
                if (cache != null) {
                    // 加载缓存
                    boolean onCache = true;
                    ret = cache.getIfPresent(key);
                    if (ret == null) {
                        ret = this.cacheLoader(key);
                        cache.put(key, ret);
                        onCache = false;
                    }
                    if (isLogEnable(localCache)) {
                        log.info("本地缓存：{} 参数：{} 处理完毕，{}命中缓存，返回结果：{}", methodKey, JSONObject.toJSONString(args), onCache ? "已" : "未", JSONObject.toJSONString(ret));
                    }
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
    private Object cacheLoader(LocalCacheKey k) {
        try {
            if (isLogEnable(k.getLocalCache())) {
                log.info("本地缓存：{} 执行被代理方法加载最新值，执行参数：{}", k.getMethodKey(), JSONObject.toJSONString(k.getArgs()));
            }
            return k.getMethod().invoke(k.getTarget(), k.getArgs());
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
    private Cache<LocalCacheKey, Object> getOrInitCache(LocalCacheKey key) {
        Cache<LocalCacheKey, Object> cache = cacheMap.get(key.getMethodKey());
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
    private Cache<LocalCacheKey, Object> initMethodCacheAndGet(LocalCacheKey key) {
        String methodKey = key.getMethodKey();
        LocalCache localCache = key.getLocalCache();
        reentrantLock.lock();
        Cache<LocalCacheKey, Object> cache = null;
        try {
            if (isLogEnable(localCache)) {
                log.info("本地缓存：{} 开始初始化", methodKey);
            }
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
                RemovalListener<Object, Object> removalListener = isLogEnable(localCache) ? new LocalCacheDefaultLogRemovalListener() : null;
                // 执行初始化
                if (isAutoAsyncRefresh(localCache)) {
                    if (isLogEnable(localCache)) {
                        log.info("本地缓存 {} 自动异步更新缓存池初始化开始", methodKey);
                    }
                    cache = LocalCacheUtil.newCacheBuilder(methodKey).basicAutoRefreshCacheConfig(localCache.initCapacity(), localCache.maxCapacity(), localCache.expireAfterWrite(), localCache.refreshAfterWrite()).setExecutor(executor).setRemovalListener(removalListener).setTimeUnit(localCache.timeUnit()).build((this::cacheLoader));
                    if (isLogEnable(localCache)) {
                        log.info("本地缓存: {} 自动异步更新缓存池初始化结束，cache对象是否为空：{}", methodKey, cache == null);
                    }
                } else {
                    if (isLogEnable(localCache)) {
                        log.info("本地缓存 {} 普通本地缓存池初始化开始", methodKey);
                    }
                    cache = LocalCacheUtil.newCacheBuilder(methodKey).basicCacheConfig(localCache.initCapacity(), localCache.maxCapacity(), localCache.expireAfterWrite()).setExecutor(executor).setRemovalListener(removalListener).setTimeUnit(localCache.timeUnit()).build();
                    if (isLogEnable(localCache)) {
                        log.info("本地缓存: {} 普通本地缓存池初始化结束，cache对象是否为空：{}", methodKey, cache == null);
                    }
                }
                cacheMap.put(methodKey, cache);
                // 注册到监控去
                LocalCacheMonitor.register(methodKey, cache, key.getLocalCache());
            } else {
                if (isLogEnable(localCache)) {
                    log.info("本地缓存：{} 已被其他线程初始化，本次初始化取消", methodKey);
                }
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

    /**
     * 生成方法的键
     *
     * @param method
     * @return
     */
    private String getMethodCacheKey(Method method) {
        StringBuilder keyBuilder = new StringBuilder("METHOD-CACHE-");
        // 填充 类名#方法名
        keyBuilder.append(method.getDeclaringClass().getName());
        keyBuilder.append("#");
        keyBuilder.append(method.getName());
        // 填充参数类
        keyBuilder.append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length > 0) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (i > 0) {
                    keyBuilder.append(",");
                }
                keyBuilder.append(parameterTypes[i].getName());
            }
        }
        keyBuilder.append(")");
        return keyBuilder.toString();
    }
}
