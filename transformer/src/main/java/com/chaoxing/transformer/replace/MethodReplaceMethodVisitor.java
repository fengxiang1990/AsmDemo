package com.chaoxing.transformer.replace;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

public class MethodReplaceMethodVisitor extends MethodVisitor {
    private final String className;
    private final Map<String, MethodReplaceInfo> methodReplaceMap;

    public MethodReplaceMethodVisitor(int api, MethodVisitor mv,
                                      String className,
                                      Map<String, MethodReplaceInfo> methodReplaceMap) {
        super(api, mv);
        this.className = className;
        this.methodReplaceMap = methodReplaceMap;
        for(String key : this.methodReplaceMap.keySet()){
            System.out.println("MethodReplaceMethodVisitor fxa key:"+key+" info:"+methodReplaceMap.get(key));
        }

    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name,
                                String descriptor, boolean isInterface) {
        // 跳过工具类自身的调用
        if ("com/chaoxing/transformer/MethodReplaceUtil".equals(owner)) {
            System.out.println("fxa skip MethodReplaceUtil");
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            return;
        }


        if(className.equals("com/example/testlibrary/LibraryTestUtil")){
            System.out.println("methodReplaceMap LibraryTestUtil->"+methodReplaceMap.size()+" methodReplaceMap:"+methodReplaceMap.hashCode());
            for(String key : this.methodReplaceMap.keySet()){
                System.out.println("LibraryTestUtil MethodReplaceMethodVisitor fxa key:"+key+" info:"+methodReplaceMap.get(key));
            }
            String key1 = "androidx/core/app/NotificationCompat$Builder.setGroup(Ljava/lang/String;)Landroidx/core/app/NotificationCompat$Builder;";
            MethodReplaceInfo info1 = methodReplaceMap.get(key1);
            System.out.println("fxa info1->"+info1 +" key:"+key1);
        }else{
            System.out.println("methodReplaceMap other->"+methodReplaceMap.size()+" methodReplaceMap:"+methodReplaceMap.hashCode());
        }

        // 按原始 descriptor 去 lookup
        String key = owner + "." + name + descriptor;
        MethodReplaceInfo info = methodReplaceMap.get(key);
        System.out.println("fxa key->"+key +" info:"+info);
        System.out.println("fxa class name->"+className +" owner:"+owner+" method:"+name+" descriptor:"+descriptor);
        if (info != null) {
            System.out.println("fxa Replacing->"+className);
            System.out.println("Replacing call " + key
                + " in " + className + " -> "
                + info.targetClass + "." + info.targetMethod
                + " [type=" + info.methodType + "]");
            int newOpcode;
            String newDescriptor = descriptor;
            if ("static".equalsIgnoreCase(info.methodType)) {
                newOpcode = Opcodes.INVOKESTATIC;
            } else if ("virtual".equalsIgnoreCase(info.methodType)) {
                // 为 util 方法插入 owner 类型的实例参数
                System.out.println("Replacing with static call to newDescriptor0: " + newDescriptor);
                String ownerType = "L" + owner + ";";
                // 提取原始方法的参数部分
                String originalArgs = descriptor.substring(1, descriptor.indexOf(')'));
                String returnType = descriptor.substring(descriptor.indexOf(')')+1);
                // 构造新的描述符，添加原始对象作为第一个参数
                newDescriptor = "(" + ownerType + originalArgs + ")" + returnType;
                newOpcode = Opcodes.INVOKESTATIC;
                System.out.println("Replacing with static call to MethodReplaceUtil1: " + newDescriptor);
            } else {
                // 默认当静态处理
                newOpcode = Opcodes.INVOKESTATIC;
            }

            super.visitMethodInsn(
                newOpcode,
                info.targetClass,
                info.targetMethod,
                newDescriptor,
                false
            );
        } else {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }
}
