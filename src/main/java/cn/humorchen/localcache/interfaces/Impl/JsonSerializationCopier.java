package cn.humorchen.localcache.interfaces.Impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ClassUtil;
import cn.humorchen.localcache.interfaces.ILocalCacheResultCopier;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author  humorchen
 * date: 2024/1/16
 * description:
 **/
@Slf4j
public class JsonSerializationCopier implements ILocalCacheResultCopier {
    public static final JsonSerializationCopier INSTANCE = new JsonSerializationCopier();

    /**
     * 拷贝结果
     *
     * @param result
     * @return
     */
    @Override
    public Object copy(Object result) {
        // 如果是java基本数据类型(使用hutool工具判断)则直接返回，否则使用使用json序列化拷贝结果
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
        // 如果是List则循环返回
        if (result instanceof List) {
            List<Object> list = (List<Object>) result;
            return CollectionUtil.isNotEmpty(list) ? JSONArray.parseArray(JSONObject.toJSONString(list), list.get(0).getClass()) : Collections.emptyList();
        }
        // set集合循环拷贝返回
        if (result instanceof Set) {
            Set<Object> set = (Set<Object>) result;
            return CollectionUtil.isNotEmpty(set) ? Sets.newHashSet(JSONArray.parseArray(JSONObject.toJSONString(set), set.iterator().next().getClass())) : Collections.emptySet();
        }
        // map循环copy返回
        if (result instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) result;
            try {
                Map copy = map.getClass().newInstance();
                map.forEach((key, value) -> copy.put(copy(key), copy(value)));
                return copy;
            } catch (Exception e) {
                log.error("使用 JSON Serialization 拷贝结果失败，result={}", result, e);
                return result;
            }
        }
        try {
            return JSONObject.parseObject(JSONObject.toJSONString(result), result.getClass());
        } catch (Exception e) {
            log.error("使用 JSON Serialization 拷贝结果失败，直接返回原对象，result={}", result, e);
            return result;
        }
    }
}
