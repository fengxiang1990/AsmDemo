package com.fxa.transformer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodReplace {
    String targetClass(); // 被替换方法的类路径
    String targetMethod(); // 被替换方法的名称
    String methodType(); // 方法类型
    Class<?>[] targetParameterTypes() default {};//参数类型表,必须先后顺序一致
}