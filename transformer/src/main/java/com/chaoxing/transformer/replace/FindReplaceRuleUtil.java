package com.chaoxing.transformer.replace;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class FindReplaceRuleUtil {

    // 单例：使用静态内部类方式实现线程安全的懒加载
    private FindReplaceRuleUtil() {}

    private static class Holder {
        private static final FindReplaceRuleUtil INSTANCE = new FindReplaceRuleUtil();
    }

    public static FindReplaceRuleUtil getInstance() {
        return Holder.INSTANCE;
    }

    // 保存方法替换信息的映射表
    private final Map<String, MethodReplaceInfo> methodReplaceMap = new HashMap<>();

    public synchronized Map<String, MethodReplaceInfo> getMethodReplaceMap() {
        return methodReplaceMap;
    }

    // 可选的便捷方法
    public void put(String key, MethodReplaceInfo info) {
        methodReplaceMap.put(key, info);
    }

    public MethodReplaceInfo get(String key) {
        return methodReplaceMap.get(key);
    }

    public void clear() {
        methodReplaceMap.clear();
    }
}
