package cn.humorchen.localcache.config;

import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.util.StrUtil;
import cn.humorchen.localcache.enums.CleanStrategyEnum;
import cn.humorchen.localcache.listener.LocalCacheDefaultLogRemovalListener;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

