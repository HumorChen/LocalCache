package cn.humorchen.LocalCache.test;

import cn.humorchen.LocalCache.LocalCacheMonitor;
import cn.humorchen.LocalCache.LocalCacheTestApplication;
import cn.humorchen.LocalCache.service.IParseTestService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: humorchen
 * date: 2023/12/1
 * description: 测试
 **/
@SpringBootTest(classes = LocalCacheTestApplication.class)
@RunWith(SpringRunner.class)
public class LocalCacheTest {
    @Autowired
    private IParseTestService parseService;
    @Autowired
    private LocalCacheMonitor localCacheMonitor;

    /**
     * 测试普通缓存
     */
    @Test
    public void testCache(){
        System.out.println(parseService.parse("1"));
        System.out.println(parseService.parse("2"));
        System.out.println(parseService.parse("1"));
    }

    /**
     * 测试自动异步更新缓存
     *
     * 测试结果
     * 2023-12-01 17:31:27.521  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 开始初始化
     * 2023-12-01 17:31:27.524  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存 METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 自动异步更新缓存池初始化开始
     * 2023-12-01 17:31:27.547  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存: METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 自动异步更新缓存池初始化结束，cache对象是否为空：false
     * 2023-12-01 17:31:27.582  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 执行被代理方法加载最新值，执行参数：["张三"]
     * 2023-12-01 17:31:27.629  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 参数：["张三"] 处理完毕，未命中缓存，返回结果："Hi 张三 2023-12-01 17:31:27"
     * Hi 张三 2023-12-01 17:31:27
     * 2023-12-01 17:31:27.629  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 执行被代理方法加载最新值，执行参数：["李四"]
     * 2023-12-01 17:31:27.630  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 参数：["李四"] 处理完毕，未命中缓存，返回结果："Hi 李四 2023-12-01 17:31:27"
     * Hi 李四 2023-12-01 17:31:27
     * 2023-12-01 17:31:30.649  INFO 12860 --- [  【本地缓存】thread3] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 执行被代理方法加载最新值，执行参数：["张三"]
     * 2023-12-01 17:31:30.654  INFO 12860 --- [  【本地缓存】thread4] .L.l.LocalCacheDefaultLogRemovalListener : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String)  参数：["张三"] 被移除，移除原因：REPLACED，当前方法值为："Hi 张三 2023-12-01 17:31:27"
     * 2023-12-01 17:31:30.654  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 参数：["张三"] 处理完毕，已命中缓存，返回结果："Hi 张三 2023-12-01 17:31:27"
     * Hi 张三 2023-12-01 17:31:27
     * 2023-12-01 17:31:34.658  INFO 12860 --- [  【本地缓存】thread6] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 执行被代理方法加载最新值，执行参数：["张三"]
     * 2023-12-01 17:31:34.658  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 参数：["张三"] 处理完毕，已命中缓存，返回结果："Hi 张三 2023-12-01 17:31:30"
     * Hi 张三 2023-12-01 17:31:30
     * 2023-12-01 17:31:34.659  INFO 12860 --- [  【本地缓存】thread7] .L.l.LocalCacheDefaultLogRemovalListener : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String)  参数：["张三"] 被移除，移除原因：REPLACED，当前方法值为："Hi 张三 2023-12-01 17:31:30"
     * 2023-12-01 17:31:35.173  INFO 12860 --- [           main] c.h.LocalCache.aspect.LocalCacheAspect   : 本地缓存：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 参数：["张三"] 处理完毕，已命中缓存，返回结果："Hi 张三 2023-12-01 17:31:34"
     * Hi 张三 2023-12-01 17:31:34
     * 2023-12-01 17:31:35.173  INFO 12860 --- [           main] c.h.LocalCache.LocalCacheMonitor         : -----------------------本地缓存状态打印-----------------------
     * 2023-12-01 17:31:35.198  INFO 12860 --- [           main] c.h.LocalCache.LocalCacheMonitor         : 【本地缓存状态】key：METHOD-CACHE-cn.humorchen.LocalCache.service.impl.ParseTestServiceImpl#sayHi(java.lang.String) 总请求数：5 次 ，缓存命中率：60.00%，平均加载耗时：1.905 ms，淘汰key次数：0 次，缓存命中次数：3 次，缓存未命中次数：2次 缓存配置：{"initCapacity":128,"enableLog":false,"maxCapacity":4096,"expireAfterWrite":20,"refreshAfterWrite":2,"timeUnit":"SECONDS"}
     * 2023-12-01 17:31:35.198  INFO 12860 --- [           main] c.h.LocalCache.LocalCacheMonitor         : -----------------------本地缓存状态打印结束-----------------------
     * @throws Exception
     */
    @Test
    public void testRefreshAfterWriteCache() throws Exception{
        System.out.println(parseService.sayHi("张三"));
        System.out.println(parseService.sayHi("李四"));
        Thread.sleep(3000);
        System.out.println(parseService.sayHi("张三"));
        Thread.sleep(4000);
        System.out.println(parseService.sayHi("张三"));
        Thread.sleep(500);
        System.out.println(parseService.sayHi("张三"));

        localCacheMonitor.printScheduled();
    }
}
