package cn.humorchen.LocalCache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author: humorchen
 * date: 2023/11/24
 * description: 本地缓存的所有配置
 **/
@Component
@Data
@ConfigurationProperties(prefix = "local.cache")
public class LocalCacheGlobalConfig {
    /**
     * 全局开启日志，优先级高于注解的 默认打开可关闭
     */
    private boolean enableLog = true;
    /**
     * 监控缓存状态并打印 默认打开可关闭
     */
    private boolean monitor = true;
    /**
     * 是否全局禁用，禁用后即使有缓存注解也会失效，手动编码加的缓存是无法通过该配置禁用的
     */
    private boolean disabled = false;

}

