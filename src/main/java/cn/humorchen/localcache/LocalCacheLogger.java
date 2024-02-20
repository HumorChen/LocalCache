package cn.humorchen.localcache;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

/**
 * @author  humorchen
 * date: 2023/12/27
 * description: 日志
 **/
@Slf4j
public class LocalCacheLogger {
    /**
     * 是否启用日志
     */
    private static boolean enableLog = true;
    /**
     * 单条日志最大长度，超过了则截取前n个字符
     */
    private static Integer oneLogMaxLength;

    /**
     * 设置是否启用日志
     *
     * @param enableLog
     */
    public static void setEnableLog(boolean enableLog) {
        LocalCacheLogger.enableLog = enableLog;
    }

    /**
     * 是否启用日志了
     *
     * @return
     */
    public static boolean isEnableLog() {
        return LocalCacheLogger.enableLog;
    }

    /**
     * 设置单条日志最大长度，超过了则截取前n个字符
     *
     * @param oneLogMaxLength
     * @return
     */
    public static void setOneLogMaxLength(int oneLogMaxLength) {
        LocalCacheLogger.oneLogMaxLength = oneLogMaxLength;
    }

    /**
     * 设置单条日志最大长度，超过了则截取前n个字符
     *
     * @return
     */
    public static int getOneLogMaxLength() {
        return LocalCacheLogger.oneLogMaxLength;
    }

    /**
     * 是否启用日志
     *
     * @param localCache
     * @return
     */
    public static boolean isEnableLog(LocalCache localCache) {
        return isEnableLog() || (localCache != null && localCache.enableLog());
    }

    /**
     * 日志操作变字符串
     *
     * @param content
     * @param args
     * @return
     */
    public static String log2String(String content, Object... args) {
        if (StrUtil.isBlank(content)) {
            return "";
        }
        StringBuilder builder = new StringBuilder(content);
        int len = args != null && args.length > 0 ? args.length : 0;
        int i = 0;
        while (i < len) {
            int index = builder.indexOf("{}");
            if (index > 0) {
                Object arg = args[i];
                String str = ClassUtil.isSimpleValueType(arg.getClass()) ? arg.toString() : JSONObject.toJSONString(arg);
                builder.replace(index, index + 2, str);
            }
            i++;
        }
        return builder.toString();
    }

    /**
     * 截断log
     *
     * @param log
     * @return
     */
    public static String logCutoff(String log) {
        if (LocalCacheLogger.oneLogMaxLength != null && LocalCacheLogger.oneLogMaxLength > 0 && StrUtil.isNotBlank(log) && log.length() > LocalCacheLogger.oneLogMaxLength) {
            return log.substring(0, LocalCacheLogger.oneLogMaxLength) + " (文字超过长度限制" + LocalCacheLogger.oneLogMaxLength + ", " + (log.length() - LocalCacheLogger.oneLogMaxLength) + "字符被截断,原长度" + log.length() + ")";
        }
        return log;
    }


    /**
     * debug日志
     *
     * @param args
     */
    public static void debug(LocalCache localCache, String content, Object... args) {
        if (isEnableLog(localCache)) {
            log.debug(logCutoff(log2String(content, args)));
        }
    }

    /**
     * debug日志
     *
     * @param throwable
     */
    public static void debug(LocalCache localCache, String content, Throwable throwable) {
        if (isEnableLog(localCache)) {
            log.debug(logCutoff(content), throwable);
        }
    }

    /**
     * debug日志
     *
     * @param args
     */
    public static void info(LocalCache localCache, String content, Object... args) {
        if (isEnableLog(localCache)) {
            log.info(logCutoff(log2String(content, args)));
        }
    }

    /**
     * debug日志
     *
     * @param throwable
     */
    public static void info(LocalCache localCache, String content, Throwable throwable) {
        if (isEnableLog(localCache)) {
            log.info(logCutoff(content), throwable);
        }
    }

    /**
     * debug日志
     *
     * @param args
     */
    public static void warn(LocalCache localCache, String content, Object... args) {
        if (isEnableLog(localCache)) {
            log.warn(logCutoff(log2String(content, args)));
        }
    }

    /**
     * debug日志
     *
     * @param throwable
     */
    public static void warn(LocalCache localCache, String content, Throwable throwable) {
        if (isEnableLog(localCache)) {
            log.warn(logCutoff(content), throwable);
        }
    }

    /**
     * debug日志
     *
     * @param args
     */
    public static void error(LocalCache localCache, String content, Object... args) {
        if (isEnableLog(localCache)) {
            log.error(logCutoff(log2String(content, args)));
        }
    }

    /**
     * debug日志
     *
     * @param throwable
     */
    public static void error(LocalCache localCache, String content, Throwable throwable) {
        if (isEnableLog(localCache)) {
            log.error(logCutoff(content), throwable);
        }
    }
}
