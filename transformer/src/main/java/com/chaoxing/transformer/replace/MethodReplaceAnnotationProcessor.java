package com.chaoxing.transformer.replace;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.Map;

public class MethodReplaceAnnotationProcessor extends ClassVisitor {
    private String className;
    private final Map<String, MethodReplaceInfo> methodReplaceMap;

    public MethodReplaceAnnotationProcessor(ClassVisitor classVisitor, Map<String, MethodReplaceInfo> methodReplaceMap) {
        super(Opcodes.ASM9, classVisitor);
        this.methodReplaceMap = methodReplaceMap;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor originalMv = super.visitMethod(access, name, descriptor, signature, exceptions);

        // Collect information about methods annotated with @MethodReplace
        return new MethodVisitor(Opcodes.ASM9, originalMv) {
            @Override
            public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
                if ("Lcom/fxa/transformer/annotation/MethodReplace;".equals(annotationDescriptor)) {
                    System.out.println("MethodReplacePlugin Found @MethodReplace annotation on " + className + "." + name + descriptor);
                    return new AnnotationVisitor(Opcodes.ASM9, super.visitAnnotation(annotationDescriptor, visible)) {
                        String targetClass;
                        String targetMethod;
                        String methodType;
                        Class<?>[] targetParameterTypes = new Class<?>[0]; // Default to empty array

                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            if ("targetParameterTypes".equals(name)) {
                                return new AnnotationVisitor(Opcodes.ASM9, super.visitArray(name)) {
                                    @Override
                                    public void visit(String arrayElementName, Object value) {
                                        // ASM represents Class<?> values as Type objects in annotations
                                        Type type = (Type) value;
                                        try {
                                            // Convert the Type to a Class<?>
                                            Class<?> clazz = Class.forName(type.getClassName());
                                            // Resize the array and add the class
                                            Class<?>[] newArray = new Class<?>[targetParameterTypes.length + 1];
                                            System.arraycopy(targetParameterTypes, 0, newArray, 0, targetParameterTypes.length);
                                            newArray[targetParameterTypes.length] = clazz;
                                            targetParameterTypes = newArray;
                                        } catch (ClassNotFoundException e) {
                                            System.out.println("Error: Could not load class for targetParameterTypes: " + type.getClassName());
                                            e.printStackTrace();
                                        }
                                    }
                                };
                            }
                            return super.visitArray(name);
                        }


                        @Override
                        public void visit(String annotationName, Object value) {
                            switch (annotationName) {
                                case "targetClass":
                                    targetClass = ((String) value).replace('.', '/');
                                    break;
                                case "targetMethod":
                                    targetMethod = (String) value;
                                    break;
                                case "methodType":
                                    methodType = (String) value;
                                    break;
                            }
                        }

                        @Override
                        public void visitEnd() {
                            if("virtual".equalsIgnoreCase(methodType)){
                                StringBuilder targetDescriptor = new StringBuilder("(");
                                for (Class<?> paramType : targetParameterTypes) {
                                    targetDescriptor.append(Type.getDescriptor(paramType));
                                }
                                targetDescriptor.append(")");
                                String returnType = descriptor.substring(descriptor.lastIndexOf(')') + 1);
                                targetDescriptor.append(returnType);
                                String newDescriptor = targetDescriptor.toString();
                                System.out.println("MethodReplacePlugin fxa targetDescriptor2->" + newDescriptor);
                                String key = targetClass + "." + targetMethod + newDescriptor;
                                MethodReplaceInfo info = new MethodReplaceInfo(className, name, methodType);
                                methodReplaceMap.put(key, info);
                                System.out.println("fxa descriptor->"+descriptor);
                                System.out.println("MethodReplacePlugin 添加了替换1：" + key + " -> " + className + "." + name);
                            }else{
                                String key = targetClass + "." + targetMethod + descriptor;
                                MethodReplaceInfo info = new MethodReplaceInfo(className, name, methodType);
                                System.out.println("fxa MethodReplacePlugin Added replacement: " + key + "-> " + className + "." + name+" info:"+info);
                                methodReplaceMap.put(key,info);
                            }
                        }
                    };
                }
                return super.visitAnnotation(annotationDescriptor, visible);
            }
        };
    }
}