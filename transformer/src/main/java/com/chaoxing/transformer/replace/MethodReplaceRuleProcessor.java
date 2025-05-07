package com.chaoxing.transformer.replace;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

// 新增Gson依赖
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class MethodReplaceRuleProcessor {
    private final Map<String, MethodReplaceInfo> methodReplaceMap;

    public MethodReplaceRuleProcessor(Map<String, MethodReplaceInfo> methodReplaceMap) {
        this.methodReplaceMap = methodReplaceMap;
    }

    // 新增JSON解析方法
    public void process() {
        try {
            String path = System.getProperty("user.dir") + File.separator + "method_replace_rules.json";
            System.out.println("fxa replace rule path->"+path);
            File file = new File(path);
            if (!file.exists()) {
                throw new RuntimeException("method_replace_rules.json not found at " + file.getAbsolutePath());
            }

            try (InputStream is = new FileInputStream(file)) {
                Type listType = new TypeToken<List<MethodReplaceRuleProcessor.MethodReplaceRule>>(){}.getType();
                List<MethodReplaceRule> rules = new Gson().fromJson(new InputStreamReader(is), listType);
                for (MethodReplaceRule rule : rules) {
                    // 生成参数类型描述符
                    StringBuilder descriptor = new StringBuilder("(");
                    for (String paramType : rule.targetParameterTypes) {
                        String param_Descriptor = getDescriptorFromJavaType(paramType);
                        if(needL(param_Descriptor)){
                            descriptor.append("L");
                        }
                        descriptor.append(param_Descriptor);
                    }
                    descriptor.append(")");
                    descriptor.append(getDescriptorFromJavaType(rule.returnType));

                    // 规范化 targetClass 为内部类名
                    String targetClass = rule.targetClass.replace('.', '/');

                    // 构建 key
                    String key = targetClass + "." + rule.targetMethod + descriptor;
                    System.out.println("fxa Generated key: " + key);

                    // 解析 replacementMethod
                    String[] replacementParts = rule.replacementMethod.split("\\.");
                    String replacementClass = String.join("/",
                        java.util.Arrays.asList(replacementParts).subList(0, replacementParts.length - 1));
                    String replacementName = replacementParts[replacementParts.length - 1];

                    MethodReplaceInfo info = new MethodReplaceInfo(replacementClass, replacementName, rule.methodType);
                    methodReplaceMap.put(key, info);
                    System.out.println("fxa Added to methodReplaceMap: " + key + " -> " + info);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static boolean needL(String param_descriptor){
        if(param_descriptor.equals("V")
            || param_descriptor.equals("I")
            || param_descriptor.equals("B")
            || param_descriptor.equals("C")
            || param_descriptor.equals("S")
            || param_descriptor.equals("J")
            || param_descriptor.equals("F")
            || param_descriptor.equals("D")
            || param_descriptor.equals("Z")){
            return false;
        }
        return true;
    }

    private static String getDescriptorFromJavaType(String javaType) {
        if (javaType == null || javaType.isEmpty()) {
            return "V";
        }
        switch (javaType) {
            case "void": return "V";
            case "int": return "I";
            case "byte": return "B";
            case "char": return "C";
            case "short": return "S";
            case "long": return "J";
            case "float": return "F";
            case "double": return "D";
            case "boolean": return "Z";
            default:
                return javaType+";";
        }
    }


    // JSON规则POJO类
    public static class MethodReplaceRule {
        String targetClass;
        String targetMethod;
        String[] targetParameterTypes;
        String methodType;
        String replacementMethod;
        String returnType;
    }
}


