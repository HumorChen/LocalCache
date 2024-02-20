package cn.humorchen.localcache.interfaces.Impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ClassUtil;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author  humorchen
 * date: 2024/1/15
 * description: 使用spring bean utils拷贝结果
 **/
@Slf4j
public class SpringBeanUtilCopier implements ILocalCacheResultCopier {
    public static final SpringBeanUtilCopier INSTANCE = new SpringBeanUtilCopier();

    /**
     * 拷贝结果
     *
     * @param result
     * @return
     */
    @Override
    public Object copy(Object result) {
        // 如果是java基本数据类型(使用hutool工具判断)则直接返回，否则使用spring bean utils拷贝
        if (result == null) {
            return null;
        }
        // 基础类型
        if (ClassUtil.isBasicType(result.getClass()) || result instanceof String) {
            return result;
        }
        // 枚举
        if (result.getClass().isEnum()) {
            return result;
        }
        // 如果是List循环copy返回
        if (result instanceof List) {
            List<Object> list = (List<Object>) result;
            return list.stream().map(this::copy).collect(Collectors.toList());
        }
        // set集合循环拷贝返回
        if (result instanceof Set) {
            Set<Object> set = (Set<Object>) result;
            return CollectionUtil.isNotEmpty(set) ? set.stream().map(this::copy).collect(Collectors.toSet()) : Collections.emptySet();
        }
        // map循环copy返回
        if (result instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) result;
            try {
                Map copy = map.getClass().newInstance();
                map.forEach((key, value) -> copy.put(copy(key), copy(value)));
                return copy;
            } catch (Exception e) {
                log.error("使用spring bean utils拷贝结果失败，result={}", result, e);
                return result;
            }
        }
        try {
            Object newInstance = result.getClass().newInstance();
            BeanUtils.copyProperties(result, newInstance);
            return newInstance;
        } catch (Exception e) {
            log.error("使用spring bean utils拷贝结果失败，直接返回原对象，result={}", result, e);
            return result;
        }
    }


}
