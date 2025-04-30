package com.chaoxing.transformer.replace;

public class MethodReplaceInfo {
    public final String targetClass;
    public final String targetMethod;
    public final String methodType;

    public MethodReplaceInfo(String targetClass, String targetMethod, String methodType) {
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
        this.methodType = methodType;
    }

    @Override
    public String toString() {
        return "MethodReplaceInfo{" + "targetClass='" + targetClass + '\'' + ", targetMethod='" + targetMethod + '\'' + ", methodType='" + methodType + '\'' + '}';
    }
}
