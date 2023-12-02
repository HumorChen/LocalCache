# LocalCache
 一个线程安全、异步加载缓存的缓存工具，基于caffeine
 支持注解使用普通的方法缓存和高效的自更新方法缓存（缓存后每n秒自动异步执行一次方法更新缓存值）
 支持自动每分钟打印缓存监控，掌握缓存命中、耗时等指标数据 
 支持查看每次是否命中缓存，每次执行参数、返回值等 
 支持设置初始容量、最大容量、线程安全、不会内存溢出 
 
## 普通缓存示范 
@LocalCache(expireAfterWrite = 5) 
## 异步自更新缓存示范
@LocalCache(expireAfterWrite = 10,refreshAfterWrite = 3) 
写入的缓存10秒过期，3秒后若有请求访问该key，这次直接返回缓存内的值，然后触发异步加载这个key（key对象里提前保存好了方法所有参数，调用方法执行） 
是否为相同key的自定义判定可以通过覆写方法参数对象类的equals方法和hash方法实现，判定规则为equals方法为true或json序列化结果相同，优先equals